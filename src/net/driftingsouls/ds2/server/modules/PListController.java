package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.uilibs.PlayerList;

import java.io.IOException;
import java.io.Writer;

/**
 * Zeigt die Spielerliste an.
 *
 * @author Christopher Jung
 */
@Module(name = "plist")
public class PListController extends Controller
{

	/**
	 * Konstruktor.
	 *
	 */
	public PListController()
	{
		super();
	}

	/**
	 * Zeigt die Spielerliste an.
	 *
	 * @param compopup {@code true}, falls die Spielerliste als Popup der PM-Verwaltung dient
	 * @throws IOException
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(boolean compopup) throws IOException
	{
		Writer echo = getContext().getResponse().getWriter();
		if (compopup)
		{
			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("function playerPM(id)\n");
			echo.append("{\n");
			echo.append("opener.parent.frames['main'].location.href='./ds?module=comm&to='+id;\n");
			echo.append("window.close()\n");
			echo.append("}\n");
			echo.append("// -->\n");
			echo.append("</script>\n");
		}

		echo.append("<div class='gfxbox' style='width:365px'>");

		new PlayerList().draw(getContext());

		echo.append("</div>");
	}

}
