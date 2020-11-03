package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.notification.Notifier;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.driftingsouls.ds2.server.comm.PM.TASK;

@Service
public class PmService {
    @PersistenceContext
    private EntityManager em;

    private final ConfigService configService;
    private final UserValueService userValueService;
    private final TaskManager taskManager;

    public PmService(ConfigService configService, UserValueService userValueService, TaskManager taskManager) {
        this.configService = configService;
        this.userValueService = userValueService;
        this.taskManager = taskManager;
    }

    /**
     * Sendet eine PM von einem Spieler zu einem anderen.
     * @param from Der versendende Spieler
     * @param to Der Spieler, der die PM erhalten soll
     * @param title Der Titel der PM
     * @param txt Der Text
     */
    public Optional<String> send( User from, int to, String title, String txt ) {
        return send( from, to, title, txt, 0);
    }

    /**
     * Sendet eine PM an alle Admins (spezifiziert durch den Konfigurationseintrag <code>ADMIN_PMS_ACCOUT</code>).
     * @param from Der versendende Spieler
     * @param title Der Titel der PM
     * @param txt Der Text
     * @param flags Flags, welche die PM erhalten soll
     */
    public List<String> sendToAdmins(User from, String title, String txt, int flags  ) {
        String[] adminlist = configService.getValue(WellKnownConfigValue.ADMIN_PMS_ACCOUNT).split(",");
        List<String> errors = new ArrayList<>();
        for( String admin : adminlist ) {
            var possibleError = send(from, Integer.parseInt(admin), title, txt, flags);
            possibleError.ifPresent(errors::add);
        }

        return errors;
    }

    /**
     * Sendet eine PM von einem Spieler zu einer Allianz.
     * @param from Der versendende Spieler
     * @param to Die Allianz, welche die PM erhalten soll
     * @param title Der Titel der PM
     * @param txt Der Text
     * @param flags Flags, welche die PM erhalten soll
     */
    public void sendToAlly( User from, Ally to, String title, String txt, int flags ) {
        String msg = "an Allianz "+to.getName()+"\n"+txt;

        if( title.length() > 100 ) {
            title = title.substring(0,100);
        }

        List<User> members = em.createQuery("from User where ally=:ally", User.class)
            .setParameter("ally", to)
            .getResultList();
        for (User member: members)
        {
            PM pm = new PM(from, member, title, msg);
            pm.setFlags(flags);
            em.persist(pm);
            sendNotification(pm);
        }
    }

    /**
     * Sendet eine PM von einem Spieler zu einer Allianz.
     * @param from Der versendende Spieler
     * @param to Die Allianz, welche die PM erhalten soll
     * @param title Der Titel der PM
     * @param txt Der Text
     */
    public void sendToAlly( User from, Ally to, String title, String txt ) {
        sendToAlly( from, to, title, txt, 0);
    }

    /**
     * Sendet eine PM von einem Spieler zu einem anderen Spieler.
     *
     * @param from  Der versendende Spieler
     * @param to    Die ID des Spielers, welche die PM erhalten soll
     * @param title Der Titel der PM
     * @param txt   Der Text
     * @param flags Flags, welche die PM erhalten soll
     * @return <code>null</code> bei erfolgreichem Senden, sonst eine Fehlermeldung
     */
    public Optional<String> send(User from, int to, String title, String txt, int flags) {

        /*
         *  Normale PM
         */

        if (to != TASK) {
            if (title.length() > 100) {
                title = title.substring(0, 100);
            }
            if (txt.length() > 5000) {
                txt = txt.substring(0, 5000);
            }

            User user = em.find(User.class, to);
            if (user != null) {
                PM pm = new PM(from, user, title, txt);
                pm.setFlags(flags);
                em.persist(pm);
                sendNotification(pm);

                for (int forward : userValueService.getUserValues(user, WellKnownUserValue.TBLORDER_PMS_FORWARD)) {
                    if (forward != 0) {
                        send(user, forward, "Fwd: " + title,
                            "[align=center][color=green]- Folgende Nachricht ist soeben eingegangen -[/color][/align]\n" +
                                "[b]Absender:[/b] [userprofile=" + from.getId() + "]" + from.getName() + "[/userprofile] (" + from.getId() + ")\n\n" +
                                txt, flags);
                    }
                }
            } else {
                return Optional.of("Transmission an Spieler " + to + " fehlgeschlagen");
            }
        }
        /*
         * Taskverarbeitung (Spezial-PM)
         */
        else {
            if (txt.equals("handletm")) {
                taskManager.handleTask(title, "pm_yes");
            } else {
                taskManager.handleTask(title, "pm_no");
            }
        }

        return Optional.empty();
    }

    /**
     * Sendet eine Nachricht an alle Spieler (jeden mit ID > 0)
     *
     * @param from Der Absender
     * @param title Der Titel der PM
     * @param msg Der Text der PM
     * @param flags Flags der PM
     */
    public void sendToAll(User from, String title, String msg, int flags) {
        List<User> players = em.createQuery("from User where id>0", User.class).getResultList();
        for(User player: players) {
            PM pm = new PM(from, player, title, msg);
            pm.setFlags(flags);
            em.persist(pm);
            sendNotification(pm);
        }
    }

    private void sendNotification(PM pm) {
        var apiKey = userValueService.getApiKey(pm.getEmpfaenger());
        if(apiKey.isBlank()) {
            return;
        }

        new Notifier(apiKey).sendMessage("DS2: "+pm.getTitle(), "Nachricht von "+pm.getSender().getPlainname()+": \n"+ pm.getInhalt());
    }
}
