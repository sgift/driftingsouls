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
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.*;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.units.TransientUnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Transfer von Einheiten zwischen Basen und Schiffen.
 */
@Module(name = "unittausch")
public class UnitTauschController extends Controller
{
	private static class MultiTarget
	{
		private String name;
		private String targetlist;

		MultiTarget(String name, String targetlist)
		{
			this.name = name;
			this.targetlist = targetlist;
		}

		/**
		 * Gibt den Namen des MultiTargets zurueck.
		 *
		 * @return Der Name
		 */
		String getName()
		{
			return name;
		}

		/**
		 * Gibt eine |-separierte Liste mit Zielen zurueck.
		 *
		 * @return Liste der Ziele
		 */
		String getTargetList()
		{
			return targetlist;
		}
	}

	private abstract static class TransportFactory
	{
		TransportFactory()
		{
			//EMPTY
		}

		abstract List<TransportTarget> createTargets(int role, String target);
	}

	private static class BaseTransportFactory extends TransportFactory
	{
		BaseTransportFactory()
		{
			// EMPTY
		}

		@Override
		List<TransportTarget> createTargets(int role, String target)
		{
			List<TransportTarget> list = new ArrayList<>();

			int[] fromlist = Common.explodeToInt("|", target);
			for (int aFromlist : fromlist)
			{
				TransportTarget handler = new BaseTransportTarget();
				handler.create(role, aFromlist);
				if (list.size() > 0)
				{
					Location loc = list.get(0).getLocation();
					Location thisLoc = handler.getLocation();
					if (!loc.sameSector(list.get(0).getSize(), thisLoc, handler.getSize()))
					{
						continue;
					}
				}
				list.add(handler);
			}

			return list;
		}
	}

	private static class ShipTransportFactory extends TransportFactory
	{
		ShipTransportFactory()
		{
			// EMPTY
		}

		@Override
		List<TransportTarget> createTargets(int role, String target)
		{
			List<TransportTarget> list = new ArrayList<>();

			String[] fromlist = StringUtils.split(target, '|');
			for (String aFromlist : fromlist)
			{
				if (aFromlist.equals("fleet"))
				{
					if (list.size() == 0)
					{
						throw new ValidierungException("Es wurde kein Schiff angegeben, zu dem die Flotte ausgewaehlt werden soll");
					}
					ShipTransportTarget handler = (ShipTransportTarget) list.remove(list.size() - 1);
					if (handler.getFleet() == null)
					{
						throw new ValidierungException("Das angegebene Schiff befindet sich in keiner Flotte");
					}

					Session db = ContextMap.getContext().getDB();

					Location loc = handler.getLocation();

					List<?> fleetlist = db.createQuery("from Ship " +
							"where id>0 and fleet=:fleet and x=:x and y=:y and system=:sys")
							.setEntity("fleet", handler.getFleet())
							.setInteger("x", loc.getX())
							.setInteger("y", loc.getY())
							.setInteger("sys", loc.getSystem())
							.list();
					for (Object aFleetlist : fleetlist)
					{
						ShipTransportTarget shiphandler = new ShipTransportTarget();
						shiphandler.create(role, (Ship) aFleetlist);
						list.add(shiphandler);
					}
				}
				else
				{
					ShipTransportTarget handler = new ShipTransportTarget();
					handler.create(role, Integer.parseInt(aFromlist));
					if (list.size() > 0)
					{
						Location loc = list.get(0).getLocation();
						Location thisLoc = handler.getLocation();
						if (!loc.sameSector(list.get(0).getSize(), thisLoc, handler.getSize()))
						{
							continue;
						}
					}
					list.add(handler);
				}
			}

			return list;
		}
	}

	private abstract static class TransportTarget
	{
		@SuppressWarnings("unused")
		protected int role;
		protected int id;
		protected int owner;
		protected UnitCargo unitcargo;
		protected long maxUnitSpace;
		protected int maxunitsize;

		static final int ROLE_SOURCE = 1;
		static final int ROLE_TARGET = 1;

		/**
		 * Konstruktor.
		 */
		public TransportTarget()
		{
			// EMPTY
		}

		/**
		 * Erstellt ein neues TransportTarget.
		 *
		 * @param role Die Rolle (Source oder Target)
		 * @param id Die ID
		 */
		void create(int role, int id)
		{
			this.role = role;
			this.id = id;
		}

		/**
		 * Gibt die ID des Objekts zurueck.
		 *
		 * @return Die ID
		 */
		int getId()
		{
			return this.id;
		}

