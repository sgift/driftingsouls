/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.framework;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.tick.TickContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Basisklasse fuer Applikationen von DS.
 * @author Christopher Jung
 *
 */
public abstract class DSApplication {
	private static final Log log = LogFactory.getLog(DSApplication.class) ;
	private Context context;

	private Map<Integer,Writer> logTargets;
	private int handleCounter;
	private final Session db;

	/**
	 * Konstruktor.
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public DSApplication(String[] args) throws Exception {
		log.info("Booting DS...");

		CmdLineRequest request = new CmdLineRequest(args);

		new FileSystemXmlApplicationContext("/"+request.getParameterString("config")+"/spring.xml");

		try {
			new DriftingSouls(log, request.getParameterString("config"), true);
		}
		catch( Exception e ) {
			log.fatal("", e);
			throw new Exception(e);
		}
		db = HibernateUtil.getSessionFactory().openSession();

		SimpleResponse response = new SimpleResponse();

		this.context = new TickContext(db, request, response);

		logTargets = new HashMap<Integer,Writer>();
		handleCounter = 0;

		logTargets.put(-1, new OutputStreamWriter(System.out));
	}

	/**
	 * Gibt den aktuellen Context zurueck.
	 * @return der Kontext
	 */
	public Context getContext() {
		return this.context;
	}

	/**
	 * Gibt eine Datenbankinstanz des Kontexts zurueck.
	 * @return eine Datenbankinstanz
	 * @deprecated Bitte Hibernate verwenden
	 */
	@Deprecated
	public Database getDatabase() {
		return this.context.getDatabase();
	}

	/**
	 * Gibt die Hibernate-Session des Kontexts zurueck.
	 * @return die Hibernate-Session
	 */
	public org.hibernate.Session getDB() {
		return db;
	}

	/**
	 * Fuegt ein neues Ziel fuer geloggte Daten hinzu.
	 * @param file Das Ziel, zu dem geloggt werden soll. Das Ziel muss schreibbar sein
	 * @param append Sollen die Daten angehangen werden?
	 *
	 * @return Handle des Log-Ziels oder -1 (gescheitert)
	 * @throws IOException
	 */
	public int addLogTarget( String file, boolean append ) throws IOException {
		File f = new File(file);
		if( !f.exists() ) {
			f.createNewFile();
		}
		Writer w = new FileWriter(f, append);

		logTargets.put(handleCounter, w);

		handleCounter++;

		return (handleCounter-1);
	}

	/**
	 * Entfernt ein Ziel fuer geloggte Daten.
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
	 * Beendet die Applikation und gibt alle Resourcen wieder frei.<br>
	 * Diese Funktion sollte innerhalb der main() aufgerufen werden, nachdem
	 * das Programm komplett durchgelaufen ist.
	 */
	public void dispose() {
		int[] handles = new int[logTargets.size()];

		int index = 0;
		for( int handle : logTargets.keySet() ) {
			handles[index++] = handle;
		}
		for( int i=0; i < handles.length; i++ ) {
			try {
				removeLogTarget(handles[i]);
			}
			catch( IOException e ) {
				// EMPTY
			}
		}
		System.exit(0);
	}

	/**
	 * Loggt einen String.
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
	 * Loggt eine Zeile. Fuer den Zeilenumbruch wird automatisch gesorgt.
	 * @param string Die zu loggende Zeile
	 */
	protected void log(String string) {
		slog(string+"\n");
	}
}
