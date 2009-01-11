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
package net.driftingsouls.ds2.server.install.checks;

import net.driftingsouls.ds2.server.framework.Configuration;

/**
 * Ueberprueft, ob die config.xml existiert.
 * @author Christopher Jung
 *
 */
public class ConfigXmlValidCheck implements Checkable {
	@Override
	public void doCheck() throws CheckFailedException {
		try {
			Configuration.init("WEB-INF/cfg/");
		}
		catch( Exception e ) {
			throw new CheckFailedException("WEB-INF/cfg/config.xml hat ungueltiges Format", e);
		}
	}

	@Override
	public String getDescription() {
		return "config.xml validieren";
	}

}
