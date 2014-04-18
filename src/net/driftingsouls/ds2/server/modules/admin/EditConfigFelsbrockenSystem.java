package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

@AdminMenuEntry(category = "Systeme", name="Felsbrocken-System")
public class EditConfigFelsbrockenSystem extends AbstractEditPlugin8<ConfigFelsbrockenSystem>
{
	public EditConfigFelsbrockenSystem()
	{
		super(ConfigFelsbrockenSystem.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<ConfigFelsbrockenSystem> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.ifUpdating().label("Sternensystem", ConfigFelsbrockenSystem::getSystem);
		List<StarSystem> systeme = Common.cast(getDB().createQuery("from StarSystem s where s not in(select system from ConfigFelsbrockenSystem)")
				.list());
		form.ifAdding().field("Sternensystem", StarSystem.class, ConfigFelsbrockenSystem::getSystem, ConfigFelsbrockenSystem::setSystem).withOptions(systeme.stream().collect(Collectors.toMap(StarSystem::getID, (s) -> s)));
		form.field("Anzahl Felsbrocken", Integer.class, ConfigFelsbrockenSystem::getCount, ConfigFelsbrockenSystem::setCount);
		form.label("Anzahl Layouts", (t) -> t.getFelsbrocken().size());
	}
}
