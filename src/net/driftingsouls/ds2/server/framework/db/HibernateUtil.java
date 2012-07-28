package net.driftingsouls.ds2.server.framework.db;

import java.io.File;
import java.net.URL;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import net.driftingsouls.ds2.server.framework.Configuration;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 * Eine Hilfsklasse, um die Hibernate SessionFactory zu initialisieren.
 * Die Factory wird als Singleton direkt beim Start intialisiert und danach
 * wiederverwendet
 *
 * @see HibernateSessionRequestFilter
 * @author Drifting-Souls Team
 */
public class HibernateUtil
{
    static
    {
        try
        {
        	AnnotationConfiguration configuration = new AnnotationConfiguration();
        	configuration.configure(new File(Configuration.getSetting("configdir")+"hibernate.xml"));

    		// Configure connection
    		configuration.setProperty("hibernate.connection.url", Configuration.getSetting("db_url"));
    		configuration.setProperty("hibernate.connection.username", Configuration.getSetting("db_user"));
    		configuration.setProperty("hibernate.connection.password", Configuration.getSetting("db_password"));

    		// Add ds-specific utility functions
    		configuration.addSqlFunction("pow", new StandardSQLFunction("pow", DoubleType.INSTANCE));
    		configuration.addSqlFunction("floor", new StandardSQLFunction("floor", LongType.INSTANCE));
    		configuration.addSqlFunction("ncp", new NullCompFunction());
    		configuration.addSqlFunction("bit_and", new SQLFunctionTemplate(IntegerType.INSTANCE, "?1 & ?2"));
    		configuration.addSqlFunction("bit_or", new SQLFunctionTemplate(IntegerType.INSTANCE, "?1 | ?2"));

            //Find all annotated classes and add to configuration
			URL[] urls = ClasspathUrlFinder.findResourceBases("META-INF/ds.marker");
			AnnotationDB db = new AnnotationDB();
			db.scanArchives(urls);
			SortedSet<String> entityClasses = new TreeSet<String>(db.getAnnotationIndex().get(javax.persistence.Entity.class.getName()));
			for( String cls : entityClasses )
			{
				try
				{
					Class<?> clsObject = Class.forName(cls);
					configuration.addAnnotatedClass(clsObject);
				}
				catch( ClassNotFoundException e )
				{
					// Not all classes are always available - ignore
				}
			}

			// Create the SessionFactory from hibernate.xml
			sessionFactory = configuration.buildSessionFactory();
        }
        catch (Throwable ex)
        {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Gibt die SessionFactory zurueck.
     * Zu jeder Zeit existiert nur eine.
     *
     * @return Die SessionFactory.
     */
    public static SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }

    private static final SessionFactory sessionFactory;

    /**
     * Gibt den momentanen Inhalt der Session, aufgelistet nach Entitynamen/Collectionrolle und der zugehoerigen Anzahl
     * an Eintraegen in der Session zurueck.
     * @param db Die Session zu der die Daten ermittelt werden sollen
     * @return Die Daten, wobei der Schluessel der Entityname/die Collectionrolle ist
     */
    public static SortedMap<String,Integer> getSessionContentStatistics(Session db)
    {
    	SortedMap<String,Integer> counter = new TreeMap<String,Integer>();
		for( Object obj : db.getStatistics().getEntityKeys() )
		{
			EntityKey key = (EntityKey)obj;
			if( !counter.containsKey(key.getEntityName()) )
			{
				counter.put(key.getEntityName(), 1);
			}
			else
			{
				counter.put(key.getEntityName(), counter.get(key.getEntityName())+1);
			}
		}
		for( Object obj : db.getStatistics().getCollectionKeys() )
		{
			CollectionKey key = (CollectionKey)obj;
			if( !counter.containsKey(key.getRole()) )
			{
				counter.put(key.getRole(), 1);
			}
			else
			{
				counter.put(key.getRole(), counter.get(key.getRole())+1);
			}
		}
		return counter;
    }
}