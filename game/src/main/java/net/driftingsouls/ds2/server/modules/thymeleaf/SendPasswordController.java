package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.authentication.PasswordGenerator;
import net.driftingsouls.ds2.server.framework.authentication.PasswordMailer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SendPasswordController extends StaticController {
    private static final Log log = LogFactory.getLog(SendPasswordController.class);

    private final DSController errorController;
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();
    private final PasswordMailer passwordMailer = new PasswordMailer();

    public SendPasswordController(DSController errorController) {
        super("send_password");
        this.errorController = errorController;
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        var username = request.getParameter("username");
        if (username == null || username.isBlank()) {
            errorController.process(request, response, servletContext, templateEngine);
            return;
        }

        createNewPassword(request, username);

        super.process(request, response, servletContext, templateEngine);
    }

    private void createNewPassword(HttpServletRequest request, String username) {
        var db = ContextMap.getContext().getDB();
        User user = (User) db.createQuery("from User where un = :username")
            .setString("username", username)
            .uniqueResult();
        if (user != null) {
            if (!"".equals(user.getEmail())) {
                String password = passwordGenerator.generate();
                String enc_pw = Common.md5(password);

                user.setPassword(enc_pw);

                passwordMailer.sendNewPasswordMail(username, user.getEmail(), password);

                log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung -> Erfolgreich\n");
            } else {
                log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung -> Keine E-Mailadresse\n");
            }
        } else {
            log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> <" + username + "> Passwortanforderung -> Nutzer nicht gefunden\n");
        }
    }
}
