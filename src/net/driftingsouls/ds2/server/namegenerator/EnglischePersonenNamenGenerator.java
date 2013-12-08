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

import java.io.IOException;

import net.driftingsouls.ds2.server.namegenerator.producer.NameProducer;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducerManager;

import static net.driftingsouls.ds2.server.namegenerator.NameGeneratorUtils.*;

/**
 * Generator fuer terranische Namen. Die Namen werden aus einem
 * generierten Namennamen und einem zufaellig aus einer Liste 
 * ausgewaehlten Vornamen zusammengesetzt. Als Vorlage dienen
 * britische Vor- und Nachnamen.
 * @author Christopher Jung
 *
 */
public class EnglischePersonenNamenGenerator
{
	private NameProducer lastname;
	private NameProducer firstName;

	/**
	 * Konstruktor.
	 */
	public EnglischePersonenNamenGenerator()
	{
		this.lastname = NameProducerManager.INSTANCE.getMarkovNameProducer(EnglischePersonenNamenGenerator.class.getResource("englisch_nachnamen.txt"));
		this.firstName = NameProducerManager.INSTANCE.getListBasedNameProducer(EnglischePersonenNamenGenerator.class.getResource("englisch_vornamen.txt"));
	}

	private String generateLastName()
	{
		String name = this.lastname.generateNext();

		while( name.length() <= 2 ) {
			name = this.lastname.generateNext();
		}

		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

		name = upperAfterStrings(name, "Mc", "-", "'", " ");

		return name;
	}

	public String generate()
	{
		String lastname = generateLastName();
		lastname = firstName.generateNext() + " " + lastname;
		return lastname;
	}
}
