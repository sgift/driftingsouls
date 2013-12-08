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
public class TerranGenerator implements NameGenerator
{
	private NameProducer lastname;
	private NameProducer firstName;

	/**
	 * Konstruktor.
	 * @throws IOException
	 */
	public TerranGenerator() throws IOException
	{
		this.lastname = NameProducerManager.INSTANCE.getMarkovNameProducer(TerranGenerator.class.getResource("british.txt"));
		this.firstName = NameProducerManager.INSTANCE.getListBasedNameProducer(TerranGenerator.class.getResource("british_firstnames.txt"));
	}

	private String[] generateLastNames(int count)
	{
		String[] result = new String[count];

		for( int i = 0; i < result.length; i++ )
		{
			String name = this.lastname.generateNext();

			while( name.length() <= 2 ) {
				name = this.lastname.generateNext();
			}
			
			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			name = upperAfterStrings(name, "Mc", "-", "'", " ");

			result[i] = name;
		}

		return result;
	}

	@Override
	public String[] generate(int count)
	{
		String[] lastnames = generateLastNames(count);
		for( int i = 0; i < lastnames.length; i++ )
		{
			lastnames[i] = firstName.generateNext() + " " + lastnames[i];
		}
		return lastnames;
	}
}
