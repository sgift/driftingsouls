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
package net.driftingsouls.ds2.server.werften;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.bases.Werft;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Repraesentiert eine Werft auf einer Basis in DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("B")
public class BaseWerft extends WerftObject {
	@OneToOne(fetch=FetchType.LAZY, mappedBy="werft")
	private Base base;

	@Transient
	private int fieldid = -1;

	/**
	 * Konstruktor.
	 *
	 */
	protected BaseWerft() {
		// EMPTY
	}

	/**
	 * Erstellt eine neue Werft.
	 * @param base Die Basis auf der die Werft stehen soll
	 */
	public BaseWerft(Base base) {
		super(WerftTyp.BASIS);
		this.base = base;
	}

	/**
	 * Gibt die ID der Basis zurueck, auf dem die Werft steht.
	 * @return Die ID der Basis
	 */
	public int getBaseID() {
		return this.base.getId();
	}

	/**
	 * Gibt die Feld-ID zurueck, auf dem die Werft steht. Sollte
	 * die Feld-ID unbekannt sein, so wird <code>-1</code> zurueckgegeben
	 * @return Die Feld-ID oder -1
	 */
	public int getBaseField() {
		return this.fieldid;
	}

	/**
	 * Setzt die Feld-ID, auf dem die Werft steht.
	 * @param field Die Feld-ID oder -1
	 */
	public void setBaseField(int field) {
		this.fieldid = field;
	}

	@Override
	public Cargo getCargo(boolean localonly) {
		return base.getCargo();
	}

	@Override
	public void setCargo(Cargo cargo, boolean localonly) {
		for (ResourceEntry entry : cargo.getResourceList())
		{
			if( entry.getCount1() < 0 )
			{
				throw new IllegalArgumentException("Der Cargo kann nicht negativ sein ("+entry.getId()+": "+entry.getCount1());
			}
		}

		base.setCargo(cargo);
	}

	@Override
	public long getMaxCargo( boolean localonly ) {
		return base.getMaxCargo();
	}

	@Override
	public int getCrew() {
		return base.getBewohner()-base.getArbeiter();
	}

	@Override
	public int getMaxCrew() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void setCrew(int crew) {
		if( crew < 0 )
		{
			throw new IllegalArgumentException("Crew < 0 (ist "+crew+")");
		}
		int bewohner = crew + base.getArbeiter();
		base.setBewohner(bewohner);
	}

	@Override
	public int getEnergy() {
		return base.getEnergy();
	}

	@Override
	public void setEnergy(int e) {
		if( e < 0 ) {
			throw new RuntimeException("ERROR: BaseWerft.setEnergy(): e ("+e+") kleiner 0");
		}

		base.setEnergy(e);
	}

	@Override
	public int canTransferOffis() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void transferOffi(int offi) {
		Offizier offizier = Offizier.getOffizierByID(offi);
		offizier.stationierenAuf(this.base);
	}

	@Override
	public String getUrlBase() {
		if( this.fieldid == -1 ) {
			throw new RuntimeException("BaseWerft: Werftfeld nicht gesetzt");
		}
		return "./ds?module=building&amp;col="+this.base.getId()+"&amp;field="+fieldid;
	}

	@Override
	public String getFormHidden() {
		if( this.fieldid == -1 ) {
			throw new RuntimeException("BaseWerft: Werftfeld nicht gesetzt");
		}
		return "<input type=\"hidden\" name=\"col\" value=\""+this.base.getId()+"\" />\n"+
			"<input type=\"hidden\" name=\"field\" value=\""+fieldid+"\" />\n"+
			"<input type=\"hidden\" name=\"module\" value=\"building\" />\n";
	}

	@Override
	public int getWerftSlots() {
		return 6;
	}

	@Override
	public int getX() {
		return base.getX();
	}

	@Override
	public int getY() {
		return base.getY();
	}

	@Override
	public int getSystem() {
		return this.base.getSystem();
	}

	@Override
	public User getOwner() {
		return this.base.getOwner();
	}

	@Override
	public String getName() {
		return base.getName();
	}

	@Override
	public int getSize() {
		return base.getSize();
	}

	@Override
	public String getWerftPicture() {
		User user = (User)ContextMap.getContext().getActiveUser();
		if( fieldid != -1 ) {
			Building building = Building.getBuilding(base.getBebauung()[fieldid]);
			return building.getPictureForRace(user.getRace());
		}

		org.hibernate.Session db = ContextMap.getContext().getDB();
		Werft building = (Werft)db.createQuery("from WerftBuilding")
			.setMaxResults(1)
			.uniqueResult();

		return building.getPictureForRace(user.getRace());
	}

	@Override
	public String getWerftName() {
		return base.getName();
	}

	@Override
	public boolean isLinkableWerft() {
		return false;
	}

	@Override
	public String getObjectUrl() {
		return Common.buildUrl("default", "module", "base", "col", base.getId());
	}

	@Override
	public double getWorkerPercentageAvailable()
	{
		return 1d;
	}

	@Override
	public void destroy()
	{
		this.base.setWerft(null);

		super.destroy();
	}

	@Override
	public String toString()
	{
		return "BaseWerft{" +
			   "id=" + this.getWerftID()+
			   ", base=" + base +
			   ", fieldid=" + fieldid +
			   '}';
	}
}
