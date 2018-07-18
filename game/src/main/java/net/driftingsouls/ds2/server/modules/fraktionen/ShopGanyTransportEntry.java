package net.driftingsouls.ds2.server.modules.fraktionen;

import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopEntry;
import net.driftingsouls.ds2.server.framework.Common;

/**
 * Repraesentiert ein Shopeintrag, welcher einen Ganymede-Transport enthaelt.
 *
 * @author Christopher Jung
 */
class ShopGanyTransportEntry extends ShopEntry
{
	/**
	 * Die Schiffstypen-ID einer Ganymede.
	 */
	public static final int SHIPTYPE_GANYMEDE = 33;

	private long minprice = Long.MAX_VALUE;
	private long maxprice = Long.MIN_VALUE;
	private int ganytransid;

	/**
	 * Konstruktor.
	 *
	 * @param data Die SQL-Ergebniszeile des Shopeintrags
	 */
	public ShopGanyTransportEntry(FactionShopEntry[] data)
	{
		super(data[0]);

		for (FactionShopEntry aData : data)
		{
			if (aData.getPrice() < this.minprice)
			{
				this.minprice = aData.getPrice();
			}
			if (aData.getPrice() > this.maxprice)
			{
				this.maxprice = aData.getPrice();
			}
			this.ganytransid = data[0].getId();
		}
	}

	@Override
	public long getLpKosten()
	{
		return 0;
	}

	@Override
	public int getID()
	{
		return ganytransid;
	}

	@Override
	public long getPrice()
	{
		return (this.minprice != this.maxprice) ? (this.minprice + this.maxprice) / 2
				: this.minprice;
	}

	@Override
	public String getPriceAsText()
	{
		return (this.minprice != this.maxprice) ? (Common.ln(this.minprice) + " - " + Common
				.ln(this.maxprice)) : (Common.ln(this.minprice)) + "<br />pro System";
	}

	@Override
	public String getName()
	{
		return "Ganymede-Transport";
	}

	@Override
	public String getImage()
	{
		return "./data/interface/ganymede_transport.png";
	}

	@Override
	public String getLink()
	{
		return "#";
	}

	@Override
	public boolean showAmountInput()
	{
		return false;
	}

	@Override
	public int getAvailability()
	{
		return 0;
	}
}
