/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.user.authentication;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationException;
import net.driftingsouls.ds2.server.framework.authentication.LoginEventListener;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

/**
 * Listener fuer Loginereignisse der sicher stellt, dass auf Accounts
 * waehrend sie vom Tick gesperrt sind, nicht zugegriffen werden kann.
 * @author Christopher Jung
 *
 */
public class TickBlockLoginEventListener implements LoginEventListener {
	public void onLogin(BasicUser basicUser) throws AuthenticationException {
		Context context = ContextMap.getContext();
		Request request = context.getRequest();
		
		User user = (User)basicUser;
		
		if( user.isBlocked() ) {
			context.addError( "Im Moment werden einige Tick-Berechnungen f&uuml;r sie durchgef&uuml;hrt. Bitte haben sie daher ein wenig Geduld", 
					request.getRequestURL() + 
						(request.getQueryString() != null ? "?" + request.getQueryString() : "") 
			);
			
			throw new TickInProgressException();
		}
	}

}
