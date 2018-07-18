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
package net.driftingsouls.ds2.server.namegenerator.producer;

import java.util.HashMap;
import java.util.Map;

/**
 * Generiert aus einer Markov-Kette/Tabelle zufaellige Namen. Die Kette/Tabelle wird dabei aus einem
 * Eingabestrom eingelesen. Das Generieren wird von {@link MarkovTrainer} uebernommen.
 * 
 * @author Christopher Jung
 * 
 */
public class Markov implements NameProducer
{
	private float[][][] table;
	private int a;
	private int b;
	private Map<Integer,Integer> charMap;

	/**
	 * Konstruktor.
	 * @param table Die zu verwendende Markov-Tabelle
	 */
	public Markov(float[][][] table, Map<Integer,Integer> charMap)
	{
		this.table = table.clone();
		this.charMap = new HashMap<>();
		for (Map.Entry<Integer, Integer> entry : charMap.entrySet())
		{
			this.charMap.put(entry.getValue(), entry.getKey());
		}

	}

	@Override
	public synchronized String generateNext()
	{
		String result = null;
		StringBuilder word = new StringBuilder();

		while( result == null ) {
			float random = (float)Math.random();
			int c = 0;
			int tableIndex = 0;
			float probability = this.table[a][b][tableIndex];
			while( c < charMap.size() && probability < random )
			{
				probability += this.table[a][b][++tableIndex];
				++c;
			}

			if( c == charMap.size() - 1 )
			{
				a = 0;
				b = 0;
				word.setLength(0);
				continue;
			}

			if( c == 0 || word.length() > 32 )
			{
				if( word.length() > 1 )
				{
					result = word.toString();
				}
				word.setLength(0);
			}
			else
			{
				word.append(toCharacter(c));
			}

			assert (c != charMap.size());
			//c = c & charMap.size();
			a = b;
			b = c;
		}

		return result;
	}

	private char toCharacter(int c)
	{
		return (char)(int)this.charMap.get(c);
	}
}
