package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Editiert die Werte von Cores.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Core editieren")
public class EditCore extends AbstractEditPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		Request req = context.getRequest();
		int coreId = req.getParameterInt("entityId");

		if( this.isUpdateExecuted() )
		{
			Core core = (Core)db.get(Core.class, coreId);
			core.setName(req.getParameterString("name"));
			core.setAstiType(req.getParameterInt("asti"));
			core.setArbeiter(req.getParameterInt("worker"));
			core.setEVerbrauch(req.getParameterInt("everbrauch"));
			core.setEProduktion(req.getParameterInt("eproduktion"));
			core.setEps(req.getParameterInt("eps"));
			core.setBewohner(req.getParameterInt("room"));
			core.setShutDown("true".equals(req.getParameterString("shutdown")));
			core.setTechReq(req.getParameterInt("tech"));
			core.setBuildcosts(new Cargo(Cargo.Type.ITEMSTRING, req.getParameterString("buildcosts")));
			core.setConsumes(new Cargo(Cargo.Type.ITEMSTRING, req.getParameterString("consumes")));
			core.setProduces(new Cargo(Cargo.Type.ITEMSTRING, req.getParameterString("produces")));

			echo.append("<p>Update abgeschlossen.</p>");
		}

		List<Core> cores = Common.cast(db.createQuery("from Core").list());

		beginSelectionBox(echo, page, action);
		for (Core core: cores)
		{
			addSelectionOption(echo, core.getId(), core.getName()+" ("+core.getId()+")");
		}
		endSelectionBox(echo);

		if(coreId > 0)
		{
			Core core = (Core)db.get(Core.class, coreId);

			if(core == null)
			{
				return;
			}

			beginEditorTable(echo, page, action, coreId);

			editField(echo, "Name", "name", String.class, core.getName());
			editField(echo, "Astitype", "asti", Integer.class, core.getAstiType());
			editField(echo, "Arbeiter", "worker", Integer.class, core.getArbeiter());
			editField(echo, "Energieverbrauch", "everbrauch", Integer.class, core.getEVerbrauch());
			editField(echo, "Energieproduktion", "eproduktion", Integer.class, core.getEProduktion());
			editField(echo, "EPS", "eps", Integer.class, core.getEPS());
			editField(echo, "Wohnraum", "room", Integer.class, core.getBewohner());
			editField(echo, "Auto Abschalten", "shutdown", Boolean.class, core.isShutDown());
			editField(echo, "Forschung", "tech", Forschung.class, core.getTechRequired());
			editField(echo, "Baukosten", "buildcosts", Cargo.class, core.getBuildCosts());
			editField(echo, "Verbrauch", "consumes", Cargo.class, core.getConsumes());
			editField(echo, "Produktion", "produces", Cargo.class, core.getProduces());

			endEditorTable(echo);
		}
	}
}
