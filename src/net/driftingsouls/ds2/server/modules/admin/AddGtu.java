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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungResource;
import net.driftingsouls.ds2.server.entities.fraktionsgui.VersteigerungSchiff;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.HtmlUtils;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ermoeglicht das Einfuegen von neuen Versteigerungen in die GTU.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Versteigern", permission = WellKnownAdminPermission.ADD_GTU)
public class AddGtu implements AdminPlugin {
	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		int ship = context.getRequest().getParameterInt("ship");
		String resource = context.getRequest().getParameterString("resource");
		int dauer = context.getRequest().getParameterInt("dauer");
		int preis = context.getRequest().getParameterInt("preis");
		int menge = context.getRequest().getParameterInt("menge");
		
		org.hibernate.Session db = context.getDB();
		
		if( (ship == 0) && ((resource.length() == 0) || resource.equals("-1") ) ) {
			echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
			echo.append("Schiffe:\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table width=\"300\">\n");
			echo.append("<tr><td width=\"60\">Schifftyp:</td><td>");
			List<ShipType> shipTypes = Common.cast(db.createQuery("from ShipType").list());
			HtmlUtils.select(echo, "ship", false, shipTypes.stream().collect(Collectors.toMap(ShipType::getId, (st) -> st)), null);
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Dauer:</td><td>");
			HtmlUtils.textInput(echo, "dauer", false, Integer.class, 30);
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Gebot:</td><td>");
			HtmlUtils.textInput(echo, "preis", false, Integer.class, null);
			echo.append("</td></tr>\n");
			echo.append("<tr><td colspan=\"2\" align=\"center\">\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"einf&uuml;gen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("</div>");
			echo.append("<br />\n");

			echo.append("<div class='gfxbox adminEditor' style='width:700px'>");
			echo.append("Resourcen:\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table width=\"300\">\n");
			echo.append("<tr><td width=\"60\">Item:</td><td>");
			HtmlUtils.select(echo, "resource", false, Resources.getResourceList().getResourceList().stream().collect(Collectors.toMap((r) -> r.getId().getItemID(), (r) -> r)), null);
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Menge:</td><td>");
			HtmlUtils.textInput(echo, "menge", false, Integer.class, 1);
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Dauer:</td><td>");
			HtmlUtils.textInput(echo, "dauer", false, Integer.class, 30);
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Gebot:</td><td>");
			HtmlUtils.textInput(echo, "preis", false, Integer.class, null);
			echo.append("</td></tr>\n");
			echo.append("<tr><td colspan=\"2\" align=\"center\">\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"einf&uuml;gen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("</div>");
		}
		else if( ship != 0 ) {
			int tick = context.get(ContextCommon.class).getTick();

			User gtu = (User)db.get(User.class, -2);
			ShipType type = (ShipType)db.get(ShipType.class, ship);
			
			VersteigerungSchiff verst = new VersteigerungSchiff(gtu, type, preis);
			verst.setTick(tick+dauer);
			db.persist(verst);

			echo.append("Schiff eingef&uuml;gt<br />");
		}
		else if( resource.length() > 0 ) {
			int tick = context.get(ContextCommon.class).getTick();

			Cargo cargo = new Cargo();
			cargo.addResource( new ItemID(Integer.parseInt(resource)), menge );

			User gtu = (User)db.get(User.class, -2);
			
			VersteigerungResource verst = new VersteigerungResource(gtu, cargo, preis);
			verst.setTick(tick+dauer);
			db.persist(verst);

			echo.append("Resource eingef&uuml;gt<br />");
		}	
	}
}
