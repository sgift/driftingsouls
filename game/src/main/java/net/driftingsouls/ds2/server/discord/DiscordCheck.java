package net.driftingsouls.ds2.server.discord;

import java.util.ArrayList;
import java.util.List;

/**
 * Ergebnis der Verbindungspruefung des Discord-Bots fuer den Adminbereich.
 */
public class DiscordCheck
{
	public static class Item
	{
		public final boolean ok;
		public final String text;

		Item(boolean ok, String text)
		{
			this.ok = ok;
			this.text = text;
		}
	}

	public final List<Item> items = new ArrayList<>();

	void ok(String text)
	{
		items.add(new Item(true, text));
	}

	void error(String text)
	{
		items.add(new Item(false, text));
	}
}
