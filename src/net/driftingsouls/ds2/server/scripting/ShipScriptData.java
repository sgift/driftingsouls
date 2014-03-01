package net.driftingsouls.ds2.server.scripting;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Die Scriptdaten eines Schiffes.
 *
 * @author Drifting-Souls Team
 */
@Entity
@Table(name="ship_script_data")
public class ShipScriptData
{
	@Id @GeneratedValue
	private int shipid;
	@Lob
	private String script;
	@Lob
	private byte[] scriptexedata;

	/**
	 * Konstruktor.
	 */
	public ShipScriptData()
	{
		// EMPTY
	}

	/**
	 * @return Die ID des dazugehoerigen Schiffes.
	 */
	public int getShipid()
	{
		return shipid;
	}

	/**
	 * Setzt die ID des dazugehoerigen Schiffes.
	 *
	 * @param shipid Die Schiffsid.
	 */
	public void setShipid(int shipid)
	{
		this.shipid = shipid;
	}

	/**
	 * @return Das vom Schiff ausgefuehrte Script.
	 */
	public String getScript()
	{
		return script;
	}

	/**
	 * Setzt ein neues Script fuer das Schiff.
	 *
	 * @param script Das neue Script.
	 */
	public void setScript(String script)
	{
		this.script = script;
	}

	/**
	 * @return Der aktuelle Ausfuehrungsstand des Schiffsscripts.
	 */
	public byte[] getScriptexedata()
	{
		return scriptexedata;
	}

	/**
	 * Setzt einen neuen Ausfuehrungsstand.
	 *
	 * @param scriptexedata Der neue Ausfuehrungsstand.
	 */
	public void setScriptexedata(byte[] scriptexedata)
	{
		this.scriptexedata = scriptexedata;
	}
}
