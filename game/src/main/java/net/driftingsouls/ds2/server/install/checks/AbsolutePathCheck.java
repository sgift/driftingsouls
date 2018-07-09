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

import java.io.File;

/**
 * Ueberprueft, ob die config.xml existiert.
 * @author Christopher Jung
 *
 */
public class AbsolutePathCheck implements Checkable {
	@Override
	public void doCheck() throws CheckFailedException {
		final String absolutePath = Configuration.getAbsolutePath();
		if( !new File(absolutePath).isDirectory() ) {
			throw new CheckFailedException("ABSOLUTE_PATH ist ungueltig");
		}
		
		if( !absolutePath.endsWith("/") ) {
			throw new CheckFailedException("ABSOLUTE_PATH endet nicht auf /");
		}
		
		if( !new File(absolutePath+"data").isDirectory() ) {
			throw new CheckFailedException("ABSOLUTE_PATH enthaelt kein data-Verzeichnis");
		}
	}
	
	@Override
	public String getDescription() {
		return "config.xml:ABSOLUTE_PATH pruefen";
	}

}