		/**
		 * Gibt den Radius des Objekts zurueck.
		 *
		 * @return Der Radius
		 */
		abstract int getSize();

		/**
		 * Gibt die ID des Besitzers zurueck.
		 *
		 * @return Die ID des Besitzers
		 */
		int getOwner()
		{
			return owner;
		}

		/**
		 * Setzt den Besitzer auf den angegebenen Wert.
		 *
		 * @param owner Der neue Besitzer
		 */
		void setOwner(int owner)
		{
			this.owner = owner;
		}

		/**
		 * Gibt den maximalen UnitSpace zurueck.
		 *
		 * @return Der maximale UnitSpace
		 */
		long getMaxUnitSpace()
		{
			return maxUnitSpace;
		}

		/**
		 * Gibt die maximale Einheitengroesze zurueck.
		 *
		 * @return Die maximale Einheitengroesze
		 */
		int getMaxUnitSize()
		{
			return maxunitsize;
		}

		/**
		 * Setzt die maximale Einheitengroesze.
		 *
		 * @param maxunitsize Die maximale Einheitengroesze
		 */
		void setMaxUnitSize(int maxunitsize)
		{
			this.maxunitsize = maxunitsize;
		}

		/**
		 * Setzt den maximalen UntSpace.
		 *
		 * @param unitspace der neue maximale UnitSpace
		 */
		void setMaxUnitSpace(long unitspace)
		{
			this.maxUnitSpace = unitspace;
		}

		/**
		 * Gibt die Einheiten zurueck.
		 *
		 * @return Der UnitCargo
		 */
		UnitCargo getUnits()
		{
			return unitcargo;
		}

		/**
		 * Setzt den UnitCargo auf den angegebenen Wert.
		 *
		 * @param unitcargo der neue UnitCargo
		 */
		void setUnitCargo(UnitCargo unitcargo)
		{
			this.unitcargo = unitcargo;
		}

		/**
		 * Gibt die Position des Objekts zurueck.
		 *
		 * @return Die Position
		 */
		abstract Location getLocation();

		/**
		 * Schreibt die Daten in die Datenbank.
		 */
		abstract void write();

		/**
		 * Gibt die MultiTarget-Variante zurueck.
		 * Wenn keine MultiTarget-Variante verfuegbar ist, so wird <code>null</code> zurueckgegeben
		 *
		 * @return Die MultiTarget-Variante oder <code>null</code>
		 */
		abstract MultiTarget getMultiTarget();

		/**
		 * Gibt den Namen des Target-Typen zurueck.
		 *
		 * @return Der Name
		 */
		abstract String getTargetName();

		/**
		 * Gibt den Namen des konkreten Objekts zurueck (z.B. der Name des Schiffes/der Basis).
		 *
		 * @return Der Name
		 */
		abstract String getObjectName();
	}

	private static class ShipTransportTarget extends TransportTarget
	{
		private Ship ship;

		/**
		 * Konstruktor.
		 */
		public ShipTransportTarget()
		{
			// EMPTY
		}

		void create(int role, Ship ship)
		{
			if ((ship == null) || (ship.getId() < 0))
			{
				throw new ValidierungException("Eines der angegebenen Schiffe existiert nicht");
			}

			super.create(role, ship.getId());

			if (ship.getBattle() != null)
			{
				throw new ValidierungException("Das Schiff (id:" + ship.getId() + ") ist in einen Kampf verwickelt");
			}

			if (role == ROLE_TARGET)
			{
				User user = (User) ContextMap.getContext().getActiveUser();
				if ((ship.getStatus().contains("disable_iff")) && (ship.getOwner() != user))
				{
					throw new ValidierungException("Zu dem angegebenen Schiff (id:" + ship.getId() + ") k&ouml;nnen sie keine Waren transportieren");
				}
			}

			ShipTypeData tmptype = ship.getTypeData();

			if (tmptype.hasFlag(ShipTypeFlag.KEIN_TRANSFER))
			{
				throw new ValidierungException("Sie k&ouml;nnen keine Einheiten zu oder von diesem Schiff (id:" + ship.getId() + ") transferieren");
			}

			setOwner(ship.getOwner().getId());
			setMaxUnitSpace(tmptype.getUnitSpace());
			setMaxUnitSize(tmptype.getMaxUnitSize());
			this.ship = ship;
			setUnitCargo(ship.getUnits());
		}

		@Override
		void create(int role, int shipid)
		{
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Ship ship = (Ship) db.get(Ship.class, shipid);

			create(role, ship);
		}

