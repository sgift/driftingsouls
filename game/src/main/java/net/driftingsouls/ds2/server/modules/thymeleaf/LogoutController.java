package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.utils.SpringUtils;
import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogoutController extends StaticController {
    private AuthenticationManager authenticationManager;

    public LogoutController() {
        super("logout");
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        // Lazy initialization when needed
        if (authenticationManager == null) {
            authenticationManager = SpringUtils.getBean(AuthenticationManager.class);
        }

        authenticationManager.logout();
        super.process(request, response, servletContext, templateEngine);
    }
}
