package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.config.Medals;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetEntry;
import net.driftingsouls.ds2.server.entities.ComNetVisit;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import net.driftingsouls.ds2.server.framework.bbcode.Smilie;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ValidierungException;
import net.driftingsouls.ds2.server.services.ComNetService;

import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.persistence.EntityManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * Das ComNet - Alle Funktionalitaeten des ComNets befinden sich in
 * dieser Klasse.
 *
 * @author Gregor Fuhs
 */
public class ComNetController implements DSController, PermissionResolver {
    private Context context;

    @Override
    public final boolean hasPermission(PermissionDescriptor permission)
    {
      return this.context.hasPermission(permission);
    }

    private enum Action{
      VORSCHAU,
      SEARCH,
      READ,
      SENDEN,
      WRITE,
      DEFAULT
    }
  
    /**
     * Erzeugt die ComNetSeite (/comnet). 
     * URL-Parameter:
     * action: 
     *  <code>null</code>, default 
     *  vorschau
     *  search
     *  read
     *  senden
     *  write
     */
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
        context = ContextMap.getContext();
        Action action;
        try {
          action = Action.valueOf(request.getParameter("action").toUpperCase());
        }catch(Exception e){
          action = Action.DEFAULT;
        }

        Integer channelnr = getChannelNumber(action, ctx, request);
        if (channelnr == null){
          templateEngine.process("comnet", ctx, response.getWriter());
          return;
        }
        ComNetChannel channel = ComNetChannel.getChannelByNr(channelnr, context);
        if(!validiereComNetChannel(channel,ctx))
        {
          templateEngine.process("comnet", ctx, response.getWriter());
          return;
        }
        switch(action){
          case VORSCHAU:
            vorschauAction(ctx, request, channel);
            break;
          case SEARCH:
            searchAction(ctx, request, channel);
            break;
          case READ:
            readAction(ctx, request, channel);
            break;
          case SENDEN:
            sendenAction(ctx, request, channel);
            break;
          case WRITE:
            writeAction(ctx, request, channel);
            break;
          default:
            defaultAction(ctx, request, channel);
            break;
        }

