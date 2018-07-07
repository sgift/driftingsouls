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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * Repraesentiert eine Werft auf einem Schiff in DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("S")
public class ShipWerft extends WerftObject {
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="shipid")
	@ForeignKey(name="werften_fk_ships")
	private Ship ship;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="linked", nullable=true)
	@ForeignKey(name="werften_fk_bases")
	private Base linked;

	/**
	 * Konstruktor.
	 *
	 */
	public ShipWerft() {
		// EMPTY
	}

	/**
	 * Erstellt eine neue Schiffswerft.
	 * @param ship Das Schiff auf dem sich die Werft befinden soll
	 */
	public ShipWerft(Ship ship) {
		this.ship = ship;
		this.linked = null;
	}

	@Override
	public ShipType getOneWayFlag() {
		return this.ship.getTypeData().getOneWayWerft();
	}

	/**
	 * Gibt zurueck, ob die Werft mit einer Basis verbunden ist.
	 * @return true, falls eine Verbindung existiert
	 */
	public boolean isLinked() {
		return linked != null;
	}

	/**
	 * Gibt die verbundenen Basis zurueck.
	 * @return Die Basis
	 */
	public Base getLinkedBase() {
		return linked;
	}

	/**
	 * Setzt die Verbindung mit einer Basis zurueck. Die Verbindung
	 * existiert folglich danach nicht mehr
	 */
	public void resetLink() {
		setLink(null);
	}

	/**
	 * Erstellt eine Verbindung mit einer angegebenen Basis.
	 * @param base Die Basis
	 */
	public void setLink( Base base ) {
		// In einem Komplex darf eine Basis nur einmal vorkommen
		if( (base != null) && (getKomplex() != null) ) {
			WerftObject[] members = getKomplex().getMembers();
			for (WerftObject member1 : members)
			{
				if (!(member1 instanceof ShipWerft))
				{
					continue;
				}
				if (member1.getWerftID() == this.getWerftID())
				{
					continue;
				}

				ShipWerft member = (ShipWerft) member1;
				if (member.getLinkedBase() == base)
				{
					return;
				}
			}
		}

		linked = base;
	}

	/**
	 * Gibt die Schiffs-ID zurueck, auf dem sich die Werft befindet.
	 * @return Die ID des Schiffs, auf dem sich die Werft befindet
	 */
	public int getShipID() {
		return ship.getId();
	}

	/**
	 * Gibt das Schiff zurueck, auf dem sich die Werft befindet.
	 * @return Das Schiff
	 */
	public Ship getShip() {
		return this.ship;
	}

	@Override
	public Cargo getCargo(boolean localonly) {
		Cargo cargo = ship.getCargo();

		if( linked != null && !localonly ) {
			Cargo basecargo = linked.getCargo();

			cargo.addCargo( basecargo );

		}
		return cargo;
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

		if( (this.linked != null) && !localonly ) {
			ShipTypeData shiptype = this.ship.getTypeData();

			Cargo basecargo = this.linked.getCargo();

			cargo.substractCargo( basecargo );

			ResourceList reslist = cargo.getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() < 0 ) {
					basecargo.addResource( res.getId(), cargo.getResourceCount( res.getId() ) );
					cargo.setResource( res.getId(), 0 );
				}
			}

			// Ueberpruefen, ob wir nun zu viel Cargo auf dem Schiff haben
			long cargocount = cargo.getMass();

			if( cargocount > shiptype.getCargo() ) {
				Cargo shipcargo = cargo.cutCargo(shiptype.getCargo());
				basecargo.addCargo(cargo);
				cargo = shipcargo;
			}
			this.linked.setCargo(basecargo);
		}

		this.ship.setCargo(cargo);
	}

	@Override
	public long getMaxCargo( boolean localonly ) {
		ShipTypeData shiptype = this.ship.getTypeData();
		long cargo = shiptype.getCargo();
		if( this.linked != null && !localonly ) {
			cargo += this.linked.getMaxCargo();
		}
		return cargo;
	}

	@Override
	public int getCrew() {
		int crew = this.ship.getCrew();
		if( this.linked != null ) {
			crew += (this.linked.getBewohner()-this.linked.getArbeiter());
		}
		return crew;
	}

	@Override
	public int getMaxCrew() {
		if( this.linked == null ) {
			ShipTypeData shiptype = this.ship.getTypeData();
			return shiptype.getCrew();
		}
		return 99999;
	}

	@Override
	public String toString()
	{
		return "ShipWerft{" +
			   "id=" + this.getWerftID()+
			   ", ship=" + ship +
			   ", linked=" + linked +
			   '}';
	}

	@Override
	public void setCrew(int crew) {
		if( crew < 0 )
		{
			throw new IllegalArgumentException("Crew < 0 (ist "+crew+")");
		}
		if( crew > this.getMaxCrew() ) {
			crew = getMaxCrew();
		}
		int shipcrew;
		if( this.linked != null )
		{
			ShipTypeData shiptype = this.ship.getTypeData();
			int basecrew = this.linked.getBewohner()-this.linked.getArbeiter();
			shipcrew = this.ship.getCrew();

			if( crew < shipcrew+basecrew ) {
				crew -= shipcrew;
				if( crew < 0 ) {
					shipcrew += crew;
					crew = 0;
				}
				basecrew = crew;
			}
			else {
				crew -= basecrew;
				if( crew < 1 ) {
					basecrew += crew-1;
					crew = 0;
				}

				if( crew > shiptype.getCrew() ) {
					basecrew += crew-shiptype.getCrew();
					crew = shiptype.getCrew();
				}
				shipcrew = crew;
			}

			int bewohner = basecrew + this.linked.getArbeiter();
			this.linked.setBewohner(bewohner);
		}
		else
		{
			shipcrew = crew;
		}
		this.ship.setCrew(shipcrew);
	}

	@Override
	public int getEnergy() {
		int e = this.ship.getEnergy();

		if( this.linked != null ) {
			e += this.linked.getEnergy();
		}

		return e;
	}

	@Override
	public void setEnergy(int e) {
		if( e < 0 ) {
			throw new RuntimeException("ERROR: ShipWerft.setEnergy(): e ("+e+") kleiner 0");
		}

		if( this.linked != null ) {
			int basee = this.linked.getEnergy();

			e -= basee;

			if( e < 0 ) {
				basee += e;
				e = 0;
			}

			this.linked.setEnergy(basee);
		}

		this.ship.setEnergy(e);
	}

	@Override
	public int canTransferOffis() {
		if( this.linked == null ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();

			int officount = ((Number)db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest")
				.setEntity("dest", this.ship)
				.iterate().next()).intValue();
			int maxoffis = 1;
			ShipTypeData shiptype = this.ship.getTypeData();
			if( shiptype.hasFlag(ShipTypeFlag.OFFITRANSPORT) ) {
				maxoffis = shiptype.getCrew();
			}
			if( officount >= maxoffis ) {
				return 0;
			}
			return maxoffis-officount;
		}
		return 99999;
	}

	@Override
	public void transferOffi(int offi)
	{
		if( this.canTransferOffis() == 0 )
		{
			throw new RuntimeException("ERROR: ShipWerft.transferOffi(): Kein Platz f&uuml;r weitere Offiziere");
		}

		Offizier offizier = Offizier.getOffizierByID(offi);

		if( this.linked == null )
		{
			offizier.stationierenAuf(this.ship);
		}
		else
		{
			Offizier myoffi = this.ship.getOffizier();
			if( myoffi != null )
			{
				offizier.stationierenAuf(this.linked);
			}
			else
			{
				offizier.stationierenAuf(this.ship);
			}
		}
	}

	@Override
	public String getUrlBase() {
		return "./ds?module=werft&amp;ship="+this.ship.getId();
	}

	@Override
	public String getFormHidden() {
		return "<input type=\"hidden\" name=\"ship\" value=\""+this.ship.getId()+"\" />\n"+
			"<input type=\"hidden\" name=\"module\" value=\"werft\" />\n";
	}

	@Override
	public int getWerftSlots() {
		return this.ship.getTypeData().getWerft();
	}

	@Override
	public int getX() {
		return this.ship.getX();
	}

	@Override
	public int getY() {
		return this.ship.getY();
	}

	@Override
	public int getSystem() {
		return this.ship.getSystem();
	}

	@Override
	public String getName() {
		return this.ship.getName();
	}

	@Override
	public User getOwner() {
		return this.ship.getOwner();
	}

	@Override
	public void onFinishedBuildProcess(int shipid) {
		super.onFinishedBuildProcess(shipid);

		// Falls es sich um eine Einwegwerft handelt, dann diese zerstoeren
		if( getType() == WerftTyp.EINWEG ) {
			getShip().destroy();
		}
	}

	@Override
	public boolean repairShip(@Nonnull Ship ship, boolean testonly) {
		boolean result = super.repairShip(ship, testonly);

		this.ship.recalculateShipStatus();
		return result;
	}

	@Override
	public void removeModule( @NotNull Ship ship, int slot ) {
		super.removeModule( ship, slot );

		this.ship.recalculateShipStatus();
	}

	@Override
	public void addModule(@Nonnull Ship ship, int slot, int item ) {
		super.addModule( ship, slot, item );

		this.ship.recalculateShipStatus();
	}

	@Override
	public boolean dismantleShip(@Nonnull Ship ship, boolean testonly) {
		boolean result = super.dismantleShip(ship, testonly);

		this.ship.recalculateShipStatus();
		return result;
	}

    @Override
    public boolean isEinwegWerft()
    {
        return this.getType() == WerftTyp.EINWEG || this.getOneWayFlag() != null;
    }

	@Override
	public boolean buildShip( int build, int item, boolean costsPerTick, boolean testonly ) {
		boolean result = super.buildShip(build, item, costsPerTick, testonly);

        // Reste aufräumen, die hier reingehören.
        if(result && !testonly)
        {
            if( this.getOneWayFlag() != null) {
                // Einweg-Werft-Code
                Context context = ContextMap.getContext();
                User user = this.getOwner();

                ShipType newtype = this.getOneWayFlag();

                String currentTime = Common.getIngameTime(context.get(ContextCommon.class).getTick());
                String history = "Baubeginn am "+currentTime+" durch "+user.getName()+" ("+user.getId()+")";

                Ship ship = this.getShip();
                ship.getHistory().addHistory(history);
                ship.setName("Baustelle");
                ship.setBaseType(newtype);
                ship.setHull(newtype.getHull());
                ship.setAblativeArmor(newtype.getAblativeArmor());
                ship.setCrew(newtype.getCrew());
                ship.setEnergy(newtype.getEps());
                ship.setEnergy(newtype.getEps());
                ship.setOwner(user);
                ship.recalculateModules();

                this.setType(WerftTyp.EINWEG);
            }
        }
		this.ship.recalculateShipStatus();
		return result;
	}

	@Override
	public String getWerftPicture() {
		return this.ship.getTypeData().getPicture();
	}

	@Override
	public String getWerftName() {
		return this.ship.getName();
	}

	@Override
	public boolean isLinkableWerft() {
		return this.ship.getTypeData().hasFlag(ShipTypeFlag.WERFTKOMPLEX);
	}

	@Override
	public String getObjectUrl() {
		return Common.buildUrl("default", "module", "schiff", "ship", ship.getId());
	}

	@Override
	public void addToKomplex(@Nonnull WerftKomplex linkedWerft) {
		super.addToKomplex(linkedWerft);

		// Falls notwendig den Link auf die Basis entfernen - in einem Komplex darf eine Basis
		// nur einmal vorkommen
		if( this.linked != null ) {
			WerftObject[] members = linkedWerft.getMembers();
			for (WerftObject member1 : members)
			{
				if (!(member1 instanceof ShipWerft))
				{
					continue;
				}
				if (member1.getWerftID() == this.getWerftID())
				{
					continue;
				}

				ShipWerft member = (ShipWerft) member1;
				if (member.getLinkedBase() == this.getLinkedBase())
				{
					this.setLink(null);
					return;
				}
			}
		}
	}

	@Override
	public void createKomplexWithWerft(@Nonnull WerftObject werft) {
		super.createKomplexWithWerft(werft);

		// Falls notwendig den Link auf die Basis entfernen - in einem Komplex darf eine Basis
		// nur einmal vorkommen
		if( this.linked != null ) {
			WerftObject[] members = this.getKomplex().getMembers();
			for (WerftObject member1 : members)
			{
				if (!(member1 instanceof ShipWerft))
				{
					continue;
				}
				if (member1.getWerftID() == this.getWerftID())
				{
					continue;
				}

				ShipWerft member = (ShipWerft) member1;
				if (member.getLinkedBase() == this.getLinkedBase())
				{
					this.setLink(null);
					return;
				}
			}
		}
	}

	@Override
	public double getWorkerPercentageAvailable()
	{
		return this.ship.getCrew() / (double)this.ship.getTypeData().getCrew();
	}
}
