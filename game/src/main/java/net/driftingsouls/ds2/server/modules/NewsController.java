package net.driftingsouls.ds2.server.modules;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Zeigt die News der letzten Zeit als RSS Feed an.
 * 
 * @author Sebastian Gift
 */
@KeinLoginNotwendig
@Module(name="news")
public class NewsController extends Controller
{
	private Logger log = Logger.getLogger(NewsController.class);

	/**
	 * Gibt den News RSS Feed aus.
	 */
	@Action(value = ActionType.DEFAULT, outputHandler = EmptyHeaderFooterOutputHandler.class)
	public void defaultAction() throws IOException
	{
		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Drifting-Souls News");
		feed.setLink("http://ds.drifting-souls.net");
		feed.setDescription("Drifting-Souls Newsfeed");

		Session db = getDB();
		List<SyndEntry> entries = new ArrayList<>();
		List<NewsEntry> allNews = Common.cast(db.createQuery("from NewsEntry").list());
		for (NewsEntry news : allNews)
		{
			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(news.getTitle());
			entry.setPublishedDate(new Date(news.getDate() * 1000));
			entry.setLink("./ds?module=newsdetail&action=default&newsid=" + news.getId());

			SyndContent description = new SyndContentImpl();
			description.setType("text/plain");
			description.setValue(news.getShortDescription());
			entry.setDescription(description);

			entries.add(entry);
		}
		feed.setEntries(entries);

		SyndFeedOutput result = new SyndFeedOutput();
		Writer writer = getContext().getResponse().getWriter();

		try
		{
			result.output(feed, writer);
		}
		catch (FeedException e)
		{
			log.error("Could not write out rss feed due to errors", e);
		}
	}
}
