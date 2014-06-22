package net.driftingsouls.ds2.server;


import net.driftingsouls.ds2.server.framework.AppConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server",
		excludeFilters = {@ComponentScan.Filter(value = AppConfig.class, type = FilterType.ASSIGNABLE_TYPE)})
public class TestAppConfig
{
}
