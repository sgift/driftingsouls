package net.driftingsouls.ds2.server.modules.fraktionen;

import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Repraesentiert ein Shopeintrag, welcher ein Schiff enthaelt.
 *
 * @author Christopher Jung
 */
class ShopShipEntry extends ShopEntry
{
	private final ShipTypeData shiptype;

	/**
	 * Konstruktor.
	 *
	 * @param data Die SQL-Ergebniszeile des Shopeintrags
	 */
	public ShopShipEntry(ShipTypeData shipType, FactionShopEntry data)
	{
		super(data);
		this.shiptype = shipType;
	}

	@Override
	public String getName()
	{
		return this.shiptype.getNickname();
	}

	@Override
	public String getImage()
	{
		return this.shiptype.getPicture();
	}

	@Override
	public String getLink()
	{
		return Common.buildUrl("default", "module", "schiffinfo", "ship", shiptype.getTypeId());
	}
}
