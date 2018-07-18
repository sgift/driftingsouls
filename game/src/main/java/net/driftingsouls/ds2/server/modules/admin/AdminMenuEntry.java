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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gibt an unter welchen Namen und welcher Kategorie das AdminPlugin aufgefuert werden soll.
 * @author Christopher Jung
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdminMenuEntry {
	/**
	 * Gibt den Namen der Kategorie zurueck, in der das Plugin aufgefuehrt werden soll.
	 */
	String category();
	
	/**
	 * Gibt den Namen des Plugins selbst zurueck.
	 */
	String name();

	/**
	 * Gibt die notwendige Berechtigung zur Benutzung des Adminmoduls zurueck.
	 * @return Die Berechtigung
	 */
	WellKnownAdminPermission permission();
}
