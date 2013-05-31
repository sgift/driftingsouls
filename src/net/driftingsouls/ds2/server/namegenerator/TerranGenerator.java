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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.driftingsouls.ds2.server.namegenerator.markov.Markov;

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
	private Markov markov;
	private List<String> firstNames = new ArrayList<>();

	/**
	 * Konstruktor.
	 * @throws IOException
	 */
	public TerranGenerator() throws IOException
	{
		this.markov = new Markov(TerranGenerator.class.getResourceAsStream("british.jmk"));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TerranGenerator.class
				.getResourceAsStream("british_firstnames.txt"))))
		{
			String firstName;
			while ((firstName = reader.readLine()) != null)
			{
				firstNames.add(firstName);
			}
		}
	}

	private String[] generateLastNames(int count)
	{
		String[] result = this.markov.generate(count);

		for( int i = 0; i < result.length; i++ )
		{
			String name = result[i];

			while( name.length() <= 2 ) {
				name = this.markov.generate(1)[0];
			}
			
			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			name = upperAfterString(name, "Mc");
			name = upperAfterString(name, "-");
			name = upperAfterString(name, "'");
			name = upperAfterString(name, " ");

			result[i] = name;
		}

		return result;
	}

	private String upperAfterString(String name, String str)
	{
		int index = -1;
		while( (index = name.indexOf(str, index+1)) > -1 )
		{
			if( index+1 + str.length() >= name.length() )
			{
				break;
			}
			String firstPart = name.substring(0, index);
			String middlePart = name.substring(index, index+str.length());
			String endPart = name.substring(index+str.length());
			
			name = firstPart + middlePart + Character.toUpperCase(endPart.charAt(0)) + endPart.substring(1);
		}
		return name;
	}

	@Override
	public String[] generate(int count)
	{
		Random rnd = new Random();

		String[] lastnames = generateLastNames(count);
		for( int i = 0; i < lastnames.length; i++ )
		{
			lastnames[i] = firstNames.get(rnd.nextInt(firstNames.size())) + " " + lastnames[i];
		}
		return lastnames;
	}
}
