/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

class ShipTypeDataPictureWrapper extends AbstractShipTypeDataWrapper {
	private boolean forceServer;
	
	ShipTypeDataPictureWrapper(ShipTypeData type, boolean forceServer) {
		super(type);
		this.forceServer = forceServer;
	}

	@Override
	public String getPicture() {
		Context context = ContextMap.getContext();
		
		if( (context != null) && !forceServer && !getInner().isHide() && (context.getActiveUser() != null) ) {
			return context.getActiveUser().getImagePath()+getInner().getPicture();	
		}
		return BasicUser.getDefaultImagePath()+getInner().getPicture();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
