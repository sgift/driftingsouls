package net.driftingsouls.ds2.server.modules;

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.uilibs.PlayerList;

/**
 * Zeigt die Spielerliste an.
 * @author Christopher Jung
 * 
 * @urlparam Integer compopup != 0, falls die Spielerliste als Popup der PM-Verwaltung dient
 *
 */
public class PListController extends DSGenerator {

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public PListController(Context context) {
		super(context);
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException {
		parameterNumber("compopup");
		
		Writer echo = getContext().getResponse().getWriter();
		if( getInteger("compopup") != 0 ) {
			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("function playerPM(id)\n");
			echo.append("{\n");
			echo.append("opener.parent.frames['main'].location.href='./ds?module=comm&to='+id;\n");
			echo.append("window.close()\n");;
			echo.append("}\n");
			echo.append("// -->\n");
			echo.append("</script>\n");
		}

		echo.append(Common.tableBegin(325,"left"));

		new PlayerList().draw(getContext());
		
		echo.append(Common.tableEnd());
	}

}
