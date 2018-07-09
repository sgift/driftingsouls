package net.driftingsouls.ds2.server.modules.fraktionen;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopEntry;
import net.driftingsouls.ds2.server.framework.Common;

/**
 * Ein Eintrag im Shop.
 *
 * @author Christopher Jung
 */
abstract class ShopEntry
{
	private int id;
	private User factionID;
	private FactionShopEntry.Type type;
	private String resource;
	private long price;
	private long lpKosten;
	private int availability;

	/**
	 * Konstruktor.
	 *
	 * @param data Die SQL-Ergebniszeile zum Eintrag
	 */
	public ShopEntry(FactionShopEntry data)
	{
		this.id = data.getId();
		this.factionID = data.getFaction();
		this.type = data.getType();
		this.resource = data.getResource();
		this.price = data.getPrice();
		this.lpKosten = data.getLpKosten();
		this.availability = data.getAvailability();
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 *
	 * @return Die ID
	 */
	public int getID()
	{
		return this.id;
	}

	/**
	 * Gibt die Fraktion zurueck, der der Eintrag gehoert.
	 *
	 * @return Die Fraktion
	 */
	@SuppressWarnings("unused")
	public User getFactionID()
	{
		return this.factionID;
	}

	/**
	 * Gibt den Typ des Eintrags zurueck.
	 *
	 * @return Der Typ
	 */
	public FactionShopEntry.Type getType()
	{
		return this.type;
	}

	/**
	 * Gibt den Namen des Eintrags zurueck.
	 *
	 * @return Der Name
	 */
	public abstract String getName();

	/**
	 * Gibt das zum Eintrag gehoerende Bild zurueck.
	 *
	 * @return Das Bild
	 */
	public abstract String getImage();

	/**
	 * Gibt einen zum Eintrag gehoerenden Link zurueck.
	 *
	 * @return Der Link
	 */
	public abstract String getLink();

	/**
	 * Gibt die LP-Kosten fuer den Eintrag zurueck.
	 *
	 * @return Die LP-Kosten
	 */
	public long getLpKosten()
	{
		return this.lpKosten;
	}

	/**
	 * Gibt die Verfuegbarkeit des Eintrags zurueck.
	 *
	 * @return Die Verfuegbarkeit
	 */
	public int getAvailability()
	{
		return this.availability;
	}

	/**
	 * Gibt die Verfuegbarkeit des Eintrags als Text zurueck.
	 *
	 * @return Die Verfuegbarkeit als Text
	 */
	public String getAvailabilityName()
	{
		switch (this.getAvailability())
		{
			case 0:
				return "Genug vorhanden";
			case 1:
				return "Nur noch 1-3 vorhanden";
			case 2:
				return "Nicht verf&uuml;gbar";
		}
		return "";
	}

	/**
	 * Gibt die mit der Verfuegbarkeit assoziierte Farbe zurueck.
	 *
	 * @return Die Farbe der Verfuegbarkeit
	 */
	public String getAvailabilityColor()
	{
		switch (this.getAvailability())
		{
			case 0:
				return "#55DD55";
			case 1:
				return "#FFFF44";
			case 2:
				return "#CC2222";
		}
		return "";
	}

	/**
	 * Soll die Verkaufsmenge angezeigt werden?
	 *
	 * @return <code>true</code>, falls die Verkaufsmenge angezeigt werden soll
	 */
	public boolean showAmountInput()
	{
		return true;
	}

	/**
	 * Gibt den Kaufpreis zurueck.
	 *
	 * @return Der Kaufpreis
	 */
	public long getPrice()
	{
		return price;
	}

	/**
	 * Gibt den Kaufpreis als Text zurueck.
	 *
	 * @return Der Kaufpreis als Text
	 */
	public String getPriceAsText()
	{
		return Common.ln(this.getPrice());
	}

	/**
	 * Gibt den Verkaufsinhalt, den der Eintrag enthaelt, zurueck. Der Typ ist Abhaengig vom
	 * Typen des Eintrags.
	 *
	 * @return Der Verkaufsinhalt
	 */
	public String getResource()
	{
		return this.resource;
	}
}
