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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
	 * <p>Speichert die Struktur in angegebenen Stream ab. Der
	 * Stream wird nicht geschlossen!</p>
	 * <p>Nach dem Speichern sind keine weiteren Aenderungen an der Struktur
	 * mehr moeglich.</p>
	 * @param out Der Stream in den die Daten gespeichert werden sollen.
	 * @throws IOException
	 */
	public void save(OutputStream out) throws IOException
	{
		this.calculateProbabilities();
		
		// save the file
		ObjectOutputStream oout = new ObjectOutputStream(out);

		Header h = new Header();
		oout.writeObject(h);
		oout.writeObject(this.table);
	}
	
	/**
	 * Programm zum Generieren und Ablegen einer Markov-Kette/Tabelle
	 * in einer Datei. Das erste Argument ist die Zieldatei fuer die Ausgabe.
	 * Das zweite eine Eingabedatei in welcher pro Zeile ein Name steht.
	 * @param args Die Argumente
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if( args.length < 2 ) {
			System.err.println("Usage: MarkovTrainer output-file input-file");
			return;
		}
		final File outputFile = new File(args[0]);
		final File inputFile = new File(args[1]);
		if( !inputFile.isFile() ) {
			throw new FileNotFoundException(inputFile+" nicht gefunden");
		}
		
		System.out.println("Generiere Markov-Kette für "+inputFile.getName());
		
		final MarkovTrainer trainer = new MarkovTrainer();
		
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		try {
			int counter = 0;
			String name;
			while( (name = reader.readLine()) != null ) {
				name = name.trim();
				if( name.isEmpty() ) {
					continue;
				}
				counter++;
				trainer.add(name);
			}
			System.out.println(counter+" Namen verarbeitet");
		}
		finally {
			reader.close();
		}
		
		System.err.println("Schreibe "+outputFile.getName());
		FileOutputStream out = new FileOutputStream(outputFile);
		try {
			trainer.save(out);
		}
		finally {
			out.close();
		}
	}
}
