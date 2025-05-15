package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.framework.authentication.AccountDisabledException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationException;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.authentication.LoginDisabledException;
import net.driftingsouls.ds2.server.framework.authentication.TickInProgressException;
import net.driftingsouls.ds2.server.framework.authentication.WrongPasswordException;
import net.driftingsouls.ds2.server.framework.utils.SpringUtils;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginController extends StaticController {
    private final DSController errorController;
    private AuthenticationManager authenticationManager;

    public LoginController(DSController errorController) {
        super("portal");
        this.errorController = errorController;
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        // Lazy initialization when needed
        if (authenticationManager == null) {
            authenticationManager = SpringUtils.getBean(AuthenticationManager.class);
        }

        var username = request.getParameter("username");
        var password = request.getParameter("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            errorController.process(request, response, servletContext, templateEngine);
            return;
        }

        try {
            authenticationManager.login(username, password);
            response.sendRedirect(response.encodeRedirectURL("./ds?module=main&action=default"));
            return;
        } catch (LoginDisabledException e) {
            //TODO: Login disabled message variable
        } catch (AccountInVacationModeException e) {
            //TODO: Still in vac message variable, e.getDauer to write how long
        } catch (WrongPasswordException e) {
            //TODO: Password error message variable
        } catch (AccountDisabledException e) {
            //TODO: Account disabled message variable
        } catch (TickInProgressException e) {
            //TODO: Tick in progress message variable
        } catch (AuthenticationException e) {
            // EMPTY
        }

        errorController.process(request, response, servletContext, templateEngine);
    }
}
