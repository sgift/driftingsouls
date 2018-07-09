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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import net.driftingsouls.ds2.server.units.UnitType;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigInteger;
import java.util.List;

/**
 * Die Kaserne.
 *
 */
@Entity(name="KaserneBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Kaserne")
public class KasernenBuilding extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Kaserne.
	 */
	public KasernenBuilding() {
		// EMPTY
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
	public void build(Base base, int building) {
		super.build(base, building);

		Kaserne kaserne = new Kaserne(base);
		ContextMap.getContext().getDB().persist(kaserne);
	}


	@Override
	public void cleanup(Context context, Base base, int building) {
		super.cleanup(context, base, building);

		org.hibernate.Session db = context.getDB();
		Kaserne kaserne = (Kaserne)db.createQuery("from Kaserne where base=:base")
			.setEntity("base", base)
			.uniqueResult();

		if( kaserne != null ) {
			kaserne.destroy();
		}
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		StringBuilder result = new StringBuilder(200);

		Kaserne kaserne = (Kaserne)db.createQuery("from Kaserne where base=:base")
			.setEntity("base", base)
			.uniqueResult();
		if( kaserne != null ) {
			if( !kaserne.isBuilding() ) {
				result.append("<a class=\"back tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[B]<span class='ttcontent'>").append(this.getName()).append("</span></a>");
			}
			else {
				StringBuilder popup = new StringBuilder(100);
				popup.append(this.getName()).append(":<br />");
				for( KaserneEntry entry : kaserne.getQueueEntries() )
				{
					UnitType unittype = entry.getUnit();
					popup.append("<br />Aktuell im Bau: ").append(entry.getCount()).append("x ").append(unittype.getName()).append(" <img src='./data/interface/time.gif' alt='Dauer: ' />").append(entry.getRemaining());
				}

				result.append("<a class=\"error tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[B]<span style=\"font-weight:normal\">");
				result.append(kaserne.getQueueEntries().size());
				result.append("</span><span class='ttcontent'>").append(popup).append("</span></a>");
			}
		}

		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Kaserne kaserne = (Kaserne)db.createQuery("from Kaserne where base=:base")
			.setEntity("base", base)
			.uniqueResult();
		if( kaserne != null ) {
			return kaserne.isBuilding();
		}

		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();

		Kaserne kaserne = (Kaserne)db.createQuery("from Kaserne where base=:base")
			.setEntity("base", base)
			.uniqueResult();

		User owner = base.getOwner();

		int cancel = context.getRequest().getParameterInt("cancel");
		int queueid = context.getRequest().getParameterInt("queueid");
		int newunit = context.getRequest().getParameterInt("unitid");
		int newcount = context.getRequest().getParameterInt("count");

		TemplateViewResultFactory templateViewResultFactory = context.getBean(TemplateViewResultFactory.class, null);
		TemplateEngine t = templateViewResultFactory.createEmpty();
		if( !t.setFile( "_BUILDING", "buildings.kaserne.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		if( kaserne == null ) {
			context.addError("Diese Kaserne verf&uuml;gt &uuml;ber keinen Kaernen-Eintrag in der Datenbank");
			return "";
		}

		if( cancel == 1 && queueid > 0 )
		{
			KaserneEntry entry = (KaserneEntry)db.get(KaserneEntry.class, queueid);
			if(entry == null)
			{
				t.setVar("kaserne.message", "Der Eintrag konnte nicht gel&ouml;scht werden, da nicht vorhanden.");
			}
			else if(entry.getKaserne().getId() != kaserne.getId())
			{
				t.setVar("kaserne.message", "Dieser Eintrag geh&ouml;rt nicht zu dieser Kaserne.");
			}
			else
			{
				db.delete(entry);
				t.setVar("kaserne.message", "Eintrag entfernt.");
			}

		}

		t.setVar(
				"base.name",	base.getName(),
				"base.id",		base.getId(),
				"base.field",	field );

		//---------------------------------
		// Eine neue Einheit ausbilden
		//---------------------------------

		if( newunit != 0 && newcount > 0) {
			UnitType unittype = (UnitType)db.get(UnitType.class, newunit);

			Cargo cargo = new Cargo(base.getCargo());
			Cargo buildcosts = unittype.getBuildCosts();
			BigInteger konto = owner.getKonto();
			String msg = "";

			boolean ok = true;

			for(ResourceEntry res : buildcosts.getResourceList())
			{
				// Wenn nicht alles im eigenen Cargo da ist
				if( !cargo.hasResource(res.getId(), res.getCount1()*newcount) )
				{
					// Handelt es sich um Geld
					if(res.getId().equals(Resources.RE))
					{
						// Genug Geld auf dem Konto
						if(konto.intValue() >= res.getCount1()*newcount - cargo.getResourceCount(res.getId()))
						{
							// Fresse Cargo leer danach das Konto
							konto = konto.subtract(BigInteger.valueOf( res.getCount1()*newcount - cargo.getResourceCount(res.getId()) ));
							cargo.setResource(res.getId(), 0);
						}
						else
						{
							// Mensch sind wir echt sooo pleite?
							ok = false;
							msg += "Sie haben nicht genug "+res.getPlainName()+"<br />";
						}

					}
					else
					{
						// Es handelt sich nicht um Geld und wir haben nicht genug.
						ok = false;
						msg += "Sie haben nicht genug "+res.getName()+"<br />";
					}
				}
				else
				{
					// Wir haben genug
					cargo.substractResource(res.getId(), res.getCount1()*newcount);
				}
			}

			if( ok ) {
				msg += newcount+" "+unittype.getName()+" werden ausgebildet.";

				base.setCargo(cargo);
				owner.setKonto(konto);

				kaserne.addEntry(unittype, newcount);
			}
			if( !msg.isEmpty() )
			{
				t.setVar( "kaserne.message",msg);
			}
		}

		//-----------------------------------------------
		// werden gerade Einheiten ausgebildet? Bauschlange anzeigen!
		//-----------------------------------------------

		if( kaserne.isBuilding() ) {
			t.setVar(
					"kaserne.show.training", 1);

			t.setBlock("_BUILDING", "kaserne.training.listitem", "kaserne.training.list");

			for( KaserneEntry entry : kaserne.getQueueEntries() )
			{
				UnitType unittype = entry.getUnit();

				if(unittype == null)
				{
					t.setVar("kaserne.message", "Unbekannte Einheit gefunden");
				}

				t.setVar(	"trainunit.id", 		unittype.getId(),
							"trainunit.name", 		unittype.getName(),
							"trainunit.menge", 		entry.getCount(),
							"trainunit.remaining",	entry.getRemaining(),
							"trainunit.queue.id",	entry.getId() );

				t.parse("kaserne.training.list", "kaserne.training.listitem", true);
			}
		}

		//--------------------------------------------------
		// Ausbildbare Einheiten anzeigen
		//--------------------------------------------------

		t.setBlock("_BUILDING", "kaserne.unitlist.listitem", "kaserne.unitlist.list");

		List<UnitType> unitlist = Common.cast(db.createQuery("from UnitType").list());

		for(UnitType unittype : unitlist)
		{
			if(owner.hasResearched(unittype.getRes()))
			{
				String buildingcosts = "";
				Cargo buildcosts = unittype.getBuildCosts();

				for(ResourceEntry res : buildcosts.getResourceList())
				{
					buildingcosts = buildingcosts+" <span class='nobr'><img style=\"vertical-align:middle\" src=\""+res.getImage()+"\" alt=\""+res.getPlainName()+"\" title=\""+res.getPlainName()+"\" />"+res.getCargo1()+"</span>";
				}

				t.setVar( 	"unit.id", 			unittype.getId(),
						"unit.name", 		unittype.getName(),
						"unit.picture", 	unittype.getPicture(),
						"unit.dauer", 		unittype.getDauer(),
						"unit.buildcosts", 	buildingcosts.trim());

				t.parse("kaserne.unitlist.list", "kaserne.unitlist.listitem", true);
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
