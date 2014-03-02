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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.io.IOException;

/**
 * Aktualisierungstool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Systeme", name = "System editieren")
public class EditSystem extends AbstractEditPlugin<StarSystem> implements AdminPlugin
{
	public EditSystem()
	{
		super(StarSystem.class);
	}

	@Override
	protected void update(StatusWriter statusWriter, StarSystem system) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = getDB();
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
	}

	@Override
	protected void edit(EditorForm form, StarSystem system)
	{
		form.editField("Name", "name", String.class, system.getName());
		form.editField("Breite", "width", Integer.class, system.getWidth());
		form.editField("Höhe", "height", Integer.class, system.getHeight());
		form.editField("Allow Military", "military", Boolean.class, system.isMilitaryAllowed());
		form.editField("Max Colonies (-1 = keine Begrenzung)", "maxcolonies", Integer.class, system.getMaxColonies());
		form.editField("In Sternenkarte sichtbar", "starmap", Boolean.class, system.isStarmapVisible());
		form.editField("OrderLocations(Form: x/y|x/y)", "orderloc", String.class, system.getOrderLocationString());
		form.editField("GTU Dropzone(Form: x/y)", "gtuDropZone", String.class, system.getDropZoneString());
		form.editField("Zugriffsrechte(1=Jeder;2=NPC;3=Admin)", "access", Integer.class, system.getAccess());
		form.editField("Beschreibung", "descrip", String.class, system.getDescription());
		form.editField("Ressourcenvorkommen", "spawnableress", String.class, system.getSpawnableRess());
	}
}
