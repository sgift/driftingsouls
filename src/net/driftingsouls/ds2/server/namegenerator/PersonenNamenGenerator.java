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

import net.driftingsouls.ds2.server.namegenerator.producer.NameProducer;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducerManager;

import static net.driftingsouls.ds2.server.namegenerator.NameGeneratorUtils.upperAfterStrings;

/**
 * Namensgeneratoren fuer Personennamen.
 *
 * @author Christopher Jung
 */
public enum PersonenNamenGenerator
{
	/**
	 * (Alt)Aegyptische Namen.
	 */
	AEGYPTISCH("Ägyptisch")
	{
		private NameProducer markov = NameProducerManager.INSTANCE.getMarkovNameProducer(PersonenNamenGenerator.class.getResource("aegyptische_nachnamen.txt"));

		@Override
		public String generiere()
		{
			String name = markov.generateNext();

			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			name = upperAfterStrings(name, "-", "'", " ");

			return name;
		}
	},
	/**
	 * Englische Namen (Vor- und Nachname).
	 */
	ENGLISCH("Englisch")
	{
		private VorNachnamePersonenNamenGenerator generator = new VorNachnamePersonenNamenGenerator("englisch_vornamen.txt", "englisch_nachnamen.txt", new String[]{"Mc", "-", "'", " "});

		@Override
		public String generiere()
		{
			return generator.generate();
		}
	},
	/**
	 * Franzoesische Namen (Vor- und Nachname).
	 */
	FRANZOESISCH("Französisch")
	{
		private VorNachnamePersonenNamenGenerator generator = new VorNachnamePersonenNamenGenerator("franzoesische_vornamen.txt", "franzoesische_nachnamen.txt", new String[]{"-", "'", " "});

		@Override
		public String generiere()
		{
			return generator.generate();
		}
	},
	/**
	 * Spanische Namen (Vorname und zwei Nachnamen).
	 */
	SPANISCH("Spanisch")
	{
		private SpanischeNamenGenerator generator = new SpanischeNamenGenerator("spanische_vornamen.txt", "spanische_nachnamen.txt", new String[]{"-", "'", " "});

		@Override
		public String generiere()
		{
			return generator.generate();
		}
	};

	private final String label;

	PersonenNamenGenerator(final String label)
	{
		this.label = label;
	}

	/**
	 * Gibt den Anzeigenamen des Namensgenerators zurueck.
	 *
	 * @return Der Anzeigename
	 */
	public String getLabel()
	{
		return this.label;
	}

	/**
	 * Generiert einen Namen.
	 *
	 * @return Der generierte Namen
	 */
	public abstract String generiere();
}