		@Override
		MultiTarget getMultiTarget()
		{
			ShipFleet fleet = ship.getFleet();
			if (fleet == null)
			{
				return null;
			}

			return new MultiTarget("Flotte", ship.getId() + "|fleet");
		}

		@Override
		String getTargetName()
		{
			return "Schiff";
		}

		@Override
		void write()
		{
			this.ship.setUnits(getUnits());
			this.ship.recalculateShipStatus();
		}

		@Override
		Location getLocation()
		{
			return this.ship.getLocation();
		}

		@Override
		String getObjectName()
		{
			return this.ship.getName();
		}

		@Override
		int getSize()
		{
			return 0;
		}

		/**
		 * Gibt die Flotte zurueck, zu der das Schiff gehoert.
		 *
		 * @return Die Flotte
		 */
		ShipFleet getFleet()
		{
			return this.ship.getFleet();
		}
	}

	private static class BaseTransportTarget extends TransportTarget
	{
		private Base base;

		/**
		 * Konstruktor.
		 */
		public BaseTransportTarget()
		{
			// EMPTY
		}

		@Override
		void create(int role, int baseid)
		{
			super.create(role, baseid);
			org.hibernate.Session db = ContextMap.getContext().getDB();

			Base base = (Base) db.get(Base.class, baseid);

			if (base == null)
			{
				throw new ValidierungException("Die angegebene Basis (id:" + baseid + ") existiert nicht");
			}

			setOwner(base.getOwner().getId());
			setMaxUnitSpace(-1);
			setMaxUnitSize(-1);

			setUnitCargo(base.getUnits());
			this.base = base;
		}

		@Override
		MultiTarget getMultiTarget()
		{
			return null;
		}

		@Override
		String getTargetName()
		{
			return "Basis";
		}

		@Override
		void write()
		{
			base.setUnits(getUnits());
		}

		@Override
		Location getLocation()
		{
			return base.getLocation();
		}

		@Override
		String getObjectName()
		{
			return base.getName();
		}

		@Override
		int getSize()
		{
			return base.getSize();
		}
	}

	private TemplateViewResultFactory templateViewResultFactory;
	private Map<String, TransportFactory> wayhandler;

	@Autowired
	public UnitTauschController(TemplateViewResultFactory templateViewResultFactory)
	{
		this.templateViewResultFactory = templateViewResultFactory;

		setPageTitle("Einheitentransfer");

		wayhandler = new HashMap<>();
		wayhandler.put("s", new ShipTransportFactory());
		wayhandler.put("b", new BaseTransportFactory());
	}

	private void validiereEinheitenKoennenZwischenQuelleUndZielTransferiertWerden(List<TransportTarget> from, List<TransportTarget> to, String fromKey, String toKey)
	{
		if ((from.size() == 0) || (to.size() == 0))
		{
			throw new ValidierungException("Sie muessen mindestens ein Quell- und ein Zielobjekt angeben");
		}

		/*
			Check ob das selbe Objekt in Quelle in Ziel vorkommt
		*/
		if (fromKey.equals(toKey))
		{
			for (TransportTarget afrom : from)
			{
				for (TransportTarget aTo : to)
				{
					if (aTo.getId() == afrom.getId())
					{
						throw new ValidierungException("Sie können keine Einheiten zu sich selbst transportieren");
					}
				}
			}
		}

		/*
			Sind die beiden Objekte auch im selben Sektor?
		*/

		Location fromLoc = from.get(0).getLocation();
		Location toLoc = to.get(0).getLocation();
		if (!fromLoc.sameSector(from.get(0).getSize(), toLoc, to.get(0).getSize()))
		{
			throw new ValidierungException("Die angegebenen Objekte befinden sich nicht im selben Sektor");
		}
	}

	private List<TransportTarget> parseListeDerTransportZiele(String key, String toString)
	{
		List<TransportTarget> to = new ArrayList<>();
		if (wayhandler.containsKey(key))
		{
			to.addAll(wayhandler.get(key).createTargets(TransportTarget.ROLE_TARGET, toString));
		}
		else
		{
			throw new ValidierungException("Ungültiges Transportziel", "./ds?module=ueber");
		}

		validiereAlleTransportZieleImSelbenSektor(to);
		validiereAlleTransportZieleImSelbenSektor(to);
		return to;
	}


	private void validiereAlleTransportZieleGehoerenDenSpieler(List<TransportTarget> from)
	{
		for (TransportTarget afrom : from)
		{
			if (afrom.getOwner() != getUser().getId())
			{
				throw new ValidierungException("Das Schiff gehört ihnen nicht", Common.buildUrl("default", "module", "ueber"));
			}
		}
	}

