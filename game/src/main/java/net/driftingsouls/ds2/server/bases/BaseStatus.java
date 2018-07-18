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

import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;

/**
 * <code>BaseStatus</code> repraesentiert den aktuellen Status einer Basis. Dieser beinhaltet
 * Bevoelkerungszahlen, Gebaeudedaten sowie Verbrauchs/Produktionsbilanzen.
 * @author Christopher Jung
 *
 */
public class BaseStatus {
	private Cargo status;
    private Cargo nettoproductionstatus;
    private Cargo nettoconsumptionstatus;
	private int e;
	private int livingSpace;
	private int arbeiter;
	private Map<Integer,Integer> blocations;
	private Integer[] active;
	
	protected BaseStatus(Cargo status, Cargo nettoproductionstatus, Cargo nettoconsumptionstatus, int e, int bewohner, int arbeiter, Map<Integer,Integer> blocations, Integer[] active ) {
		super();
		this.status = status;
        this.nettoproductionstatus = nettoproductionstatus;
        this.nettoconsumptionstatus = nettoconsumptionstatus;
		this.e = e;
		this.livingSpace = bewohner;
		this.arbeiter = arbeiter;
		this.blocations = blocations;
		this.active = active;
	}

	/**
	 * Gibt die Liste der (de)aktivierten Gebaeude zurueck.
	 * Der Index ist die Feldnummer. Ein aktiviertes Gebaeude besitzt eine 1,
	 * ein deaktiviertes eine 0.
	 * 
	 * @return Liste der (de)aktivierten Gebaeude.
	 */
	public Integer[] getActiveBuildings() 
	{
		Integer[] copy = new Integer[active.length];
		System.arraycopy(active, 0, copy, 0, active.length);
		return copy;
	}

	/**
	 * Gibt die Arbeiter zurueck.
	 * @return die Arbeiter.
	 */
	public int getArbeiter() {
		return arbeiter;
	}

	/**
	 * Gibt den Wohnraum zurueck.
	 * 
	 * @return der Wohnraum.
	 */
	public int getLivingSpace() {
		return livingSpace;
	}

	/**
	 * Gibt die Position der einzelnen Gebaeude auf der Basis zurueck.
	 * Schluessel ist die Gebaeude-ID.
	 * 
	 * @return Die Gebaeudepositionen
	 */
	public Map<Integer,Integer> getBuildingLocations() {
		return blocations;
	}

	/**
	 * Gibt die pro Tick produzierte Energie zurueck.
	 * @return Die pro Tick produzierte Energie
	 */
	public int getEnergy() {
		return e;
	}

	/**
	 * Gibt den pro Tick produzierten/verbrauchten Cargo zurueck.
	 * 
	 * @return Die Cargobilanz
	 */
	public Cargo getProduction() {
		return status;
	}

    /**
     * Gibt den pro Tick produzierten Cargo zurueck.
     *
     * @return Der produzierte Cargo
     */
    public Cargo getNettoProduction() { return nettoproductionstatus; }

    /**
     * Gibt den pro Tick verbrauchten Cargo zurueck.
     *
     * @return Der verbrauchte Cargo
     */
    public Cargo getNettoConsumption() { return nettoconsumptionstatus; }
}
