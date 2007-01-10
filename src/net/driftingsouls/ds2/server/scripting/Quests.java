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
package net.driftingsouls.ds2.server.scripting;

import java.sql.Blob;
import java.util.Random;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

import org.apache.commons.lang.StringUtils;

/**
 * Hilfsfunktionen zur Questverarbeitung
 * @author Christopher Jung
 *
 */
public class Quests implements Loggable {
	/**
	 * Ereignis fuer einen Kommunikationsversuch zwischen zwei Schiffen
	 */
	public static final String EVENT_ONCOMMUNICATE = "1";
	/**
	 * Ereignis bei einem Tick
	 */
	public static final String EVENT_RUNNING_ONTICK = "2";
	/**
	 * Ereignis bei Einflug eines (beliebigen) Schiffes in einen bestimmten Sektor
	 */
	public static final String EVENT_ONENTER = "3";
	/**
	 * Ereignis beim Bewegen eines bestimmten Schiffes. Wird vor dem Bewegen ausgeloesst und kann
	 * die Flugrichtung und die Anzahl der zu fliegenden Sektoren veraendern
	 */
	public static final String EVENT_ONMOVE = "4";
	
	/**
	 * Die fuer in Ereignissen produzierte Ausgabe zu verwendenden URL-Parameter
	 */
	public static final ThreadLocal<String> currentEventURL = new ThreadLocal<String>() {
		// EMPTY
	};
	
	/**
	 * Die fuer in Ereignissen produzierte Ausgabe zu verwendenden URL-Basis
	 */
	public static final ThreadLocal<String> currentEventURLBase = new ThreadLocal<String>() {
		// EMPTY
	};
	
	/**
	 * Baut eine URL fuer eine Antwort in einem Questscript
	 * @param answerid Die Antwort-ID
	 * @return Die URL
	 */
	public static String buildQuestURL( String answerid ) {
		return currentEventURLBase.get()+currentEventURL.get()+"&execparameter="+answerid;
	}
	
	/**
	 * Fuehrt einen Lock-String aus (bzw das dem Lock-String zugeordnete Script, sofern vorhanden)
	 * @param scriptparser Der ScriptParser, mit dem der Lock ausgefuehrt werden soll
	 * @param lock Der Lock-String
	 * @param user Der User unter dem der Lock ausgefuehrt werden soll
	 */
	public static void executeLock( ScriptParser scriptparser, String lock, User user ) {
		String[] lockArray = StringUtils.split(lock, ':');
		if( lockArray[0].length() == 0 || Integer.parseInt(lockArray[0]) <= 0 ) {
			return;
		}

		String usequest = lockArray[1];
		int usescript = Integer.parseInt(lockArray[0]);
		
		if( usescript == 0 ) {
			return;
		}

		Database db = ContextMap.getContext().getDatabase();
	
		String execparameter = "-1";	
	
		SQLQuery runningdata = null;
		if( usequest.charAt(0) != 'r' ) {
			runningdata = db.query("SELECT id,execdata FROM quests_running WHERE questid='",usequest,"' AND userid='",user.getID(),"'");
		}
		else {
			String rquestid = usequest.substring(1);
			runningdata = db.query("SELECT id,execdata FROM quests_running WHERE id='",rquestid,"'");	
		}
		
		if( !runningdata.next() ) {
			return;
		}
		
		try {
			Blob execdata = runningdata.getBlob("execdata");
			if( (execdata != null) && (execdata.length() > 0) ) { 
				scriptparser.setExecutionData(execdata.getBinaryStream() );
			}
		}
		catch( Exception e ) {
			runningdata.free();
			LOG.warn("Setting Script-ExecData failed: ",e);
			return;
		}

		SQLResultRow script = db.first("SELECT script FROM scripts WHERE id='",usescript,"'");
		if( script.isEmpty() ) {
			LOG.error("Konnte Script '"+usescript+"' nicht finden");
			return;
		}
		
		scriptparser.setRegister("USER", Integer.toString(user.getID()));
		if( usequest.length() > 0 ) {
			scriptparser.setRegister("QUEST", "r"+runningdata.getInt("id"));
		}
		scriptparser.setRegister("SCRIPT", Integer.toString(usescript));	
		scriptparser.setRegister("LOCKEXEC", "1");	
		scriptparser.executeScript(db, script.getString("script"), execparameter);
		
		runningdata.free();
	}
	
