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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.springframework.stereotype.Component;

/**
 * Schiffsmodul fuer die Anzeige des Schiffscargos.
 * @author Christopher Jung
 *
 */
@Component
public class CargoDefault implements SchiffPlugin {
	@Action(ActionType.DEFAULT)
	public String action(Parameters caller,
			String max,
			String act,
			long unload,
			long load,
			boolean setautodeut,
			int autodeut,
			boolean setautomine,
			int automine,
			boolean setstartfighter,
			int startfighter,
			boolean setgotosecondrow,
			int gotosecondrow,
			long usenahrung,
			boolean setfeeding,
			boolean isfeeding,
			boolean setallyfeeding,
			boolean isallyfeeding,
			Ship othership) {
		Ship ship = caller.ship;

		String output = "";

		/*if( act.equals("load") ) {
			if( !max.equals("") ) {
				load = 10000;
			}

			Cargo cargo = ship.getCargo();

			int e = ship.getEnergy();
			if( load > e ) {
				load = e;
			}
			if( load > cargo.getResourceCount( Resources.LBATTERIEN ) ) {
				load = cargo.getResourceCount( Resources.LBATTERIEN );
			}
			if( load < 0 ) {
				load = 0;
			}

			output += Common._plaintitle(ship.getName())+" l&auml;dt "+load+" "+Cargo.getResourceName(Resources.BATTERIEN)+" auf<br /><br />\n";
			cargo.addResource( Resources.BATTERIEN, load );
			cargo.substractResource( Resources.LBATTERIEN, load );

			ship.setEnergy((int)(ship.getEnergy() - load));
			ship.setCargo(cargo);
		}
		else if( act.equals("unload") ) {
			if( !max.equals("") ) {
				unload = 10000;
			}

			int maxeps = caller.shiptype.getEps();

			Cargo cargo = ship.getCargo();

			int e = ship.getEnergy();
			if( (unload + e) > maxeps ) {
				unload = maxeps - e;
			}
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN );
			}
			if( unload < 0 ) {
				unload = 0;
			}

			output += ship.getName()+" entl&auml;dt "+unload+" "+Cargo.getResourceName(Resources.BATTERIEN)+"<br /><br />\n";
			cargo.substractResource( Resources.BATTERIEN, unload );
			cargo.addResource( Resources.LBATTERIEN, unload );

			ship.setEnergy((int)(ship.getEnergy() + unload));
			ship.setCargo(cargo);
		}*/
		if( act.equals("usenahrung"))
		{
			if( !max.equals("") )
			{
				usenahrung = 1000000000;
			}
			if(usenahrung > 0)
			{
				long maxnahrung = ship.getTypeData().getNahrungCargo();
				Cargo cargo = ship.getCargo();
				if( (usenahrung + ship.getNahrungCargo()) > maxnahrung)
				{
					usenahrung = maxnahrung - ship.getNahrungCargo();
				}
				if(usenahrung > cargo.getResourceCount(Resources.NAHRUNG))
				{
					usenahrung = cargo.getResourceCount(Resources.NAHRUNG);
				}
				ship.setNahrungCargo(ship.getNahrungCargo()+usenahrung);
				cargo.substractResource(Resources.NAHRUNG, usenahrung);

				ship.setCargo(cargo);
				output += usenahrung + " Nahrung in den Speicher transferiert.<br />";
			}
		}
		else if( act.equals("feedcargo"))
		{
			if(othership == null)
			{
				output += "<span style=\"color:red\">Sie m&uuml;ssen auch eine korrekte ID angeben.</span><br />\n";
				return output;
			}

			if( othership.getOwner().getId() != ship.getOwner().getId())
			{
				output += "<span style=\"color:red\">Sie k&ouml;nnen nur von eigenen Schiffen aufladen.</span><br />\n";
				return output;
			}

			if( !othership.getLocation().equals(ship.getLocation()))
			{
				output += "<span style=\"color:red\">Die Schiffe m&uuml;ssen sich im selben Sektor befinden.</span><br />\n";
				return output;
			}

			usenahrung = ship.getTypeData().getNahrungCargo() - ship.getNahrungCargo();
			Cargo cargo = othership.getCargo();
			if(usenahrung > cargo.getResourceCount(Resources.NAHRUNG))
			{
				usenahrung = cargo.getResourceCount(Resources.NAHRUNG);
			}
			ship.setNahrungCargo(ship.getNahrungCargo()+usenahrung);
			cargo.substractResource(Resources.NAHRUNG, usenahrung);

			if( usenahrung > 0)
			{
				long feeding = usenahrung / 100000 + 1;
				for(int i=0; i < feeding; i++)
				{
					output += "*mampf*<br />";
				}
				output += "*Bauch streichel* Das tat gut.<br />";
			}

			othership.setCargo(cargo);
			output += usenahrung + " Nahrung in den Speicher transferiert.<br />";
		}
		else if( setautodeut ) {
			if( caller.shiptype.getDeutFactor() <= 0 ) {
				output += "<span style=\"color:red\">Nur Tanker k&ouml;nnen automatisch Deuterium sammeln</span><br />\n";
				return output;
			}

			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setAutoDeut(autodeut != 0);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Deuteriumsammeln "+(autodeut != 0 ? "":"de")+"aktiviert<br />\n";
		}
		else if( setautomine ) {
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setAutoMine(automine != 0);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Felsbrockenabbauen "+(automine != 0 ? "":"de")+"aktiviert<br />\n";
		}
		else if(setstartfighter)
		{
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setStartFighters(startfighter != 0);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Starten von J&auml;gern "+(startfighter != 0 ? "":"de")+"aktiviert<br />\n";
		}
		else if(setgotosecondrow)
		{
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setGotoSecondrow(gotosecondrow != 0);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Verlegen in die 2. Reihe "+(gotosecondrow != 0 ? "":"de")+"aktiviert<br />\n";
		}
		else if( setfeeding )
		{
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setFeeding(isfeeding);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Versorgen "+(isfeeding ? "":"de")+"aktiviert<br />\n";
		}
		else if( setallyfeeding )
		{
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setAllyFeeding(isallyfeeding);
			einstellungen.persistIfNecessary(ship);

			output += "Automatisches Versorgen von Allianzschiffen "+(isallyfeeding ? "":"de")+"aktiviert<br />\n";
		}

		return output;
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship ship = caller.ship;
		ShipTypeData shiptype = caller.shiptype;

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.cargo.default.html");

		Cargo cargo = ship.getCargo();
		cargo.setOption(Cargo.Option.LINKCLASS,"schiffwaren");

		t.setBlock("_CARGO","schiff.cargo.reslist.listitem","schiff.cargo.reslist.list");
		ResourceList reslist = cargo.getResourceList();
		Resources.echoResList( t, reslist, "schiff.cargo.reslist.list" );

		t.setVar(	"schiff.cargo.empty",					Common.ln(shiptype.getCargo()-cargo.getMass()),
					"global.pluginid",						pluginid,
					"ship.id",								ship.getId(),
					//"schiff.cargo.batterien",				cargo.hasResource( Resources.BATTERIEN ),
					//"schiff.cargo.lbatterien",				cargo.hasResource( Resources.LBATTERIEN ),
					"schiff.cargo.tanker",					shiptype.getDeutFactor(),
					"schiff.cargo.tanker.autodeut",			ship.getEinstellungen().getAutoDeut(),
					"schiff.cargo.miner",					      ship.getTypeData().getShipClass() == ShipClasses.MINER,
					"schiff.cargo.miner.automine",			ship.getEinstellungen().getAutoMine(),
					"schiff.cargo.traeger",					shiptype.getJDocks() > 0 ? 1 : 0,
					"schiff.cargo.traeger.startfighter",	ship.getEinstellungen().startFighters(),
					"schiff.cargo.secondrow",				shiptype.hasFlag(ShipTypeFlag.SECONDROW),
					"schiff.cargo.secondrow.gotosecondrow", ship.getEinstellungen().gotoSecondrow(),
					"schiff.cargo.versorger",				shiptype.hasFlag(ShipTypeFlag.VERSORGER),
					"schiff.cargo.versorger.isfeeding",		ship.getEinstellungen().isFeeding(),
					"schiff.cargo.versorger.isallyfeeding",	ship.getEinstellungen().isAllyFeeding(),
					"schiff.cargo.mangel_nahrung",			(ship.getStatus().contains("mangel_nahrung")),
					"schiff.cargo.speicher",				(shiptype.getNahrungCargo() > 0),
					"schiff.cargo.speicher.amount",			Common.ln(ship.getNahrungCargo()),
					"schiff.cargo.speicher.maxamount",		Common.ln(shiptype.getNahrungCargo()),
					"resource.RES_DEUTERIUM.image",			Cargo.getResourceImage(Resources.DEUTERIUM) );

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
