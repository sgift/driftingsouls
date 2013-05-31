package net.driftingsouls.ds2.server.modules;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
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
@Module(name="news")
public class NewsController extends TemplateGenerator 
{
	private Logger log = Logger.getLogger(NewsController.class);

	/**
	 * Legt den RSS Feed an.
	 *
	 * @param context Der Kontext.
	 */
	public NewsController(Context context)
	{
		super(context);
	}

	@Override
	protected void printHeader(String action) throws IOException
	{}

	@Override
	protected void printFooter(String action) throws IOException
	{}

	@Override
	protected boolean validateAndPrepare(String action)
	{
		return true;
	}

	/**
	 * Gibt den News RSS Feed aus.
	 */
	@Override
	@Action(ActionType.DEFAULT)
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
		for(NewsEntry news: allNews)
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
		catch( FeedException e )
		{
			log.error("Could not write out rss feed due to errors", e);
		}
	}
}
