/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * Repraesentiert eine Menge von Werften (Werftkomplex).
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("K")
public class WerftKomplex extends WerftObject {
	//Used within the database to check for komplex
	@SuppressWarnings("unused")
	private final boolean komplex = true;

	@OneToMany(fetch = FetchType.EAGER, mappedBy = "linkedWerft")
	private List<WerftObject> werften = new ArrayList<>();

	@Transient
	private boolean exists = true;

	/**
	 * Konstruktor.
	 */
	public WerftKomplex() {
	}

	public List<WerftObject> getWerften() {
		return werften;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}

	@Override
	public int canTransferOffis() {
		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.canTransferOffis();
		}
		return count;
	}

	@Override
	public Cargo getCargo(boolean localonly) {
		Cargo cargo = new Cargo();
		for (WerftObject aWerften : werften)
		{
			cargo.addCargo(aWerften.getCargo(localonly));
		}
		return cargo;
	}

	@Override
	public int getCrew() {
		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getCrew();
		}
		return Math.min(count, 99999);
	}

	@Override
	public int getEnergy() {
		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getEnergy();
		}
		return count;
	}

	@Override
	public String getFormHidden() {
		if (!isExistant())
		{
			return "";
		}

		return werften.get(0).getFormHidden();
	}

	@Override
	public long getMaxCargo(boolean localonly) {
		long count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getMaxCargo(localonly);
		}
		return count;
	}

	@Override
	public int getMaxCrew() {
		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getMaxCrew();
		}
		return count;
	}

	@Override
	public String getName() {
		return "Werftkomplex "+this.getWerftID()+" ("+werften.size()+" Werften)";
	}

	@Override
	public User getOwner() {
		if (!isExistant())
		{
			return null;
		}

		return werften.get(0).getOwner();
	}

	@Override
	public int getSystem() {
		if (!isExistant())
		{
			return 0;
		}

		return werften.get(0).getSystem();
	}

	@Override
	public String getUrlBase() {
		if (!isExistant())
		{
			return "";
		}

		return werften.get(0).getUrlBase();
	}

	@Override
	public String getWerftName() {
		return getName();
	}

	@Override
	public String getWerftPicture() {
		if (!isExistant())
		{
			return "";
		}

		return werften.get(0).getWerftPicture();
	}

	@Override
	public int getWerftSlots() {
		int slots = 0;
		for (WerftObject aWerften : werften)
		{
			slots += aWerften.getWerftSlots();
		}
		return slots;
	}

	@Override
	public int getX() {
		if (!isExistant())
		{
			return 0;
		}

		return werften.get(0).getX();
	}

	@Override
	public int getY() {
		if (!isExistant())
		{
			return 0;
		}

		return werften.get(0).getY();
	}

	@Override
	public boolean isLinkableWerft() {
		return false;
	}

	// TODO: statt setCrew sollte es die Funktion addCrew bzw removeCrew geben.
	// FIXME: Das hier funktioniert jedenfalls nicht richtig bei sehr grossen Werftkomplexen mit Basen.....
	@Override
	public void setCrew(int crew) {
		// Zuerst von allen Verfügbaren Basen ziehen
		int oldCrew = getCrew();
		int subCrew = oldCrew - crew;

		for (WerftObject aWerften : werften)
		{
			if (((ShipWerft) aWerften).getLinkedBase() != null)
			{
				Base linked = ((ShipWerft) aWerften).getLinkedBase();
				int baseCrew = linked.getBewohner() - linked.getArbeiter();
				if (baseCrew >= subCrew)
				{
					baseCrew -= subCrew;
					subCrew = 0;
				}
				else
				{
					subCrew -= baseCrew;
					baseCrew = 0;
				}
				linked.setBewohner(baseCrew + linked.getArbeiter());
			}
			if (subCrew == 0)
			{
				return;
			}
		}
		crew -= oldCrew - crew - subCrew;
		oldCrew = getCrew();

		// Danach alles gemaess den vorher vorhandenen Verhaeltnissen verteilen
		// Da alle Basen bereits ihr Maximum ausgeschoepft haben wird mit getCrew()
		// nur noch die Crew von den Werften erwischt
		double factor = 0;
		if( oldCrew > 0 ) {
			factor = crew/(double)oldCrew;
		}

		for (WerftObject aWerften : werften)
		{
			int newCrew = Math.min((int) (aWerften.getCrew() * factor), aWerften.getMaxCrew());

			crew -= newCrew;
			if (crew < 0)
			{
				newCrew += crew;
			}
			aWerften.setCrew(newCrew);

			if( crew < 0 )
			{
				break;
			}
		}

		// Falls noch Crew uebrig geblieben ist, diese auf die erst beste Werft schicken
		if( crew > 0 ) {
			for (WerftObject aWerften : werften)
			{
				int freeSpace = aWerften.getMaxCrew() - aWerften.getCrew();
				if (freeSpace > 0)
				{
					int transfer = Math.min(freeSpace, crew);
					aWerften.setCrew(aWerften.getCrew() + transfer);

					crew -= transfer;
					if (crew == 0)
					{
						return;
					}
				}
			}
		}
	}

	@Override
	public String toString()
	{
		return "WerftKomplex{" +
			   "id=" + this.getWerftID()+
			   "}";
	}

	@Override
	public void setEnergy(int e) {
		// Zuerst alles gemaess der vorher vorhandenen Verhaeltnisse verteilen
		int oldE = getEnergy();
		double factor = 0;
		if( oldE > 0 ) {
			factor = e/(double)oldE;
		}

		// Es gilt die Annahme, dass Energie nur weniger werden kann...
		List<Integer> maxE = new ArrayList<>();

		for( int i=0; i < werften.size(); i++ ) {
			maxE.add(i, werften.get(i).getEnergy());

			int newE = Math.min((int)(werften.get(i).getEnergy()*factor), werften.get(i).getEnergy());

			e -= newE;
			if( e < 0 ) {
				newE += e;
			}
			werften.get(i).setEnergy(newE);
		}

		// Falls noch Energie uebrig geblieben ist, diese auf die erst beste Werft schicken
		if( e > 0 ) {
			for( int i=0; i < werften.size(); i++ ) {
				int freeSpace = maxE.get(i)-werften.get(i).getEnergy();
				if( freeSpace > 0 ) {
					int transfer = Math.min(freeSpace, e);
					werften.get(i).setEnergy(werften.get(i).getEnergy()+transfer);

					e -= transfer;
					if( e == 0 ) {
						return;
					}
				}
			}
		}
	}

	@Override
	public void transferOffi(int offi) {
		for (WerftObject aWerften : werften)
		{
			if (aWerften.canTransferOffis() > 0)
			{
				aWerften.transferOffi(offi);
				return;
			}
		}
	}

	/**
	 * Laedt die Daten des Werftkomplexes neu.
	 *
	 */
	public void refresh() {
		this.werften = null;
	}

	/**
	 * Gibt die Mitglieder im Werftkomplex zurueck.
	 * @return Die Werften
	 */
	public List<WerftObject> getMembers() {
		return new ArrayList<>(werften);
	}

	@Override
	public String getObjectUrl() {
		if (!isExistant())
		{
			return "";
		}

		return werften.get(0).getObjectUrl();
	}

	@Override
	public double getWorkerPercentageAvailable()
	{
		double value = 0;

		for (WerftObject aWerften : werften)
		{
			value += aWerften.getWorkerPercentageAvailable();
		}
		return value / werften.size();
	}

	/**
	 * Gibt zurueck, ob dieser Werftkomplex noch existiert.
	 * Wird vom WerftTick verwendet, falls Werftkomplexe ohne Werften vorhanden sind
	 * @return <code>true</code>, falls der Werftkomplex existiert
	 */
	public boolean isExistant()
	{
		return exists;
	}
}
