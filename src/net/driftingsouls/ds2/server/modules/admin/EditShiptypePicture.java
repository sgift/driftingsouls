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

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Aktualisierungstool fuer Schiffstypen-Grafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Schiffe", name = "Typengrafik editieren")
public class EditShiptypePicture implements AdminPlugin
{
	private static final Logger log = LogManager.getLogger(EditShiptypePicture.class);

	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int shipid = context.getRequest().getParameterInt("shipid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<div class='gfxbox'><form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select name=\"shipid\" size='1'>\n");
		List<ShipType> shipTypes = Common.cast(db.createQuery("from ShipType order by id").list());
		for( ShipType st : shipTypes )
		{
			echo.append("<option value='"+st.getId()+"' "+
					(st.getId()==shipid?"selected='selected'":"")+">"+
					st.getNickname()+" ("+st.getId()+")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form></div>");

		if(update && shipid != 0)
		{
			ShipType shipType = (ShipType)db.get(ShipType.class, shipid);

			if(shipType != null) {
				for( FileItem file : context.getRequest().getUploadedFiles() )
				{
					if( "image".equals(file.getFieldName()) && file.getSize() > 0 )
					{
						String oldImg = shipType.getPicture();
						shipType.setPicture("data/dynamicContent/"+DynamicContentManager.add(file));
						if( oldImg.startsWith("data/dynamicContent/") )
						{
							DynamicContentManager.remove(oldImg);
						}
					}
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Kein Schiffstyp gefunden.</p>");
			}

			recalculateShipModules(db, shipType);
		}

		if(shipid != 0)
		{
			ShipType shipType = (ShipType)db.get(ShipType.class, shipid);

			if(shipType == null)
			{
				return;
			}

			echo.append("<div class='gfxbox' style='width:700px'>");
			echo.append("<form action=\"./ds\" method=\"post\" enctype='multipart/form-data'>");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shipid\" value=\"" + shipid + "\" />\n");

			echo.append("<table width=\"100%\">");
			echo.append("<tr><td >Name: </td>" +
					"<td>"+shipType.getNickname()+"</td><td></td></tr>\n");
			echo.append("<tr><td>Bild: </td>" +
					"<td><input type=\"file\" name=\"image\"\"></td>"+
					"<td><img src='"+shipType.getPicture()+"' /></td></tr>\n");

			Number count = (Number)db.createQuery("select count(*) from Ship s where s.shiptype=:type and s.modules is not null")
				.setParameter("type", shipType)
				.iterate()
				.next();

			echo.append("<tr><td>Zu aktualisieren:</td><td>"+count+" Schiffe mit Modulen</td><td></td></tr>\n");

			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td><td></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
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
