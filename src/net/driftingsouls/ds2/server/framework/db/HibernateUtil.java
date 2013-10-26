package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

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
        	org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
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
			SortedSet<String> entityClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(javax.persistence.Entity.class);
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

			if( !"true".equals(Configuration.getSetting("PRODUCTION")) )
			{
				writeSchemaToDisk(configuration);
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

	private static void writeSchemaToDisk(org.hibernate.cfg.Configuration configuration) throws IOException
	{
		String[] dropSQL = configuration.generateDropSchemaScript( new MySQL5InnoDBDialect() );
		String[] createSQL = configuration.generateSchemaCreationScript( new MySQL5InnoDBDialect()  );
		try (FileOutputStream writer = new FileOutputStream(new File(Configuration.getSetting("configdir") + "schema.sql")))
		{
			IOUtils.write(StringUtils.join(dropSQL, "\n"), writer, "UTF-8");
			IOUtils.write("\n\n\n\n", writer, "UTF-8");
			IOUtils.write(StringUtils.join(createSQL, "\n"), writer, "UTF-8");
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
    	SortedMap<String,Integer> counter = new TreeMap<>();
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