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
package net.driftingsouls.ds2.server.tick;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;

/**
 * Basisklasse fuer Ticks
 * @author Christopher Jung
 *
 */
public abstract class TickController {
	/**
	 * Log-Ziel: Standardausgabe
	 */
	public static final String STDOUT = "java://STDOUT";
			
	private long exectime;
	private Map<Integer,Writer> logTargets;
	private int handleCounter;
	private Context context;
	
	/**
	 * Erstellt eine neue Instanz
	 */
	public TickController() {
		logTargets = new HashMap<Integer,Writer>();
		handleCounter = 0;
		this.context = ContextMap.getContext();
	
		exectime = System.currentTimeMillis();
	}
	
	/**
	 * Beendet den Tick und gibt alle Resourcen wieder frei
	 */
	public void dispose() {
		for( int handle : logTargets.keySet() ) {
			try {
				removeLogTarget(handle);
			}
			catch( IOException e ) {
				// EMPTY
			}
		}
	}
	
	/**
	 * Hier koennen Vorbereitungen getroffen werden.
	 * Sollten Fehler auftreten wird der Tick hiernach abgebrochen.
	 */
	protected abstract void prepare();
	
	/**
	 * Der eigendliche Tick...
	 */
	protected abstract void tick();
	
	/**
	 * Blockiert den angegebenen Spieler bzw alle Spieler. Unter
	 * blockieren ist hierbei die Fehlermeldung "Im Moment werden
	 * einige Tick-Berechnungen durchgefuehrt" zu verstehen.
	 * 
	 * @param userid Der zu sperrende User oder 0 (default) fuer alle User
	 */
	protected void block(int userid) {
		if( userid != 0 ) {
			getContext().getDB()
				.createQuery("update User set blocked=1 where id=?")
				.setInteger(0, userid)
				.executeUpdate();
		}
		else {
			getContext().getDB()
				.createQuery("update User set blocked=1")
				.executeUpdate();
		}
	}
	
	/**
	 * Entsperrt den angegebenen Spieler bzw alle Spieler.
	 * @see #block(int)
	 * 
	 * @param userid Der zu entsperrende User oder 0 (default) fuer alle User
	 */
	protected void unblock( int userid ) {
		if( userid != 0 ) {
			getContext().getDB()
				.createQuery("update User set blocked=0 where id=?")
				.setInteger(0, userid)
				.executeUpdate();
		}
		else {
			getContext().getDB()
				.createQuery("update User set blocked=0")
				.executeUpdate();
		}
	}
	
	/**
	 * Startet die Tickausfuehrung
	 */
	public void execute() {
		try {
			log("-----------------"+Common.date("d.m.Y H:i:s")+"-------------------");
			
			prepare();
			if( getErrorList().length == 0 ) {
				tick();
			}
			
			if( getErrorList().length > 0 ) {
				log("");
				log("Fehlerliste:");
	
				for( net.driftingsouls.ds2.server.framework.pipeline.Error error : getErrorList() ) {
					slog("* ");
					log(error.getDescription());
				}
			}
			
			if( context.getResponse().getContent().length() > 0 ) {
				log("\n-------------Weitere Ausgaben---------------\n");
				log(context.getResponse().getContent().toString());
			}
			context.getResponse().setContent("");
			
			log("");
			log("QCount: "+getDatabase().getQCount());
			
			log("Execution-Time: "+(System.currentTimeMillis()-exectime)+"s");
			
			getContext().commit();
		}
		catch( Exception e ) {
			Common.mailThrowable(e, "Tickabbruch "+this.getClass().getSimpleName(), "");
			
			e.printStackTrace();
			getDB().getTransaction().rollback();
		}
	}
	
	/**
	 * Loggt einen String
	 * @param string Der zu loggende String
	 */
	protected void slog(String string) {
		for( int i : logTargets.keySet() ) {
			try {
				logTargets.get(i).write(string);
				logTargets.get(i).flush();
			}
			catch( IOException e ) {
				System.err.println("Fehler beim Schreiben - schliesse Handler: "+e);
				try{
					removeLogTarget(i);
				}
				catch(IOException f) {
					// EMPTY
				}
			}
		}
	}
	
	/**
	 * Loggt eine Zeile. Fuer den Zeilenumbruch wird automatisch gesorgt
	 * @param string Die zu loggende Zeile
	 */
	protected void log(String string) {
		slog(string+"\n");
	}
	
	/**
	 * Fuegt ein neues Ziel fuer geloggte Daten hinzu
	 * @param file Das Ziel, zu dem geloggt werden soll. Das Ziel muss schreibbar sein
	 * @param append Sollen die Daten angehangen werden?
	 * 
	 * @return Handle des Log-Ziels oder -1 (gescheitert)
	 * @throws IOException 
	 */
	public int addLogTarget( String file, boolean append ) throws IOException {
		Writer w = null;
		if( file.equals(STDOUT) ) {
			w = new OutputStreamWriter(System.out);
		}
		else {
			File f = new File(file);
			if( !f.exists() ) {
				f.createNewFile();
			}
			w = new FileWriter(f, append);
		}
	
		if( w == null ) {
			return -1;
		}
		
		logTargets.put(handleCounter, w);
		
		handleCounter++;
		
		return (handleCounter-1);
	}
	
	/**
	 * Entfernt ein Ziel fuer geloggte Daten
	 * @param handle Das Handle des Log-Ziels
	 * 
	 * @return true bei erfolgreichem entfernen
	 * @throws IOException 
	 */
	public boolean removeLogTarget( int handle ) throws IOException {		
		logTargets.get(handle).close();
		logTargets.remove(handle);

		return true;
	}
	
	/**
	 * Gibt den aktuellen Context zurueck
	 * @return der Kontext
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * Gibt eine Datenbankinstanz des Kontexts zurueck
	 * @return eine Datenbankinstanz
	 */
	public Database getDatabase() {
		return context.getDatabase();
	}
	
	/**
	 * Gibt die Hibernate DB-Session des Kontexts zurueck
	 * @return die DB-Session
	 */
	public org.hibernate.Session getDB() {
		return context.getDB();
	}
	
	/**
	 * Gibt die Fehlerliste des Kontexts zurueck
	 * @return die Fehlerliste
	 */
	public net.driftingsouls.ds2.server.framework.pipeline.Error[] getErrorList() {
		return context.getErrorList();
	}
}