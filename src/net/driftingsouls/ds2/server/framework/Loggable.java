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
package net.driftingsouls.ds2.server.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

/**
 * Stattet eine Klasse mit dem Default-Logger von DS aus.
 * Es sollte diese Klasse verwendet werden und nicht System.out
 * oder System.err fuer Log-Ausgaben.
 * 
 * @author Christopher Jung
 * @deprecated Bitte fuer jede Klasse einen eigenen Logger benutzen
 */
@Deprecated
public interface Loggable {
	/**
	 * Das Default-Log-Objekt. Hierueber sollten alle normalen Log-Ausgaben geschehen
	 */
	public static Log LOG = new LogFactoryImpl().getInstance("DS2");
}
