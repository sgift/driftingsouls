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
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public void build(Base base, Building building) {
		throw new IllegalArgumentException("shouldn't be called anymore");
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
		throw new IllegalArgumentException("should not be called!");
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
		throw new IllegalArgumentException("Shouldn't be called anymore!");
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}

	public static class AcademyQueueEntryComparator implements Comparator<AcademyQueueEntry>
	{
		@Override
		public int compare(AcademyQueueEntry o1, AcademyQueueEntry o2)
		{
			return o1.getPosition() - o2.getPosition();
		}
	}

	public static Map<Integer, String> getAttributes() {
		return attributes;
	}

	public static Map<Integer, String> getOfficers() {
		return offis;
	}
}
