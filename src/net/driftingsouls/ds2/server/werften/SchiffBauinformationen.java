package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

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

	/**
	 * Generiert die ID fuer diesen Satz von Bauinformationen;
	 * @return Die ID
	 */
	public String getId()
	{
		String id = quelle.name()+"#";
		if( quelle == BauinformationenQuelle.FORSCHUNG )
		{
			id += baudaten.getId();
		}
		else
		{
			id += item.toString();
		}
		return id;
	}

	@Override
	public int compareTo(@NotNull SchiffBauinformationen schiffBauinformationen)
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
			return this.baudaten.getId()-schiffBauinformationen.baudaten.getId();
		}

		return this.item.getItemID()-schiffBauinformationen.item.getItemID();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}

		SchiffBauinformationen that = (SchiffBauinformationen) o;

		if (quelle != that.quelle)
		{
			return false;
		}
		if (item != null ? !item.equals(that.item) : that.item != null)
		{
			return false;
		}
		if( quelle == BauinformationenQuelle.FORSCHUNG )
		{
			return baudaten != null ? baudaten.equals(that.baudaten) : that.baudaten == null;
		}


		return true;
	}

	@Override
	public int hashCode()
	{
		int result = baudaten != null ? baudaten.hashCode() : 0;
		result = 31 * result + quelle.hashCode();
		result = 31 * result + (item != null ? item.hashCode() : 0);
		return result;
	}

	/**
	 * Laedt die Bauinformationen zu einer Bauinformationen-ID.
	 * @param id Die ID
	 * @return Die Bauinformationen
	 * @see #getId()
	 */
	public static SchiffBauinformationen fromId(String id)
	{
		if( id == null || id.trim().length() < 3 || !id.contains("#") )
		{
			throw new IllegalArgumentException("Keine gueltige ID: "+id);
		}
		String quelleStr = id.substring(0, id.indexOf('#')).trim();
		String idStr = id.substring(id.indexOf('#')+1);

		ShipBaubar baudaten;
		ResourceID item = null;
		BauinformationenQuelle quelle = BauinformationenQuelle.valueOf(quelleStr);

		Session db = ContextMap.getContext().getDB();
		if( quelle == BauinformationenQuelle.FORSCHUNG )
		{
			baudaten = (ShipBaubar) db.get(ShipBaubar.class, Integer.parseInt(idStr));
		}
		else
		{
			item = ItemID.fromString(idStr);
			Item itemData = (Item) db.get(Item.class, item.getItemID());
			baudaten = ((IEDraftShip)itemData.getEffect()).toShipBaubar();
		}

		return new SchiffBauinformationen(baudaten, quelle, item);
	}
}
