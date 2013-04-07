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
package net.driftingsouls.ds2.server.namegenerator.markov;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * Generiert aus einer Markov-Kette/Tabelle zufaellige Namen. Die Kette/Tabelle wird dabei aus einem
 * Eingabestrom eingelesen. Das Generieren wird von {@link MarkovTrainer} uebernommen.
 * 
 * @author Christopher Jung
 * 
 */
public class Markov
{
	private float[][][] table = new float[32][32][32];
	private int a;
	private int b;

	/**
	 * Konstruktor.
	 * 
	 * @param in Der Eingabestrom, welcher eine Markov-Kette/Tabelle enthaelt
	 * @throws IOException
	 */
	public Markov(InputStream in) throws IOException
	{
		try
		{
			ObjectInputStream oin = new ObjectInputStream(in);
			Header h = (Header)oin.readObject();

			if( h.version != 1 || h.order != 3 || h.width != 32 )
			{
				throw new IllegalArgumentException("Dateiformat ungueltig: " + h.version + " "
						+ h.order + " " + h.width + "!");
			}

			this.table = (float[][][])oin.readObject();
		}
		catch( ClassNotFoundException e )
		{
			throw new IOException("Kann Markov-Tabelle nicht laden", e);
		}
	}

	/**
	 * Generiert die angegebene Anzahl an Namen.
	 * 
	 * @param count Die Anzahl
	 * @return Die generierten Namen
	 */
	public synchronized String[] generate(int count)
	{
		String[] result = new String[count];
		StringBuilder word = new StringBuilder();

		while( count > 0 )
		{
			float random = (float)Math.random();
			int c = 0;
			int tableIndex = 0;
			float probability = this.table[a][b][tableIndex];
			while( c < 32 && probability < random )
			{
				probability += this.table[a][b][++tableIndex];
				++c;
			}

			if( c == 0 )
			{
				if( word.length() > 1 )
				{
					result[--count] = word.toString();
				}
				word.setLength(0);
			}
			else
			{
				word.append(toCharacter(c));
			}

			assert (c != 32);
			c = c & 31;
			a = b;
			b = c;
		}

		return result;
	}

	private char toCharacter(int c)
	{
		if( c == 27 )
		{
			return '-';
		}
		else if( c == 28 )
		{
			return ' ';
		}
		else if( c == 29 )
		{
			return '\'';
		}
		else if( c > 26 )
		{
			return '.';
		}
		else
		{
			return (char)('a' - 1 + c);
		}
	}
}
