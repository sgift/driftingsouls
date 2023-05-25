package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.repositories.StarsystemRepository;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.KeineTicksperre;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;

import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@KeineTicksperre
public class GameMasterController implements DSController {
    private Context context;

    private enum Action{
      TICK,
      START,
      END,
      DEFAULT
    }

    /**
     * Erzeugt die GameMastersseite (/gamemaster).
     * URL-Parameter:
     * action: <code>null</code> oder "tick"
     */
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        context = ContextMap.getContext();
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        User user = (User) context.getActiveUser();
        if(! user.hasFlag(UserFlag.GAMEMASTER)) {
          Error error = new Error("Du hast keine Berechtigung f&uuml;r diese Seite.","./ds?module=ueber&amp;action=default");
          ctx.setVariable("error",error);
          templateEngine.process("gamemaster", ctx, response.getWriter());
          return;
        }

        Action action = Action.DEFAULT;
        try{
         action = Action.valueOf(request.getParameter("action").toUpperCase());
        } catch(Exception ex){}

        switch(action){
          case TICK:
            tickAction(ctx, request, user);
            break;
          case START:
            startAction(ctx, request, user);
            break;
          case END:
            endAction(ctx, request, user);
            break;
          default:
            defaultAction(ctx, request, user);
            break;
        }

        templateEngine.process("gamemaster", ctx, response.getWriter());
    }


    /**
     * Startet den Kampagnentick
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     */
    private void tickAction(WebContext ctx, HttpServletRequest request, User user) throws ClassNotFoundException {

      Integer tick = Integer.valueOf(new ConfigService().get(WellKnownConfigValue.TICK).getValue());
      if(tick == 1){
        Error error = new Error("Es l&auml;uft bereits ein Tick.", "./gamemaster");
        ctx.setVariable("error",error);
        defaultAction(ctx, request, user);
        return;
      }

        String[] systems = request.getParameterValues("systemcheckbox");

        new AdminCommands().executeCommand("tick campaign run "+String.join(";", systems));
        ctx.setVariable("out", "Kampagnentick f&uuml;r Systeme "+String.join(", ",systems)+" gestartet");
        defaultAction(ctx, request, user);

    }


    /**
     * Startet die Kampagne fuer die ausgewaehlten Spieler
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     */
    private void startAction(WebContext ctx, HttpServletRequest request, User user) throws ClassNotFoundException {

      String query = "select u from User u ";
      org.hibernate.Session db = context.getDB();
      List<User> userlist = Common.cast(db.createQuery(query).list());
      for(User u: userlist){
        if(u.isCampaignParticipant()){
          Error error = new Error("Es wurde bereits eine Kampagne gestartet", "./gamemaster");
          ctx.setVariable("error",error);
          defaultAction(ctx, request, user);
          return;
        }
      }

      String[] users = request.getParameterValues("usercheckbox");
      if(users.length == 0){
        Error error = new Error("Ohne Spieler keine Kampagne.", "./gamemaster");
        ctx.setVariable("error",error);
        ctx.setVariable("link","./gamemaster");
        defaultAction(ctx, request, user);
        return;
      }

      Set<Integer> selectedUsers = new HashSet<>();
      for(String u : users) {
          selectedUsers.add(Integer.valueOf(u));
      }
      query = "select u from User u where u.id in (:ids)";
      userlist = Common.cast(db.createQuery(query).setParameterList("ids", selectedUsers).list());
      for(User u: userlist){
        u.setCampaignParticipant(true);
      }
      ctx.setVariable("out", "Kampagne fuuml;r User "+String.join(", ",users)+" gestartet");
      defaultAction(ctx, request, user);

    }

    /**
     * Beendet die Kampagne fuer die ausgewaehlten Spieler
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     */
    private void endAction(WebContext ctx, HttpServletRequest request, User user) throws ClassNotFoundException {


      //Iterate over all Users and set campaign_participant = false
      String query = "select u from User u ";
      org.hibernate.Session db = context.getDB();
      List<User> userlist = Common.cast(db.createQuery(query).list());
      for(User u: userlist){
        u.setCampaignParticipant(false);
      }
      ctx.setVariable("out", "Kampagne beendet. Teilnehmerstatus zur&uuml;ckgesetzt.");
      defaultAction(ctx, request, user);
    }

    /**
     * Zeigt die GameMasterSeite an
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param user der User, der die Seite aufruft
     */
    private void defaultAction(WebContext ctx, HttpServletRequest request, User user){

      String query = "";
      if( (user == null) || !context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) ) {
        query = "select u from User u where locate('hide',u.flags)=0 order by u.id";
      }
      else{
        query = "select u from User u left join fetch u.ally a order by u.id";
      }

      //Liste aller Spieler
      org.hibernate.Session db = context.getDB();
      List<User> userlist = Common.cast(db.createQuery(query).list());
      ctx.setVariable("users", userlist);

      //Liste nicht bereiter Spieler
      query = "select u from User u where u.campaign_participant = 1 and u.tick_ready = 0 order by u.id";
      userlist = Common.cast(db.createQuery(query).list());
      ctx.setVariable("not_ready_users", userlist);

      List<StarsystemsList> systemsViewModel = new ArrayList<>();

      var starsystems = StarsystemRepository.getInstance().getStarsystemsData();

      query = "select u from User u where u.campaign_participant = 1 order by u.id";
      userlist = Common.cast(db.createQuery(query).list());
      //keine Teilnehmer = keine Kampagne, also lassen wir auch keine Systeme fuer den Tick auswaehlen
      if(userlist.size() > 0){
        for (var starsystem: starsystems) {
            if(!starsystem.isVisibleFor(user)) continue;
            systemsViewModel.add(new StarsystemsList(starsystem.id, starsystem.name + " ("+ starsystem.id +")"));
        }
      }

      ctx.setVariable("starsystems", systemsViewModel);
    }

    private static class Error{
      public final String text;
      public final String link;

      public Error(String text, String link){
        this.text = text;
        this.link = link;
      }
    }

    public static class StarsystemsList
    {
        public final int id;
        public final String name;

        public StarsystemsList(int id, String name)
        {

            this.id = id;
            this.name = name;
        }
    }
}