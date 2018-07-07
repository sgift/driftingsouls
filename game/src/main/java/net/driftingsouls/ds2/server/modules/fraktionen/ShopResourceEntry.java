package net.driftingsouls.ds2.server.modules.fraktionen;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopEntry;
import net.driftingsouls.ds2.server.framework.Common;

/**
 * Repraesentiert ein Shopeintrag, welcher eine Resource enthaelt.
 *
 * @author Christopher Jung
 */
class ShopResourceEntry extends ShopEntry
{
	private ResourceEntry resourceEntry;

	/**
	 * Konstruktor.
	 *
	 * @param data Die SQL-Ergebniszeile des Shopeintrags
	 */
	public ShopResourceEntry(FactionShopEntry data)
	{
		super(data);

		Cargo cargo = new Cargo();
		cargo.addResource(Resources.fromString(this.getResource()), 1);
		cargo.setOption(Cargo.Option.SHOWMASS, false);
		cargo.setOption(Cargo.Option.LARGEIMAGES, true);
		this.resourceEntry = cargo.getResourceList().iterator().next();
	}

	@Override
	public String getName()
	{
		return Cargo.getResourceName(resourceEntry.getId());
	}

	@Override
	public String getImage()
	{
		return resourceEntry.getImage();
	}

	@Override
	public String getLink()
	{
		return Common.buildUrl("details", "module", "iteminfo", "item", resourceEntry
				.getId().getItemID());
	}
}
