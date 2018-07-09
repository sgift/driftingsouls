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
package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.install.checks.AbsolutePathCheck;
import net.driftingsouls.ds2.server.install.checks.CheckFailedException;
import net.driftingsouls.ds2.server.install.checks.Checkable;
import net.driftingsouls.ds2.server.install.checks.ConfigXmlExistsCheck;
import net.driftingsouls.ds2.server.install.checks.ConfigXmlValidCheck;
import net.driftingsouls.ds2.server.install.checks.ConfigXmlValidXmlCheck;
import net.driftingsouls.ds2.server.install.checks.DatabaseConnectionCheck;
import net.driftingsouls.ds2.server.install.checks.LoxPathCheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Ueberprueft die Installation von Drifting Souls auf moegliche Probleme.
 * @author Christopher Jung
 *
 */
public class CheckInstallation {
	private static final List<Checkable> checks = new ArrayList<>();

	static {
		checks.add(new ConfigXmlExistsCheck());
		checks.add(new ConfigXmlValidXmlCheck());
		checks.add(new ConfigXmlValidCheck());

		// Die Configuration ist zu diesem Zeitpunkt nun initalisiert
		checks.add(new DatabaseConnectionCheck());
		checks.add(new AbsolutePathCheck());
		checks.add(new LoxPathCheck());
	}

	/**
	 * Bringt einen String auf eine feste Laenge. Zu lange Strings werden
	 * abgeschnitten. Zu kurze mit Leerzeichen aufgefuellt.
	 * @param string Der String
	 * @param length Die gewuenschte Laenge
	 * @return Der neue String
	 */
	private static String toFixedLength(String string, int length) {
		if( string.length() == length ) {
			return string;
		}
		if( string.length() > length ) {
			return string.substring(0, length);
		}

		StringBuilder builder = new StringBuilder(length);
		builder.append(string);
		while( builder.length() < length ) {
			builder.append(" ");
		}
		return builder.toString();
	}

	/**
	 * Die Main.
	 * @param args Kommandozeilenargumente
	 */
	public static void main(String[] args) {
		System.out.println("Ueberpruefe DriftingSouls Installation");
		try {
			for (final Checkable check : checks)
			{
				System.out.print(toFixedLength(check.getDescription(), 40));
				check.doCheck();
				System.out.println("[OK]");
			}
		}
		catch( CheckFailedException e ) {
			System.out.println("[FAILED]\n");
			e.printStackTrace();
		}
	}

}
