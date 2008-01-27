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
package net.driftingsouls.ds2.server.framework.db;

import java.io.File;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.stat.Statistics;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Hilfsklasse zur Initalisierung von Hibernate
 * @author Christopher Jung
 *
 */
public class HibernateFacade implements Loggable {
	private static final Object LOCK = new Object();
	
	private static SessionFactory sessionFactory;
	private static org.hibernate.cfg.AnnotationConfiguration conf; 
	
	static {
		conf = new AnnotationConfiguration();
		conf.configure(new File(Configuration.getSetting("configdir")+"hibernate.xml"));
		
		// Datenbankverbindung eintragen
		conf.setProperty("hibernate.connection.url", "jdbc:mysql://"+Configuration.getSetting("db_server")+"/"+Configuration.getSetting("db_database"));
		conf.setProperty("hibernate.connection.username", Configuration.getSetting("db_user"));
		conf.setProperty("hibernate.connection.password", Configuration.getSetting("db_password"));
		
		// Einige Funktionen hinzufuegen
		conf.addSqlFunction("pow", new StandardSQLFunction("pow", Hibernate.DOUBLE));
		conf.addSqlFunction("floor", new StandardSQLFunction("floor", Hibernate.LONG));
		conf.addSqlFunction("ncp", new NullCompFunction());
		conf.addSqlFunction("bit_and", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 & ?2"));
		conf.addSqlFunction("bit_or", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 | ?2"));
		
		// Mappings lesen
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"hibernatemappings.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "mappings/mapping");
			for( int i=0; i < nodes.getLength(); i++ ) {
				String cls = nodes.item(i).getAttributes().getNamedItem("class").getNodeValue();
				try {
					Class clsObject = Class.forName(cls);
					conf.addAnnotatedClass(clsObject);
				}
				catch( ClassNotFoundException e ) {
					// Es sind nicht immer alle Klassen verfuegbar - daher ignorieren
				}
			}
		}
		catch( Exception e ) {
			LOG.fatal("HibernateFacade init fehlgeschlagen", e);
			throw new ExceptionInInitializerError(e);
		}
	}
	
	/**
	 * Gibt die Instanz der SessionFactory zurueck
	 * @return Die SessionFactory von Hibernate
	 */
	public static SessionFactory getSessionFactory() {
		synchronized(LOCK) {
			if( sessionFactory == null ) {
				sessionFactory = conf.buildSessionFactory();
				sessionFactory.getStatistics().setStatisticsEnabled(true);
			}
			return sessionFactory;
		}
	}
	
	/**
	 * Gibt die Hibernate-Statistikdaten zurueck
	 * @return Die Hibernate-Statistikdaten
	 */
	public static Statistics getStatistics() {
		return getSessionFactory().getStatistics();
	}
	
	/**
	 * Oeffnet eine neue Hibernate-Session
	 * @return eine neue Hibernate-Session
	 */
	public static Session openSession() {
		return getSessionFactory().openSession();
	}
	
	/**
	 * Gibt alle belegten Resourcen frei. Es ist Aufgabe der Anwendung dafür zu Sorgen,
	 * dass keine offenen Sessions mehr vorhanden sind 
	 *
	 */
	public static void free() {
		synchronized(LOCK) {
			sessionFactory.close();
			sessionFactory = null;
		}
	}
}
