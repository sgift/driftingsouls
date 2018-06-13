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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.*;

/**
 * Die Akademie.
 *
 */
@Entity(name="AcademyBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.AcademyBuilding")
public class AcademyBuilding extends DefaultBuilding {
	private static final Log log = LogFactory.getLog(AcademyBuilding.class);
	private static final Map<Integer,String> offis = new HashMap<>();
	private static final Map<Integer,String> attributes = new HashMap<>();

	static {
		offis.put(1, "Ingenieur");
		offis.put(2, "Navigator");
		offis.put(3, "Sicherheitsexperte");
		offis.put(4, "Captain");

		attributes.put(1, "Technik");
		attributes.put(2, "Waffen");
		attributes.put(3, "Navigation");
		attributes.put(4, "Sicherheit");
		attributes.put(5, "Kommandoeffizienz");
	}

	/**
	 * Erstellt eine neue Academy-Instanz.
	 */
	public AcademyBuilding() {
		// EMPTY
	}

	@Override
	public void build(Base base, int building) {
		super.build(base, building);

		Academy academy = new Academy(base);

		org.hibernate.Session db = ContextMap.getContext().getDB();

		db.persist(academy);

		base.setAcademy(academy);
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return true;
	}

	@Override
	public void cleanup(Context context, Base base, int building) {
		super.cleanup(context, base, building);
		org.hibernate.Session db = context.getDB();


		Academy academy = base.getAcademy();
		if( academy != null )
		{
			// Bereinige Queue Eintraege
			academy.getQueueEntries().clear();

			base.setAcademy(null);
			db.delete(academy);
		}


		for( Offizier offizier : Offizier.getOffiziereByDest(base) )
		{
			offizier.setTraining(false);
		}
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		Academy academy = base.getAcademy();
		if( (academy != null) && (academy.getTrain()) ) {
			return true;
		}
		else if( academy == null ) {
			log.warn("Die Akademie auf Basis "+base.getId()+" verfuegt ueber keinen DB-Eintrag");
		}
		return false;
	}

