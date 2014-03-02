package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;

/**
 * Editiert die Werte von Cores.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Core editieren")
public class EditCore extends AbstractEditPlugin<Core>
{
	public EditCore()
	{
		super(Core.class);
	}

	@Override
	protected void update(StatusWriter writer, Core core) throws IOException
	{
		Request req = ContextMap.getContext().getRequest();
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
	}

	@Override
	protected void edit(EditorForm form, Core core)
	{
		form.field("Name", "name", String.class, core.getName());
		form.field("Astitype", "asti", Integer.class, core.getAstiType());
		form.field("Arbeiter", "worker", Integer.class, core.getArbeiter());
		form.field("Energieverbrauch", "everbrauch", Integer.class, core.getEVerbrauch());
		form.field("Energieproduktion", "eproduktion", Integer.class, core.getEProduktion());
		form.field("EPS", "eps", Integer.class, core.getEPS());
		form.field("Wohnraum", "room", Integer.class, core.getBewohner());
		form.field("Auto Abschalten", "shutdown", Boolean.class, core.isShutDown());
		form.field("Forschung", "tech", Forschung.class, core.getTechRequired());
		form.field("Baukosten", "buildcosts", Cargo.class, core.getBuildCosts());
		form.field("Verbrauch", "consumes", Cargo.class, core.getConsumes());
		form.field("Produktion", "produces", Cargo.class, core.getProduces());
	}
}
