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

import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.util.Random;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.scripting.entities.Quest;
import net.driftingsouls.ds2.server.scripting.entities.RunningQuest;
import net.driftingsouls.ds2.server.scripting.entities.Script;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;

/**
 * Hilfsfunktionen zur Questverarbeitung.
 * @author Christopher Jung
 *
 */
public class Quests {
	private static final Log log = LogFactory.getLog(Quests.class);

	/**
	 * Ereignis fuer einen Kommunikationsversuch zwischen zwei Schiffen.
	 */
	public static final String EVENT_ONCOMMUNICATE = "1";
	/**
	 * Ereignis bei einem Tick.
	 */
	public static final String EVENT_RUNNING_ONTICK = "2";

	/**
	 * Die fuer in Ereignissen produzierte Ausgabe zu verwendenden URL-Parameter.
	 */
	public static final ThreadLocal<String> currentEventURL = new ThreadLocal<String>() {
		// EMPTY
	};

	/**
	 * Die fuer in Ereignissen produzierte Ausgabe zu verwendenden URL-Basis.
	 */
	public static final ThreadLocal<String> currentEventURLBase = new ThreadLocal<String>() {
		// EMPTY
	};

	/**
	 * Baut eine URL fuer eine Antwort in einem Questscript.
	 * @param answerid Die Antwort-ID
	 * @return Die URL
	 */
	public static String buildQuestURL( String answerid ) {
		return currentEventURLBase.get()+currentEventURL.get()+"&execparameter="+answerid;
	}

