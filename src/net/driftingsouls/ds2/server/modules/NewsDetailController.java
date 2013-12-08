package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.io.IOException;

/**
 * Ein einzelner Newseintrag.
 *
 * @author Sebastian Gift
 */
@Module(name = "newsdetail")
public class NewsDetailController extends TemplateController
{
	/**
	 * Legt den News Detail Eintrag an.
	 *
	 * @param context Der Kontext.
	 */
	public NewsDetailController(Context context)
	{
		super(context);
	}

	@Override
	protected void printHeader()
	{
	}

	/**
	 * Zeigt den Newseintrag an.
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(NewsEntry newsid) throws IOException
	{
		TemplateEngine t = getTemplateEngine();

		if (newsid != null)
		{
			t.setVar("news.headline", newsid.getTitle(),
					"news.date", Common.date("d.m.Y H:i", newsid.getDate()),
					"news.text", Common._text(newsid.getNewsText()));
		}
		else
		{
			t.setVar("news.headline", "ES EXISTIERT KEIN EINTRAG MIT DIESER ID");
		}
	}
}
