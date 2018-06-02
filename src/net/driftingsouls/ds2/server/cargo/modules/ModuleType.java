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
package net.driftingsouls.ds2.server.cargo.modules;


/**
 * Allgmeine (Schiffs)Modulfunktionen und Konstanten.
 * @author Christopher Jung
 *
 */
public enum ModuleType {
	/**
	 * Ein Frachtkontainer-Modul.
	 * @see ModuleContainerShip
	 */
	CONTAINER_SHIP(1) {
		@Override
		public Module createModule(ModuleEntry moduledata)
		{
			return new ModuleContainerShip( moduledata.getSlot(), moduledata.getData() );
		}
	},
	/**
	 * Ein Schiffsbild-Modul.
	 * @see ModuleShipPicture
	 */
	SHIP_PICTURE(2) {
		@Override
		public Module createModule(ModuleEntry moduledata)
		{
			return new ModuleShipPicture( moduledata.getSlot(), moduledata.getData() );
		}
	},
	/**
	 * Ein Item-Modul.
	 * @see ModuleItemModule
	 */
	ITEMMODULE(3) {
		@Override
		public Module createModule(ModuleEntry moduledata)
		{
			return new ModuleItemModule( moduledata.getSlot(), moduledata.getData() );
		}
	};

	private final int ordinal;

	ModuleType(int ordinal)
	{
		this.ordinal = ordinal;
	}

	/**
	 * Gibt die Ordinal (interne ID) zurueck.
	 * @return Die Ordinal
	 */
	public int getOrdinal()
	{
		return this.ordinal;
	}

	/**
	 * Erstellt aus dem Moduleintrag eine Modulrepraesentation.
	 * @param moduledata Der Moduleintrag
	 * @return Das Modul
	 */
	public abstract Module createModule(ModuleEntry moduledata);

	/**
	 * Gibt zu einer Ordinal den zugenoerigen Modultyp zurueck. Falls
	 * die Ordinal unbekannt ist wird <code>null</code> zurueckgegeben.
	 * @param ordinal Die Ordinal
	 * @return Der Modultyp oder <code>null</code>
	 */
	public static ModuleType fromOrdinal(int ordinal)
	{
		for( ModuleType m : values() )
		{
			if( m.ordinal == ordinal )
			{
				return m;
			}
		}
		return null;
	}
}
