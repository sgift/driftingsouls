package net.driftingsouls.ds2.server.framework;

import net.sf.json.JSON;

/**
 * Marker-Interface fuer JSON-Support.
 * @author Christopher Jung
 *
 */
public interface JSONSupport
{
	/**
	 * Konvertiert das Objekt in ein JSON-Objekt.
	 * @return Das Objekt als JSON-Objekt
	 */
	public JSON toJSON();
}
