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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Parameter fuer die Schiffsmodule.
 * @author Christopher Jung
 *
 */
public class Parameters {
	/**
	 * Der Schiffscontroller.
	 */
	public SchiffController controller;
	/**
	 * Die ID des Plugins.
	 */
	public String pluginId;
	/**
	 * Die Zielvariable im Template.
	 */
	public String target;
	/**
	 * Das Template in dem die Ausgabe erfolgen soll.
	 */
	public TemplateEngine t;
	/**
	 * Das Schiff.
	 */
	public Ship ship;
	/**
	 * Der Schiffstyp.
	 */
	public ShipTypeData shiptype;
	/**
	 * Der Offizier oder <code>null</code>.
	 */
	public Offizier offizier;
}
