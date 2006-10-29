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
package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;


/**
 * Basisklasse fuer alle Controller
 * 
 * @author bktheg
 *
 */
public abstract class Generator {
	private Context context;
	
	public Generator(Context context) {
		this.context = context;
	}
	
	public final User getCachedUser( int id ) {
		return context.getCachedUser(id);
	}
	
	public final User createUserObject( int id, String ... prepare ) {
		return context.createUserObject(id, prepare);
	}
	
	public final void cacheUser( User userobj ) {
		context.cacheUser(userobj);
	}
	
	public final Database getDatabase() {
		return context.getDatabase();
	}
	
	public final User getActiveUser() {
		return context.getActiveUser();
	}
	
	public final void setActiveUser( User user ) {
		context.setActiveUser(user);
	}
	
	public final void addError( String error ) {
		context.addError(error);
	}
	
	public final void addError( String error, String link ) {
		context.addError(error, link);
	}
	
	public final Error getLastError() {
		return context.getLastError();
	}
	
	public final Error[] getErrorList() {
		return context.getErrorList();
	}
	
	public final Response getResponse() {
		return context.getResponse();
	}

	public final Request getRequest() {
		return context.getRequest();
	}
	
	public Context getContext() {
		return context;
	}
}
