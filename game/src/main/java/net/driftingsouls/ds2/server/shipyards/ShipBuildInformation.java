package net.driftingsouls.ds2.server.shipyards;

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Die Bauinformationen fuer ein Schiff zusammen mit ihren Quellinformationen.
 */
public class ShipBuildInformation implements Comparable<ShipBuildInformation>
{
	private final ShipBaubar buildData;
	private final BuildInformationSource source;
	private final ResourceID item;

	ShipBuildInformation(ShipBaubar buildData, BuildInformationSource source, ResourceID item)
	{
		this.buildData = buildData;
		this.source = source;
		this.item = item;
	}

	/**
	 * Gibt die konkreten Daten zum Bau des Schiffes zurueck.
	 * @return Die Baudaten
	 */
	public ShipBaubar getBuildData()
	{
		return buildData;
	}

	/**
	 * Gibt die Quelle zurueck, aus der die Bauinformationen stammen.
	 * @return Die Quelle
	 */
	public BuildInformationSource getSource()
	{
		return source;
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
		String id = source.name()+"#";
		if( source == BuildInformationSource.FORSCHUNG )
		{
			id += buildData.getId();
		}
		else
		{
			id += item.toString();
		}
		return id;
	}

	@Override
	public int compareTo(@NotNull ShipBuildInformation buildInformation)
	{
		int diff = this.source.compareTo(buildInformation.source);
		if( diff != 0 )
		{
			return diff;
		}
		if( this.source == BuildInformationSource.FORSCHUNG )
		{
			diff = this.buildData.getType().getNickname().compareTo(buildInformation.buildData.getType().getNickname());
			if( diff != 0 )
			{
				return diff;
			}
			return this.buildData.getId()-buildInformation.buildData.getId();
		}

		return this.item.getItemID()-buildInformation.item.getItemID();
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

		ShipBuildInformation that = (ShipBuildInformation) o;

		if (source != that.source)
		{
			return false;
		}
		if (!Objects.equals(item, that.item))
		{
			return false;
		}
		if( source == BuildInformationSource.FORSCHUNG )
		{
			return Objects.equals(buildData, that.buildData);
		}


		return true;
	}

	@Override
	public int hashCode()
	{
		int result = buildData != null ? buildData.hashCode() : 0;
		result = 31 * result + source.hashCode();
		result = 31 * result + (item != null ? item.hashCode() : 0);
		return result;
	}

	/**
	 * Laedt die Bauinformationen zu einer Bauinformationen-ID.
	 * @param id Die ID
	 * @return Die Bauinformationen
	 * @see #getId()
	 */
	public static ShipBuildInformation fromId(String id)
	{
		if( id == null || id.trim().length() < 3 || !id.contains("#") )
		{
			throw new IllegalArgumentException("Keine gueltige ID: "+id);
		}
		String sourceStr = id.substring(0, id.indexOf('#')).trim();
		String idStr = id.substring(id.indexOf('#')+1);

		ShipBaubar buildData;
		ResourceID item = null;
		BuildInformationSource source = BuildInformationSource.valueOf(sourceStr);

		Session db = ContextMap.getContext().getDB();
		if( source == BuildInformationSource.FORSCHUNG )
		{
			buildData = (ShipBaubar) db.get(ShipBaubar.class, Integer.parseInt(idStr));
		}
		else
		{
			item = ItemID.fromString(idStr);
			if(item == null) {
				throw new IllegalArgumentException("Keine gueltige ID: "+id);
			}
			Item itemData = (Item) db.get(Item.class, item.getItemID());
			buildData = ((IEDraftShip)itemData.getEffect()).toShipBaubar();
		}

		return new ShipBuildInformation(buildData, source, item);
	}
}
