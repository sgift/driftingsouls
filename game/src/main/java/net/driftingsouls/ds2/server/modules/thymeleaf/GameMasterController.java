package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.TickAdminCommand;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.repositories.StarsystemRepository;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.KeineTicksperre;
import net.driftingsouls.ds2.server.WellKnownConfigValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;

@KeineTicksperre
public class GameMasterController implements DSController {
    private Context context;

    private enum Action{
      TICK,
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
          Error error = new Error("Du hast keine Berechtigung f&uuml;r diese Seite.");
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
            tickAction(ctx, request);
            break;
          default:
            defaultAction(ctx, request, user, response, templateEngine);
            break;
        }

        templateEngine.process("gamemaster", ctx, response.getWriter());
    }


    /**
     * Zeigt die GameMasterSeite an
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param user der User, der den Tick startet
     */
    private void tickAction(WebContext ctx, HttpServletRequest request) throws ClassNotFoundException {

        String[] systems = request.getParameterValues("systemcheckbox");

        new AdminCommands().executeCommand("tick campaign run "+String.join(";", systems));
        ctx.setVariable("out", "Kampagnentick gestartet");

    }



    /**
     * Zeigt die GameMasterSeite an
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param user der User, der die Seite aufruft
     */
    private void defaultAction(WebContext ctx, HttpServletRequest request, User user, HttpServletResponse response, ITemplateEngine templateEngine){

      int tick = new ConfigService().get(WellKnownConfigValue.TICK);
      if(tick == 1){
        Error error = new Error("Es l&auml;uft bereits ein Tick.");
        ctx.setVariable("error",error);
        templateEngine.process("gamemaster", ctx, response.getWriter());
        return;
      }

      List<StarsystemsList> systemsViewModel = new ArrayList<>();

      var starsystems = StarsystemRepository.getInstance().getStarsystemsData();

      for (var starsystem: starsystems) {
          if(!starsystem.isVisibleFor(user)) continue;
          systemsViewModel.add(new StarsystemsList(starsystem.id, starsystem.name + " ("+ starsystem.id +")"));
      }

      ctx.setVariable("starsystems", systemsViewModel);
    }

    private static class Error{
      public final String text;

      public Error(String text){
        this.text = text;
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