	private List<TransportTarget> parseListeDerTransportQuellen(String key, String fromString)
	{
		List<TransportTarget> from = new ArrayList<>();
		if (wayhandler.containsKey(key))
		{
			from.addAll(wayhandler.get(key).createTargets(TransportTarget.ROLE_SOURCE, fromString));
		}
		else
		{
			throw new ValidierungException("Ungültige Transportquelle", "./ds?module=ueber");
		}

		validiereAlleTransportZieleGehoerenDenSpieler(from);
		validiereAlleTransportZieleImSelbenSektor(from);

		return from;
	}

	private void validiereAlleTransportZieleImSelbenSektor(List<TransportTarget> to)
	{
		Location toLoc = to.get(0).getLocation();
		for (int i = 1; i < to.size(); i++)
		{
			if (!toLoc.sameSector(to.get(0).getSize(), to.get(i).getLocation(), to.get(i).getSize()))
			{
				throw new ValidierungException("Die angegebenen Objekte befinden sich nicht im selben Sektor");
			}
		}
	}

	private long transferSingleUnit(StringBuilder message, TransportTarget fromUnit, TransportTarget toUnit, int unitid, long count, UnitCargo newfromc, UnitCargo newtoc, MutableLong cargofrom, MutableLong cargoto, StringBuilder msg, char mode, String rawFrom, String rawTo, String rawWay)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		UnitType unittype = (UnitType) db.get(UnitType.class, unitid);

		if (mode == 't')
		{
			if (unittype.getSize() > toUnit.getMaxUnitSize() && toUnit.getMaxUnitSize() != -1)
			{
				message.append(" - Diese Einheit ist zu groß für dieses Schiff.\n");
				return 0;
			}
		}
		else
		{
			if (unittype.getSize() > fromUnit.getMaxUnitSize() && fromUnit.getMaxUnitSize() != -1)
			{
				message.append(" - Diese Einheit ist zu groß für dieses Schiff.\n");
				return 0;
			}
		}

		boolean out = false;

		if (count > newfromc.getUnitCount(unittype))
		{
			count = newfromc.getUnitCount(unittype);

			out = true;
			message.append(" - Nur [unit=").append(unittype.getId()).append("]").append(count).append("[/unit] vorhanden");
			if (count < 0)
			{
				count = 0;
			}
		}

		if (cargoto.longValue() != -1 && cargoto.longValue() - UnitCargo.getUnitMass(unitid, count) < 0)
		{
			count = cargoto.longValue() / UnitCargo.getUnitMass(unitid, 1);

			if (count < 0)
			{
				Common.writeLog("transport.error.log", Common.date("d.m.y H:i:s") + ": " + getUser().getId() + " -> " + toUnit.getOwner() + " | " + rawFrom + " -> " + rawTo + " [" + rawWay + "] : " + mode + unitid + "@" + count + " ; " + msg + "\n---------\n");
				count = 0;
			}

			out = true;
			message.append(" - Nur noch Platz für [unit=").append(unittype.getId()).append("]").append(count).append("[/unit] vorhanden");
		}

		newtoc.addUnit(unittype, count);
		newfromc.substractUnit(unittype, count);

		if (count > 0)
		{
			msg.append("[unit=").append(unitid).append("]").append(count).append("[/unit] umgeladen\n");
		}

		if (mode == 't')
		{
			if (cargofrom.longValue() != -1)
			{
				cargofrom.setValue(fromUnit.getMaxUnitSpace() - newfromc.getMass());
			}
			if (cargoto.longValue() != -1)
			{
				cargoto.setValue(toUnit.getMaxUnitSpace() - newtoc.getMass());
			}
		}
		else
		{
			if (cargofrom.longValue() != -1)
			{
				cargofrom.setValue(toUnit.getMaxUnitSpace() - newfromc.getMass());
			}
			if (cargoto.longValue() != -1)
			{
				cargoto.setValue(fromUnit.getMaxUnitSpace() - newtoc.getMass());
			}
		}

		if ((fromUnit.getOwner() == toUnit.getOwner()) || (toUnit.getOwner() == 0))
		{
			out = true;
			if( mode == 't' ) {
				message.append(" - jetzt [unit=").append(unittype.getId()).append("]").append(newtoc.getUnitCount(unittype)).append("[/unit]auf ").append(Common._plaintitle(toUnit.getObjectName())).append(" vorhanden");
			}
			else {
				message.append(" - jetzt [unit=").append(unittype.getId()).append("]").append(newtoc.getUnitCount(unittype)).append("[/unit] auf ").append(Common._plaintitle(fromUnit.getObjectName())).append(" vorhanden");
			}
		}

