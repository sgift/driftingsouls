package net.driftingsouls.ds2.server.cargo;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.entities.User;

/**
 * Interface fuer Objekte, welche den Transfer von Resourcen
 * von sich zu einem anderen {@link Transfering}-Objekt beherrschen.
 * 
 * @author Sebastian Gift
 *
 */
public interface Transfering extends Locatable {
	/**
	 * Gibt den Cargo des Objekts zurueck.
	 * @return Der Cargo
	 */
    Cargo getCargo();
	/**
	 * Setzt den Cargo des Objekts.
	 * @param cargo Der Cargo
	 */
    void setCargo(Cargo cargo);
	/**
	 * Gibt den maximalen Cargo, den das Objekt aufnehmen kann, zurueck.
	 * @return Der maximale Cargo
	 */
    long getMaxCargo();
	/**
	 * Transfers resources to another object.
	 * 
	 * @param to Any object that can transfer/accept transfers.
	 * @param resource Resource that should be transfered.
	 * @param count Amout to be transfered.
	 * @return Informations about the outcome.
	 */
    String transfer(Transfering to, ResourceID resource, long count);
	/**
	 * Gibt den Besitzer des Objekts zurueck.
	 * @return Der Besitzer
	 */
    User getOwner();
}
