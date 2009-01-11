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

import net.driftingsouls.ds2.server.namegenerator.markov.Markov;

/**
 * Generator fuer vasudanische Namen. Alle Namen werden zufaellig
 * generiert. Als Vorlage dienen altaegyptische Namen.
 * @author Christopher Jung
 *
 */
public class VasudanGenerator implements NameGenerator
{
	private Markov markov;

	/**
	 * Konstruktor.
	 * @throws IOException
	 */
	public VasudanGenerator() throws IOException
	{
		this.markov = new Markov(VasudanGenerator.class.getResourceAsStream("vasudan.jmk"));
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
		String[] result = this.markov.generate(count);

		for( int i = 0; i < result.length; i++ )
		{
			String name = result[i];

			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			name = upperAfterString(name, "-");
			name = upperAfterString(name, "'");
			name = upperAfterString(name, " ");

			result[i] = name;
		}

		return result;
	}
}
