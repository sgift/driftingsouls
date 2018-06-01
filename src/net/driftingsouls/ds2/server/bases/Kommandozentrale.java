/*
# *	Drifting Souls 2
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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Factory;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.statistik.StatVerkaeufe;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.werften.BaseWerft;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Die Kommandozentrale.
 * @author Christopher Jung
 *
 */
@Entity(name="KommandozentraleBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Kommandozentrale")
public class Kommandozentrale extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Kommandozentrale.
	 */
	public Kommandozentrale() {
		// EMPTY
	}

	@Override
	public void cleanup(Context context, Base base, int building) {
		super.cleanup(context, base, building);
		org.hibernate.Session db = context.getDB();

		// Loesche alle GTU-Aktionen
		base.setAutoGTUActs(new ArrayList<>());
		// Setze Besitzer auf 0
		User nullUser = (User)context.getDB().get(User.class, 0);
		User oldUser = base.getOwner();
		base.setOwner(nullUser);
		// Fahre Basis runter
		Integer[] active = base.getActive();
		for(int i = 0; i < active.length; i++)
		{
			active[i] = 0;
		}

		base.setActive(active);
		base.setCoreActive(false);

		// Loesche Forschung
		Forschungszentrum zentrum = base.getForschungszentrum();

		if(zentrum != null)
		{
			zentrum.setForschung(null);
			zentrum.setDauer(0);
		}

		// Ueberstelle Offiziere
		db.createQuery("update Offizier set owner=:owner where stationiertAufBasis=:dest")
		  .setEntity("owner", nullUser)
		  .setEntity("dest", base)
		  .executeUpdate();

		// Loesche Verbindungen
		db.createQuery("update ShipWerft set linked=null where linked=:base")
			.setEntity("base", base)
			.executeUpdate();

		// Loesche Eintraege der Basiswerft
		BaseWerft werft = base.getWerft();

		if( werft != null)
		{
			werft.clearQueue();
		}

		// Fabriken abschalten
		Set<Factory> factories = base.getFactories();

		for(Factory factory : factories)
		{
			factory.setProduces(new Factory.Task[0]);
		}

		// Auftraege der Kaserne loeschen
		Kaserne kaserne = (Kaserne)db.createQuery("from Kaserne where base=:base")
			.setEntity("base", base)
			.uniqueResult();

		if( kaserne != null ) {
			for( KaserneEntry entry : kaserne.getQueueEntries() )
			{
				db.delete(entry);
			}
		}

		// Auftraege der Akademie loeschen
		Academy academy = base.getAcademy();

		if( academy != null)
		{
			academy.getQueueEntries().clear();
		}

		//Check if we need to change the drop zone of the player to another system
		Set<Integer> systems = oldUser.getAstiSystems();

		if(!systems.contains(oldUser.getGtuDropZone()))
		{
			int defaultDropZone = new ConfigService().getValue(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);
			if(oldUser.getGtuDropZone() != defaultDropZone)
			{
				PM.send(nullUser, oldUser.getId(), "GTU Dropzone ge&auml;ndert.", "Sie haben ihren letzten Asteroiden in System "+ oldUser.getGtuDropZone() +" aufgegeben. Ihre GTU Dropzone wurde auf System "+ defaultDropZone +" gesetzt.");
				oldUser.setGtuDropZone(defaultDropZone);
			}
		}
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		return "<a class=\"back tooltip\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[K]<span class='ttcontent'>"+this.getName()+"</span></a>";
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();

		String show = context.getRequest().getParameter("show");
		if( show == null ) {
			show = "";
		}
		String baction = context.getRequest().getParameterString("baction");

		if( !show.equals("general") && !show.equals("autogtu") ) {
			show = "general";
		}

		TemplateViewResultFactory templateViewResultFactory = context.getBean(TemplateViewResultFactory.class, null);
		TemplateEngine t = templateViewResultFactory.createEmpty();
		if( !t.setFile( "_BUILDING", "buildings.kommandozentrale.html" ) )
		{
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		t.setVar(	"base.name",	base.getName(),
					"base.id",		base.getId(),
					"base.field",	field,
					"base.system",	base.getSystem(),
					"base.size",	base.getSize());

		GtuWarenKurse kurseRow = (GtuWarenKurse)db.get(GtuWarenKurse.class, "asti");
		Cargo kurse = new Cargo(kurseRow.getKurse());
		kurse.setOption( Cargo.Option.SHOWMASS, false );

		Cargo cargo = new Cargo(base.getCargo());

		StringBuilder message = new StringBuilder();

		/*
			Resourcen an die GTU verkaufen
		 */

		if( baction.equals("sell") ) {
			BigInteger totalRE = BigInteger.ZERO;

			int tick = context.get(ContextCommon.class).getTick();
			int system = base.getSystem();

			StatVerkaeufe statsRow = (StatVerkaeufe)db.createQuery("from StatVerkaeufe where tick=:tick and place=:place and system=:sys")
				.setInteger("tick", tick)
				.setString("place", "asti")
				.setInteger("sys", system)
				.uniqueResult();
			Cargo stats = new Cargo();
			if( statsRow != null ) {
				stats = new Cargo(statsRow.getStats());
			}

			boolean changed = false;

			ResourceList reslist = kurse.getResourceList();
			for( ResourceEntry res : reslist ) {
				long tmp = context.getRequest().getParameterInt(res.getId()+"to");

				if( tmp > 0 ) {
					if( tmp > cargo.getResourceCount(res.getId()) ) {
						tmp = cargo.getResourceCount(res.getId());
					}

					if( tmp <= 0 ) {
						continue;
					}

					BigDecimal get = BigDecimal.valueOf(tmp).multiply(new BigDecimal(res.getCount1()/1000d));

					message.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(Common.ln(tmp)).append(" f&uuml;r ").append(Common.ln(get)).append(" RE verkauft<br />\n");
					totalRE = totalRE.add(get.toBigInteger());

					changed = true;
					cargo.substractResource(res.getId(), tmp);
					stats.addResource(res.getId(), tmp);
				}
			}
			if( changed ) {
				if( statsRow == null ) {
					statsRow = new StatVerkaeufe(tick, system, "asti");
					statsRow.setStats(stats);
					db.persist(statsRow);
				}
				else {
					statsRow.setStats(stats);
				}

				base.setCargo(cargo);
				user.transferMoneyFrom(Faction.GTU, totalRE, "Warenverkauf Asteroid "+base.getId()+" - "+base.getName(), false, UserMoneyTransfer.Transfer.SEMIAUTO );

				message.append("<br />");
			}
		}

		/*
			Batterien aufladen
		*/

/*		if( baction.equals("load") ) {
			long load = context.getRequest().getParameterInt("load");

			if( load < 0 ) {
				load *= -1;
			}

			int e = base.getEnergy();

			if( load > e ) {
				load = e;
			}
			if( load > cargo.getResourceCount( Resources.LBATTERIEN ) ) {
				load = cargo.getResourceCount( Resources.LBATTERIEN );
			}

			if( load != 0 ) {
				message.append(Common._plaintitle(base.getName())).append(" l&auml;dt <img src=\"").append(Cargo.getResourceImage(Resources.LBATTERIEN)).append("\" alt=\"\" />").append(load).append(" ").append(Cargo.getResourceName(Resources.LBATTERIEN)).append(" auf<br/ ><br />\n");

				cargo.substractResource( Resources.LBATTERIEN, load );
				cargo.addResource( Resources.BATTERIEN, load );
				e -= load;

				base.setCargo(cargo);
				base.setEnergy(e);
			}
		}*/

		/*
			Batterien entladen
		*/

		/*if( baction.equals("unload") ) {
			long unload = context.getRequest().getParameterInt("unload");

			if( unload < 0 ) {
				unload *= -1;
			}

			int e = base.getEnergy();

			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN );
			}

			if( unload != 0 ) {
				message.append(Common._plaintitle(base.getName())).append(" entl&auml;dt <img src=\"").append(Cargo.getResourceImage(Resources.BATTERIEN)).append("\" alt=\"\" />").append(unload).append(" ").append(Cargo.getResourceName(Resources.BATTERIEN)).append("<br/ ><br />\n");

				cargo.substractResource( Resources.BATTERIEN, unload );
				cargo.addResource( Resources.LBATTERIEN, unload );
				e += unload;

				base.setCargo(cargo);
				base.setEnergy(e);
			}
		}*/

		/*
			Batterien herstellen
		*/

		/*if( baction.equals("create") ) {
			long create = context.getRequest().getParameterInt("create");

			if( create > cargo.getResourceCount( Resources.PLATIN ) ) {
				create = cargo.getResourceCount( Resources.PLATIN );
			}

            if(create < 0) {
                create = Math.abs(create);
            }

			if( create != 0 ) {
				message.append(Common._plaintitle(base.getName())).append(" produziert <img src=\"").append(Cargo.getResourceImage(Resources.LBATTERIEN)).append("\" alt=\"\" />").append(create).append(" ").append(Cargo.getResourceName(Resources.LBATTERIEN)).append("<br/ ><br />\n");

				cargo.substractResource( Resources.PLATIN, create );
				cargo.addResource( Resources.LBATTERIEN, create );

				base.setCargo(cargo);
			}
		}*/

		/*
			Allyitems an die Allianz ueberstellen
		*/

		if( baction.equals("item") ) {
			int itemid = context.getRequest().getParameterInt("item");

			Ally ally = user.getAlly();
			Item item = (Item)db.get(Item.class, itemid);

			if( ally == null ) {
				message.append("Sie sind in keiner Allianz<br /><br />\n");
			}
			else if( item == null || item.getEffect().hasAllyEffect() ) {
				message.append("Kein passenden Itemtyp gefunden<br /><br />\n");
			}
			else if( !cargo.hasResource( new ItemID(itemid) ) ) {
				message.append("Kein passendes Item vorhanden<br /><br />\n");
			}
			else {
				Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, ally.getItems() );
				allyitems.addResource( new ItemID(itemid), 1 );
				cargo.substractResource( new ItemID(itemid), 1 );

				ally.setItems(allyitems.save());
				base.setCargo(cargo);

				String msg = "Ich habe das Item \""+item.getName()+"\" der Allianz zur Verf&uuml;gung gestellt.";
				PM.sendToAlly(user, ally, "Item &uuml;berstellt", msg);

				message.append("Das Item wurde an die Allianz &uuml;bergeben<br /><br />\n");
			}
		}

		/*
			Einen Auto-Verkauf Eintrag hinzufuegen
		*/
		if( baction.equals("gtuadd") ) {
			ResourceID resid = Resources.fromString(context.getRequest().getParameterString("resid"));

			int actid = context.getRequest().getParameterInt("actid");
			int count = context.getRequest().getParameterInt("count");

			if( (actid >= 0) && (actid <= 1 ) && (count != 0 || (actid == 1)) ) {
				BaseStatus basedata = Base.getStatus(base);
				Cargo stat = (Cargo)basedata.getProduction().clone();
				stat.setResource(Resources.NAHRUNG, 0);

				if( stat.getResourceCount(resid) != 0 && kurse.getResourceCount(resid) != 0 ) {
					List<AutoGTUAction> acts = base.getAutoGTUActs();
					acts.add(new AutoGTUAction(resid,actid,count));
					base.setAutoGTUActs(acts);

					message.append("Automatischer Verkauf von <img style=\"vertical-align:middle\" src=\"").append(Cargo.getResourceImage(resid)).append("\" alt=\"\" />").append(Cargo.getResourceName(resid)).append(" hinzugef&uuml;gt<br /><br />\n");
				}
			}
		}

		/*
			Einen Auto-Verkauf Eintrag entfernen
		*/
		if( baction.equals("gtudel") ) {
			String gtuact = context.getRequest().getParameterString("gtuact");

			if( gtuact.length() != 0 ) {
				List<AutoGTUAction> autoactlist = base.getAutoGTUActs();

				for( AutoGTUAction autoact : autoactlist ) {
					if( gtuact.equals(autoact.toString()) ) {
						autoactlist.remove(autoact);
						message.append("Eintrag entfernt<br /><br />\n");

						break;
					}
				}
				base.setAutoGTUActs(autoactlist);
			}
		}

		t.setVar("building.message", message.toString());

		if( show.equals("general") ) {
			t.setVar("show.general", 1);

			t.setBlock("_BUILDING", "general.itemconsign.listitem", "general.itemconsign.list");
			t.setBlock("_BUILDING", "general.shiptransfer.listitem", "general.shiptransfer.list");
			t.setBlock("_BUILDING", "general.basetransfer.listitem", "general.basetransfer.list");
			t.setBlock("_BUILDING", "general.sell.listitem", "general.sell.list");

			t.setVar(	"res.batterien.image",	Cargo.getResourceImage(Resources.BATTERIEN),
						"res.lbatterien.image",	Cargo.getResourceImage(Resources.LBATTERIEN),
						"res.platin.image",		Cargo.getResourceImage(Resources.PLATIN) );

			List<ItemCargoEntry> itemlist = cargo.getItems();
			if( itemlist.size() != 0 ) {
				Ally ally = user.getAlly();
				if( ally != null ) {
					for( ItemCargoEntry item : itemlist ) {
						Item itemobject = item.getItem();
						if( itemobject.getEffect().hasAllyEffect() ) {
							t.setVar(	"item.id",		item.getItemID(),
										"item.name",	itemobject.getName() );
							t.parse("general.itemconsign.list", "general.itemconsign.listitem", true);
						}
					}
				}
			}

			/*
				Waren zu Schiffen/Basen im Orbit transferieren
			*/

			List<?> ships = db.createQuery("from Ship " +
					"where id>:minid and (x between :minx and :maxx) and " +
							"(y between :miny and :maxy) and " +
							"system= :sys and locate('l ',docked)=0 and battle is null " +
					"order by x,y,owner.id,id")
				.setInteger("minid", 0)
				.setInteger("minx", base.getX()-base.getSize())
				.setInteger("maxx", base.getX()+base.getSize())
				.setInteger("miny", base.getY()-base.getSize())
				.setInteger("maxy", base.getY()+base.getSize())
				.setInteger("sys", base.getSystem())
				.list();
			if( !ships.isEmpty() ) {
				Location oldLoc = null;

				for (Object ship1 : ships)
				{
					Ship ship = (Ship) ship1;

					if (oldLoc == null)
					{
						oldLoc = ship.getLocation();

						if (base.getSize() != 0)
						{
							t.setVar("ship.begingroup", 1);
						}
					}
					else if (!oldLoc.equals(ship.getLocation()))
					{
						oldLoc = ship.getLocation();

						if (base.getSize() != 0)
						{
							t.setVar("ship.begingroup", 1,
									"ship.endgroup", 1);
						}
					}
					else
					{
						t.setVar("ship.begingroup", 0,
								"ship.endgroup", 0);
					}

					t.setVar("ship.id", ship.getId(),
							"ship.name", Common._plaintitle(ship.getName()),
							"ship.x", ship.getX(),
							"ship.y", ship.getY());

					if (ship.getOwner().getId() != user.getId())
					{
						t.setVar("ship.owner.name", Common.escapeHTML(ship.getOwner().getPlainname()));
					}
					else
					{
						t.setVar("ship.owner.name", "");
					}

					t.parse("general.shiptransfer.list", "general.shiptransfer.listitem", true);
				}
			}

			List<?> targetbases = db.createQuery("from Base where x= :x and y= :y and system= :sys and id!= :id and owner= :owner")
				.setInteger("x", base.getX())
				.setInteger("y", base.getY())
				.setInteger("sys", base.getSystem())
				.setInteger("id", base.getId())
				.setEntity("owner", base.getOwner())
				.list();
			for (Object targetbase1 : targetbases)
			{
				Base targetbase = (Base) targetbase1;

				t.setVar("targetbase.id", targetbase.getId(),
						"targetbase.name", Common._plaintitle(targetbase.getName()));
				t.parse("general.basetransfer.list", "general.basetransfer.listitem", true);
			}


			/*
				Waren verkaufen
			*/
			ResourceList reslist = kurse.compare(cargo, false);
			for( ResourceEntry res : reslist ) {
				if( res.getCount2() == 0 ) {
					continue;
				}

				t.setVar(	"res.image",	res.getImage(),
							"res.name",		res.getName(),
							"res.cargo2",	res.getCargo2(),
							"res.id",		res.getId() );

				if( res.getCount1() <= 5 ) {
					t.setVar("res.cargo1", "Kein Bedarf");
				}
				else {
					t.setVar("res.cargo1", Common.ln(res.getCount1()/1000d)+" RE");
				}

				t.parse("general.sell.list", "general.sell.listitem", true);
			}
		}
		else if( show.equals("autogtu") ) {
			t.setVar("show.autogtu", 1);
			t.setBlock("_BUILDING", "autogtu.acts.listitem", "autogtu.acts.list");
			t.setBlock("_BUILDING", "autogtu.reslist.listitem", "autogtu.reslist.list");

			List<AutoGTUAction> autoactlist = base.getAutoGTUActs();
			for( AutoGTUAction autoact : autoactlist ) {
				if( (autoact.getActID() != 1) && (autoact.getCount() == 0) ) {
					continue;
				}

				t.setVar(	"res.image",		Cargo.getResourceImage(autoact.getResID()),
							"res.name",			Cargo.getResourceName(autoact.getResID()),
							"res.sellcount"	,	Common.ln(autoact.getCount()),
							"res.action.total",	autoact.getActID() == 0,
							"res.action.limit",	autoact.getActID() == 1,
							"res.actionstring",	autoact.toString() );

				t.parse("autogtu.acts.list", "autogtu.acts.listitem", true);
			}

			BaseStatus basedata = Base.getStatus(base);
			Cargo stat = (Cargo)basedata.getProduction().clone();
			stat.setResource( Resources.NAHRUNG, 0 );
			stat.setOption( Cargo.Option.NOHTML, true );
			ResourceList reslist = stat.compare(kurse, false);
			for( ResourceEntry res : reslist ) {
				if( (res.getCount1() > 0) && (res.getCount2() > 0) ) {
					t.setVar(	"res.id",	res.getId(),
								"res.name",	res.getName() );
					t.parse("autogtu.reslist.list", "autogtu.reslist.listitem", true);
				}
			}
		}

		t.parse( "OUT", "_BUILDING" );
		return t.getVar("OUT");
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
