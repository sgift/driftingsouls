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
package net.driftingsouls.ds2.server.framework.pipeline.configuration;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;

/**
 * Repraesentiert einen Regel innerhalb der Pipeline-Konfiguration.
 * Aus allgemeinen Regeln werden die konkreten, fuer den jeweiligen Kontext
 * angepassten Pipelines generiert.
 * 
 * @author Christopher Jung
 *
 */
interface Rule {
	/**
	 * Prueft, ob die Regel ausgefuehrt werden kann.
	 * @param context Der Kontext, in dem geprueft werden soll
	 * @return true, falls die Regel ausgefuehrt werden kann
	 * @throws Exception
	 */
    boolean executeable(Context context) throws Exception;
	
	/**
	 * Fuehrt die Regel aus und liefert die sich daraus ergebende Pipeline zurueck.
	 * 
	 * @param context Der Kontext, in dem die Regel ausgefuehrt werden soll
	 * @return Die aus der Regel abgeleitete Pipeline
	 * @throws Exception
	 */
    Pipeline execute(Context context) throws Exception;
}
