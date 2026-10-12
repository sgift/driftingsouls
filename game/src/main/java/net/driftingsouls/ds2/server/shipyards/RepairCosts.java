package net.driftingsouls.ds2.server.shipyards;

import net.driftingsouls.ds2.server.cargo.Cargo;

/**
 * Die Reparaturkosten eines Schiffes.
 *
 */
public class RepairCosts
{
	/**
	 * Die Energiekosten.
	 */
	public int e;
	/**
	 * Die Resourcenkosten.
	 */
	public Cargo cost;

	RepairCosts() {
		//EMPTY
	}
}
