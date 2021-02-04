/*
# *	Drifting Souls 2
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
package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.framework.Context;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Die Kommandozentrale.
 * @author Christopher Jung
 *
 */
@Entity(name="KommandozentraleBuilding")
@DiscriminatorValue("net.driftingsouls.ds2.server.bases.Kommandozentrale")
public class Kommandozentrale extends DefaultBuilding {
	/**
	 * Erstellt eine neue Instanz der Kommandozentrale.
	 */
	public Kommandozentrale() {
		// EMPTY
	}

	@Override
	public void cleanup(Context context, Base base, int building) {
		throw new IllegalArgumentException("Should not be called!");
	}

	@Override
	public String echoShortcut(Context context, Base base, int field, int building) {
		return "<a class=\"back tooltip\" href=\"./ds?module=building&amp;col="+base.getId()+"&amp;field="+field+"\">[K]<span class='ttcontent'>"+this.getName()+"</span></a>";
	}

	@Override
	public boolean classicDesign() {
		return true;
	}

	@Override
	public boolean printHeader() {
		return false;
	}

	@Override
	public String output(Context context, Base base, int field, int building) {
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isSupportsJson()
	{
		return false;
	}
}
