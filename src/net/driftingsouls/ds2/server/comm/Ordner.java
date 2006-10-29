/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.comm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert einen Ordner im Postfach
 * @author Christoph Peltz
 * @author Christopher Jung
 */
public class Ordner {
	/**
	 * Ein normaler Ordner
	 */
	public static final int FLAG_NORMAL = 0;
	/**
	 * Der Muelleimer
	 */
	public static final int FLAG_TRASH 	= 1;
	
	private SQLResultRow data;
	
	private Ordner(SQLResultRow data) {
		this.data = data;
	}
	
	/**
	 * Gibt den Ordner mit der angegebenen ID des angegebenen Benutzers zurueck.
	 * Sollte kein solcher Ordner existieren, so wird <code>null</code> zurueckgegeben.
	 * 
	 * @param id Die ID des Ordners
	 * @param user_id Die ID des Benutzers
	 * @return Der Ordner
	 */
	public static Ordner getOrdnerByID( int id, int user_id ) {	//findet den Ordner mit der einer bestimmten ID
		if( id != 0 ) {
			Database db = ContextMap.getContext().getDatabase();
			String userstatement = "";
			if( user_id != 0 ) {
				userstatement = " AND playerid="+user_id;
			} 
			
			SQLResultRow data = db.first("SELECT * FROM ordner WHERE id=",id,userstatement);
			if( !data.isEmpty() ) {
				return new Ordner( data );
			}
			return null;
		}
		SQLResultRow row = new SQLResultRow();
		row.put("id", 0);
		row.put("name", "Hauptverzeichniss");
		row.put("flags", 0);
		return new Ordner( row );
	}
	
	/**
	 * Gibt den Papierkorb eines Benutzers zurueck. Jeder Benutzer hat einen Papierkorb...
	 * @param user_id Die ID des Benutzers
	 * @return Der Papierkorb
	 */
	public static Ordner getTrash ( int user_id ) {	//gibt die id des Papierkorbes des Spielers zurueck
		Database db = ContextMap.getContext().getDatabase();
		return new Ordner( db.first("SELECT * FROM ordner WHERE playerid='",user_id,"' AND (flags & ",Ordner.FLAG_TRASH,")") );
	}
	
	public static int deleteOrdnerByID( int ordner_id, int user_id ) {
		Database db = ContextMap.getContext().getDatabase();
		int result = 0;
		if( (result = PM.deleteAllInOrdner( ordner_id, user_id )) != 0 ){
			return result;
		}
		SQLQuery subordner_id = db.query("SELECT id FROM ordner WHERE parent=",ordner_id," AND playerid=",user_id);
		while( subordner_id.next() ){
			if( (result = deleteOrdnerByID( subordner_id.getInt("id"), user_id )) != 0 ){
				subordner_id.free();
				return result;
			}
		}
		subordner_id.free();
		db.update("DELETE FROM ordner WHERE id=",ordner_id);
		return 0;
	}
	
	public static void createNewOrdner( String name, int parent_id, int user_id ) {
		Database db = ContextMap.getContext().getDatabase();
		int trash = Ordner.getTrash( user_id ).getID();
		if( trash != parent_id ) {
			db.update("INSERT INTO ordner SET name='",name,"', parent=",parent_id,", playerid=",user_id);
		}
	}
	
	public static boolean existsOrdnerWithID( int ordner_id, int user_id ) {
		Database db = ContextMap.getContext().getDatabase();
		if( ordner_id != 0 ) {
			return !db.first("SELECT id FROM ordner WHERE id=",ordner_id," AND playerid=",user_id).isEmpty();
		}
		return true;
	}
	
	public static List<Integer> getAllChildIDs ( int parent_id, int user_id ) {
		Database db = ContextMap.getContext().getDatabase();
		
		List<Integer> childs = new ArrayList<Integer>();
		
		SQLQuery child = db.query("SELECT id FROM ordner WHERE parent=",parent_id," AND playerid=",user_id);
		while( child.next() ){
			childs.add(child.getInt("id"));
		}

		child.free();
	
		for( int i=0; i < childs.size(); i++ ){
			childs.addAll( getAllChildIDs( childs.get(i), user_id ) );
		}

		return childs;
	}
	
	public static int countPMInOrdner( int ordner_id, int user_id ) {
		Database db = ContextMap.getContext().getDatabase();
		int trash = getTrash( user_id ).getID();

		int gelesen = 2;
		if( ordner_id == trash ){
			gelesen = 10;
		}
		return db.first("SELECT count(*) count FROM transmissionen WHERE empfaenger='",user_id,"' AND ordner=",ordner_id," AND gelesen<",gelesen).getInt("count");
	}
	
	public static Map<Integer,Integer> countPMInAllOrdner( int parent_id, int user_id ) { 
		Database db = ContextMap.getContext().getDatabase();
		List<Integer> ordners = getAllChildIDs( parent_id, user_id );

		Map<Integer,Integer> result = new HashMap<Integer,Integer>(); 

		for( int i=0; i < ordners.size(); i++ ) {
			result.put(ordners.get(i), countPMInOrdner( ordners.get(i), user_id ));
		}

		for( int i=0; i < ordners.size(); i++ ) {
			SQLQuery child = db.query("SELECT id FROM ordner WHERE parent='",ordners.get(i),"'");
			while( child.next() ){
				result.put(ordners.get(i), result.get(i) + result.get(child.getInt("id")));
			}
			child.free();
		}

		return result;
	}
	
	public int getID() {
		return this.data.getInt("id");
	}

	public String getName() {
		return this.data.getString("name");
	}

	public void setName( String name ) {
		Database db = ContextMap.getContext().getDatabase();
		db.prepare("UPDATE ordner SET name= ? WHERE id= ?").update(name, this.data.getInt("id"));
		this.data.put("name", name);
	}

	public int getFlags() {
		return this.data.getInt("flags");
	}
}
