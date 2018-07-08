package net.driftingsouls.ds2.server.namegenerator;

import net.driftingsouls.ds2.server.namegenerator.producer.NameProducer;
import net.driftingsouls.ds2.server.namegenerator.producer.NameProducerManager;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;

/**
 * Generatoren fuer Schiffsnamen (ohne Schiffsklassenprefix).
 */
public enum SchiffsNamenGenerator
{
	SCHIFFSTYP("Schiffstyp") {
		@Override
		public String generiere(ShipTypeData schiffstyp)
		{
			return schiffstyp.getNickname();
		}
	},
	STAEDTE("Städtenamen") {
		private NameProducer nameProducer = NameProducerManager.INSTANCE.getListBasedNameProducer(SchiffsNamenGenerator.class.getResource("staedtenamen.txt"), false);

		@Override
		public String generiere(ShipTypeData schiffsTyp)
		{
			ShipClasses schiffsKlasse = schiffsTyp.getShipClass();
			switch(schiffsKlasse) {
				case UNBEKANNT:
				case EMTPY:
					return "";
				case CONTAINER:
					return generiereContainerNamen();
				case SCHROTT:
					return "Weltraumschrott";
				case RETTUNGSKAPSEL:
					return "Rettungskapsel";
				case FELSBROCKEN:
					return "Felsbrocken";
				default:
					return StringUtils.capitalize(nameProducer.generateNext());

			}
		}
	},
	AEGYPTISCHE_NAMEN("Ägyptische Namen") {
		private NameProducer markov = NameProducerManager.INSTANCE.getMarkovNameProducer(PersonenNamenGenerator.class.getResource("aegyptische_nachnamen.txt"));

		@Override
		public String generiere(ShipTypeData schiffsTyp)
		{
			ShipClasses schiffsKlasse = schiffsTyp.getShipClass();
			switch(schiffsKlasse) {
				case UNBEKANNT:
				case EMTPY:
					return "";
				case CONTAINER:
					return generiereContainerNamen();
				case SCHROTT:
					return "Weltraumschrott";
				case RETTUNGSKAPSEL:
					return "Rettungskapsel";
				case FELSBROCKEN:
					return "Felsbrocken";
				default:
					return StringUtils.capitalize(markov.generateNext());

			}
		}
	};

	private final String label;
	private Random rnd = new Random();

	SchiffsNamenGenerator(final String label)
	{
		this.label = label;
	}

	protected String generiereContainerNamen() {
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String part1 = Integer.toString(rnd.nextInt(10000));
		char part2 = alpha.charAt(rnd.nextInt(alpha.length()));
		String part3 = Integer.toString(rnd.nextInt(1000));

		return "C-"+ StringUtils.leftPad(part1, 4, '0')+"-"+part2+"-"+StringUtils.leftPad(part3, 3, '0');
	}

	/**
	 * Gibt den Anzeigenamen des Generators zurueck.
	 *
	 * @return Der Anzeigename
	 */
	public String getLabel()
	{
		return this.label;
	}

	/**
	 * Generiert einen Namen.
	 * @param schiffsTyp Der Schiffstyp fuer den der Name generiert werden soll
	 * @return Der Name
	 */
	public abstract String generiere(ShipTypeData schiffsTyp);
}
