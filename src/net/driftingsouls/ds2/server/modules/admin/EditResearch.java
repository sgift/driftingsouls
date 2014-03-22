/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;

import javax.annotation.Nonnull;

/**
 * Ermoeglicht das Bearbeiten von Forschungen.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Techs", name="Bearbeiten")
public class EditResearch extends AbstractEditPlugin8<Forschung> {
	public EditResearch()
	{
		super(Forschung.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<Forschung> form)
	{
		form.allowAdd();
		form.field("Name", String.class, Forschung::getName, Forschung::setName);
		form.field("Bild", String.class, Forschung::getImage, Forschung::setImage);
		form.field("Rasse", Rasse.class, Integer.class, Forschung::getRace, Forschung::setRace);
		form.field("Dauer", Integer.class, Forschung::getTime, Forschung::setTime);
		form.field("Forschungskosten", Cargo.class, Forschung::getCosts, Forschung::setCosts);
		form.field("Spezialisierungspunkte", Integer.class, Forschung::getSpecializationCosts, Forschung::setSpecializationCosts);
		form.textArea("Beschreibung", Forschung::getDescription, Forschung::setDescription);
		form.field("Benötigt 1", Forschung.class, Integer.class, (f) -> f.getRequiredResearch(1), Forschung::setReq1);
		form.field("Benötigt 2", Forschung.class, Integer.class, (f) -> f.getRequiredResearch(2), Forschung::setReq2);
		form.field("Benötigt 3", Forschung.class, Integer.class, (f) -> f.getRequiredResearch(3), Forschung::setReq3);
	}
}
