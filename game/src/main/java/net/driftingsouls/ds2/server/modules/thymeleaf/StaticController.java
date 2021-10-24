package net.driftingsouls.ds2.server.modules.thymeleaf;


import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A controller for static html pages.
 */
public class StaticController implements DSController {
    private final String template;

    public StaticController(String template) {
        this.template = template;
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        templateEngine.process(template, ctx, response.getWriter());
    }
}
