package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.SectorTemplateManager;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.TransientUnitCargo;
import org.hibernate.Session;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.persistence.EntityManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

//TODO: Whole class should be autowired, don't get your own config service
public class RegisterController implements DSController {
    private final EntityManager db = ContextMap.getContext().getEM();
    private final ConfigService configService = new ConfigService(db);

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        var disableRegister = configService.getValue(WellKnownConfigValue.DISABLE_REGISTER);
        if (disableRegister != null && !disableRegister.isBlank()) {
            templateEngine.process("no_register", ctx, response.getWriter());
            return;
        }

        populateRaceOptions(ctx);
        populateSystemOptions(ctx);

        var action = request.getParameter("action");
        if (action == null || action.isBlank()) {
            templateEngine.process("register", ctx, response.getWriter());
            return;
        }

        var loginName = request.getParameter("login_name");
        var email = request.getParameter("email");
        var acceptAgb = Boolean.parseBoolean(request.getParameter("accept_agb"));

        int race = -1;
        int system = -1;
        try {
            race = Integer.parseInt(request.getParameter("race"));
            system = Integer.parseInt(request.getParameter("system"));
        } catch (NumberFormatException ex) {
            ctx.setVariable("errors", List.of("Rasse und System müssen gesetzt sein."));
        }

        var errors = checkPrerequisites(loginName, email, acceptAgb);
        if (!errors.isEmpty()) {
            ctx.setVariable("errors", errors);
            templateEngine.process("register", ctx, response.getWriter());
            return;
        }

        errors = register(loginName, email, race, system);
        if (!errors.isEmpty()) {
            ctx.setVariable("errors", errors);
            templateEngine.process("register", ctx, response.getWriter());
            return;
        }

