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

import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.io.IOException;

/**
 * Aktualisierungstool fuer Schiffstypen-Grafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Schiffe", name = "Typengrafik editieren")
public class EditShiptypePicture extends AbstractEditPlugin<ShipType> implements AdminPlugin
{
	private static final Logger log = LogManager.getLogger(EditShiptypePicture.class);

	public EditShiptypePicture()
	{
		super(ShipType.class);
	}

	@Override
	protected void update(StatusWriter statusWriter, ShipType shipType) throws IOException
	{
		String img = this.processDynamicContent("image", shipType.getPicture());
		String oldImg = shipType.getPicture();
		shipType.setPicture("data/dynamicContent/"+img);
		if( oldImg.startsWith("data/dynamicContent/") )
		{
			DynamicContentManager.remove(oldImg);
		}

		recalculateShipModules(getDB(), shipType);
	}

	@Override
	protected void edit(EditorForm form, ShipType shipType)
	{
		form.label("Name", shipType.getNickname());
		form.dynamicContentField("Bild", "image", shipType.getPicture());

		Number count = (Number)getDB().createQuery("select count(*) from Ship s where s.shiptype=:type and s.modules is not null")
							   .setParameter("type", shipType)
							   .iterate()
							   .next();

		form.label("Zu aktualisieren", count + " Schiffe mit Modulen");
	}

	private void recalculateShipModules(org.hibernate.Session db, ShipType shipType)
	{
		int count = 0;

		ScrollableResults ships = db.createQuery("from Ship s left join fetch s.modules where s.shiptype= :type")
			.setEntity("type", shipType)
			.setCacheMode(CacheMode.IGNORE)
			.scroll(ScrollMode.FORWARD_ONLY);
		while (ships.next())
		{
			Ship ship = (Ship) ships.get(0);
			try
			{
				ship.recalculateModules();

				count++;
				if (count % 20 == 0)
				{
					db.flush();
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
					HibernateUtil.getSessionFactory().getCurrentSession().evict(ShipModules.class);
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Offizier.class);
				}
			}
			catch(Exception e)
			{
				//Riskant, aber, dass nach einem Fehler alle anderen Schiffe nicht aktualisiert werden muss verhindert werden
				log.error("Das Schiff mit der ID " + ship.getId() + " konnte nicht aktualisiert werden. Fehler: " + e.getMessage());
			}
		}
	}
}
