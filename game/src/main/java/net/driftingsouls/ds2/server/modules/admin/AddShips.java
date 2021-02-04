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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.NoSuchWeaponException;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ermoeglicht das Erstellen von Schiffen.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Schiffe", name="hinzufügen", permission = WellKnownAdminPermission.ADD_SHIPS)
@Component
public class AddShips implements AdminPlugin {
	@PersistenceContext
	private EntityManager em;

	private final JavaSession javaSession;
	private final ShipActionService shipActionService;

	public AddShips(JavaSession javaSession, ShipActionService shipActionService) {
		this.javaSession = javaSession;
		this.shipActionService = shipActionService;
	}

	@Override
	public void output(StringBuilder echo) {
		Context context = ContextMap.getContext();
		User user = (User)javaSession.getUser();

		int shipTypeId = context.getRequest().getParameterInt("ship");
		int count = context.getRequest().getParameterInt("count");

		if( shipTypeId == 0 ) {

			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("var shipdata = new Array();\n");

			Set<String> knownwpntypes = new HashSet<>();

			List<ShipType> shipTypes = em.createQuery("from ShipType order by id", ShipType.class).getResultList();
			for(ShipType shipType: shipTypes) {
				echo.append("shipdata[").append(shipType.getId()).append("] = Array();\n");
				echo.append("shipdata[").append(shipType.getId()).append("][0] = ").append(shipType.getJDocks()).append(";\n");
				echo.append("shipdata[").append(shipType.getId()).append("][1] = Array();\n");

				Set<String> thisammolist = new HashSet<>();
				int i = 0;
				Map<String,Integer> weapons = shipType.getWeapons();
				for( String weapon : weapons.keySet() ) {
					try {
						Weapons.get().weapon(weapon);
					}
					catch( NoSuchWeaponException e ) {
						continue;
					}
					for( String ammotype : Weapons.get().weapon(weapon).getMunitionstypen() )
					{
						knownwpntypes.add(ammotype);
						if( !thisammolist.contains(ammotype) ) {
							thisammolist.add(ammotype);
							echo.append("shipdata[").append(shipType.getId()).append("][1][").append(i++).append("] = \"").append(ammotype).append("\";\n");
						}
					}
				}
			}

			echo.append("var ammodata = Array();\n");
			int i = 0;
			for( String ammo : knownwpntypes ) {
				echo.append("ammodata[").append(i++).append("] = \"").append(ammo).append("\"\n");
			}

			echo.append("-->\n");
			echo.append("</script>\n");
			echo.append("<script src=\"./data/javascript/modules/admin.addships.js\" type=\"text/javascript\"></script>\n");

			echo.append("<div class='gfxbox' style='width:560px'>");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" style=\"width:80px\">Schifftyp:</td>");
			echo.append("<td class=\"noBorderX\">");
			echo.append("<select name=\"ship\" size=\"1\" onchange=\"Admin_AddShips.shipSelectChange(this.options[this.options.selectedIndex].value)\">");
			for( i=0; i < shipTypes.size(); i++ ) {
				echo.append("<option value=\"").append(shipTypes.get(i).getId()).append("\">").append(Common._plaintitle(shipTypes.get(i).getNickname())).append(" (").append(shipTypes.get(i).getId()).append(")</option>\n");
			}
			echo.append("</select>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Spieler:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"owner\" size=\"6\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Pos:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<input type=\"text\" name=\"system\" size=\"3\" />:<input type=\"text\" name=\"x\" size=\"3\" />/<input type=\"text\" name=\"y\" size=\"3\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Menge:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"count\" size=\"20\" value=\"1\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Name:</td>\n");
			echo.append("<td class=\"noBorderX\"><input type=\"text\" name=\"name\" size=\"20\" value=\"noname\" /></td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_ammo\">\n");
			echo.append("<td class=\"noBorderX\" style=\"vertial-align:top\">Munition:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<div id=\"tbl_ammo_div\" style=\"margin:0px;border:0px;padding:0px;position:relative;top:0px;left:0px;\">\n");
			for( String ammo : knownwpntypes ) {
				echo.append("<div style=\"margin:0px;border:0px;padding:0px;display:inline;position:absolute;top:0px;left:0px\" id=\"select_ammo_").append(ammo).append("\">\n");
				echo.append("<select id=\"select_ammo_").append(ammo).append("_element\" name=\"ammo_").append(ammo).append("\" size=\"1\">\n");
				echo.append("<option id=\"0\">[Nichts]</option>\n");
				List<Munition> ammos = em.createQuery("from Munition where munitionsdefinition.type = :ammo", Munition.class)
					.setParameter("ammo", ammo)
					.getResultList();
				for(Munition ammoObj: ammos) {
					echo.append("<option value=\"").append(ammoObj.getID()).append("\">").append(Common._plaintitle(ammoObj.getName())).append("</option>\n");
				}
				echo.append("</select>\n");
				echo.append("</div>\n");
			}
			echo.append("</div>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">Offizier (Typ,Name):</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<select name=\"offitype\" size=\"1\">\n");
			echo.append("<option value=\"-1\" selected=\"selected\">keiner</option>\n");
			List<OrderableOffizier> orderOffiList = em.createQuery("from OrderableOffizier", OrderableOffizier.class).getResultList();
			for (Object anOrderOffiList : orderOffiList) {
				OrderableOffizier offi = (OrderableOffizier) anOrderOffiList;
				echo.append("<option value=\"").append(offi.getId()).append("\">").append(offi.getName()).append("</option>\n");
			}
			echo.append("</select>\n");
			echo.append("<input type=\"text\" name=\"offiname\" value=\"Captain\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_jaeger\" style=\"visibility:visible\">\n");
			echo.append("<td class=\"noBorderX\">J&auml;ger:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<select name=\"jaeger\" size=\"1\" onchange=\"Admin_AddShips.jaegerSelectChange(this.options[this.options.selectedIndex].value)\">\n");
			echo.append("<option id=\"0\">[Nichts]</option>\n");
			List<ShipType> fighter = em.createQuery("from ShipType where locate(:jaeger, flags)!=0", ShipType.class)
				.setParameter("jaeger", ShipTypeFlag.JAEGER.getFlag())
				.getResultList();
			for(ShipType st: fighter) {
				echo.append("<option value=\"").append(st.getId()).append("\">").append(Common._plaintitle(st.getNickname())).append(" (").append(st.getId()).append(")</option>\n");
			}
			echo.append("</select>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr id=\"tbl_jaeger_ammo\" style=\"visibility:hidden\">\n");
			echo.append("<td class=\"noBorderX\" style=\"vertial-align:top\">Munition [J&auml;ger]:</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append("<div id=\"tbl_jaeger_ammo_div\" style=\"margin:0px;border:0px;padding:0px;position:relative;top:0px;left:0px;\">\n");
			for( String ammo : knownwpntypes ) {
				echo.append("<div style=\"margin:0px;border:0px;padding:0px;display:inline;position:absolute;top:0px;left:0px\" id=\"select_jaeger_ammo_").append(ammo).append("\">\n");
				echo.append("<select name=\"jaeger_ammo_").append(ammo).append("\" size=\"1\">\n");
				echo.append("<option id=\"0\">[Nichts]</option>\n");
				List<Munition> ammos = em.createQuery("from Munition where munitionsdefinition.type = :ammo", Munition.class)
					.setParameter("ammo", ammo)
					.getResultList();
				for(Munition ammoObj: ammos) {
					echo.append("<option value=\"").append(ammoObj.getID()).append("\">").append(Common._plaintitle(ammoObj.getName())).append("</option>\n");
				}
				echo.append("</select>\n");
				echo.append("</div>\n");
			}
			echo.append("</div>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\">\n");
			echo.append("<input type=\"checkbox\" name=\"inteliid\" id=\"form_inteliid\" value=\"1\" /><label for=\"form_inteliid\">Erste freie ID verwenden</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\">\n");
			echo.append("<input type=\"checkbox\" name=\"lowid\" id=\"form_lowid\" value=\"1\" /><label for=\"form_lowid\">Low ID (&lt;10000 - nur wenn es WIRKLICH notwendig ist)</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" colspan=\"2\" id=\"tbl_jaeger_lowid\" style=\"visibility:hidden\">\n");
			echo.append("<input type=\"checkbox\" name=\"jlowid\" id=\"form_jlowid\" value=\"1\" /><label for=\"form_jlowid\">Low ID f&uuml;r J&auml;ger (&lt;10000 - s.o.)</label>\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" colspan=\"2\" align=\"center\">\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<br /><input type=\"submit\" value=\"senden\" style=\"width:200px\" />\n");
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("<script type=\"text/javascript\">\n");
			echo.append("<!--\n");
			echo.append("Admin_AddShips.shipSelectChange(1);\n");
			echo.append("-->\n");
			echo.append("</script>\n");
			echo.append("</div>");
		}
		else if( count > 0 ) {
			final int ownerId = context.getRequest().getParameterInt("owner");
			int system = context.getRequest().getParameterInt("system");
			int x = context.getRequest().getParameterInt("x");
			int y = context.getRequest().getParameterInt("y");
			String name = context.getRequest().getParameterString("name");
			int offitype = context.getRequest().getParameterInt("offitype");
			String offiname = context.getRequest().getParameterString("offiname");
			final int jaegerTypeId = context.getRequest().getParameterInt("jaeger");
			int inteliid = context.getRequest().getParameterInt("inteliid");
			int lowid = context.getRequest().getParameterInt("lowid");
			int jlowid = context.getRequest().getParameterInt("jlowid");

			String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());

