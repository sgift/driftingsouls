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

public class WarenID implements ResourceID {
	private int resid;
	
	public WarenID(int resid) {
		this.resid = resid;
	}
	
	public int getID() {
		return resid;
	}
	
	public boolean isItem() {
		return false;
	}
	
	public int getItemID() {
		return 0;
	}
	
	public int getUses() {
		return 0;
	}
	
	public int getQuest() {
		return 0;
	}
	
	@Override
	public String toString() {
		return Integer.toString(resid);
	}
	
	@Override
	public boolean equals(Object obj) {
		if( !(obj instanceof ResourceID) ) {
			return false;
		}
		ResourceID id = (ResourceID)obj;
		
		if( id.isItem() ) {
			return false;
		}
		return id.getID() == getID();
	}
	
	@Override
	public int hashCode() {
		return resid;
	}
}
