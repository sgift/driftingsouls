package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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
	private static final ThreadLocal<EntityManager> CURRENT_ENTITY_MANAGER = new ThreadLocal<EntityManager>() {
		@Override
		protected EntityManager initialValue()
		{
			return entityManagerFactory.createEntityManager();
		}
	};

	public static EntityManager getCurrentEntityManager()
	{
		return CURRENT_ENTITY_MANAGER.get();
	}

	public static void removeCurrentEntityManager()
	{
		CURRENT_ENTITY_MANAGER.remove();
	}

    public static synchronized void init(String configdir, String dbUrl, String dbUser, String dbPassword)
    {
        try
        {
        	org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        	configuration.configure(new File(configdir+"hibernate.xml"));

    		// Configure connection
    		configuration.setProperty("hibernate.connection.url", dbUrl);
    		configuration.setProperty("hibernate.connection.username", dbUser);
    		configuration.setProperty("hibernate.connection.password", dbPassword);

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


			final ServiceRegistry serviceRegistry =  new ServiceRegistryBuilder()
					.applySettings(configuration.getProperties())
					.buildServiceRegistry();

			// Create the SessionFactory from hibernate.xml
			HibernateUtil.configuration = configuration;
//			sessionFactory = configuration.buildSessionFactory(serviceRegistry);

			HibernateUtil.entityManagerFactory = new EntityManagerFactoryImpl(PersistenceUnitTransactionType.RESOURCE_LOCAL, true, null, configuration, serviceRegistry, null);
			sessionFactory = entityManagerFactory.getSessionFactory();
        }
        catch (Throwable ex)
        {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

	public static void writeSchemaUpdateToDisk(Connection con, String targetFile) throws IOException, SQLException
	{
		String[] updateSQL = configuration.generateSchemaUpdateScript(new MySQL5InnoDBDialect(), new DatabaseMetadata(con, new MySQL5InnoDBDialect()));
		try (FileOutputStream writer = new FileOutputStream(new File(targetFile)))
		{
			IOUtils.write(StringUtils.join(updateSQL, ";\n"), writer, "UTF-8");
		}
	}

	public static void writeSchemaToDisk(String targetFile) throws IOException
	{
		String[] dropSQL = configuration.generateDropSchemaScript( new MySQL5InnoDBDialect() );
		String[] createSQL = configuration.generateSchemaCreationScript(new MySQL5InnoDBDialect());
		try (FileOutputStream writer = new FileOutputStream(new File(targetFile)))
		{
			IOUtils.write(StringUtils.join(dropSQL, ";\n"), writer, "UTF-8");
			IOUtils.write("\n\n\n\n", writer, "UTF-8");
			IOUtils.write(StringUtils.join(createSQL, ";\n"), writer, "UTF-8");
		}
	}

	/**
     * Gibt die SessionFactory zurueck.
     * Zu jeder Zeit existiert nur eine.
     *
     * @return Die SessionFactory.
     */
    public synchronized static SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
	public synchronized static Configuration getConfiguration() { return configuration; }
	public synchronized static EntityManagerFactory getEntityManagerFactory() { return entityManagerFactory; }

    private static SessionFactory sessionFactory;
	private static Configuration configuration;
	private static EntityManagerFactoryImpl entityManagerFactory;

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