package net.driftingsouls.ds2.server;


import net.driftingsouls.ds2.server.framework.AppConfig;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.SimpleThreadScope;

import javax.persistence.EntityManager;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server",
		excludeFilters = {@ComponentScan.Filter(value = AppConfig.class, type = FilterType.ASSIGNABLE_TYPE)})
public class TestAppConfig
{
	/**
	 * Stellt den EntityManager bereit, den Beans ausserhalb des Web-Kontexts (z.B. TaskHandler,
	 * TickController) per Konstruktor-Injektion erwarten. Spiegelt
	 * {@link AppConfig#threadScopedEntityManager()}, da Tests wie ein Hintergrund-Thread (Tick)
	 * und nicht wie ein Web-Request laufen. Ohne diesen Bean kann Spring TaskHandler mit
	 * {@code EntityManager}-Konstruktorparameter in Tests nicht instanziieren. Bewusst
	 * {@code prototype} statt eines eigenen "thread"-Scopes - siehe
	 * {@link AppConfig#threadScopedEntityManager()}.
	 */
	@Bean(destroyMethod = "")
	@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public EntityManager threadScopedEntityManager()
	{
		return HibernateUtil.getCurrentEntityManager();
	}

	/**
	 * In Produktion registriert der Servlet-Container (via {@code RequestContextListener}/
	 * {@code DispatcherServlet}) den "request"-Scope automatisch. Tests laufen ausserhalb eines
	 * echten Web-Requests, sodass Beans mit {@code @RequestScope} (z.B. {@code AllianzService})
	 * sonst mit "No Scope registered for scope name 'request'" fehlschlagen. {@link SimpleThreadScope}
	 * ist Springs eigene, fuer genau diesen Testfall gedachte Ersatzimplementierung.
	 */
	@Bean
	public static CustomScopeConfigurer testRequestScopeConfigurer()
	{
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("request", new SimpleThreadScope());
		return configurer;
	}
}
