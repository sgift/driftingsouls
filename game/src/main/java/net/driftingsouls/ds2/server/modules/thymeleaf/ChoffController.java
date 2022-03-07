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
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        var action = request.getParameter("action");
        int offid = -1;
        try{
          offid = Integer.parseInt(request.getParameter("off"));
        }
        catch (NumberFormatException e){
          throw new ValidierungException("Der angegebene Offizier ist ung&uuml;ltig", Common.buildUrl("default", "module", "ueber"));
        }
        org.hibernate.Session db = ContextMap.getContext().getDB();

        Offizier offizier = (Offizier) db.createQuery("from Offizier where id =:id").setParameter("id", offid).uniqueResult();
        validiereOffizier(offizier);
        switch(action){
          case "rename":
            renameAction(ctx, request, offizier);
          default:
            defaultAction(ctx, request, offizier);
        }
        populateNews(ctx);

        templateEngine.process("choff", ctx, response.getWriter());
    }

    private void validiereOffizier(Offizier offizier)
    {
      Context context = ContextMap.getContext();
      User user = (User) context.getActiveUser();

      if (offizier == null)
      {
        throw new ValidierungException("Der angegebene Offizier ist ung&uuml;ltig", Common.buildUrl("default", "module", "ueber"));
      }

      if (offizier.getOwner() != user)
      {
        throw new ValidierungException("Dieser Offizier untersteht nicht ihrem Kommando", Common.buildUrl("default", "module", "ueber"));
      }
    }

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
      Choff choff = new Choff(message);
      ctx.setVariable("choff", choff);
    }

    private void defaultAction(WebContext ctx, HttpServletRequest request, Offizier offizier){
      ctx.setVariable("offizier", offizier);
      ctx.setVariable("baseid",offizier.getStationiertAufBasis() != null ? offizier.getStationiertAufBasis().getId() : 0 );
      ctx.setVariable("shipid",offizier.getStationiertAufSchiff() != null ? offizier.getStationiertAufSchiff().getId() : 0)
    }

    private static class Choff{
      public static String message;

      public Choff(String message){
        this.message = message;
      }

    }
}
