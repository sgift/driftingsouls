package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server")
@EnableScheduling
@EnableTransactionManagement
public class AppConfig
{
	@Bean
	@Scope(value = WebApplicationContext.SCOPE_SESSION,
	proxyMode = ScopedProxyMode.TARGET_CLASS)
	JavaSession session() {
		return new JavaSession();
	}

	@Bean(destroyMethod = "")
	@Scope("request")
	Context currentContext()
	{
		return ContextMap.getContext();
	}

	@Bean
	public LocalSessionFactoryBean sessionFactory() {
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
		sessionFactory.setDataSource(dataSource());
		sessionFactory.setPackagesToScan("net.driftingsouls.ds2.server");
		sessionFactory.setHibernateProperties(hibernateProperties());

		return sessionFactory;
	}

	@Bean
	public DataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(net.driftingsouls.ds2.server.framework.Configuration.getDbUrl());
		dataSource.setUsername(net.driftingsouls.ds2.server.framework.Configuration.getDbUser());
		dataSource.setPassword(net.driftingsouls.ds2.server.framework.Configuration.getDbPassword());

		return dataSource;
	}

	@Bean
	public PlatformTransactionManager hibernateTransactionManager() {
		HibernateTransactionManager transactionManager
			= new HibernateTransactionManager();
		transactionManager.setSessionFactory(sessionFactory().getObject());
		return transactionManager;
	}

	private Properties hibernateProperties() {
		Properties hibernateProperties = new Properties();
		hibernateProperties.setProperty(
			"hibernate.dialect.storage_engine", "innodb");

		return hibernateProperties;
	}
}
