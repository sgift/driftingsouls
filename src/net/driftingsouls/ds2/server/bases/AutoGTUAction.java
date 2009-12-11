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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.cargo.ResourceID;

/**
 * Repraesentiert eine auf der Basis durchgefuehrte automatische GTU-Aktion.
 * Instanzen dieser Klasse sind nicht veraenderbar (immutable).
 * @author Christopher Jung
 *
 */
public final class AutoGTUAction implements Cloneable {
	private ResourceID resid;
	private int actid;
	private long count;
	
	/**
	 * Erstellt eine neue Instanz.
	 * @param resid Die Resourcen-ID
	 * @param actid Der Aktionstyp
	 * @param count Die Resourcenmenge
	 */
	public AutoGTUAction( ResourceID resid, int actid, long count ) {
		this.resid = resid;
		this.actid = actid;
		this.count = count;
	}

	/**
	 * Gibt den Aktionstyp zurueck.
	 * @return der Aktionstyp
	 */
	public int getActID() {
		return actid;
	}

	/**
	 * Gibt die Resourcenmenge zurueck.
	 * @return die Resourcenmenge
	 */
	public long getCount() {
		return count;
	}

	/**
	 * Gibt die Resourcen-ID zurueck.
	 * @return die Resourcen-ID
	 */
	public ResourceID getResID() {
		return resid;
	}
	
	@Override
	public String toString() {
		return resid.toString()+":"+actid+":"+count;
	}

	@Override
	public Object clone() {
		try {
			AutoGTUAction act = (AutoGTUAction)super.clone();
			act.actid = this.actid;
			act.count = this.count;
			act.resid = this.resid;
			return act;
		}
		catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Legt fest, dass die Resource in angegebener Hoehe verkauft werden soll.
	 */
	public static final int SELL_ALL = 0;
	/**
	 * Legt fest, dass die Resource bis zu einem Minimum verkauft werden soll.
	 */
	public static final int SELL_TO_LIMIT = 1;
}
