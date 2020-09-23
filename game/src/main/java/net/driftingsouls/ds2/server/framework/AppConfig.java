package net.driftingsouls.ds2.server.framework;

import org.hibernate.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.persistence.EntityManager;

@Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server")
@EnableScheduling
public class AppConfig
{
	@Bean(destroyMethod = "")
	@Scope("request")
	Context currentContext()
	{
		return ContextMap.getContext();
	}

	@Bean(destroyMethod = "")
	@Scope("request")
	Session currentSession()
	{
		return ContextMap.getContext().getDB();
	}

	@Bean(destroyMethod = "")
	@Scope("request")
	EntityManager currentEntityManager()
	{
		return ContextMap.getContext().getEM();
	}
}