			ShipType shiptype = em.find(ShipType.class, shipTypeId);
			Cargo cargo = new Cargo();
			cargo.addResource( Resources.DEUTERIUM, shiptype.getRd() * 10L );
			cargo.addResource( Resources.URAN, shiptype.getRu() * 10L );
			cargo.addResource( Resources.ANTIMATERIE, shiptype.getRa() * 10L );

			Map<String,Integer> weapons = shiptype.getWeapons();
			for( String weapon : weapons.keySet() ) {
				for( String ammotype : Weapons.get().weapon(weapon).getMunitionstypen() )
				{
					if (context.getRequest().getParameterInt("ammo_" + ammotype) > 0)
					{
						cargo.addResource(
								new ItemID(context.getRequest().getParameterInt("ammo_" + ammotype)),
								weapons.get(weapon) * 10L);
					}
				}
			}
			for( int i=0; i < count; i++ ) {
				User auser = em.find(User.class, ownerId);
				String history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getId()+") [hide]Admin: "+user.getId()+"[/hide]";

				Ship ship = new Ship(auser, shiptype, system, x, y);
				ship.getHistory().addHistory(history);
				ship.setName(name);
				ship.setHull(shiptype.getHull());
				ship.setEnergy(shiptype.getEps());
				ship.setCrew(shiptype.getCrew());
				ship.setShields(shiptype.getShields());
				ship.setCargo(cargo);
				ship.setNahrungCargo(shiptype.getNahrungCargo());
				ship.setAblativeArmor(shiptype.getAblativeArmor());
				ship.setEngine(100);
				ship.setWeapons(100);
				ship.setComm(100);
				ship.setSensors(100);

				// Schiff erstellen
				if( inteliid != 0 ) {
					int shouldId = 10000;
					if( lowid != 0 ) {
						shouldId = 1;
					}

					int shipid = (Integer)em.createNativeQuery("select newIntelliShipId(:shouldId)", Integer.class)
						.setParameter("shouldId", shouldId)
						.getSingleResult();

					ship.setId(shipid);
				}
				em.persist(ship);

				if( shiptype.getWerft() != 0 ) {
					ShipWerft werft = new ShipWerft(ship);
					em.persist(werft);
				}

				echo.append("<a href='ds?module=schiff&action=default&ship=").append(ship.getId()).append("'>Schiff (").append(ship.getId()).append(")</a> hinzugef&uuml;gt<br />");

				// Offizier einfuegen
				if( (offitype > 0) && (offiname.length() > 0) &&
						((ship.getTypeData().getSize() > ShipType.SMALL_SHIP_MAXSIZE) ||
								ship.getTypeData().getShipClass() == ShipClasses.RETTUNGSKAPSEL) ) {
					OrderableOffizier offi = em.find(OrderableOffizier.class, offitype);
					if( offi != null ) {
						Offizier offizier = new Offizier(auser, offiname);
						offizier.setRang(offi.getRang());
						offizier.setAbility(Offizier.Ability.ING, offi.getIng());
						offizier.setAbility(Offizier.Ability.WAF, offi.getWaf());
						offizier.setAbility(Offizier.Ability.NAV, offi.getNav());
						offizier.setAbility(Offizier.Ability.SEC, offi.getSec());
						offizier.setAbility(Offizier.Ability.COM, offi.getCom());
						offizier.stationierenAuf(ship);
						offizier.setSpecial(Offizier.Special.values()[ThreadLocalRandom.current().nextInt(Offizier.Special.values().length)]);
						em.persist(offizier);

						echo.append("Offizier '").append(offiname).append("' hinzugef&uuml;gt<br />\n");
					}
				}

				shipActionService.recalculateShipStatus(ship);

				// Jaeger einfuegen
				if( (jaegerTypeId > 0) && (shiptype.getJDocks()>0) ) {
					echo.append("F&uuml;ge J&auml;ger ein:<br />\n");
					ShipType jshiptype = em.find(ShipType.class, jaegerTypeId);

					ShipFleet fleet = new ShipFleet(name+"-Staffel");
					em.persist(fleet);

					Cargo jcargo = new Cargo();
					jcargo.addResource( Resources.DEUTERIUM, jshiptype.getRd()*10L );
					jcargo.addResource( Resources.URAN, jshiptype.getRu()*10L );
					jcargo.addResource( Resources.ANTIMATERIE, jshiptype.getRa()*10L );

					weapons = jshiptype.getWeapons();
					for( String weapon : weapons.keySet() ) {
						for (String ammotype : Weapons.get().weapon(weapon).getMunitionstypen())
						{
							if( context.getRequest().getParameterInt("jaeger_ammo_"+ammotype) > 0 )	{
								jcargo.addResource(
										new ItemID(context.getRequest().getParameterInt("jaeger_ammo_"+ammotype)),
										weapons.get(weapon) * 10L);
							}
						}
					}

					for( int j=1; j <= shiptype.getJDocks(); j++ ) {
						history = "Indienststellung am "+currentTime+" durch "+auser.getName()+" ("+auser.getId()+") [hide]Admin: "+user.getId()+"[/hide]";

						Ship jaeger = new Ship(auser, jshiptype, system, x, y);
						jaeger.getHistory().addHistory(history);

						jaeger.setName(name+" "+j);
						jaeger.setHull(jshiptype.getHull());
						jaeger.setEnergy(jshiptype.getEps());
						jaeger.setCrew(jshiptype.getCrew());
						jaeger.setShields(jshiptype.getShields());
						jaeger.setDocked("l "+ship.getId());
						jaeger.setFleet(fleet);
						jaeger.setCargo(jcargo);
						jaeger.setAblativeArmor(jshiptype.getAblativeArmor());
						jaeger.setEngine(100);
						jaeger.setWeapons(100);
						jaeger.setComm(100);
						jaeger.setSensors(100);

						// Jaeger erstellen
						if( inteliid != 0 ) {
							int shouldId = 10000;
							if( jlowid != 0 ) {
								shouldId = 1;
							}

							jaeger.setId(shouldId);
						}
						em.persist(jaeger);

						shipActionService.recalculateShipStatus(jaeger);

						echo.append("J&auml;ger (").append(jaeger.getId()).append(") hinzugef&uuml;gt<br />");
					} // For jdocks
				} // if Jaeger

			} // For Schiffe
		} // if Schiffe erstellen
	}
}