	/**
	 * Fuehrt einen Ereignis-String aus (bzw das zugehoerige Script)
	 * @param scriptparser Der ScriptParser, mit dem das Ereignis ausgefuhert werden soll
	 * @param handler Der Ereignis-String
	 * @param userid Die Benutzer-ID unter der das Ereignis ausgefuehrt werden soll
	 * @param execparameter Weitere Parameter zur Ausfuehrung
	 * @return <code>true</code>, falls das Ereignis ausgefuert werden konnte
	 */
	public static boolean executeEvent( ScriptParser scriptparser, String handler, int userid, String execparameter ) {
		String[] handlerArray = StringUtils.split( handler, ';' );
		
		String usequest = "0";
		int usescript = -1;
		int usechance = -1; 
		boolean forcenew = false;
		boolean breakme = false;
		boolean parserest = false;
		for( int i=0; i < handlerArray.length; i++ ) {
			String[] hentry = StringUtils.split(handlerArray[i], ':');

			if( hentry[0].equals(Integer.toString(userid)) ) {
				usechance = -1;
				forcenew = false;
				
				usescript = Integer.parseInt(hentry[1]);
				if( hentry.length > 2 ) {
					usequest = hentry[2];
				}
				breakme = true;
				parserest = true;
			}	
			else if( hentry[0].equals("*") ) {
				usescript = Integer.parseInt(hentry[1]);
				if( hentry.length > 2 ) {
					usequest = hentry[2];
				}
				parserest = true;
			}
			
			// Optionale Parameter verarbeiten
			if( parserest && hentry.length > 3 ) {
				for( int j=3; j<hentry.length; j++ ) {
					if( hentry[j].charAt(0) == '%' ) {
						usechance = Integer.parseInt(hentry[j].substring(1));
					}	
					else if( hentry[j].charAt(0) == 'n' ) {
						forcenew = true;
					}
				}	
				parserest = false;
			}
			
			if( breakme ) {
				break;	
			}
		}
		
		// %-Parameter verarbeiten
		if( (usechance > -1) && (execparameter.length() > 0) && Integer.parseInt(execparameter) != 0 ) {
			int random = new Random().nextInt(100)+1;
			//echo $random."/".$usechance."<br />\n";
			if( random > usechance ) {
				return false;
			}
		}
		
		if( usescript == -1 ) {
			return false;
		}
	
		int runningID = 0;
		
		Database db = ContextMap.getContext().getDatabase();
		
		if( !usequest.equals("0") ) {
			SQLQuery runningdata = null;
			
			if( !forcenew && (usequest.charAt(0) != 'r') ) {
				runningdata = db.query("SELECT id,execdata FROM quests_running WHERE questid='",usequest,"' AND userid='",userid,"'");
			}
			else if( !forcenew ) {
				String rquestid = usequest.substring(1);
				runningdata = db.query("SELECT id,execdata FROM quests_running WHERE id='",rquestid,"'");	
			}
								
			if( runningdata.next() ) {
				runningID = runningdata.getInt("id");
				try {
					Blob execdata = runningdata.getBlob("execdata");
					if( (execdata != null) && (execdata.length() > 0) ) { 
						scriptparser.setExecutionData(execdata.getBinaryStream() );
					}
				}
				catch( Exception e ) {
					runningdata.free();
					LOG.warn("Setting Script-ExecData failed: ",e);
					return false;
				}
			}
			else {
				db.update("INSERT INTO quests_running (questid,userid) VALUES ('",usequest,"','",userid,"')");
				
				runningID = db.insertID();
			}
			runningdata.free();
		}
		
		SQLResultRow script = db.first("SELECT script FROM scripts WHERE id='",usescript,"'");
		if( script.isEmpty() ) {
			LOG.error("Konnte Script '"+usescript+"' nicht finden");
			return false;
		}
		scriptparser.setRegister("USER", Integer.toString(userid));
		if( !usequest.equals("0") ) {
			scriptparser.setRegister("QUEST", "r"+runningID);
		}
		scriptparser.setRegister("SCRIPT", Integer.toString(usescript));	
		scriptparser.executeScript(db, script.getString("script"), execparameter);
		
		usequest = scriptparser.getRegister("QUEST");
							
		if( (usequest.length() > 0) && !usequest.equals("0") ) {
			SQLQuery runningdata = null;
			if( usequest.charAt(0) != 'r' ) {
				runningdata = db.query("SELECT execdata FROM quests_running WHERE questid='",usequest,"' AND userid='",userid,"'");
			}
			else {
				String rquestid = usequest.substring(1);
				runningdata = db.query("SELECT execdata FROM quests_running WHERE id='",rquestid,"'");	
			}
			try {
				Blob execdata = runningdata.getBlob("execdata");
				if( (execdata != null) && (execdata.length() > 0) ) { 
					scriptparser.writeExecutionData( execdata.setBinaryStream(1) );
				}
			}
			catch( Exception e ) {
				runningdata.free();
				LOG.warn("Writing back Script-ExecData failed: ",e);
				return false;
			}
			
			runningdata.free();
		}
		return true;
	}
}
