package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.hibernate.Session;
import org.springframework.context.annotation.*;

import javax.persistence.EntityManager;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server")
@ImportResource( { "/WEB-INF/cfg/spring.xml" } )
public class AppConfig
{
	@Bean(destroyMethod = "")
	@Scope("request")
	Context currentContext()
	{
		return ContextMap.getContext();
	}

	@Bean(destroyMethod = "")
	@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
	Session currentSession()
	{
		return ContextMap.getContext().getDB();
	}

	@Bean(destroyMethod = "")
	@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
	EntityManager currentEntityManager()
	{
		return HibernateUtil.getCurrentEntityManager();
	}
}
