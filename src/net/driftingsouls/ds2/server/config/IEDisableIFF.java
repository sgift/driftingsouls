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
package net.driftingsouls.ds2.server.config;

import org.w3c.dom.Node;

/**
 * <h1>Item-Effekt "IFF deaktivieren"</h1>
 * <p>Schiffe ohne IFF-Kennung koennen nicht angegriffen, gekapert, gepluendert oder Ziel eines
 * Warentransfers sein. Zudem ist ihr Besitzer nicht erkennbar.</p>
 * <p>Der Effekttyp ist lediglich ein "Marker" und besitzt selbst keine Eigenschaften</p>
 * @author Christopher Jung
 *
 */
public class IEDisableIFF extends ItemEffect {
	protected IEDisableIFF() {
		super(ItemEffect.Type.DISABLE_IFF);
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		return new IEDisableIFF();
	}
}
