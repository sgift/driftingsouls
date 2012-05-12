package net.driftingsouls.ds2.server.modules;

import java.io.IOException;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import org.hibernate.Session;

/**
 * Ein einzelner Newseintrag.
 * 
 * @author Sebastian Gift
 */
@Module(name="newsdetail")
public class NewsDetailController extends TemplateGenerator
{
	/**
	 * Legt den News Detail Eintrag an.
	 * 
	 * @param context Der Kontext.
	 */
	public NewsDetailController(Context context)
	{
		super(context);
		
		setTemplate("newsdetail.html");
	}

	@Override
	protected boolean validateAndPrepare(String action)
	{
		return true;
	}
	
	@Override
	protected void printHeader( String action ) 
	{}
	
	/**
	 * Zeigt den Newseintrag an.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException
	{
		TemplateEngine t = getTemplateEngine();
		Session db = getDB();
		
		parameterNumber("newsid");
		NewsEntry entry = (NewsEntry)db.get(NewsEntry.class, getInteger("newsid"));
		if(entry != null)
		{
			t.setVar("news.headline", entry.getTitle(),
					 "news.date", Common.date("d.m.Y H:i", entry.getDate()),
					 "news.text", Common._text(entry.getNewsText()));
		}
		else
		{
			t.setVar("news.headline", "ES EXISTIERT KEIN EINTRAG MIT DIESER ID");
		}
	}
}
