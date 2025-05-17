package net.driftingsouls.ds2.server.framework;

import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import javax.persistence.EntityManager;

@Configuration
public class ConfigServiceConfiguration {
    @Bean
    @RequestScope
    @Primary
    @Conditional(WebApplicationCondition.class)
    public ConfigService requestScopedConfigService(@Lazy EntityManager em) {
        return new ConfigService(em);
    }

    @Bean
    @Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
    @Conditional(InvertedWebApplicationCondition.class)
    public ConfigService threadScopedConfigService(@Lazy EntityManager em) {
        return new ConfigService(em);
    }

}
