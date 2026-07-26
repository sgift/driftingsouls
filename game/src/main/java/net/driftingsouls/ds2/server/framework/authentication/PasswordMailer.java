package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.Common;

/**
 * Sends the mails that carry a newly issued password to its user.
 */
public class PasswordMailer {
    /**
     * Sends the welcome mail a newly registered user receives, containing the password issued to them.
     *
     * @param loginName the name the user registered with
     * @param email the address to send to
     * @param password the issued password in cleartext
     */
    public void sendRegistrationMail(String loginName, String email, String password) {
        String subject = "Anmeldung bei Drifting Souls 2";

        String message = "Hallo {loginName},\n" +
            "Du hast Dich als \"{loginName}\" angemeldet. Dein Passwort lautet \"{password}\" (ohne \\\"\\\"). Im Spiel heißt Du noch Kolonist. Dies sowie das Passwort kannst Du aber unter \"Optionen\" ändern.\n" +
            "\n" +
            "Das Admin-Team wünscht einen angenehmen Aufenthalt in DS2!\n" +
            "Gruß Guzman\n" +
            "Admin\n" +
            "{date} Serverzeit";
        message = message.replace("{loginName}", loginName);
        message = message.replace("{password}", password);
        message = message.replace("{date}", Common.date("H:i j.m.Y"));

        Common.mail(email, subject, message);
    }

    /**
     * Sends the mail a user receives after requesting a new password.
     *
     * @param username the name of the user
     * @param email the address to send to
     * @param password the issued password in cleartext
     */
    public void sendNewPasswordMail(String username, String email, String password) {
        String subject = "Neues Passwort für Drifting Souls 2";

        String message = "Hallo {username},\n" +
            "Du hast ein neues Passwort angefordert. Dein neues Passwort lautet \"{password}\" und wurde verschlüsselt gespeichert. Wenn es verloren geht, musst Du Dir über die \"Passwort vergessen?\"-Funktion der Login-Seite ein neues erstellen lassen.\n" +
            "Bitte beachte, dass Dein Passwort nicht an andere Nutzer weiter gegeben werden darf.\n" +
            "Das Admin-Team wünscht weiterhin einen angenehmen Aufenthalt in Drifting Souls 2\n" +
            "Gruß Guzman\n" +
            "Admin\n" +
            "{date} Serverzeit";
        message = message.replace("{username}", username);
        message = message.replace("{password}", password);
        message = message.replace("{date}", Common.date("H:i j.m.Y"));

        Common.mail(email, subject, message);
    }
}
