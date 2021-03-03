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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.List;

/**
 * Die Werft.
 * @author Christopher Jung
 *
 */
@Entity(name="WerftBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Werft")
public class Werft extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Werft.
	 */
	public Werft() {
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
	public void build(Base base, Building building) {
		throw new IllegalArgumentException("shouldn't be called!");
	}


	@Override
	public void cleanup(Context context, Base base, int building) {
		throw new IllegalArgumentException("shouldn't be called!");
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		StringBuilder result = new StringBuilder(200);

		BaseWerft werft = base.getWerft();
		if( werft != null ) {
			werft.setBaseField(field);
			if( !werft.isBuilding() ) {
				result.append("<a class=\"back tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[W]<span class='ttcontent'>").append(this.getName()).append("</span></a>");
			}
			else {
				WerftObject werftObj = werft;
				if( werftObj.getKomplex() != null ) {
					werftObj = werftObj.getKomplex();
				}
				final List<WerftQueueEntry> entries = werftObj.getBuildQueue();
				final int totalSlots = werftObj.getWerftSlots();
				int usedSlots = 0;
				int buildingCount = 0;
				StringBuilder imBau = new StringBuilder();
				for (WerftQueueEntry entry : entries)
				{
					if( entry.isScheduled() ) {
						usedSlots += entry.getSlots();
						buildingCount++;
						imBau.append("<br />Aktuell im Bau: ").append(entry.getBuildShipType().getNickname()).append(" <img src='./data/interface/time.gif' alt='Dauer: ' />").append(entry.getRemainingTime());
					}
				}

				result.append("<a class=\"error tooltip\" href=\"./ds?module=building");
				result.append("&amp;col=");
				result.append(base.getId());
				result.append("&amp;field=");
				result.append(field);
				result.append("\">[W]<span style=\"font-weight:normal\">");
				result.append(entries.size());
				String popup = this.getName() + ":<br />" +
						"Belegte Werftslots: <img style='vertical-align:middle;border:0px' src='./data/interface/schiffinfo/werftslots.png' alt='' />" + usedSlots + "/" + totalSlots + "<br />" +
						"Im Bau: " + buildingCount + " Schiffe<br />" +
						"In der Warteschlange: " + (entries.size() - buildingCount) +
						imBau;
				result.append("</span><span class='ttcontent'>").append(popup).append("</span></a>");
			}
		}

		return result.toString();
	}

	@Override
	public boolean isActive(Base base, int status, int field) {
		BaseWerft werft = base.getWerft();
		if( werft != null ) {
			werft.setBaseField(field);
			return (werft.isBuilding());
		}

		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		//TODO: REMOVE
		return null;
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
