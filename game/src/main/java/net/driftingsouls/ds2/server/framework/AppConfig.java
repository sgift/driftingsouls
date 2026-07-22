package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.hibernate.Session;
import org.springframework.context.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import javax.persistence.EntityManager;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server")
@ImportResource( { "/WEB-INF/cfg/spring.xml" } )
public class AppConfig
{
	@Bean(destroyMethod = "")
	@RequestScope
	Context currentContext()
	{
		return ContextMap.getContext();
	}

	@Bean(destroyMethod = "")
	@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	Session currentSession()
	{
		return ContextMap.getContext().getDB();
	}

	@Bean(destroyMethod = "")
	@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	@Primary
	@Conditional(WebApplicationCondition.class)
	public EntityManager requestScopedEntityManager() {
		return HibernateUtil.getCurrentEntityManager();
	}

	/**
	 * Liefert fuer Nicht-Web-Aufrufer (z.B. Ticks) bei jedem Zugriff ueber den Proxy einen frischen
	 * EntityManager. Bewusst {@code prototype} statt eines eigenen "thread"-Scopes: Ticks laufen auf
	 * von Quartz wiederverwendeten Worker-Threads, sodass ein pro-Thread gecachter EntityManager
	 * ueber viele, voneinander unabhaengige Tick-Ausfuehrungen hinweg denselben (irgendwann
	 * geschlossenen) EntityManager liefern wuerde. {@code prototype} + Scoped-Proxy sorgt dafuer,
	 * dass jeder Methodenaufruf ueber den Proxy diese Fabrikmethode neu ausfuehrt.
	 */
	@Bean(destroyMethod = "")
	@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
	@Conditional(InvertedWebApplicationCondition.class)
	public EntityManager threadScopedEntityManager() {
		return HibernateUtil.getCurrentEntityManager();
	}
}
