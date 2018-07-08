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
package net.driftingsouls.ds2.server.ships;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

/**
 * Historieninformationen zu einem Schiff. Protokolliert fuer das Schiff relevante Aktionen 
 * wie beispielsweise Uebergaben an andere Spieler.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_history")
public class ShipHistory
{
	private static final Log log = LogFactory.getLog(ShipHistory.class);
	
	@SuppressWarnings("unused")
	@Id @GeneratedValue(generator="foreign")   
	@GenericGenerator(name="foreign", strategy = "foreign", parameters={@Parameter(name="property",value="ship")})  
	private int id;

	@SuppressWarnings("unused")
	@OneToOne()
	@JoinColumn(name="id")
	private Ship ship;

	@Lob
	private String history;
	
	protected ShipHistory()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param ship Das Schiff, zu dem die Historienangaben gehoeren.
	 */
	public ShipHistory(Ship ship)
	{
		this.history = "";
		this.ship = ship;
	}

	/**
	 * Gibt die Schiffshistorie zurueck.
	 * @return Die Schiffshistorie
	 */
	public String getHistory()
	{
		return history;
	}

	/**
	 * Setzt die Schiffshistorie.
	 * @param history Die neue Schiffshistorie
	 */
	public void setHistory(String history)
	{
		this.history = history;
	}
	
	/**
	 * Fuegt einen Eintrag zur Schiffshistorie hinzu.
	 * @param history Der Text des Eintrags
	 */
	public void addHistory(String history)
	{
		this.history += history+"\n";
	}
	
	/**
	 * Gibt den Tick zurueck, an dem die letzte Uebergabe des Schiffes an einen anderen Spieler stattgefunden hat.
	 * Falls noch keine Uebergabe durchgefuehrt wurde wird <code>-1</code> zurueckgegeben.
	 * @return Der Tick oder <code>-1</code>
	 */
	public int getLastUebergabeTick()
	{
		String[] history = StringUtils.split(this.history.trim(), '\n');
		if( history.length > 0 ) {
			String lastHistory = history[history.length-1].trim();

			final int length = "&Uuml;bergeben am [tick=".length();

			if( lastHistory.startsWith("&Uuml;bergeben am [tick=") )
			{
				int endIndex = lastHistory.indexOf("] an ",length);
				if( endIndex > -1 ) {				
					try
					{

						return Integer.parseInt(
								lastHistory.substring(
										length,
										endIndex
								)
						);
					}
					catch( StringIndexOutOfBoundsException e )
					{
						log.warn("[Ships.generateLoot] Fehler beim Parsen des Schiffshistoryeintrags '"+lastHistory+"' - "+length+", "+endIndex);
					}
				}
				else
				{
					log.warn("[Ships.generateLoot] Fehler beim Parsen des Schiffshistoryeintrags '"+lastHistory+"'");
				}
			}
		}
		return -1;
	}
}
