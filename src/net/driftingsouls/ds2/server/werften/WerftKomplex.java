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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;

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
	private boolean komplex = true;

	@Transient
	private boolean exists = true;

	/**
	 * Konstruktor.
	 */
	public WerftKomplex() {
	}

	@Transient
	private WerftObject[] werften;

	/**
	 * Ueberprueft, ob noch alle Werften an der selben Position sind. Wenn dies nicht mehr der Fall ist,
	 * werden Werften an anderen Positionen entfernt so, dass sich wieder nur Werften an der selben Position
	 * befinden.
	 *
	 */
	public void checkWerftLocations() {
		loadData();

		// Werften in eine Positionsmap eintragen. Die Position mit den meisten Werften merken
		Location maxLocation = null;
		Map<Location,List<WerftObject>> werftPos = new HashMap<>();

		for (WerftObject aWerften : werften)
		{
			Location loc = aWerften.getLocation();

			if (!werftPos.containsKey(loc))
			{
				werftPos.put(loc, new ArrayList<>());
			}

			werftPos.get(loc).add(aWerften);

			if (maxLocation == null)
			{
				maxLocation = loc;
			}
			else if (!maxLocation.equals(loc))
			{
				if (werftPos.get(maxLocation).size() < werftPos.get(loc).size())
				{
					maxLocation = loc;
				}
			}
		}

		// Alle Werften, die nicht an der Stelle mit den meisten Werften sind, entfernen
		if( werftPos.keySet().size() != 1 ) {
			for( Map.Entry<Location, List<WerftObject>> entry: werftPos.entrySet() ) {
				Location loc = entry.getKey();
				if( !loc.equals(maxLocation) ) {
					List<WerftObject> werftListe = entry.getValue();

					for (WerftObject aWerftListe : werftListe)
					{
						aWerftListe.removeFromKomplex();
					}
				}
			}
		}
	}

	private void loadData()
	{
		if( werften == null )
		{
			org.hibernate.Session db = ContextMap.getContext().getDB();

			List<WerftObject> werftList = Common.cast(db.createQuery("from WerftObject where linkedWerft=:werft order by id")
				.setEntity("werft", this)
				.list());

			List<WerftObject> werften = new ArrayList<>(werftList.size());

			for( WerftObject aWerft : werftList )
			{
				// Sicherheitshalber eine weitere Pruefung, da ggf eine Werft gerade dabei
				// ist den Komplex zu verlassen (aber die Daten noch nicht geschrieben sind)
				if(aWerft.getKomplex() != null && aWerft.getKomplex().getWerftID() == this.getWerftID() )
				{
					werften.add(aWerft);
				}
			}

			this.werften = werften.toArray(new WerftObject[werften.size()]);
			if( werften.isEmpty())
			{
				this.werften = null;
				db.delete(this);
				this.exists = false;
			}
		}
	}

	@Override
	public int canTransferOffis() {
		loadData();

		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.canTransferOffis();
		}
		return count;
	}

	@Override
	public Cargo getCargo(boolean localonly) {
		loadData();

		Cargo cargo = new Cargo();
		for (WerftObject aWerften : werften)
		{
			cargo.addCargo(aWerften.getCargo(localonly));
		}
		return cargo;
	}

	@Override
	public int getCrew() {
		loadData();

		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getCrew();
		}
		return Math.min(count, 99999);
	}

	@Override
	public int getEnergy() {
		loadData();

		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getEnergy();
		}
		return count;
	}

	@Override
	public String getFormHidden() {
		loadData();

		if (!isExistant())
		{
			return "";
		}

		return werften[0].getFormHidden();
	}

	@Override
	public long getMaxCargo(boolean localonly) {
		loadData();

		long count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getMaxCargo(localonly);
		}
		return count;
	}

	@Override
	public int getMaxCrew() {
		loadData();

		int count = 0;

		for (WerftObject aWerften : werften)
		{
			count += aWerften.getMaxCrew();
		}
		return count;
	}

	@Override
	public String getName() {
		loadData();

		return "Werftkomplex "+this.getWerftID()+" ("+werften.length+" Werften)";
	}

	@Override
	public User getOwner() {
		loadData();

		if (!isExistant())
		{
			return null;
		}

		return werften[0].getOwner();
	}

	@Override
	public int getSystem() {
		loadData();

		if (!isExistant())
		{
			return 0;
		}

		return werften[0].getSystem();
	}

	@Override
	public String getUrlBase() {
		loadData();

		if (!isExistant())
		{
			return "";
		}

		return werften[0].getUrlBase();
	}

	@Override
	public String getWerftName() {
		return getName();
	}

	@Override
	public String getWerftPicture() {
		loadData();

		if (!isExistant())
		{
			return "";
		}

		return werften[0].getWerftPicture();
	}

	@Override
	public int getWerftSlots() {
		loadData();

		int slots = 0;
		for (WerftObject aWerften : werften)
		{
			slots += aWerften.getWerftSlots();
		}
		return slots;
	}

	@Override
	public int getX() {
		loadData();

		if (!isExistant())
		{
			return 0;
		}

		return werften[0].getX();
	}

	@Override
	public int getY() {
		loadData();

		if (!isExistant())
		{
			return 0;
		}

		return werften[0].getY();
	}

	@Override
	public boolean isLinkableWerft() {
		return false;
	}

	@Override
	public void setCargo(Cargo cargo, boolean localonly) {
		loadData();

		if( cargo.getMass() > this.getMaxCargo(localonly) ) {
			cargo = cargo.cutCargo(this.getMaxCargo(localonly));
		}

		// Zuerst alles gemaess der vorher vorhandenen Verhaeltnisse verteilen
		Cargo oldCargo = getCargo(localonly);
		ResourceList reslist = oldCargo.compare(cargo, true);
		for( ResourceEntry res : reslist ) {
			final long oldCount = res.getCount1();
			long count = res.getCount2();

			if( count == oldCount ) {
				continue;
			}

			double factor = 0;
			if( oldCount > 0 ) {
				factor = count/(double)oldCount;
			}

			for (WerftObject aWerften : werften)
			{
				Cargo werftCargo = aWerften.getCargo(localonly);
				long newCount = (long) (werftCargo.getResourceCount(res.getId()) * factor);

				count -= newCount;
				if (count < 0)
				{
					newCount += count;
					count = 0;
				}
				werftCargo.setResource(res.getId(), newCount);
				aWerften.setCargo(werftCargo, localonly);
			}

			// Falls noch Crew uebrig geblieben ist, diese auf die erst beste Werft schicken
			if( count > 0 ) {
				Cargo werftCargo = werften[0].getCargo(localonly);
				werftCargo.addResource(res.getId(), count);
				werften[0].setCargo(werftCargo, localonly);
			}
		}

		// MaxCargo ueberpruefen und Waren ggf umverteilen. Maximal 100 Iterationen
		for( int iteration=100; iteration>0; iteration-- ) {
			Cargo overflow = new Cargo();

			for (WerftObject aWerften : werften)
			{
				Cargo werftCargo = aWerften.getCargo(localonly);
				if (werftCargo.getMass() > aWerften.getMaxCargo(localonly))
				{
					Cargo tmp = werftCargo.cutCargo(aWerften.getMaxCargo(localonly));
					overflow.addCargo(werftCargo);
					aWerften.setCargo(tmp, localonly);
				}
				else if (!overflow.isEmpty())
				{
					if (overflow.getMass() + werftCargo.getMass() < aWerften.getMaxCargo(localonly))
					{
						werftCargo.addCargo(overflow);
						overflow = new Cargo();
					}
					else
					{
						werftCargo.addCargo(overflow.cutCargo(aWerften.getMaxCargo(localonly) - werftCargo.getMass()));
					}
					aWerften.setCargo(werftCargo, localonly);
				}
			}

			// Falls nichts mehr zu verteilen ist abbrechen
			if( overflow.isEmpty() ) {
				break;
			}
		}
	}

	// TODO: statt setCrew sollte es die Funktion addCrew bzw removeCrew geben.
	// FIXME: Das hier funktioniert jedenfalls nicht richtig bei sehr grossen Werftkomplexen mit Basen.....
	@Override
	public void setCrew(int crew) {
		loadData();

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
		loadData();

		// Zuerst alles gemaess der vorher vorhandenen Verhaeltnisse verteilen
		int oldE = getEnergy();
		double factor = 0;
		if( oldE > 0 ) {
			factor = e/(double)oldE;
		}

		// Es gilt die Annahme, dass Energie nur weniger werden kann...
		List<Integer> maxE = new ArrayList<>();

		for( int i=0; i < werften.length; i++ ) {
			maxE.add(i, werften[i].getEnergy());

			int newE = Math.min((int)(werften[i].getEnergy()*factor), werften[i].getEnergy());

			e -= newE;
			if( e < 0 ) {
				newE += e;
			}
			werften[i].setEnergy(newE);
		}

		// Falls noch Energie uebrig geblieben ist, diese auf die erst beste Werft schicken
		if( e > 0 ) {
			for( int i=0; i < werften.length; i++ ) {
				int freeSpace = maxE.get(i)-werften[i].getEnergy();
				if( freeSpace > 0 ) {
					int transfer = Math.min(freeSpace, e);
					werften[i].setEnergy(werften[i].getEnergy()+transfer);

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
		loadData();

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
	public WerftObject[] getMembers() {
		loadData();

		return werften.clone();
	}

	@Override
	public String getObjectUrl() {
		loadData();

		if (!isExistant())
		{
			return "";
		}

		return werften[0].getObjectUrl();
	}

	@Override
	public double getWorkerPercentageAvailable()
	{
		loadData();

		double value = 0;

		for (WerftObject aWerften : werften)
		{
			value += aWerften.getWorkerPercentageAvailable();
		}
		return value / werften.length;
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
