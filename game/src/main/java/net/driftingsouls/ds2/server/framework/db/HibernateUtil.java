package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
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
	private static final Logger LOG = LogManager.getLogger(HibernateUtil.class);

	private static SessionFactory sessionFactory;
	private static Configuration configuration;
	private static EntityManagerFactoryImpl entityManagerFactory;
	private static ServiceRegistry serviceRegistry;

	private static final ThreadLocal<EntityManager> CURRENT_ENTITY_MANAGER = new ThreadLocal<>() {
        @Override
        protected EntityManager initialValue() {
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

    public static synchronized void createFactories()
    {
        try
        {
			serviceRegistry =  new ServiceRegistryBuilder()
					.applySettings(configuration.getProperties())
					.buildServiceRegistry();

			HibernateUtil.entityManagerFactory = new EntityManagerFactoryImpl(PersistenceUnitTransactionType.RESOURCE_LOCAL, true, null, configuration, serviceRegistry, null);
			sessionFactory = entityManagerFactory.getSessionFactory();
        }
        catch (Throwable ex)
        {
            // Make sure you log the exception, as it might be swallowed
            LOG.fatal("Initial SessionFactory creation failed." + ex.getMessage(), ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

	public static synchronized void initConfiguration(String hibernateConfigFile, String dbUrl, String dbUser, String dbPassword)
	{
		if( configuration != null )
		{
			return;
		}
		Configuration configuration = new Configuration();
		configuration.configure(new File(hibernateConfigFile));
		configuration.setNamingStrategy(new DsNamingStrategy());

		// Configure connection
		configuration.setProperty("hibernate.connection.url", dbUrl);
		configuration.setProperty("hibernate.connection.username", dbUser);
		configuration.setProperty("hibernate.connection.password", dbPassword);

		// Add ds-specific utility functions
		configuration.addSqlFunction("pow", new StandardSQLFunction("pow", DoubleType.INSTANCE));
		configuration.addSqlFunction("floor", new StandardSQLFunction("floor", LongType.INSTANCE));
		configuration.addSqlFunction("bit_and", new SQLFunctionTemplate(IntegerType.INSTANCE, "?1 & ?2"));
		configuration.addSqlFunction("bit_or", new SQLFunctionTemplate(IntegerType.INSTANCE, "?1 | ?2"));

		//Find all annotated classes and add to configuration
		SortedSet<Class<?>> entityClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(javax.persistence.Entity.class);
		entityClasses.forEach(configuration::addAnnotatedClass);

		// Create the SessionFactory from hibernate.xml
		HibernateUtil.configuration = configuration;
	}

	public static void writeSchemaUpdateToDisk(Connection con, String targetFile) throws IOException, SQLException
	{
		String[] updateSQL = configuration.generateSchemaUpdateScript(new MySQL5InnoDBDialect(), new DatabaseMetadata(con, new MySQL5InnoDBDialect()));
		try (FileOutputStream writer = new FileOutputStream(new File(targetFile)))
		{
			IOUtils.write(String.join(";\n", updateSQL), writer, "UTF-8");
		}
		if( updateSQL.length == 0 )
		{
			LOG.info("Datenbank ist auf dem aktuellsten Stand");
		}
		else
		{
			LOG.info("Updatescript mit "+updateSQL.length+" Statements erzeugt");
		}
	}

	public static void writeSchemaToDisk(String targetFile) throws IOException
	{
		String[] dropSQL = configuration.generateDropSchemaScript( new MySQL5InnoDBDialect() );
		String[] createSQL = configuration.generateSchemaCreationScript(new MySQL5InnoDBDialect());
		try (FileOutputStream writer = new FileOutputStream(new File(targetFile)))
		{
			IOUtils.write(String.join(";\n", dropSQL), writer, "UTF-8");
			IOUtils.write("\n\n\n\n", writer, "UTF-8");
			IOUtils.write(String.join(";\n", createSQL), writer, "UTF-8");
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

	public static void shutdown()
	{
		if( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
		if( sessionFactory != null ) {
			sessionFactory.close();
		}
		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
		entityManagerFactory = null;
		sessionFactory = null;
		serviceRegistry = null;
	}

	private static class DsNamingStrategy extends EJB3NamingStrategy
	{
		@Override
		public String classToTableName(String className)
		{
			return addUnderscores(super.classToTableName(className));
		}

		@Override
		public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName)
		{
			return addUnderscores(ownerEntityTable) + "_" + addUnderscores(associatedEntityTable != null ?
					associatedEntityTable :
					StringHelper.unqualify(propertyName));
		}

		protected String addUnderscores(String name)
		{
			StringBuilder buf = new StringBuilder(name);
			for (int i = 1; i < buf.length() - 1; i++)
			{
				if (Character.isLowerCase(buf.charAt(i - 1)) &&
						Character.isUpperCase(buf.charAt(i)) &&
						Character.isLowerCase(buf.charAt(i + 1))
						)
				{
					buf.insert(i++, '_');
				}
			}
			return buf.toString().toLowerCase();
		}
	}
}