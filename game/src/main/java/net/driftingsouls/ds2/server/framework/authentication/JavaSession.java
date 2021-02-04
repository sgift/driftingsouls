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
package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.BasicUser;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Sessionobjekt fuer Java-Sessions.
 * @author Christopher Jung
 *
 */
public class JavaSession {
	private Integer id;
	private Integer attachedUser;

	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Setzt den mit der Session verknuepften User.
	 * @param user Der mit der Session verknuepfte User
	 */
	public void setUser(BasicUser user) {
		if( user == null ) {
			this.id = null;
		}
		else {
			this.id = user.getId();
		}
	}

	/**
	 * Gibt den mit der Session verknuepften User zurueck.
	 * @return Der mit der Session verknuepfte User
	 */
	public BasicUser getUser() {
		if( id == null ) {
			return null;
		}
		return em.find(BasicUser.class, this.id);
	}

	/**
	 * Gibt den angefuegten User oder <code>null</code> zurueck.
	 * @return Der User oder <code>null</code>
	 */
	public BasicUser getAttach() {
		if( this.attachedUser == null ) {
			return null;
		}
		return em.find(BasicUser.class, this.attachedUser);
	}

	/**
	 * Setzt den angefuegten User.
	 * @param attach Der User
	 */
	public void setAttach(BasicUser attach) {
		if( attach != null ) {
			this.attachedUser = attach.getId();
		}
		else {
			this.attachedUser = null;
		}
	}
}
