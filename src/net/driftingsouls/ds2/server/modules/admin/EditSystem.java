/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Aktualisierungstool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Systeme", name = "System editieren")
public class EditSystem extends AbstractEditPlugin implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int systemid = context.getRequest().getParameterInt("entityId");

		// Update values?
		List<?> systems = db.createQuery("from StarSystem").list();

		this.beginSelectionBox(echo, page, action);
		for (Object system1 : systems)
		{
			StarSystem system = (StarSystem) system1;
			this.addSelectionOption(echo, system.getID(), system.getName() + " (" + system.getID() + ")");
		}
		this.endSelectionBox(echo);

		if (this.isUpdateExecuted() && systemid > 0)
		{
			Request request = context.getRequest();

			String name = request.getParameterString("name");
			int width = request.getParameterInt("width");
			int height = request.getParameterInt("height");
			boolean military = request.getParameterString("military").equals("true");
			int maxcolonies = request.getParameterInt("maxcolonies");
			boolean starmap = request.getParameterString("starmap").equals("true");
			String orderloc = request.getParameterString("orderloc");
			String gtuDropZoneString = request.getParameterString("gtuDropZone");
			int access = request.getParameterInt("access");
			String descrip = request.getParameterString("descrip");
			String spawnableress = request.getParameterString("spawnableress");

			StarSystem system = (StarSystem) db.get(StarSystem.class, systemid);

			system.setName(name);
			system.setWidth(width);
			system.setHeight(height);
			system.setMilitaryAllowed(military);
			system.setMaxColonies(maxcolonies);
			system.setStarmapVisible(starmap);
			system.setOrderLocations(orderloc);
			if(!"".equals(gtuDropZoneString)) {
				system.setDropZone(Location.fromString(gtuDropZoneString));
			}
			else
			{
				system.setDropZone(new Location(0, 0, 0));
			}
			system.setAccess(access);
			system.setDescription(descrip);
			system.setSpawnableRess(spawnableress);

			// Update ships
			int count = 0;

			ScrollableResults ships = db.createQuery("from Ship where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while (ships.next())
			{
				Ship ship = (Ship) ships.get(0);
				if(ship.getX() > system.getWidth()) {
					ship.setX(system.getWidth());
				}
				if(ship.getY() > system.getHeight()) {
					ship.setY(system.getHeight());
				}
				count++;
				if (count % 20 == 0)
				{
					db.flush();
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
				}
			}

			ScrollableResults battles = db.createQuery("from Battle where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while (battles.next())
			{
				Battle battle = (Battle) battles.get(0);
				if(battle.getX() > system.getWidth())
				{
					battle.setX(system.getWidth());
				}
				if(battle.getY() > system.getHeight())
				{
					battle.setY(system.getHeight());
				}
				db.flush();
				HibernateUtil.getSessionFactory().getCurrentSession().evict(Battle.class);
			}

			ScrollableResults bases = db.createQuery("from Base where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while(bases.next())
			{
				Base base = (Base) bases.get(0);
				if(base.getX() > system.getWidth())
				{
					base.setX(system.getWidth());
				}
				if(base.getY() > system.getHeight())
				{
					base.setY(system.getHeight());
				}
				db.flush();
				HibernateUtil.getSessionFactory().getCurrentSession().evict(Base.class);
			}

			echo.append("<p>Update abgeschlossen.</p>");
		}

		// Ship choosen - get the values
		if (systemid > 0)
		{
			StarSystem system = (StarSystem) db.get(StarSystem.class, systemid);

			this.beginEditorTable(echo, page, action, systemid);
			this.editField(echo, "Name", "name", String.class, system.getName());
			this.editField(echo, "Breite", "width", Integer.class, system.getWidth());
			this.editField(echo, "Höhe", "height", Integer.class, system.getHeight());
			this.editField(echo, "Allow Military", "military", Boolean.class, system.isMilitaryAllowed());
			this.editField(echo, "Max Colonies (-1 = keine Begrenzung)", "maxcolonies", Integer.class, system.getMaxColonies());
			this.editField(echo, "In Sternenkarte sichtbar", "starmap", Boolean.class, system.isStarmapVisible());
			this.editField(echo, "OrderLocations(Form: x/y|x/y)", "orderloc", String.class, system.getOrderLocationString());
			this.editField(echo, "GTU Dropzone(Form: x/y)", "gtuDropZone", String.class, system.getDropZoneString());
			this.editField(echo, "Zugriffsrechte(1=Jeder;2=NPC;3=Admin)", "access", Integer.class, system.getAccess());
			this.editField(echo, "Beschreibung", "descrip", String.class, system.getDescription());
			this.editField(echo, "Ressourcenvorkommen", "spawnableress", String.class, system.getSpawnableRess());
			this.endEditorTable(echo);
		}
	}
}
