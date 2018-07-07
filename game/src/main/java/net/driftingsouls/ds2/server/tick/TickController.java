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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Basisklasse fuer Ticks.
 * @author Christopher Jung
 *
 */
public abstract class TickController implements ApplicationContextAware
{
	private static final Log log = LogFactory.getLog(TickController.class);

	/**
	 * Log-Ziel: Standardausgabe.
	 */
	public static final String STDOUT = "java://STDOUT";

	private long exectime;

	private Map<String,Writer> logTargets;
	private Session db;
	private Context context;
	/**
	 * Erstellt eine neue Instanz.
	 */
	public TickController()
	{
		logTargets = new HashMap<>();
		exectime = System.currentTimeMillis();
		db = HibernateUtil.getSessionFactory().openSession();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		Context context = ContextMap.getContext();
		this.context = new TickContext(db, context.getRequest(), context.getResponse(), applicationContext);
	}

	/**
	 * Beendet den Tick und gibt alle Resourcen wieder frei.
	 */
	public void dispose() {
		this.log("Beende Tick, schliesse Datenbankverbindung.");
		for( String handle : logTargets.keySet() ) {
			try {
				removeLogTarget(handle);
			}
			catch( IOException e ) {
				// EMPTY
			}
		}

		db.close();
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
	 * Startet die Tickausfuehrung.
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

			log("");
			log("Execution-Time: "+(System.currentTimeMillis()-exectime)/1000d+"s");
		}
		catch( Exception e ) {
			log.warn("Tickfehler", e);
			Common.mailThrowable(e, "Tickabbruch "+this.getClass().getSimpleName(), "");
		}
	}

	/**
	 * Loggt einen String.
	 * @param string Der zu loggende String
	 */
	protected void slog(String string) {
		for( String i : logTargets.keySet() ) {
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
	public void log(String string) {
		slog(string+"\n");
	}

	/**
	 * Fuegt ein neues Ziel fuer geloggte Daten hinzu.
	 * @param file Das Ziel, zu dem geloggt werden soll. Das Ziel muss schreibbar sein
	 * @param append Sollen die Daten angehangen werden?
	 *
	 * @throws IOException
	 */
	public void addLogTarget( String file, boolean append ) throws IOException {
		Writer w;
		if( file.equals(STDOUT) ) {
			w = new OutputStreamWriter(System.out);
		}
		else {
			log.info("Fuege Log-Ziel '"+file+"' hinzu");

			File f = new File(file);
			if( !f.exists() ) {
				try {
					f.createNewFile();
				}
				catch( IOException e ) {
					log.error("Kann Log-Ziel '"+file+"' nicht erstellen", e);
					throw e;
				}
			}
			w = new FileWriter(f, append);
		}

		logTargets.put(file, w);
	}

	/**
	 * Entfernt ein Ziel fuer geloggte Daten.
	 * @param handle Die Datei/Das Logziel, zu dem bisher geloggt wurde
	 *
	 * @return true bei erfolgreichem entfernen
	 * @throws IOException
	 */
	public boolean removeLogTarget( String handle ) throws IOException {
		// Auf keinen Fall System.out schiessen!
		if( !STDOUT.equals(handle) ) {
			logTargets.get(handle).close();
		}
		logTargets.remove(handle);

		return true;
	}

	/**
	 * Gibt den aktuellen Context zurueck.
	 * @return der Kontext
	 */
	public Context getContext()
	{
		return context;
	}

	/**
	 * Gibt die Hibernate DB-Session des Kontexts zurueck.
	 * @return die DB-Session
	 */
	public Session getDB()
	{
		return db;
	}

	/**
	 * Gibt die Fehlerliste des Kontexts zurueck.
	 * @return die Fehlerliste
	 */
	public net.driftingsouls.ds2.server.framework.pipeline.Error[] getErrorList() {
		return context.getErrorList();
	}
}
