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
package net.driftingsouls.ds2.server.config.items.effects;

import java.io.IOException;
import java.io.Writer;


/**
 * Ein leerer Item-Effekt ohne wirklichen Effekt.
 * @author Christopher Jung
 *
 */
public class IENone extends ItemEffect {
	protected IENone() {
		super(ItemEffect.Type.NONE);
	}
	
	/**
	 * Gibt das passende Fenster für das Adminmenü aus.
	 * @param echo Der Writer des Adminmenüs
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	public void getAdminTool(Writer echo) throws IOException {
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	public String toString() {
		return null;
	}
}