		if( out )
		{
			message.append("\n");
		}

		return count;
	}

	/**
	 * Transferiert die Einheiten.
	 *  @param toMap Die Menge der entsprechenden Einheit (key), welche zum Zielschiff transferiert werden soll
	 * @param fromMap Die Menge der entsprechenden Einheit (key), welche von Zielschiff runter zum Quellschiff transferiert werden soll
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult transferAction(@UrlParam(name = "#to") Map<Integer, Integer> toMap,
											 @UrlParam(name = "#from") Map<Integer, Integer> fromMap,
											 @UrlParam(name = "from") String rawFrom,
											 @UrlParam(name = "to") String rawTo,
											 @UrlParam(name = "way") String rawWay)
	{
		String[] way = StringUtils.split(rawWay, "to");

		List<TransportTarget> from = parseListeDerTransportQuellen(way[0], rawFrom);
		List<TransportTarget> to = parseListeDerTransportZiele(way[1], rawTo);

		validiereEinheitenKoennenZwischenQuelleUndZielTransferiertWerden(from, to, way[0], way[1]);

		StringBuilder message = new StringBuilder();

		boolean transfer = false;

		List<UnitCargo> newtoclist = new ArrayList<>();
		List<Long> cargotolist = new ArrayList<>();
		UnitCargo totaltocargo = new TransientUnitCargo();

		List<UnitCargo> newfromclist = new ArrayList<>();
		List<Long> cargofromlist = new ArrayList<>();
		UnitCargo totalfromcargo = new TransientUnitCargo();

		// TODO: rewrite
		for (int k = 0; k < to.size(); k++)
		{
			newtoclist.add(k, (UnitCargo) to.get(k).getUnits().clone());
			totaltocargo.addCargo(to.get(k).getUnits());
			if (to.get(k).getMaxUnitSpace() == -1)
			{
				cargotolist.add(k, -1l);
			}
			else
			{
				cargotolist.add(k, to.get(k).getMaxUnitSpace() - to.get(k).getUnits().getMass());
			}
		}

		for (int k = 0; k < from.size(); k++)
		{
			newfromclist.add(k, (UnitCargo) from.get(k).getUnits().clone());
			totalfromcargo.addCargo(from.get(k).getUnits());
			if (from.get(k).getMaxUnitSpace() == -1)
			{
				cargofromlist.add(k, -1l);
			}
			else
			{
				cargofromlist.add(k, from.get(k).getMaxUnitSpace() - from.get(k).getUnits().getMass());
			}
		}

		Map<Integer, StringBuilder> msg = new HashMap<>();

		Map<UnitType, Long[]> unitlist = totalfromcargo.compare(totaltocargo);
		for (Map.Entry<UnitType, Long[]> unit : unitlist.entrySet())
		{
			UnitType unittype = unit.getKey();
			Integer transt = toMap.get(unittype.getId());
			Integer transf = fromMap.get(unittype.getId());

			if (transt != null && transt > 0)
			{
				if( to.size() > 1 )
				{
					message.append("Transportiere je [unit=").append(unittype.getId()).append("]").append(transt).append("[/unit]\n");
				}
				else {
					message.append("Transportiere [unit=").append(unittype.getId()).append("]").append(transt).append("[/unit] zu ").append(Common._plaintitle(to.get(0).getObjectName())).append("\n");
				}

				for (int k = 0; k < from.size(); k++)
				{
					TransportTarget fromTarget = from.get(k);

					for (int j = 0; j < to.size(); j++)
					{
						TransportTarget toTarget = to.get(j);
						if (!msg.containsKey(toTarget.getOwner()))
						{
							msg.put(toTarget.getOwner(), new StringBuilder());
						}

						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if (transferSingleUnit(message, fromTarget, toTarget, unittype.getId(), transt, newfromclist.get(k), newtoclist.get(j), mCargoFrom, mCargoTo, msg.get(toTarget.getOwner()), 't', rawFrom, rawTo, rawWay) != 0)
						{
							transfer = true;
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
					}
				}
			}
			else if (transf != null && transf > 0)
			{
				if( to.size() > 1 )
				{
					message.append("Transportiere je [unit=").append(unittype.getId()).append("]").append(transt).append("[/unit]\n");
				}
				else {
					message.append("Transportiere [unit=").append(unittype.getId()).append("]").append(transt).append("[/unit] von ").append(Common._plaintitle(to.get(0).getObjectName())).append("\n");
				}

				for (int k = 0; k < from.size(); k++)
				{
					TransportTarget fromTarget = from.get(k);

					for (int j = 0; j < to.size(); j++)
					{
						TransportTarget toTarget = to.get(j);
						if ((toTarget.getOwner() != getUser().getId()) && (toTarget.getOwner() != 0))
						{
							addError("Das gehört dir nicht!");

							return new RedirectViewResult("default");
						}

						if (!msg.containsKey(toTarget.getOwner()))
						{
							msg.put(toTarget.getOwner(), new StringBuilder());
						}
						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if (transferSingleUnit(message, fromTarget, toTarget, unittype.getId(), transf, newtoclist.get(j), newfromclist.get(k), mCargoTo, mCargoFrom, msg.get(toTarget.getOwner()), 'f', rawFrom, rawTo, rawWay) != 0)
						{
							transfer = true;
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
					}
				}
			}
		}

		Map<Integer, String> ownerpmlist = new HashMap<>();

		List<String> sourceshiplist = from.stream().map(aFrom -> aFrom.getObjectName() + " (" + aFrom.getId() + ")").collect(Collectors.toList());

		for (int j = 0; j < to.size(); j++)
		{
			TransportTarget toTarget = to.get(j);
			if (getUser().getId() != toTarget.getOwner())
			{
				if (msg.containsKey(toTarget.getOwner()) && (msg.get(toTarget.getOwner()).length() > 0) && !ownerpmlist.containsKey(toTarget.getOwner()))
				{
					Common.writeLog("transport.log", Common.date("d.m.y H:i:s") + ": " + getUser().getId() + " -> " + toTarget.getOwner() + " | " + rawFrom + " -> " + rawTo + " [" + rawWay + "] : " + "\n" + msg + "---------\n");

					message.append("Transmission versandt\n");

					List<String> shiplist = new ArrayList<>();

					for (int k = j; k < to.size(); k++)
					{
						if (to.get(j).getOwner() == to.get(k).getOwner())
						{
							shiplist.add(to.get(k).getObjectName() + " (" + to.get(k).getId() + ")");
						}
					}

					String tmpmsg = Common.implode(",", sourceshiplist) + " l&auml;dt Waren auf " + Common.implode(",", shiplist) + "\n" + msg.get(toTarget.getOwner());
					PM.send((User) getUser(), toTarget.getOwner(), "Waren transferiert", tmpmsg);

					ownerpmlist.put(toTarget.getOwner(), msg.get(toTarget.getOwner()).toString());
				}
			}
		}

		if (!transfer)
		{
			return new RedirectViewResult("default").withMessage(message.toString());
		}

		/*
			"from" bearbeiten
		*/
		for (int k = 0; k < newfromclist.size(); k++)
		{
			UnitCargo newfromc = newfromclist.get(k);
			from.get(k).setUnitCargo(newfromc);
			from.get(k).write();
		}

		/*
			"to" bearbeiten
		*/
		for (int k = 0; k < newtoclist.size(); k++)
		{
			UnitCargo newtoc = newtoclist.get(k);
			to.get(k).setUnitCargo(newtoc);
			to.get(k).write();
		}

		return new RedirectViewResult("default").withMessage(message.toString());
	}

	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction(@UrlParam(name = "way") String rawWay, @UrlParam(name = "from") String rawFrom, @UrlParam(name = "to") String rawTo, RedirectViewResult redirect)
	{
		String[] way = StringUtils.split(rawWay, "to");

		List<TransportTarget> from = parseListeDerTransportQuellen(way[0], rawFrom);
		List<TransportTarget> to = parseListeDerTransportZiele(way[1], rawTo);

		validiereEinheitenKoennenZwischenQuelleUndZielTransferiertWerden(from, to, way[0], way[1]);

		TemplateEngine t = templateViewResultFactory.createFor(this);

		if( redirect != null )
		{
			t.setVar("unittausch.message", Common._text(redirect.getMessage()));
		}

		t.setVar("global.rawway", rawWay,
				"source.isbase", way[0].equals("b"),
				"target.isbase", way[1].equals("b"));

		// Die Quelle(n) ausgeben
		transportQuelleAnzeigen(rawFrom, t, from);

		// Das Ziel / die Ziele ausgeben
		transportZielAnzeigen(rawTo, t, to);

		// Transfermodi ausgeben
		transportModusAnzeigen(rawFrom, rawTo, t, from, to);

		// Soll der Zielcargo gezeigt werden?
		einheitenListeAnzeigen(t, from, to);

		return t;
	}

	private void einheitenListeAnzeigen(TemplateEngine t, List<TransportTarget> from, List<TransportTarget> to)
	{
		t.setBlock("_TRANSPORT", "res.listitem", "res.list");

		boolean showtarget = false;
		UnitCargo tocargo = new TransientUnitCargo();

		for (TransportTarget toTarget : to)
		{
			if (getUser().getId() != toTarget.getOwner())
			{
				continue;
			}
			showtarget = true;

			Map<UnitType, Long> unitlist = toTarget.getUnits().getUnitMap();
			unitlist.entrySet().stream()
					.filter(unit -> unit.getValue() > tocargo.getUnitCount(unit.getKey()))
					.forEach(unit -> tocargo.setUnit(unit.getKey(), unit.getValue()));
		}

		t.setVar("target.show", showtarget);

		UnitCargo fromcargo = new TransientUnitCargo();
		for (TransportTarget afrom : from)
		{
			Map<UnitType, Long> unitlist = afrom.getUnits().getUnitMap();
			unitlist.entrySet().stream()
					.filter(unit -> unit.getValue() > fromcargo.getUnitCount(unit.getKey()))
					.forEach(unit -> fromcargo.setUnit(unit.getKey(), unit.getValue()));
		}

		// Muss verglichen werden oder reicht unsere eigene Resliste?
		Map<UnitType, Long[]> unitlist;
		if (!showtarget)
		{
			unitlist = fromcargo.compare(new TransientUnitCargo());
		}
		else
		{
			unitlist = fromcargo.compare(tocargo);
		}

		for (Entry<UnitType, Long[]> unit : unitlist.entrySet())
		{
			UnitType unittype = unit.getKey();
			t.setVar("res.name", unittype.getName(),
					"res.image", unittype.getPicture(),
					"res.id", unittype.getId(),
					"res.cargo.source", (from.size() > 1 ? "max " : "") + unit.getValue()[0],
					"res.cargo.target", showtarget ? (to.size() > 1 ? "max " : "") + unit.getValue()[1] : 0);

			t.parse("res.list", "res.listitem", true);
		}
	}

	private void transportModusAnzeigen(String rawFrom, String rawTo, TemplateEngine t, List<TransportTarget> from, List<TransportTarget> to)
	{
		t.setBlock("_TRANSPORT", "transfermode.listitem", "transfermode.list");
		if ((to.size() > 1) || (from.size() > 1) || (to.get(0).getMultiTarget() != null) ||
				(from.get(0).getMultiTarget() != null))
		{
			TransportTarget first = to.get(0);
			TransportTarget second = from.get(0);

			MultiTarget multiTo;
			if (to.size() > 1)
			{
				multiTo = first.getMultiTarget();
				if ((multiTo == null) || !multiTo.getTargetList().equals(rawTo))
				{
					multiTo = new MultiTarget("Gruppe", rawTo);
				}
			}
			else
			{
				multiTo = first.getMultiTarget();
			}

			MultiTarget multiFrom;
			if (from.size() > 1)
			{
				multiFrom = second.getMultiTarget();
				if ((multiFrom == null) || !multiFrom.getTargetList().equals(rawFrom))
				{
					multiFrom = new MultiTarget("Gruppe", rawFrom);
				}
			}
			else
			{
				multiFrom = second.getMultiTarget();
			}

			// Single to Single
			t.setVar("transfermode.from.name", second.getTargetName(),
					"transfermode.from", second.getId(),
					"transfermode.to.name", first.getTargetName(),
					"transfermode.to", first.getId(),
					"transfermode.selected", to.size() == 1 && (from.size() <= 1));
			t.parse("transfermode.list", "transfermode.listitem", true);


			// Single to Multi
			if (multiTo != null)
			{
				t.setVar("transfermode.from.name", second.getTargetName(),
						"transfermode.from", second.getId(),
						"transfermode.to.name", multiTo.getName(),
						"transfermode.to", multiTo.getTargetList(),
						"transfermode.selected", to.size() > 1 && (from.size() <= 1));
				t.parse("transfermode.list", "transfermode.listitem", true);
			}

			// Multi to Single
			if (multiFrom != null)
			{
				t.setVar("transfermode.to.name", first.getTargetName(),
						"transfermode.to", first.getId(),
						"transfermode.from.name", multiFrom.getName(),
						"transfermode.from", multiFrom.getTargetList(),
						"transfermode.selected", (from.size() > 1) && to.size() == 1);
				t.parse("transfermode.list", "transfermode.listitem", true);
			}

			// Multi to Multi
			if ((multiFrom != null) && (multiTo != null) &&
					!multiFrom.getTargetList().equals(multiTo.getTargetList()))
			{
				t.setVar("transfermode.to.name", multiTo.getName(),
						"transfermode.to", multiTo.getTargetList(),
						"transfermode.from.name", multiFrom.getName(),
						"transfermode.from", multiFrom.getTargetList(),
						"transfermode.selected", (from.size() > 1) && to.size() > 1);
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
		}
	}

	private void transportZielAnzeigen(String rawTo, TemplateEngine t, List<TransportTarget> to)
	{
		t.setBlock("_TRANSPORT", "target.targets.listitem", "target.targets.list");

		if (to.size() == 1)
		{
			t.setVar("targetobj.name", to.get(0).getObjectName(),
					"targetobj.id", to.get(0).getId(),
					"target.cargo", (to.get(0).getMaxUnitSpace() == -1) ? "" : Common.ln(to.get(0).getMaxUnitSpace() - to.get(0).getUnits().getMass()));

			t.setVar("target.id", to.get(0).getId());
		}
		else if (to.size() < 10)
		{
			long cargo = 0;
			for (TransportTarget atod : to)
			{
				if (atod.getMaxUnitSpace() == -1)
				{
					cargo = -1;
					break;
				}
				cargo = Math.max(atod.getMaxUnitSpace() - atod.getUnits().getMass(), cargo);
				t.setVar("targetobj.name", atod.getObjectName(),
						"targetobj.id", atod.getId());

				t.parse("target.targets.list", "target.targets.listitem", true);
			}

			t.setVar("target.id", rawTo,
					"targetobj.id", to.get(0).getId(),
					"target.cargo", "max " + ((cargo == -1) ? "Infinite" : Common.ln(cargo)));
		}
		else
		{
			long cargo = 0;
			for (TransportTarget atod : to)
			{
				if (atod.getMaxUnitSpace() == -1)
				{
					cargo = -1;
					break;
				}
				cargo = Math.max(atod.getMaxUnitSpace() - atod.getUnits().getMass(), cargo);
			}
			TransportTarget first = to.get(0);

			t.setVar("targetobj.name", first.getObjectName(),
					"targetobj.id", first.getId(),
					"targetobj.addinfo", "und " + (to.size() - 1) + " weiteren Schiffen",
					"target.cargo", "max " + ((cargo == -1) ? "Infinite" : Common.ln(cargo)));

			t.setVar("target.id", rawTo);
		}
	}

	private void transportQuelleAnzeigen(String rawFrom, TemplateEngine t, List<TransportTarget> from)
	{
		t.setBlock("_TRANSPORT", "source.sources.listitem", "source.sources.list");

		if (from.size() == 1)
		{
			TransportTarget first = from.get(0);

			t.setVar("sourceobj.name", first.getObjectName(),
					"sourceobj.id", first.getId(),
					"source.cargo", (first.getMaxUnitSpace() == -1) ? "" : Common.ln(first.getMaxUnitSpace() - first.getUnits().getMass()));

			t.setVar("source.id", first.getId());
		}
		else if (from.size() < 10)
		{
			long cargo = 0;
			for (TransportTarget afromd : from)
			{
				if (afromd.getMaxUnitSpace() == -1)
				{
					cargo = -1;
					break;
				}
				cargo = Math.max(afromd.getMaxUnitSpace() - afromd.getUnits().getMass(), cargo);
				t.setVar("sourceobj.name", afromd.getObjectName(),
						"sourceobj.id", afromd.getId());

				t.parse("source.sources.list", "source.sources.listitem", true);
			}

			t.setVar("source.id", rawFrom,
					"sourceobj.id", from.get(0).getId(),
					"source.cargo", "max " + ((cargo == -1) ? "Infinite" : Common.ln(cargo)));
		}
		else
		{
			long cargo = 0;
			for (TransportTarget afromd : from)
			{
				if (afromd.getMaxUnitSpace() == -1)
				{
					cargo = -1;
					break;
				}
				cargo = Math.max(afromd.getMaxUnitSpace() - afromd.getUnits().getMass(), cargo);
			}
			TransportTarget first = from.get(0);

			t.setVar("sourceobj.name", first.getObjectName(),
					"sourceobj.id", first.getId(),
					"sourceobj.addinfo", "und " + (from.size() - 1) + " weiteren Schiffen",
					"source.cargo", "max " + ((cargo == -1) ? "Infinite" : Common.ln(cargo)));

			t.setVar("source.id", rawFrom);
		}
	}
}
