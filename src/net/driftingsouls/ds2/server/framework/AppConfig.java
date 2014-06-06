package net.driftingsouls.ds2.server.framework;

import org.springframework.context.annotation.*;

@org.springframework.context.annotation.Configuration
@ComponentScan(basePackages = "net.driftingsouls.ds2.server")
@ImportResource( { "/WEB-INF/cfg/spring.xml" } )
public class AppConfig
{
}