        templateEngine.process("comnet", ctx, response.getWriter());
    }

  private boolean validiereComNetChannel(ComNetChannel comNetChannel, WebContext ctx)
  {
    if (comNetChannel == null)
    {
      Error error = new Error("Die angegebene Frequenz existiert nicht");
      ctx.setVariable("error",error);
      return false;
    }
    return true;
  }

  public Integer getChannelNumber(Action action, WebContext ctx, HttpServletRequest request) {
    Integer channelnr = null;
    try {
      channelnr = Integer.parseInt(request.getParameter("channel"));
    } catch (NumberFormatException e) {
      if(action == Action.DEFAULT)
      {
        return 1;
      }
      else{
        Error error = new Error("Die angegebene Frequenz existiert nicht");
        ctx.setVariable("error", error);
      }
    }
    return channelnr;
  }

    /**
     * Aktion zur Vorschau eines ComNetBeitrages
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: vorschau 
     * @URL-Parameter text: der Text
     * @URL-Parameter head: der Titel
     * @URL-Parameter channel: der Kanal
     */
    private void vorschauAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      var head = request.getParameter("head");
      var text = request.getParameter("text");

      User user = (User) context.getActiveUser();

      Post post = new Post(channel, text, head, user, null, context.getEM());
      Channel c = new Channel(channel);
      post.setIngametime(Common.getIngameTime(context.get(ContextCommon.class).getTick()));
      post.setTime(Common.date("d.m.Y H:i:s"));
      c.setShowvorschau(true);
      c.setShowinputform(true);
      
      ctx.setVariable("post", post);
      ctx.setVariable("channel", c);
    }

    /**
     * Aktion zum Suchen eines ComNetBeitrages
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: search 
     * @URL-Parameter searchtitel: Suchtext Titel
     * @URL-Parameter searchinhalt: Suchtext Inhalt
     * @URL-Parameter searchsender: Suchtext Sender
     * @URL-Parameter channel: der Kanal
     * @URL-Parameter back: Offset
     */
    private void searchAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      User user = (User) context.getActiveUser();
      Channel c = new Channel(channel);
      var searchtitel = request.getParameter("searchtitel");
      var searchinhalt = request.getParameter("searchinhalt");
      Integer searchsender = (request.getParameter("searchsender") == null ||request.getParameter("searchsender").isBlank() )? null : Integer.parseInt(request.getParameter("searchsender"));


      if( (searchtitel == null || searchtitel.isBlank() ) && (searchsender == null) && (searchinhalt == null || searchinhalt.isBlank()) )
      {
        c.setShowsearchform(true);
        ctx.setVariable("channel",c);
        return;
      }

      c.setShowread(true);
      int back = 0;
      try{
        back = Integer.parseInt(request.getParameter("back"));
      } catch(NumberFormatException ignored){}

      
      if (!channel.isReadable(user, this))
      {
        Error error = new Error("Sie sind nicht berechtigt diese Frequenz zu empfangen");
        ctx.setVariable("error",error);
        return;
      }
      ComNetService service = new ComNetService();
      service.markiereKanalAlsGelesen(channel, user);
      Search s = new Search(searchinhalt, searchsender, searchtitel);
      ctx.setVariable("search", s);

      if (channel.isWriteable(user, this))
      {
        c.setIswriteable(true);
      }
      List<Post> posts = new ArrayList<>();
      try{
        List<ComNetEntry> entries = service.durchsucheKanal(channel, searchinhalt, searchtitel, searchsender, back, 10);

        if (entries.isEmpty())
        {
          c.setShowsearcherror(true);
        }
  
        int channelPostCount = entries.size();
  
        int b = back + 10;
        int v = back - 10;
        if (10 > channelPostCount)
        {
          b = 0;
        }
        c.setShowvor(v);
        c.setShowback(b);
  
        if (back > 0)
        {
          c.setShowreadnextpossible(true);
        }
       
        for (ComNetEntry entry : entries)
        {
          String head = entry.getHead();
          if (head.trim().isEmpty())
          {
            head = "-";
          }
          Post p = new Post(channel, entry.getText(), head, entry.getUser(), entry, entry.getAllyPic(), context.getEM());
          p.setTime(Common.date("d.m.Y H:i:s", entry.getTime()));
          p.setIngametime(Common.getIngameTime(entry.getTick()));
          posts.add(p);
        }
      }
      catch( IllegalArgumentException e ) {
        c.setShowsearchform(true);
        ContextMap.getContext().addError(e.getMessage());
        return;
      }
      c.setAction("search");
      ctx.setVariable("posts", posts);
      ctx.setVariable("channel", c);
    }

    /**
     * Aktion zur Anzeige von ComNetBeitraegen
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: read 
     * @URL-Parameter channel: der Kanal
     * @URL-Parameter back: Der Seitenoffset
     */
    private void readAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      User user = (User) context.getActiveUser();
      Channel c = new Channel(channel);
      c.setShowread(true);
      if (!channel.isReadable(user, this))
      {
        Error error = new Error("Sie sind nicht berechtigt diesee Frequenz zu empfangen");
        ctx.setVariable("error",error);
        return;
      }
      if (channel.isWriteable(user, this))
      {
        c.setIswriteable(true);
      }
      ComNetService service = new ComNetService();
      service.markiereKanalAlsGelesen(channel, user);
      int back = 0;
      try{
        back = Integer.parseInt(request.getParameter("back"));
      }catch(NumberFormatException e){}
      if (back < 0)
      {
        back = 0;
      }
      int channelPostCount = channel.getPostCount();

      int b = back + 10;
      int v = back - 10;

      if (b > channelPostCount)
      {
        b = 0;
      }
      c.setShowvor(v);
      c.setShowback(b);
      if (back > 0)
      {
        c.setShowreadnextpossible(true);
      }
      c.setAction("read");

      int i = 0;

      List<?> postList = context.getDB().createQuery("from ComNetEntry where channel= :channel order by post desc")
          .setEntity("channel", channel)
          .setFirstResult(back)
          .setMaxResults(10)
          .list();
      List<Post> posts = new ArrayList<>();
      for (Object aPostList : postList)
      {
        ComNetEntry post = (ComNetEntry) aPostList;

        int postNumber = channelPostCount - back - i;
        String head = post.getHead();

        if (head.isEmpty()) {
          head = "-";
        }
        Post p = new Post(channel,post.getText(), head, post.getUser(), post, post.getAllyPic(), context.getEM());
        p.setId(postNumber);
        p.setIngametime(Common.getIngameTime(post.getTick()));
        p.setTime(Common.date("d.m.Y H:i:s", post.getTime()));
        i++;
        posts.add(p);
      }
      ctx.setVariable("posts", posts);
      ctx.setVariable("channel", c);
    }

    /**
     * Aktion zum Senden eines ComNetBeitrages
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: senden 
     * @URL-Parameter text: der Text
     * @URL-Parameter head: der Titel
     * @URL-Parameter channel: der Kanal
     */
    private void sendenAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      User user = (User) context.getActiveUser();
      if (!channel.isWriteable(user, this))
      {
        Error error = new Error("Sie sind nicht berechtigt auf dieser Frequenz zu senden");
        ctx.setVariable("error",error);
        return;
      }
      var head = request.getParameter("head");
      var text = request.getParameter("text");

      //In die DB eintragen
      ComNetEntry entry = new ComNetEntry(user, channel);
      entry.setHead(head);
      entry.setText(text);
      context.getDB().persist(entry);

      Channel c = new Channel(channel);
      c.setShowsubmit(true);
      ctx.setVariable("channel", c);
    }

     /**
     * Aktion zum Schreiben eines ComNetBeitrages
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: write 
     * @URL-Parameter channel: der Kanal
     */
    private void writeAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      User user = (User) context.getActiveUser();
      if (!channel.isWriteable(user, this))
      {
        Error error = new Error("Sie sind nicht berechtigt auf dieser Frequenz zu senden");
        ctx.setVariable("error",error);
        return;
      }
      Channel c = new Channel(channel);
      c.setShowinputform(true);
      Post post = new Post(channel, "","", user, null, context.getEM());
      ctx.setVariable("post",post);
      ctx.setVariable("channel", c);
    }


    /**
     * Zeigt die Liste aller lesbaren ComNet-Kanaele an.
     * @param ctx der WebContext
     * @param request der HttpServletRequest (enthaelt die uebergebenen Parameter)
     * @URL-Parameter action: default 
     * @URL-Parameter channel: der Kanal
     */
    private void defaultAction(WebContext ctx, HttpServletRequest request, ComNetChannel channel){

      User user = (User) context.getActiveUser();
      Channel c = new Channel(channel);
      c.setShowchannellist(true);
      if (channel.isWriteable(user, this))
      {
        c.setIswriteable(true);
      }

      if (channel.isReadable(user, this))
      {
        c.setIsreadable(true);
      }

      // Letzte "Besuche" auslesen
      Map<ComNetChannel, ComNetVisit> visits = new HashMap<>();

      List<?> visitList = context.getDB().createQuery("from ComNetVisit where user= :user")
          .setEntity("user", user)
          .list();
      for (Object aVisitList : visitList)
      {
        ComNetVisit avisit = (ComNetVisit) aVisitList;
        visits.put(avisit.getChannel(), avisit);
      }
      List<Channel> channels = new ArrayList<>();
      List<Channel> allychannels = new ArrayList<>();

      Iterator<?> chnlIter = context.getDB().createQuery("from ComNetChannel order by allyOwner.id").iterate();
      while (chnlIter.hasNext())
      {
        ComNetChannel achannel = (ComNetChannel) chnlIter.next();
        Channel ac = new Channel(achannel);
        if (!achannel.isReadable(user, this))
        {
          continue;
        }

        ac.setIsreadable(true);

        if (achannel.isWriteable(user, this))
        {
          ac.setIswriteable(true);
        }

        if (achannel.getAllyOwner() != null)
        {
          ac.setShowprivateinfo(true);
        }

        ComNetVisit visit = visits.get(achannel);

        if (visit == null)
        {
          visit = new ComNetVisit(user, achannel);
          visit.setTime(0);
          context.getDB().persist(visit);
        }

        Long lastpost = (Long) context.getDB().createQuery("select max(time) from ComNetEntry where channel= :channel")
            .setEntity("channel", achannel)
            .iterate().next();

        if (lastpost == null)
        {
          lastpost = 0L;
        }
        if (achannel.getId() == channel.getId())
        {
          ac.setIsactive(true);
        }
        if (lastpost > visit.getTime())
        {
          ac.setNewposts(true);
        }
        if(ac.isShowprivateinfo())
        {
          allychannels.add(ac);
        }
        else {
          channels.add(ac);
        }
      }
      
      ctx.setVariable("channels",channels);
      ctx.setVariable("allychannels",allychannels);
      ctx.setVariable("channel",c);
    }

    private static class Post{
      public ComNetChannel channel;
      public String title;
      public String text;
      public String rawtext;
      public String rawtitle;
      public User user;
      public Rang userRank;
      public String time;
      public String ingametime;
      public Ally ally;
      public int id = 1;
      public String username;
      public ComNetEntry post;
      public Integer allypic;

      public Post(ComNetChannel channel, String rawtext, String rawtitle, User user, ComNetEntry post, EntityManager db){
        this.user = user;
        this.channel = channel;
        this.rawtext = rawtext;
        this.rawtitle = rawtitle;
        this.text = Smilie.parseSmilies(db, Common._text(rawtext));
        this.title = Common._title(rawtitle);
        this.userRank = user == null? null : Medals.get().rang(user.getRang());
        this.ally = user == null? null : user.getAlly();
        this.username = user == null ? "" : Common._title(user.getName());
        this.post = post;
        this.allypic = ally == null ? null : ally.getId();
      }
      public Post(ComNetChannel channel, String rawtext, String rawtitle, User user, ComNetEntry post, int allypic, EntityManager db) {
        this.user = user;
        this.channel = channel;
        this.rawtext = rawtext;
        this.rawtitle = rawtitle;
        this.text = Smilie.parseSmilies(db, Common._text(rawtext));
        this.title = Common._title(rawtitle);
        this.userRank = Medals.get().rang(user.getRang());
        this.ally = null;
        this.username = Common._title(user.getName());
        this.post = post;
        this.allypic = allypic;
      }
      public void setId(int id) {
        this.id = id;
      }
      public void setTime(String time) {
        this.time = time;
      }
      public void setIngametime(String ingametime) {
        this.ingametime = ingametime;
      }
    }

    private static class Search{
      public final String searchinhalt;
      public final Integer searchsender;
      public final String searchtitel;

      public Search(String searchinhalt, Integer searchsender, String searchtitel) {
        this.searchinhalt = searchinhalt;
        this.searchsender = searchsender;
        this.searchtitel = searchtitel;
      }
    }

    private static class Channel{
      public String action = "default";
      public ComNetChannel channel;
      public boolean showvorschau = false;
      public boolean showinputform = false;
      public boolean showsearchform = false;
      public boolean showsearcherror = false;
      public boolean showsubmit = false;
      public boolean showread = false;
      public boolean showchannellist = false;
      public boolean isreadable = false;
      public boolean iswriteable = false;
      public String name;
      public boolean showprivateinfo = false;
      public int showvor = 0;
      public int showback = 0;
      public boolean showreadnextpossible = false;
      public boolean isactive = false;
      public boolean newposts = false;

      public Channel(ComNetChannel channel){
        this.channel = channel;
        this.name = Common._title(channel.getName());
      }

      public void setIsactive(boolean isactive) {
        this.isactive = isactive;
      }
      public void setNewposts(boolean newposts) {
        this.newposts = newposts;
      }
      public void setAction(String action) {
        this.action = action;
      }
      public void setShowvorschau(boolean showvorschau) {
        this.showvorschau = showvorschau;
      }
      public void setShowinputform(boolean showinputform) {
        this.showinputform = showinputform;
      }
      public void setShowsearchform(boolean showsearchform) {
        this.showsearchform = showsearchform;
      }
      public void setShowsearcherror(boolean showsearcherror) {
        this.showsearcherror = showsearcherror;
      }
      public void setShowsubmit(boolean showsubmit) {
        this.showsubmit = showsubmit;
      }
      public void setShowread(boolean showread) {
        this.showread = showread;
      }
      public void setShowchannellist(boolean showchannellist) {
        this.showchannellist = showchannellist;
      }
      public void setIsreadable(boolean isreadable) {
        this.isreadable = isreadable;
      }
      public void setIswriteable(boolean iswriteable) {
        this.iswriteable = iswriteable;
      }
      public void setShowprivateinfo(boolean showprivateinfo) {
        this.showprivateinfo = showprivateinfo;
      }
      public void setShowvor(int showvor) { 
        this.showvor = showvor;
      }
      public void setShowback(int showback) {
        this.showback = showback;
      }
      public void setShowreadnextpossible(boolean showreadnextpossible) {this.showreadnextpossible = showreadnextpossible;}
      public boolean isShowprivateinfo() {return showprivateinfo;}
    }
    public static class Error{
      public String text;

      public Error(String text){
        this.text = text;
      }
    }
}
