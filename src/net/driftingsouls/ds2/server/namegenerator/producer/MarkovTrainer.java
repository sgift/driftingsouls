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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Generator fuer Markov-Ketten aus einer Menge von Namen aus einer Eingabedatei.
 * Die Ketten koennen anschliessend dazu verwendet werden um zufaellige Namen zu generieren
 * ({@link Markov}).</p>
 * <p>Die Eingabedatei darf pro Zeile nur einen Namen enthalten.</p>
 *
 * @author Christopher Jung
 */
public class MarkovTrainer
{
	private float[][][] table;
	private boolean probabilitiesCalculated = false;
	private int a = 0, b = 0, c = 0;
	private float total = 0;
	private Map<Integer, Integer> charMap = new HashMap<>();
	private List<String> words = new ArrayList<>();

	public MarkovTrainer()
	{
		charMap.put(0, 0);
	}

	/**
	 * Fuegt einen Namen zur Kette/Tabelle hinzu.
	 *
	 * @param name Der Name
	 * @throws IllegalStateException Falls die Daten bereits abschliessend verarbeitet und gespeichert wurden
	 */
	public void add(String name) throws IllegalStateException
	{
		if (this.probabilitiesCalculated)
		{
			throw new IllegalStateException("Die Markov-Tabelle wurde bereits berechnet.");
		}

		words.add(name);

		for (int i = 0; i < name.length(); ++i)
		{
			int newC = name.charAt(i);
			if (!charMap.containsKey(newC))
			{
				charMap.put(newC, charMap.size());
			}
		}
	}

	private void nextChar(int newC)
	{
		a = b;
		b = c;
		c = charMap.get(newC);

		table[a][b][c] += 1;
		total += 1;
	}

	private void calculateProbabilities()
	{
		charMap.put(charMap.size(), 0);

		int size = charMap.size();
		table = new float[size][size][size];
		for (String name : words)
		{
			for (int i = 0; i < name.length(); ++i)
			{
				nextChar(name.charAt(i));
			}

			nextChar(0);
		}

		this.probabilitiesCalculated = true;

		for (int x = 0; x < size; ++x)
		{
			for (int y = 0; y < size; ++y)
			{
				float sum = 0;
				int counter = 0;
				for (int z = 0; z < size; ++z)
				{
					sum += this.table[x][y][counter];
					++counter;
				}

				float div = sum != 0 ? (1.f / sum) : 1;
				counter = 0;
				sum = 0;
				for (int z = 0; z < size; ++z)
				{
					this.table[x][y][counter] *= div;
					sum += this.table[x][y][counter];
					++counter;
				}
				this.table[x][y][counter - 1] += 1 - sum;
			}
		}
	}

	private char c(int x) {
		for (Map.Entry<Integer, Integer> entry : charMap.entrySet())
		{
			if( entry.getValue() == x ) {
				return (char)(int)entry.getKey();
			}
		}
		return '¹';
	}

	/**
	 * Erzeugt das aus dem Lernprozess resultierende Markov-Objekt,
	 * dass wiederum zur Generierung von Namen verwendet werden kann.
	 *
	 * @return das Objekt
	 */
	public Markov toMarkov()
	{
		this.calculateProbabilities();

		return new Markov(this.table, this.charMap);
	}
}
