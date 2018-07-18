package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.BeamWeapon;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

@AdminMenuEntry(category = "Schiffe", name = "Waffe", permission = WellKnownAdminPermission.EDIT_WEAPON)
public class EditWeapon implements EntityEditor<Weapon>
{
	@Override
	public Class<Weapon> getEntityType()
	{
		return Weapon.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Weapon> form)
	{
		form.allowAdd();
		form.ifAdding().field("Id", String.class, Weapon::getId, Weapon::setId);
		form.ifUpdating().label("Id", Weapon::getId);
		form.entityClass("Implementierung", Weapon.class, BeamWeapon.class);
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
		form.field("Munitionstypen", String.class, (w) -> String.join(",", w.getMunitionstypen()), (w, s) -> {
			if(s == null || s.trim().isEmpty()) {
				w.setMunitionstypen(Collections.emptySet());
				return;
			}

			w.setMunitionstypen(new HashSet<>(Arrays.asList(s.split(","))));
		});
	}
}
