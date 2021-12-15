package net.driftingsouls.ds2.server.modules.thymeleaf;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

public class PortalController implements DSController {
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ITemplateEngine templateEngine) throws Exception {
        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        populateNews(ctx);

        templateEngine.process("portal", ctx, response.getWriter());
    }

    private void populateNews(WebContext ctx) {
        var db = ContextMap.getContext().getDB();

        List<NewsEntry> news = Common.cast(db.createQuery("FROM NewsEntry ORDER BY date DESC")
            .setMaxResults(3)
            .list());

        List<NewsItem> portalNews = news.stream()
            .map(newsEntry -> new NewsItem(newsEntry.getTitle(), newsEntry.getAuthor(), Common.date("d.m.Y H:i", newsEntry.getDate()), Common._text(newsEntry.getNewsText())))
            .collect(Collectors.toList());

        ctx.setVariable("news", portalNews);
    }

    private static class NewsItem {
        public final String title;
        public final String author;
        public final String date;
        public final String text;


        private NewsItem(String title, String author, String date, String text) {
            this.title = title;
            this.author = author;
            this.date = date;
            this.text = text;
        }
    }
}
