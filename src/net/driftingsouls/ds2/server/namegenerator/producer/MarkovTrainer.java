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

/**
 * <p>Generator fuer Markov-Ketten aus einer Menge von Namen aus einer Eingabedatei.
 * Die Ketten koennen anschliessend dazu verwendet werden um zufaellige Namen zu generieren
 * ({@link Markov}).</p>
 * <p>Die Eingabedatei darf pro Zeile nur einen Namen enthalten.</p>
 * @author Christopher Jung
 *
 */
public class MarkovTrainer
{
	private float[][][] table = new float[32][32][32];
	private boolean probabilitiesCalculated = false; 
	private int a = 0, b = 0, c = 0;
	private float total = 0;

	/**
	 * Fuegt einen Namen zur Kette/Tabelle hinzu.
	 * @param name Der Name
	 * @throws IllegalStateException Falls die Daten bereits abschliessend verarbeitet und gespeichert wurden
	 */
	public void add(String name) throws IllegalStateException
	{
		if( this.probabilitiesCalculated ) {
			throw new IllegalStateException("Die Markov-Tabelle wurde bereits berechnet.");
		}

		for( int i = 0; i < name.length(); ++i )
		{
			nextChar(name.charAt(i));
		}

		nextChar(0);
	}
	
	private void nextChar(int newC) {
		a = b; 
		b = c; 
		c = newC;

		if( c == '-' ) {
			c = 27;
		}
		else if( c == ' ' ) {
			c = 28;
		}
		else if( c == '\'' ) {
			c = 29;
		}
		// Strenge Limitierung auf a-z und A-Z
		else if( c != 0 && (Character.toLowerCase(c) < 'a' || Character.toLowerCase(c) > 'z') )
		{
			System.err.println("Unkown character '"+(char)c+"' (c="+c+")");
			c = 31;
		}
		else if( c != 0 )
		{
			c = c & 31;
		}

		table[a][b][c] += 1;
		total += 1;
	}

	private void calculateProbabilities()
	{
		this.probabilitiesCalculated = true;
		
		for( int x = 0; x < 32; ++x )
		{
			for( int y = 0; y < 32; ++y )
			{
				float sum = 0;
				int counter = 0;
				for( int z = 0; z < 32; ++z )
				{
					sum += this.table[x][y][counter];
					++counter;
				}

				float div = sum != 0 ? (1.f / sum) : 1;
				counter = 0;
				sum = 0;
				for( int z = 0; z < 32; ++z )
				{
					this.table[x][y][counter] *= div;
					sum += this.table[x][y][counter];
					++counter;
				}
				this.table[x][y][counter-1] += 1.00001 - sum;
			}
		}
	}

	/**
	 * Erzeugt das aus dem Lernprozess resultierende Markov-Objekt,
	 * dass wiederum zur Generierung von Namen verwendet werden kann.
	 * @return das Objekt
	 */
	public Markov toMarkov()
	{
		this.calculateProbabilities();

		return new Markov(this.table);
	}
}
