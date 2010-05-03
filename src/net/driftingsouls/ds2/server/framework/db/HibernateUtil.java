package net.driftingsouls.ds2.server.framework.db;

import java.io.File;
import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;

import net.driftingsouls.ds2.server.framework.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
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
    		configuration.addSqlFunction("pow", new StandardSQLFunction("pow", Hibernate.DOUBLE));
    		configuration.addSqlFunction("floor", new StandardSQLFunction("floor", Hibernate.LONG));
    		configuration.addSqlFunction("ncp", new NullCompFunction());
    		configuration.addSqlFunction("bit_and", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 & ?2"));
    		configuration.addSqlFunction("bit_or", new SQLFunctionTemplate(Hibernate.INTEGER, "?1 | ?2"));
            
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
    
    private static Log log = LogFactory.getLog(HibernateUtil.class);
    private static final SessionFactory sessionFactory;
}