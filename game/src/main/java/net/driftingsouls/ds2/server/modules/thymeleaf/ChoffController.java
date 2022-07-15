package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;

import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

public class ChoffController implements DSController {
    
    private enum Action{
      RENAME,
      DEFAULT
    }

    /**
     * Erzeugt die Offiziersseite (/choff). 
     * URL-Parameter:
     * off: Die ID des Offiziers
     * action: <code>null</code> oder "rename"
     * name: der neue Name fuer den Offizier
     */
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        Action action = Action.DEFAULT;
        try{
         action = Action.valueOf(request.getParameter("action").toUpperCase());
        } catch(Exception ex){}
        int offid = -1;
        try{
          offid = Integer.parseInt(request.getParameter("off"));
        }
        catch (NumberFormatException e){
          Error error = new Error("Der angegebene Offizier ist ung&uuml;ltig");
          ctx.setVariable("error",error);
          templateEngine.process("choff", ctx, response.getWriter());
          return;
        }
        org.hibernate.Session db = ContextMap.getContext().getDB();

        Offizier offizier = (Offizier) db.createQuery("from Offizier where id =:id").setParameter("id", offid).uniqueResult();
        if(!validiereOffizier(offizier, ctx))
        {
          templateEngine.process("choff", ctx, response.getWriter());
          return;
        }
        switch(action){
          case RENAME:
            renameAction(ctx, request, offizier);
            break;
          default:
            defaultAction(ctx, request, offizier);
            break;
        }

        templateEngine.process("choff", ctx, response.getWriter());
    }

    /**
     * prueft, ob der Spieler diesen Offizier ansehen darf
     */
    private boolean validiereOffizier(Offizier offizier, WebContext ctx)
    {
      Context context = ContextMap.getContext();
      User user = (User) context.getActiveUser();

      if (offizier == null)
      {
        Error error = new Error("Der angegebene Offizier ist ung&uuml;ltig");
        ctx.setVariable("error",error);
        return false;
      }

      if (offizier.getOwner() != user)
      {
        Error error = new Error("Dieser Offizier untersteht nicht ihrem Kommando");
        ctx.setVariable("error",error);
        return false;
      }
      return true;
    }

    /**
     * Aktion zum Umbenennen des Offiziers
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param offizier der Offizier
     * URL-Parameter: name - der neue Name fuer den Offizier
     */
    private void renameAction(WebContext ctx, HttpServletRequest request, Offizier offizier){
      var name = request.getParameter("name");
      String message;
      if (name.length() != 0)
      {
        int MAX_NAME_LENGTH = 60; //See db/offiziere_create.sql
        if (name.length() > MAX_NAME_LENGTH)
        {
          message = "<span style=\"color:red\">Der eingegebene Name ist zu lang (maximal " + MAX_NAME_LENGTH + " Zeichen)</span>";
        }
        else
        {
          offizier.setName(name);
          message = "Der Name wurde in " + Common._plaintitle(name) + " geändert";
        }
      }
      else
      {
        message = "<span style=\"color:red\">Sie müssen einen Namen angeben</span>";
      }
      Inhalt i = new Inhalt(message, offizier, offizier.getStationiertAufBasis() != null ? offizier.getStationiertAufBasis().getId() : 0,offizier.getStationiertAufSchiff() != null ? offizier.getStationiertAufSchiff().getId() : 0);
      ctx.setVariable("inhalt",i);
    }

    /**
     * Zeigt die Offiziersseite mit den Werten des Offiziers
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @param offizier der Offizier
     */
    private void defaultAction(WebContext ctx, HttpServletRequest request, Offizier offizier){
      Inhalt i = new Inhalt("",offizier,offizier.getStationiertAufBasis() != null ? offizier.getStationiertAufBasis().getId() : 0,offizier.getStationiertAufSchiff() != null ? offizier.getStationiertAufSchiff().getId() : 0);
      ctx.setVariable("inhalt", i);
    }

    private static class Inhalt{
      public final String text;
      public final Offizier off;
      public final int baseid;
      public final int shipid;

      public Inhalt(String text,Offizier off, int baseid, int shipid){
        this.text = text;
        this.off = off;
        this.baseid = baseid;
        this.shipid = shipid;
      }
    }

    private static class Error{
      public final String text;

      public Error(String text){
        this.text = text;
      }
    }
}
