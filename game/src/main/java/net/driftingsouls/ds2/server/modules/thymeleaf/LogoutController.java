package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.utils.SpringUtils;
import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogoutController extends StaticController {
    private final AuthenticationManager authenticationManager;

    public LogoutController() {
        super("logout");
        this.authenticationManager = SpringUtils.getBean(AuthenticationManager.class);
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        authenticationManager.logout();
        super.process(request, response, servletContext, templateEngine);
    }
}
