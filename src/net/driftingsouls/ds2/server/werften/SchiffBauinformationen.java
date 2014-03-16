package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.ships.ShipBaubar;

/**
 * Die Bauinformationen fuer ein Schiff zusammen mit ihren Quellinformationen.
 */
public class SchiffBauinformationen implements Comparable<SchiffBauinformationen>
{
	private ShipBaubar baudaten;
	private BauinformationenQuelle quelle;
	private ResourceID item;

	SchiffBauinformationen(ShipBaubar baudaten, BauinformationenQuelle quelle, ResourceID item)
	{
		this.baudaten = baudaten;
		this.quelle = quelle;
		this.item = item;
	}

	/**
	 * Gibt die konkreten Daten zum Bau des Schiffes zurueck.
	 * @return Die Baudaten
	 */
	public ShipBaubar getBaudaten()
	{
		return baudaten;
	}

	/**
	 * Gibt die Quelle zurueck, aus der die Bauinformationen stammen.
	 * @return Die Quelle
	 */
	public BauinformationenQuelle getQuelle()
	{
		return quelle;
	}

	/**
	 * Gibt das evt. zum Bau benoetigte Item zurueck.
	 * @return Das Item oder <code>null</code>
	 */
	public ResourceID getItem()
	{
		return item;
	}

	@Override
	public int compareTo(SchiffBauinformationen schiffBauinformationen)
	{
		int diff = this.quelle.compareTo(schiffBauinformationen.quelle);
		if( diff != 0 )
		{
			return diff;
		}
		if( this.quelle == BauinformationenQuelle.FORSCHUNG )
		{
			diff = this.baudaten.getType().getNickname().compareTo(schiffBauinformationen.baudaten.getType().getNickname());
			if( diff != 0 )
			{
				return diff;
			}
			return this.baudaten.getType().getId()-schiffBauinformationen.baudaten.getType().getId();
		}

		return this.item.getItemID()-schiffBauinformationen.item.getItemID();
	}
}
