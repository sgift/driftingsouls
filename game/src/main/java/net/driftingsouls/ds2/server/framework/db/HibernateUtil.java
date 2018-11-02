package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.DoubleType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.util.SortedSet;

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
	private static EntityManagerFactory entityManagerFactory;
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
			serviceRegistry =  new StandardServiceRegistryBuilder()
					.applySettings(configuration.getProperties())
					.build();


			//HibernateUtil.entityManagerFactory = new EntityManagerFactoryImpl(PersistenceUnitTransactionType.RESOURCE_LOCAL, true, null, configuration, serviceRegistry, null);

			//sessionFactory = entityManagerFactory.getSessionFactory();
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
}