package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.EmptyHeaderOutputHandler;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.KeinLoginNotwendig;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ein einzelner Newseintrag.
 *
 * @author Sebastian Gift
 */
@KeinLoginNotwendig
@Module(name = "newsdetail")
public class NewsDetailController extends Controller
{
	private final TemplateViewResultFactory templateViewResultFactory;
	private final BBCodeParser bbCodeParser;

	@Autowired
	public NewsDetailController(TemplateViewResultFactory templateViewResultFactory, BBCodeParser bbCodeParser)
	{
		this.templateViewResultFactory = templateViewResultFactory;
		this.bbCodeParser = bbCodeParser;
	}

	/**
	 * Zeigt den Newseintrag an.
	 */
	@Action(value = ActionType.DEFAULT, outputHandler = EmptyHeaderOutputHandler.class)
	public TemplateEngine defaultAction(NewsEntry newsid) {
		TemplateEngine t = templateViewResultFactory.createFor(this);

		if (newsid != null)
		{
			t.setVar("news.headline", newsid.getTitle(),
					"news.date", Common.date("d.m.Y H:i", newsid.getDate()),
					"news.text", Common._text(bbCodeParser, newsid.getNewsText()));
		}
		else
		{
			t.setVar("news.headline", "ES EXISTIERT KEIN EINTRAG MIT DIESER ID");
		}

		return t;
	}
}
