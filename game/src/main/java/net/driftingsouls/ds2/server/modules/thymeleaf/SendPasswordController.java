package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.ITemplateEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ThreadLocalRandom;

public class SendPasswordController extends StaticController {
    private static final Log log = LogFactory.getLog(SendPasswordController.class);

    private final DSController errorController;

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
                String password = Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
                String enc_pw = Common.md5(password);

                user.setPassword(enc_pw);

                String subject = "Neues Passwort für Drifting Souls 2";

                String message = "Hallo {username},\n" +
                    "Du hast ein neues Passwort angefordert. Dein neues Passwort lautet \"{password}\" und wurde verschlüsselt gespeichert. Wenn es verloren geht, musst Du Dir über die \"Passwort vergessen?\"-Funktion der Login-Seite ein neues erstellen lassen.\n" +
                    "Bitte beachte, dass Dein Passwort nicht an andere Nutzer weiter gegeben werden darf.\n" +
                    "Das Admin-Team wünscht weiterhin einen angenehmen Aufenthalt in Drifting Souls 2\n" +
                    "Gruß Guzman\n" +
                    "Admin\n" +
                    "{date} Serverzeit".replace("{username}", username);
                message = message.replace("{password}", password);
                message = message.replace("{date}", Common.date("H:i j.m.Y"));

                Common.mail(user.getEmail(), subject, message);

                log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung -> Erfolgreich\n");
            } else {
                log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> (" + user.getId() + ") <" + username + "> Passwortanforderung -> Keine E-Mailadresse\n");
            }
        } else {
            log.info(Common.date("j.m.Y H:i:s") + ": <" + request.getRemoteAddr() + "> <" + username + "> Passwortanforderung -> Nutzer nicht gefunden\n");
        }
    }
}