        templateEngine.process("registered", ctx, response.getWriter());
    }

    private void populateRaceOptions(WebContext ctx) {
        List<Rasse> playableRaces = db.createQuery("from rasse where playable=true", Rasse.class).getResultList();
        List<RegisterRace> registerRaces = playableRaces.stream()
            .map(race -> new RegisterRace(race.getId(), race.getName(), race.getDescription()))
            .collect(Collectors.toList());

        ctx.setVariable("races", registerRaces);
    }

    private void populateSystemOptions(WebContext ctx) {
        var startLocation = getStartLocation();
        List<StarSystem> systems = db.createQuery("from StarSystem", StarSystem.class).getResultList();
        List<RegisterSystem> registerSystems = systems.stream()
            .filter(sys -> sys.getOrderLocations().length > 0)
            .filter(sys -> startLocation.minSysDistance.containsKey(sys.getID()))
            .map(sys -> new RegisterSystem(sys.getID(), sys.getName(), sys.getDescription()))
            .collect(Collectors.toList());

        ctx.setVariable("systems", registerSystems);
    }

    private List<String> checkPrerequisites(String loginName, String email, boolean acceptAgb) {
        var errors = new ArrayList<String>();
        if (loginName == null || loginName.isBlank()) {
            errors.add("Der Login Name ist ein Pflichtfeld.");
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            errors.add("Eine gültige E-Mail Adresse ist notwendig zur Registrierung.");
        }

        if (!acceptAgb) {
            errors.add("Die AGB müssen akzeptiert werden zur Registrierung");
        }

        return errors;
    }

    private List<String> register(String loginName, String email, int raceId, int systemId) {
        var db = ContextMap.getContext().getDB();
        var errors = new ArrayList<String>();

        User loginNameUser = (User) db.createQuery("from User where un = :username")
            .setString("username", loginName)
            .setMaxResults(1)
            .uniqueResult();
        User emailUser = (User) db.createQuery("from User where email = :email")
            .setString("email", email)
            .setMaxResults(1)
            .uniqueResult();

        if (loginNameUser != null) {
            errors.add("Dieser Login wird bereits genutzt.");
        }

        if (emailUser != null) {
            errors.add("Diese E-Mail Adresse wird bereits genutzt.");
        }

        if (!Rassen.get().rasse(raceId).isPlayable()) {
            errors.add("Die Rasse existiert nicht.");
        }

        StarSystem system = (StarSystem) db.get(StarSystem.class, systemId);
        if (system == null || system.getOrderLocations().length == 0) {
            errors.add("Das gewählte System existiert nicht.");
        }

        if (!errors.isEmpty()) {
            return errors;
        }


        String password = Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
        String enc_pw = Common.md5(password);

        int maxid = (Integer) db.createQuery("SELECT max(id) FROM User").iterate().next();
        int newId = maxid + 1;

        int ticks = ContextMap.getContext().get(ContextCommon.class).getTick();

        String history = "Kolonistenlizenz erworben am " + Common.getIngameTime(ticks) + " [" + Common.date("d.m.Y H:i:s") + " Uhr]";

        User newUser = new User(loginName, enc_pw, raceId, history, new Cargo(), email);

        // Startgeld festlegen
        newUser.setKonto(BigInteger.valueOf(50000));

        // Schiffe erstellen
        RegisterController.StartLocations locations = getStartLocation();
        Location[] orderlocs = system.getOrderLocations();
        Location orderloc = orderlocs[locations.minSysDistance.get(system.getID()).orderLocationID];

        Base base = (Base) db.createQuery("from Base where klasse.id=1 and owner.id=0 and system=:sys order by sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y)) ")
            .setInteger("sys", system.getID())
            .setInteger("x", orderloc.getX())
            .setInteger("y", orderloc.getY())
            .setMaxResults(1)
            .uniqueResult();

        Nebel nebel = (Nebel) db.createQuery("from Nebel where loc.system=:sys and type<3 order by sqrt((:x-loc.x)*(:x-loc.x)+(:y-loc.y)*(:y-loc.y))*(mod(type+1,3)+1)*3")
            .setInteger("sys", system.getID())
            .setInteger("x", base.getX())
            .setInteger("y", base.getY())
            .setMaxResults(1)
            .uniqueResult();

        createBase(db, newUser, base);
        positionShips(newId, raceId, base, nebel);
        sendWelcomePm(newId);
        sendWelcomeMail(loginName, email, password);


        Common.copyFile(Configuration.getAbsolutePath() + "data/logos/user/0.gif",
            Configuration.getAbsolutePath() + "data/logos/user/" + newId + ".gif");

        return errors;
    }

    private void sendWelcomeMail(String loginName, String email, String password) {
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

        Common.mail(email, "Anmeldung bei Drifting Souls 2", message);
    }

    private void createBase(Session db, User newUser, Base base) {
        String[] baselayoutStr = configService.getValue(WellKnownConfigValue.REGISTER_BASELAYOUT).split(",");
        Integer[] activebuildings = new Integer[baselayoutStr.length];
        Integer[] baselayout = new Integer[baselayoutStr.length];
        int bewohner = 0;
        int arbeiter = 0;

        for (int i = 0; i < baselayoutStr.length; i++) {
            baselayout[i] = Integer.parseInt(baselayoutStr[i]);

            if (baselayout[i] != 0) {
                activebuildings[i] = 1;
                Building building = Building.getBuilding(baselayout[i]);
                bewohner += building.getBewohner();
                arbeiter += building.getArbeiter();
            } else {
                activebuildings[i] = 0;
            }
        }

        // Alte Gebaeude entfernen
        Integer[] bebauung = base.getBebauung();
        for (Integer aBebauung : bebauung) {
            if (aBebauung == 0) {
                continue;
            }

            Building building = Building.getBuilding(aBebauung);
            building.cleanup(ContextMap.getContext(), base, aBebauung);
        }

        BaseType basetype = (BaseType) db.get(BaseType.class, 1);

        base.setEnergy(base.getMaxEnergy());
        base.setOwner(newUser);
        base.setBebauung(baselayout);
        base.setActive(activebuildings);
        base.setArbeiter(arbeiter);
        base.setBewohner(bewohner);
        base.setWidth(basetype.getWidth());
        base.setHeight(basetype.getHeight());
        base.setMaxCargo(basetype.getCargo());
        base.setCargo(new Cargo(Cargo.Type.AUTO, configService.getValue(WellKnownConfigValue.REGISTER_BASECARGO)));
        base.setCore(null);
        base.setUnits(new TransientUnitCargo());
        base.setCoreActive(false);
        base.setAutoGTUActs(new ArrayList<>());

        for (Offizier offi : Offizier.getOffiziereByDest(base)) {
            offi.setOwner(base.getOwner());
        }

        for (Integer aBaselayout : baselayout) {
            if (aBaselayout > 0) {
                Building building = Building.getBuilding(aBaselayout);
                building.build(base, aBaselayout);
            }
        }
    }

    private void positionShips(int newId, int raceId, Base base, Nebel nebel) {
        if (raceId == 1) {
            SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER", base.getLocation(), newId);
            SectorTemplateManager.getInstance().useTemplate(db, "ORDER_TERRANER_TANKER", nebel.getLocation(), newId);
        } else {
            SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER", base.getLocation(), newId);
            SectorTemplateManager.getInstance().useTemplate(db, "ORDER_VASUDANER_TANKER", nebel.getLocation(), newId);
        }
    }

    private void sendWelcomePm(int newid) {
        User source = db.find(User.class, configService.getValue(WellKnownConfigValue.REGISTER_PM_SENDER));
        PM.send(source, newid, "Willkommen bei Drifting Souls 2",
            "[font=arial]Herzlich willkommen bei Drifting Souls 2!\n" +
                "Diese PM wird automatisch an alle neuen Spieler versandt, um\n" +
                "ihnen Hilfsquellen für den nicht immer einfachen Einstieg zu\n" +
                "nennen.\n" +
                " Falls Probleme auftreten sollten, gibt es:\n" +
                "- das [url=https://drifting-souls.fandom.com/de/wiki/Drifting_Souls_Wiki]DriftingSoulsWiki[/url]\n" +
                "- das [url=http://ds.rnd-it.de]Forum[/url]\n" +
                "- einen [url=https://discord.gg/cpxxAGy]Discord-Chat[/url] und\n" +
                "- die Möglichkeit via Nachricht/PM an die ID -16 Fragen zu stellen.\n" +
                "\n" +
                "\n" +
                "Viel Spaß bei DS2 wünschen Dir die Admins[/font]", db);
    }

    private RegisterController.StartLocations getStartLocation() {
        int systemID = 0;
        int orderLocationID = 0;
        int mindistance = 99999;
        HashMap<Integer, RegisterController.StartLocation> minsysdistance = new HashMap<>();

        List<StarSystem> systems = db.createQuery("from StarSystem order by id asc", StarSystem.class).getResultList();
        for (StarSystem system: systems) {
            Location[] locations = system.getOrderLocations();

            for (int i = 0; i < locations.length; i++) {
                int dist = 0;
                int count = 0;
                var distiter = db.createQuery("SELECT sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y)) FROM Base WHERE owner.id = 0 AND system = :system AND klasse.id = 1 ORDER BY sqrt((:x-x)*(:x-x)+(:y-y)*(:y-y))", Double.class)
                        .setParameter("x", locations[i].getX())
                        .setParameter("y", locations[i].getY())
                        .setParameter("system", system.getID())
                        .setMaxResults(15)
                        .getResultList();

                for (Double distance : distiter) {
                    dist += distance;
                    count++;
                }

                if (count < 15) {
                    continue;
                }

                if (!minsysdistance.containsKey(system.getID()) || (minsysdistance.get(system.getID()).distance > dist)) {
                    minsysdistance.put(system.getID(), new RegisterController.StartLocation(i, dist));

                    if (mindistance > dist) {
                        mindistance = dist;
                        systemID = system.getID();
                        orderLocationID = i;
                    }
                }
            }
        }
        return new RegisterController.StartLocations(systemID, orderLocationID, minsysdistance);
    }

    private static class StartLocations {
        final int systemID;
        final int orderLocationID;
        final HashMap<Integer, RegisterController.StartLocation> minSysDistance;

        StartLocations(int systemID, int orderLocationID, HashMap<Integer, RegisterController.StartLocation> minSysDistance) {
            this.systemID = systemID;
            this.orderLocationID = orderLocationID;
            this.minSysDistance = minSysDistance;
        }
    }

    private static class StartLocation {
        final int orderLocationID;
        final int distance;

        StartLocation(int orderLocationID, int distance) {
            this.orderLocationID = orderLocationID;
            this.distance = distance;
        }
    }

    private static class RegisterSystem {
        public final int id;
        public final String name;
        public final String description;

        private RegisterSystem(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }

    private static class RegisterRace {
        public final int id;
        public final String name;
        public final String description;

        private RegisterRace(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }
}