	/**
	 * Ermittelt Ausbildungskosten.
	 *
	 * @param acc die akademie
	 * @param typ gibt an ob es sich um Silizium Nahrung oder Dauer handelt (0-2)
	 * @param offizier der auszubildende offizier
	 * @param train Sparte/Faehigkeit die ausgebildet wird
	 * @return nada
	 */
	public int getUpgradeCosts(Academy acc, int typ, Offizier offizier, int train) {
		Map<Integer,Offizier.Ability> dTrain = new HashMap<>();
		dTrain.put(1, Offizier.Ability.ING);
		dTrain.put(2, Offizier.Ability.WAF);
		dTrain.put(3, Offizier.Ability.NAV);
		dTrain.put(4, Offizier.Ability.SEC);
		dTrain.put(5, Offizier.Ability.COM);

		double nahrungfactor = new ConfigService().getValue(WellKnownConfigValue.OFFIZIER_NAHRUNG_FACTOR);
		double siliziumfactor = new ConfigService().getValue(WellKnownConfigValue.OFFIZIER_SILIZIUM_FACTOR);
		double dauerfactor = new ConfigService().getValue(WellKnownConfigValue.OFFIZIER_DAUER_FACTOR);

		int plus = 0;
		List<AcademyQueueEntry> entries = acc.getQueueEntries();
		for( AcademyQueueEntry entry : entries ) {
			if( entry.getTraining() == offizier.getID() && entry.getTrainingType() == train )
			{
				plus = plus+10;
			}
		}

		switch ( typ ) {
		case 0:
			return (int)((offizier.getAbility(dTrain.get(train))+plus)*siliziumfactor)+1;
		case 1:
			return (int)((offizier.getAbility(dTrain.get(train))+plus)*nahrungfactor)+1;
		case 2:
			return (int)((offizier.getAbility(dTrain.get(train))+plus)*dauerfactor)+1;
		}
		return 0;
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		StringBuilder result = new StringBuilder(200);

		Academy acc = base.getAcademy();
		if( acc != null ) {
			if( !acc.getTrain() ) {
				result.append("<a class=\"back tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]<span class='ttcontent'>").append(this.getName()).append("</span></a>");
			}
			else {
				StringBuilder popup = new StringBuilder(200);
				popup.append(this.getName()).append(":<br /><br />");
				List<AcademyQueueEntry> entries = acc.getScheduledQueueEntries();
				for( AcademyQueueEntry entry : entries )
				{
					if( entry.getTraining() < 0 ) {
						popup.append("Bildet aus: ");
						popup.append(offis.get(entry.getTraining() * -1));
						popup.append(" Dauer: <img style='vertical-align:middle' src='");
						popup.append("./data/interface/time.gif' alt='noch ' />");
						popup.append(entry.getRemainingTime());
					}
					else
					{
						Offizier offi = Offizier.getOffizierByID(entry.getTraining());
						if( offi == null )
						{
							continue;
						}
						popup.append("Bildet aus: ");
						popup.append(offi.getName());
						popup.append(" (");
						popup.append(attributes.get(entry.getTrainingType()));
						popup.append(") ");
						popup.append("Dauer: <img style='vertical-align:middle' src='");
						popup.append("./data/interface/time.gif' alt='noch ' />");
						popup.append(entry.getRemainingTime());
					}
					popup.append("<br />");
				}

				result.append("<a class=\"error tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[A]<span style=\"font-weight:normal\">");
				result.append(acc.getNumberScheduledQueueEntries());
				result.append("</span><span class='ttcontent'>").append(popup).append("</span></a>");
			}
		}
		else {
			result.append("WARNUNG: Akademie ohne Akademieeintrag gefunden<br />\n");
		}

		return result.toString();
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		org.hibernate.Session db = context.getDB();
		TemplateViewResultFactory templateViewResultFactory = context.getBean(TemplateViewResultFactory.class, null);
		TemplateEngine t = templateViewResultFactory.createEmpty();

		int siliziumcosts = new ConfigService().getValue(WellKnownConfigValue.NEW_OFF_SILIZIUM_COSTS);
		int nahrungcosts = new ConfigService().getValue(WellKnownConfigValue.NEW_OFF_NAHRUNG_COSTS);
		int dauercosts = new ConfigService().getValue(WellKnownConfigValue.OFF_DAUER_COSTS);
		int maxoffstotrain = new ConfigService().getValue(WellKnownConfigValue.MAX_OFFS_TO_TRAIN);

		int newo = context.getRequest().getParameterInt("newo");
		int train = context.getRequest().getParameterInt("train");
		int off = context.getRequest().getParameterInt("off");
		int up = context.getRequest().getParameterInt("up");
		int down = context.getRequest().getParameterInt("down");
		int cancel = context.getRequest().getParameterInt("cancel");
		int queueid = context.getRequest().getParameterInt("queueid");
		String conf = context.getRequest().getParameterString("conf");

		if( !t.setFile( "_BUILDING", "buildings.academy.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}

		Academy academy = base.getAcademy();
		if( academy == null ) {
			context.addError("Diese Akademie verf&uuml;gt &uuml;ber keinen Akademie-Eintrag in der Datenbank");
			return "";
		}


		t.setVar(
				"base.name",	base.getName(),
				"base.id",		base.getId(),
				"base.field",	field,
				"academy.actualbuilds", academy.getNumberScheduledQueueEntries(),
				"academy.maxbuilds", maxoffstotrain);

		//--------------------------------
		// Als erstes ueberpruefen wir, ob eine Aktion durchgefuehrt wurde
		//--------------------------------
		if( up == 1 && queueid > 0 )
		{
			if( queueid == 1)
			{
				t.setVar(
						"academy.message", "<font color=\"red\">Vielen Dank fuer diesen URL-Hack.<br />Ihre Anfrage wurde soeben mitsamt Ihrer Spieler-ID an die Admins geschickt.<br />Wir wuenschen noch einen angenehmen Tag!</font>"
						);
			}
			else
			{
				AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid);
				if(thisentry != null && thisentry.getPosition() > 0 )
				{
					AcademyQueueEntry upperentry = academy.getQueueEntryByPosition(thisentry.getPosition()-1);

					thisentry.setPosition(thisentry.getPosition()-1);

					if( upperentry != null )
					{
						upperentry.setPosition(upperentry.getPosition()+1);
					}
				}
			}
		}
		if( down == 1 && queueid > 0 )
		{
			AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid );
			//Es kann ja sein, dass der Eintrag gar nicht mehr existiert.
			if(thisentry != null)
			{
				if( thisentry.isLastPosition() ) {
					t.setVar(
							"academy.message", "<font color=\"red\">Vielen Dank fuer diesen URL-Hack.<br />Ihre Anfrage wurde soeben mitsamt Ihrer Spieler-ID an die Admins geschickt.<br />Wir wuenschen noch einen angenehmen Tag!</font>"
							);
				}
				else
				{
					AcademyQueueEntry upperentry = academy.getQueueEntryByPosition(thisentry.getPosition()+1);
					thisentry.setPosition(thisentry.getPosition()+1);
					if( upperentry != null ) {
						upperentry.setPosition(upperentry.getPosition()-1);
					}
				}
			}
		}
		if( cancel == 1 && queueid > 0 )
		{
			AcademyQueueEntry thisentry = academy.getQueueEntryById(queueid);
			//Es kann ja sein, dass der Eintrag gar nicht mehr existiert.
			if(thisentry != null)
			{
				int offid = thisentry.getTraining();
				thisentry.deleteQueueEntry();
				academy.rescheduleQueue();
				if(offid > 0 )
				{
					if( !academy.isOffizierScheduled(offid))
					{
						Offizier offizier = Offizier.getOffizierByID(offid);
						offizier.setTraining(false);
					}
				}
				if( academy.getNumberScheduledQueueEntries() == 0 ) {
					academy.setTrain(false);
				}
			}
		}

		//---------------------------------
		// Einen neuen Offizier ausbilden
		//---------------------------------

		if( newo != 0 ) {
			t.setVar("academy.show.trainnewoffi", 1);

			Cargo cargo = new Cargo(base.getCargo());

			boolean ok = true;
			if( cargo.getResourceCount( Resources.SILIZIUM ) < siliziumcosts ) {
				t.setVar("trainnewoffi.error", "Nicht genug Silizium");
				ok = false;
			}
			if( cargo.getResourceCount( Resources.NAHRUNG ) < nahrungcosts ) {
				t.setVar("trainnewoffi.error", "Nicht genug Nahrung");
				ok = false;
			}

			if( ok ) {
				t.setVar("trainnewoffi.train", 1);

				cargo.substractResource( Resources.SILIZIUM, siliziumcosts);
				cargo.substractResource( Resources.NAHRUNG, nahrungcosts);
				academy.setTrain(true);
				AcademyQueueEntry entry = new AcademyQueueEntry(academy, -newo, dauercosts);
				base.setCargo(cargo);
				db.save(entry);
				academy.addQueueEntry(entry);
			}
		}

		//--------------------------------------
		// "Upgrade" eines Offiziers durchfuehren
		//--------------------------------------

		if( (train != 0) && (off != 0) ) {
			Offizier offizier = Offizier.getOffizierByID(off);
			//Auch hier kann es sein dass der Offizier nicht existiert.
			if(offizier != null )
			{
				if( offizier.getStationiertAufBasis() != null && offizier.getStationiertAufBasis().getId() == base.getId() ) {
					Map<Integer,Offizier.Ability> dTrain = new HashMap<>();
					dTrain.put(1, Offizier.Ability.ING);
					dTrain.put(2, Offizier.Ability.WAF);
					dTrain.put(3, Offizier.Ability.NAV);
					dTrain.put(4, Offizier.Ability.SEC);
					dTrain.put(5, Offizier.Ability.COM);

					int sk = getUpgradeCosts(academy, 0, offizier, train);
					int nk = getUpgradeCosts(academy, 1, offizier, train);
					int dauer = getUpgradeCosts(academy, 2, offizier, train);

					t.setVar(
							"academy.show.trainoffi", 1,
							"trainoffi.id",			offizier.getID(),
							"trainoffi.trainid",	train,
							"offizier.name",		Common._plaintext(offizier.getName()),
							"offizier.train.dauer",		dauer,
							"offizier.train.nahrung", 	nk,
							"offizier.train.silizium",	sk,
							"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
							"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM));

					if( train == 1 ) {
						t.setVar("offizier.train.ability", "Technik");
					}
					else if( train == 2 ) {
						t.setVar("offizier.train.ability", "Waffen");
					}
					else if( train == 3 ) {
						t.setVar("offizier.train.ability", "Navigation");
					}
					else if( train == 4 ) {
						t.setVar("offizier.train.ability", "Sicherheit");
					}
					else if( train == 5 ) {
						t.setVar("offizier.train.ability", "Kommandoeffizienz");
					}

					Cargo cargo = new Cargo(base.getCargo());
						boolean ok = true;
					if( cargo.getResourceCount( Resources.SILIZIUM ) < sk) {
							t.setVar("trainoffi.error", "Nicht genug Silizium");
						ok = false;
					}
					if( cargo.getResourceCount( Resources.NAHRUNG ) < nk ) {
						t.setVar("trainoffi.error", "Nicht genug Nahrung");
						ok = false;
					}

					if( !conf.equals("ok") ) {
						t.setVar("trainoffi.conf",	1);
						t.parse( "OUT", "_BUILDING" );
						return t.getVar("OUT");
					}

					if( ok ) {
						t.setVar("trainoffi.train", 1);

						cargo.substractResource( Resources.SILIZIUM, sk );
						cargo.substractResource( Resources.NAHRUNG, nk );

						AcademyQueueEntry entry = new AcademyQueueEntry(academy,offizier.getID(),dauer,train);

						offizier.setTraining(true);
						base.setCargo(cargo);
						db.save(entry);
						academy.addQueueEntry(entry);
						academy.setTrain(true);
						academy.rescheduleQueue();

						t.setVar("academy.actualbuilds", academy.getNumberScheduledQueueEntries());

						t.parse( "OUT", "_BUILDING" );
						return t.getVar("OUT");
					}
				}
			}
		}

		//--------------------------------
		// Dann berechnen wir die Ausbildungsschlange neu
		//--------------------------------
		academy.rescheduleQueue();

		t.setVar("academy.actualbuilds", academy.getNumberScheduledQueueEntries());

		db.flush();
		//-----------------------------------------------
		// werden gerade Offiziere ausgebildet? Bauschlange anzeigen!
		//-----------------------------------------------

		if( academy.getTrain() ) {
			t.setVar(
					"academy.show.training", 1);

			t.setBlock("_BUILDING", "academy.training.listitem", "academy.training.list");

			List<AcademyQueueEntry> entries = new ArrayList<>(academy.getQueueEntries());
			entries.sort(new AcademyQueueEntryComparator());
			for( AcademyQueueEntry entry : entries )
			{
				if( entry.getTraining() > 0 )
				{
					Offizier offi = Offizier.getOffizierByID(entry.getTraining());
					if( offi != null )
					{
						t.setVar(
								"trainoffizier.id", entry.getTraining(),
								"trainoffizier.name", offi.getName(),
								"trainoffizier.attribute", attributes.get(entry.getTrainingType()),
								"trainoffizier.offi", true,
								"trainoffizier.picture", offi.getPicture()
								);
					}
				}
				else
				{
					t.setVar(
							"trainoffizier.attribute", "Neuer Offizier",
							"trainoffizier.name", Common._plaintext(offis.get(-entry.getTraining())),
							"trainoffizier.offi", false
							);
				}
				t.setVar(
						"trainoffizier.remain", entry.getRemainingTime(),
						"trainoffizier.build", entry.isScheduled(),
						"trainoffizier.queue.id", entry.getId(),
						"trainoffizier.showup", true,
						"trainoffizier.showdown", true
						);

				if( entry.getPosition() == 1 )
				{
					t.setVar(
							"trainoffizier.showup", false
							);
				}
				if( entry.isLastPosition() )
				{
					t.setVar(
							"trainoffizier.showdown", false
							);
				}

				t.parse("academy.training.list", "academy.training.listitem", true);

			}
		}

		//---------------------------------
		// Liste: Neue Offiziere ausbilden
		//---------------------------------

		t.setVar(
				"academy.show.trainnew",	1,
				"resource.silizium.image",	Cargo.getResourceImage(Resources.SILIZIUM),
				"resource.nahrung.image",	Cargo.getResourceImage(Resources.NAHRUNG),
				"resource.silizium.costs", siliziumcosts,
				"resource.nahrung.costs", nahrungcosts,
				"dauer.costs", dauercosts
				);

		t.setBlock("_BUILDING", "academy.trainnew.listitem", "academy.trainnew.list");

		for( Offiziere.Offiziersausbildung offi : Offiziere.LIST.values() ) {
			t.setVar(
					"offizier.id",		offi.getId(),
					"offizier.name",	Common._title(offi.getName()),
					"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
					"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
					"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
					"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
					"offizier.com",		offi.getAbility(Offizier.Ability.COM));

			t.parse("academy.trainnew.list", "academy.trainnew.listitem", true);
		}


		//---------------------------------
		// Liste: "Upgrade" von Offizieren
		//---------------------------------

		t.setVar(
				"academy.show.offilist", 1,
				"offilist.allowactions", 1);

		t.setBlock("_BUILDING", "academy.offilistausb.listitem", "academy.offilistausb.list");

		List<Offizier> offiziere = Offizier.getOffiziereByDest(base);
		for( Offizier offi : offiziere ) {
			if( !offi.isTraining() )
			{
				continue;
			}
			t.setVar(
					"offizier.picture",	offi.getPicture(),
					"offizier.id",		offi.getID(),
					"offizier.name",	Common._plaintitle(offi.getName()),
					"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
					"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
					"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
					"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
					"offizier.com",		offi.getAbility(Offizier.Ability.COM),
					"offizier.special",	offi.getSpecial().getName() );

			t.parse("academy.offilistausb.list", "academy.offilistausb.listitem", true);
		}

		t.setBlock("_BUILDING", "academy.offilist.listitem", "academy.offilist.list");

		offiziere = Offizier.getOffiziereByDest(base);
		for( Offizier offi : offiziere )
		{
			if( offi.isTraining() )
			{
				continue;
			}
			t.setVar(
					"offizier.picture",	offi.getPicture(),
					"offizier.id",		offi.getID(),
					"offizier.name",	Common._plaintitle(offi.getName()),
					"offizier.ing",		offi.getAbility(Offizier.Ability.ING),
					"offizier.waf",		offi.getAbility(Offizier.Ability.WAF),
					"offizier.nav",		offi.getAbility(Offizier.Ability.NAV),
					"offizier.sec",		offi.getAbility(Offizier.Ability.SEC),
					"offizier.com",		offi.getAbility(Offizier.Ability.COM),
					"offizier.special",	offi.getSpecial().getName() );

			t.parse("academy.offilist.list", "academy.offilist.listitem", true);
		}

		t.parse( "OUT", "_BUILDING" );
		return t.getVar("OUT");
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}

	protected static class AcademyQueueEntryComparator implements Comparator<AcademyQueueEntry>
	{
		@Override
		public int compare(AcademyQueueEntry o1, AcademyQueueEntry o2)
		{
			return o1.getPosition() - o2.getPosition();
		}
	}
}
