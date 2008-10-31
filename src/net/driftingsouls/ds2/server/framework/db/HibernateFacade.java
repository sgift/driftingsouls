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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import net.driftingsouls.ds2.server.framework.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.EntityKey;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.stat.Statistics;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 * Hilfsklasse zur Initalisierung von Hibernate
 * @author Christopher Jung
 *
 */
public class HibernateFacade {
	private static final Log log = LogFactory.getLog(HibernateFacade.class);
	private static final Object LOCK = new Object();
	
	private static SessionFactory sessionFactory;
	private static org.hibernate.cfg.AnnotationConfiguration conf;
	private static StatisticsService statisticsService = null;
	
	static {
		conf = new AnnotationConfiguration();
		conf.configure(new File(Configuration.getSetting("configdir")+"hibernate.xml"));
		
		// Datenbankverbindung eintragen
		conf.setProperty("hibernate.connection.url", Configuration.getSetting("db_url"));
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
			URL[] urls = ClasspathUrlFinder.findResourceBases("META-INF/ds.marker");
			AnnotationDB db = new AnnotationDB();
			db.scanArchives(urls);
			SortedSet<String> entityClasses = new TreeSet<String>(db.getAnnotationIndex().get(javax.persistence.Entity.class.getName()));
			for( String cls : entityClasses ) {
				try {
					Class<?> clsObject = Class.forName(cls);
					conf.addAnnotatedClass(clsObject);
				}
				catch( ClassNotFoundException e ) {
					// Es sind nicht immer alle Klassen verfuegbar - daher ignorieren
				}
			}
		}
		catch( Exception e ) {
			log.fatal("HibernateFacade init fehlgeschlagen", e);
			throw new ExceptionInInitializerError(e);
		}
		
		createStatisticsMBean();
	}

	private static void createStatisticsMBean() {
		statisticsService = new StatisticsService();
		statisticsService.setSessionFactory(getSessionFactory());
		
		MBeanServer server = getServer();
		try {
			ObjectName name = new ObjectName("org.hibernate:Type=Statistics");
			server.registerMBean(statisticsService, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static MBeanServer getServer() {
		ArrayList<MBeanServer> mbservers = MBeanServerFactory.findMBeanServer(null);

		if( mbservers.size() > 0 ) {
			log.info("Found MBean server");
			return mbservers.get(0);
		}
		return MBeanServerFactory.createMBeanServer();
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
	
	/**
	 * Entfernt alle Objekte der Klasse(n) oder einer Unterklasse dieser Klasse(n)
	 * aus der angegebenen Session
	 * @param db Die Hibernate Session
	 * @param cls Die Klasse(n)
	 */
	@SuppressWarnings("unchecked")
	public static void evictAll(Session db, Class ... cls) {
		Set entityKeys = new HashSet(db.getStatistics().getEntityKeys());
		for( Iterator iter=entityKeys.iterator(); iter.hasNext(); ) {
			EntityKey key = (EntityKey)iter.next();
			
			Object obj = db.get(key.getEntityName(), key.getIdentifier());
			for( Class aCls : cls ) {
				if( aCls.isInstance(obj) ) {
					db.evict(obj);
				}
			}
		}
	}
}
