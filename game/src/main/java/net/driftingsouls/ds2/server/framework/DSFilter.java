package net.driftingsouls.ds2.server.framework;

import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * A base class for filters in ds with common methods.
 */
public abstract class DSFilter extends GenericFilterBean implements Filter
{
    protected boolean isStaticRequest(ServletRequest servletRequest)
    {
        //This is not really a good idea, but a usable hack as long as DS does not have separate pathes for static
        //and dynamic content
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        String url = req.getRequestURI();
        return url.contains("/data/");
    }
}
