package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.modules.thymeleaf.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

//Adapted from Thymeleaf example
public class DSApplication {
    private final TemplateEngine templateEngine;

    /**
     * Factories rather than instances: several controllers keep per-request state (the current
     * {@link net.driftingsouls.ds2.server.framework.Context}, the active user) in fields. A single
     * shared instance means concurrent requests overwrite each other's state, so every request gets
     * its own controller - the same request scoping the pipeline controllers already have.
     */
    private final Map<String, Supplier<DSController>> controllerFactoriesByURL;

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

        this.controllerFactoriesByURL = new HashMap<>();


        this.controllerFactoriesByURL.put("/", PortalController::new);
        this.controllerFactoriesByURL.put("/portal", PortalController::new);
        this.controllerFactoriesByURL.put("/agb", () -> new StaticController("agb"));
        this.controllerFactoriesByURL.put("/impressum", () -> new StaticController("impressum"));
        this.controllerFactoriesByURL.put("/password_lost", () -> new StaticController("password_lost"));
        this.controllerFactoriesByURL.put("/send_password", () -> new SendPasswordController(new StaticController("password_lost")));
        this.controllerFactoriesByURL.put("/register", RegisterController::new);
        this.controllerFactoriesByURL.put("/login", () -> new LoginController(new PortalController()));
        this.controllerFactoriesByURL.put("/logout", LogoutController::new);
        this.controllerFactoriesByURL.put("/choff", ChoffController::new);
        this.controllerFactoriesByURL.put("/comnet", ComNetController::new);
        this.controllerFactoriesByURL.put("/base", BaseController::new);
        this.controllerFactoriesByURL.put("/starmap", StarmapController::new);
        this.controllerFactoriesByURL.put("/gamemaster", GameMasterController::new);
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public DSController resolveControllerForRequest(final HttpServletRequest request) {
        final String path = getRequestPath(request);
        Supplier<DSController> factory = this.controllerFactoriesByURL.get(path);
        return factory != null ? factory.get() : null;
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
