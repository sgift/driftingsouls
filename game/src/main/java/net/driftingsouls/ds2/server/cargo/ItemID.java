/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.cargo;

import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;

/**
 * Repraesentiert eine Item-ID in DS. Jede Item-ID besteht aus der
 * Item-Typen-ID, der noch verbleibenden Anzahl an Benutzungen sowie dem zugeordneten Quest.  
 * @author Christopher Jung
 *
 */
public class ItemID implements ResourceID
{
	private int itemid;
	private int uses;
	private int quest;
	private int hashCode = 0;
	
	/**
	 * Erstellt eine neue Item-ID.
	 * @param itemid Die Item-Typen-ID
	 */
	public ItemID(int itemid)
	{
		this.itemid = itemid;
	}

	/**
	 * Erstellt eine neue Item-ID.
	 * @param item Der Item-Typ
	 */
	public ItemID(Item item)
	{
		this.itemid = item.getID();
	}
	
	/**
	 * Erstellt eine neue Item-ID.
	 * 
	 * @param itemid Die Item-Typen-ID
	 * @param uses Die noch verbleibende Anzahl an Benutzungen
	 * @param quest Das zugeordnete Quest
	 */
	public ItemID(int itemid, int uses, int quest)
	{
		this(itemid);
		this.uses = uses;
		this.quest = quest;
	}
	
	@Override
	public int getItemID()
	{
		return itemid;
	}
	
	@Override
	public int getUses()
	{
		return uses;
	}
	
	@Override
	public int getQuest()
	{
		return quest;
	}
	
	/**
	 * Prueft, ob es sich bei einem String um eine Item-ResourceID handelt.
	 * @param rid Der String
	 * @return <code>true</code>, falls es eine Item-ResourceID ist
	 */
	public static boolean isItemRID(String rid)
	{
		if( rid == null || rid.equals("") )
		{
			return false;
		}
		return rid.charAt(0) == 'i';
	}
	
	/**
	 * Wandelt einen String mit einer Item-ResourcenID in eine Item-ResourcenID um.
	 * Falls der angegebene String keine korrekte Item-ResourcenID enthaelt wird <code>null</code>
	 * zurueckgegeben.
	 *  
	 * @param rid Der String
	 * @return die Item-ResourcenID oder <code>null</code>
	 */
	public static ItemID fromString(String rid)
	{
		if( !isItemRID(rid) )
		{
			return null;
		}
		rid = rid.substring(1);
		int[] elements = Common.explodeToInt("|", rid);
		if( elements.length != 3 )
		{
			return null;
		}
		try
		{
			return new ItemID(elements[0], elements[1], elements[2]);
		}
		catch(NumberFormatException e)
		{
			return null;
		}
	}
	
	@Override
	public String toString()
	{
		return "i"+itemid+'|'+uses+'|'+quest;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if( obj == null )
		{
			return false;
		}
		if( this.getClass() != obj.getClass() )
		{
			return false;
		}
		ItemID id = (ItemID)obj;
		
		return (id.getItemID() == getItemID()) && (id.getUses() == getUses()) && (id.getQuest() == getQuest());
	}
	
	@Override
	public int hashCode()
	{
		if( hashCode == 0 ) {
			hashCode = itemid*100+quest*10+uses;
		}
		return hashCode;
	}
}