	/**
	 * Fuehrt einen Lock-String aus (bzw das dem Lock-String zugeordnete Script, sofern vorhanden).
	 * @param scriptparser Die ScriptEngine, mit dem der Lock ausgefuehrt werden soll
	 * @param lock Der Lock-String
	 * @param user Der User unter dem der Lock ausgefuehrt werden soll
	 */
	public static void executeLock( ScriptEngine scriptparser, String lock, User user ) {
		if( lock == null ) {
			return;
		}

		String[] lockArray = StringUtils.split(lock, ':');
		if( lockArray[0].length() == 0 || Integer.parseInt(lockArray[0]) <= 0 ) {
			return;
		}

		String usequest = lockArray[1];
		int usescript = Integer.parseInt(lockArray[0]);

		if( usescript == 0 ) {
			return;
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();

		String execparameter = "-1";

		RunningQuest runningdata = null;
		if( usequest.charAt(0) != 'r' ) {
			runningdata = (RunningQuest)db.createQuery("from RunningQuest where quest= :quest and user = :user")
				.setInteger("quest", Integer.parseInt(usequest))
				.setEntity("user", user)
				.uniqueResult();
		}
		else {
			int rquestid = Integer.parseInt(usequest.substring(1));
			runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
		}

		if( runningdata == null ) {
			return;
		}

		try {
			Blob execdata = runningdata.getExecData();
			if( (execdata != null) && (execdata.length() > 0) ) {
				scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
			}
		}
		catch( Exception e ) {
			log.warn("Setting Script-ExecData failed: ",e);
			return;
		}

		Script script = (Script)db.get(Script.class, usescript);
		if( script == null ) {
			log.error("Konnte Script '"+usescript+"' nicht finden");
			return;
		}

		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);

		engineBindings.put("USER", Integer.toString(user.getId()));
		if( usequest.length() > 0 ) {
			engineBindings.put("QUEST", "r"+runningdata.getId());
		}
		engineBindings.put("SCRIPT", Integer.toString(usescript));
		engineBindings.put("LOCKEXEC", "1");
		engineBindings.put("_PARAMETERS", execparameter);
		try {
			scriptparser.eval(script.getScript());
		}
		catch( ScriptException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fuehrt einen Ereignis-String aus (bzw das zugehoerige Script).
	 * @param scriptparser Die ScriptEngine, mit dem das Ereignis ausgefuhert werden soll
	 * @param handler Der Ereignis-String
	 * @param user Der Benutzer unter dem das Ereignis ausgefuehrt werden soll
	 * @param execparameter Weitere Parameter zur Ausfuehrung
	 * @param locked <code>true</code>, falls das Schiff gelockt ist
	 * @return <code>true</code>, falls das Ereignis ausgefuert werden konnte
	 */
	public static boolean executeEvent( ScriptEngine scriptparser, String handler, User user, String execparameter, boolean locked ) {
		if( handler == null ) {
			return false;
		}
		String[] handlerArray = StringUtils.split( handler, ';' );

		String usequest = "0";
		int usescript = -1;
		int usechance = -1;
		boolean forcenew = false;
		boolean breakme = false;
		boolean parserest = false;
		for( int i=0; i < handlerArray.length; i++ ) {
			String[] hentry = StringUtils.split(handlerArray[i], ':');

			if( hentry[0].equals(Integer.toString(user.getId())) ) {
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

		org.hibernate.Session db = ContextMap.getContext().getDB();

		if( !usequest.equals("0") ) {
			RunningQuest runningdata = null;

			if( !forcenew && (usequest.charAt(0) != 'r') ) {
				runningdata = (RunningQuest)db.createQuery("from RunningQuest where quest= :quest and user = :user")
					.setInteger("quest", Integer.parseInt(usequest))
					.setEntity("user", user)
					.uniqueResult();
			}
			else if( !forcenew ) {
				int rquestid = Integer.parseInt(usequest.substring(1));
				runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
			}

			if( runningdata != null ) {
				runningID = runningdata.getId();
				try {
					Blob execdata = runningdata.getExecData();
					if( (execdata != null) && (execdata.length() > 0) ) {
						scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
					}
				}
				catch( Exception e ) {
					log.warn("Setting Script-ExecData failed: ",e);
					return false;
				}
			}
			else {
				Quest quest = null;
				if( usequest.charAt(0) == 'r' ) {
					quest = (Quest)db.get(Quest.class, Integer.parseInt(usequest.substring(1)));
				}
				else {
					quest = (Quest)db.get(Quest.class, Integer.parseInt(usequest));
				}
				runningdata = new RunningQuest(quest, user);
				db.save(runningdata);

				runningID = runningdata.getId();
			}
		}

		final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);

		Script script = (Script)db.get(Script.class, usescript);
		if( script == null ) {
			log.error("Konnte Script '"+usescript+"' nicht finden");
			return false;
		}

		if( locked ) {
			engineBindings.put("LOCKEXEC", "1");
		}

		engineBindings.put("USER", Integer.toString(user.getId()));
		if( !usequest.equals("0") ) {
			engineBindings.put("QUEST", "r"+runningID);
		}
		engineBindings.put("SCRIPT", Integer.toString(usescript));
		engineBindings.put("_PARAMETERS", execparameter);
		try {
			scriptparser.eval(script.getScript());
		}
		catch( ScriptException e ) {
			throw new RuntimeException(e);
		}

		usequest = (String)engineBindings.get("QUEST");

		if( (usequest != null) && !usequest.isEmpty() && !usequest.equals("0") ) {
			RunningQuest runningdata = null;
			if( usequest.charAt(0) != 'r' ) {
				runningdata = (RunningQuest)db.createQuery("from RunningQuest where quest= :quest and user = :user")
					.setInteger("quest", Integer.parseInt(usequest))
					.setInteger("user", user.getId())
					.uniqueResult();
			}
			else {
				int rquestid = Integer.parseInt(usequest.substring(1));
				runningdata = (RunningQuest)db.get(RunningQuest.class, rquestid);
			}
			if( runningdata == null ) {
				log.error("Das Quest "+usequest+" hat keine Daten");
			}
			else {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ScriptParserContext.toStream(scriptparser.getContext(), out);
					runningdata.setExecData(Hibernate.createBlob(out.toByteArray()));
				}
				catch( Exception e ) {
					log.warn("Writing back Script-ExecData failed: ",e);
					return false;
				}
			}
		}
		return true;
	}
}
