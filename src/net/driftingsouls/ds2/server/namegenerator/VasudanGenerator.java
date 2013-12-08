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

import static net.driftingsouls.ds2.server.namegenerator.NameGeneratorUtils.*;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducer;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducerManager;

/**
 * Generator fuer vasudanische Namen. Alle Namen werden zufaellig
 * generiert. Als Vorlage dienen altaegyptische Namen.
 * @author Christopher Jung
 *
 */
public class VasudanGenerator implements NameGenerator
{
	private NameProducer markov;

	/**
	 * Konstruktor.
	 * @throws IOException
	 */
	public VasudanGenerator() throws IOException
	{
		this.markov = NameProducerManager.INSTANCE.getMarkovNameProducer(VasudanGenerator.class.getResource("vasudan.txt"));
	}

	@Override
	public String[] generate(int count)
	{
		String[] result = new String[count];

		for( int i = 0; i < result.length; i++ )
		{
			String name = markov.generateNext();

			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			name = upperAfterStrings(name, "-", "'", " ");

			result[i] = name;
		}

		return result;
	}
}
