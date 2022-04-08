package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.modules.thymeleaf.DSController;
import net.driftingsouls.ds2.server.modules.thymeleaf.LoginController;
import net.driftingsouls.ds2.server.modules.thymeleaf.LogoutController;
import net.driftingsouls.ds2.server.modules.thymeleaf.PortalController;
import net.driftingsouls.ds2.server.modules.thymeleaf.RegisterController;
import net.driftingsouls.ds2.server.modules.thymeleaf.SendPasswordController;
import net.driftingsouls.ds2.server.modules.thymeleaf.ComNetController;
import net.driftingsouls.ds2.server.modules.thymeleaf.BaseController;
import net.driftingsouls.ds2.server.modules.thymeleaf.StaticController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//Adapted from Thymeleaf example
public class DSApplication {
    private final TemplateEngine templateEngine;
    private final Map<String, DSController> controllersByURL;

    public DSApplication(final ServletContext servletContext) {
        ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);

        // HTML is the default mode, but we set it anyway for better understanding of code
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // This will convert "home" to "/WEB-INF/templates/home.html"
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        // Template cache TTL=1h. If not set, entries would be cached until expelled
        templateResolver.setCacheTTLMs(TimeUnit.HOURS.toMillis(1L));

        // Cache is set to true by default. Set to false if you want templates to
        // be automatically updated when modified.
        templateResolver.setCacheable(true);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);

        var portalController = new PortalController();
        var passwordLostController = new StaticController("password_lost");

        this.controllersByURL = new HashMap<>();


        this.controllersByURL.put("/", portalController);
        this.controllersByURL.put("/portal", portalController);
        this.controllersByURL.put("/agb", new StaticController("agb"));
        this.controllersByURL.put("/impressum", new StaticController("impressum"));
        this.controllersByURL.put("/password_lost", passwordLostController);
        this.controllersByURL.put("/send_password", new SendPasswordController(passwordLostController));
        this.controllersByURL.put("/register", new RegisterController());
        this.controllersByURL.put("/login", new LoginController(portalController));
        this.controllersByURL.put("/logout", new LogoutController());
        this.controllersByURL.put("/comnet", new ComNetController());
        this.controllersByURL.put("/base", new BaseController());
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public DSController resolveControllerForRequest(final HttpServletRequest request) {
        final String path = getRequestPath(request);
        return this.controllersByURL.get(path);
    }

    private static String getRequestPath(final HttpServletRequest request) {

        String requestURI = request.getRequestURI();
        final String contextPath = request.getContextPath();

        final int fragmentIndex = requestURI.indexOf(';');
        if (fragmentIndex != -1) {
            requestURI = requestURI.substring(0, fragmentIndex);
        }

        if (requestURI.startsWith(contextPath)) {
            return requestURI.substring(contextPath.length());
        }
        return requestURI;
    }
}
