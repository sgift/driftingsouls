package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.DSFilter;
import net.driftingsouls.ds2.server.framework.utils.SpringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class AuthenticationFilter extends DSFilter {
    private AuthenticationManager authManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Lazy initialization when actually needed
        if (authManager == null) {
            authManager = SpringUtils.getBean(AuthenticationManager.class);
        }
        authManager.authenticateCurrentSession();
        chain.doFilter(request, response);
    }
}