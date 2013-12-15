/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.namegenerator;

import net.driftingsouls.ds2.server.namegenerator.producer.NameProducer;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducerManager;

import static net.driftingsouls.ds2.server.namegenerator.NameGeneratorUtils.*;

/**
 * Generator fuer Personennamen. Die Namen werden aus einem
 * generierten Namennamen und einem zufaellig aus einer Liste 
 * ausgewaehlten Vornamen zusammengesetzt.
 * @author Christopher Jung
 *
 */
public class VorNachnamePersonenNamenGenerator
{
	private NameProducer lastname;
	private NameProducer firstName;
	private String[] kapitalisierungen;

	/**
	 * Konstruktor.
	 */
	public VorNachnamePersonenNamenGenerator(String vornamenDatei, String nachnamenDatei, String[] kapitalisierungen)
	{
		this.lastname = NameProducerManager.INSTANCE.getMarkovNameProducer(VorNachnamePersonenNamenGenerator.class.getResource(nachnamenDatei));
		this.firstName = NameProducerManager.INSTANCE.getListBasedNameProducer(VorNachnamePersonenNamenGenerator.class.getResource(vornamenDatei), true);
		this.kapitalisierungen = kapitalisierungen;
	}

	private String generateLastName()
	{
		String name = this.lastname.generateNext();

		while( name.length() <= 2 ) {
			name = this.lastname.generateNext();
		}

		name = name.toLowerCase();
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		name = upperAfterStrings(name, kapitalisierungen);

		return name;
	}

	public String generate()
	{
		String lastname = generateLastName();
		lastname = firstName.generateNext() + " " + lastname;
		return lastname;
	}
}
