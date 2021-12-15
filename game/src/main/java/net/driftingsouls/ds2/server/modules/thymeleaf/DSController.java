package net.driftingsouls.ds2.server.modules.thymeleaf;

import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DSController {
    void process(
        HttpServletRequest request, HttpServletResponse response,
        ServletContext servletContext, ITemplateEngine templateEngine)
        throws Exception;
}
