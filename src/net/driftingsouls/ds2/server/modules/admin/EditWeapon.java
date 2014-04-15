package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.entities.BeamWeapon;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@AdminMenuEntry(category = "Schiffe", name = "Waffen editieren")
public class EditWeapon extends AbstractEditPlugin8<Weapon>
{

	public EditWeapon()
	{
		super(Weapon.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<Weapon> form)
	{
		Map<String,String> clsOptions = Arrays.asList(Weapon.class, BeamWeapon.class)
				.stream()
				.collect(Collectors.toMap((c) -> c.getName(), (c) -> c.getSimpleName()));

		form.allowAdd();
		form.ifAdding().field("Id", String.class, Weapon::getId, Weapon::setId);
		form.ifUpdating().label("Id", Weapon::getId);
		form.ifAdding().field("Implementierung", String.class, (Weapon w) -> this.getEntityClass(), (Weapon w,String s) -> this.setEntityClass(s)).withOptions(clsOptions);
		form.ifUpdating().label("Implementierung", (b) -> b.getClass().getName());
		form.field("Name", String.class, Weapon::getName, Weapon::setName);
		form.field("AP-Kosten", Integer.class, Weapon::getApCost, Weapon::setApCost);
		form.field("Energiekosten", Integer.class, Weapon::getECost, Weapon::setECost);
		form.field("TWS", Integer.class, Weapon::getDefTrefferWS, Weapon::setDefTrefferWS);
		form.field("TWS Kleine Schiffe", Integer.class, Weapon::getDefSmallTrefferWS, Weapon::setDefSmallTrefferWS);
		form.field("TWS Subsysteme", Integer.class, Weapon::getDefSubWS, Weapon::setDefSubWS);
		form.field("TWS Torpedos", Double.class, Weapon::getTorpTrefferWS, Weapon::setTorpTrefferWS);
		form.field("Durch Abwehrfeuer zerstörbar", Boolean.class, Weapon::getDestroyable, Weapon::setDestroyable);
		form.field("Schüsse pro Salve", Integer.class, Weapon::getSingleShots, Weapon::setSingleShots);
		form.field("Hüllenschaden", Integer.class, Weapon::getBaseDamage, Weapon::setBaseDamage);
		form.field("Schildschaden", Integer.class, Weapon::getShieldDamage, Weapon::setShieldDamage);
		form.field("Subsystemschaden", Integer.class, Weapon::getSubDamage, Weapon::setSubDamage);
		form.field("Umgebungsschaden", Integer.class, Weapon::getAreaDamage, Weapon::setAreaDamage);
		form.multiSelection("Flags", Weapon.Flags.class, Weapon::getFlags, Weapon::setFlags);
		form.field("Munitionstypen", String.class, (w) -> StringUtils.join(w.getMunitionstypen(), ','), (w, s) -> w.setMunitionstypen(new HashSet<>(Arrays.asList(s.split(",")))));
	}
}
