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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Aktualisierungstool fuer Schiffstypen-Grafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Schiffe", name = "Typengrafik", permission = WellKnownAdminPermission.EDIT_SHIPTYPE_PICTRUE)
@Component
public class EditShiptypePicture implements EntityEditor<ShipType>
{
	@PersistenceContext
	private EntityManager em;

	private final ShipService shipService;

	public EditShiptypePicture(ShipService shipService) {
		this.shipService = shipService;
	}

	private List<Integer> liefereZuAktualisierendeSchiffe(ShipType shipType)
	{
		return em.createQuery("select s.id from Ship s where s.shiptype=:type and s.modules is not null", Integer.class)
								   .setParameter("type", shipType)
								   .getResultList();
	}

	private void aktualisiereSchiff(ShipType oldshipType, ShipType shipType, Integer schiffsId)
	{
		Ship ship = em.find(Ship.class, schiffsId);
		shipService.recalculateModules(ship);
	}

	@Override
	public Class<ShipType> getEntityType()
	{
		return ShipType.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<ShipType> form)
	{
		form.label("Name", ShipType::getNickname);
		form.dynamicContentField("Bild", ShipType::getPicture, ShipType::setPicture);

		form.postUpdateTask("Schiffe mit Modulen neu berechnen", this::liefereZuAktualisierendeSchiffe, this::aktualisiereSchiff);
	}
}
