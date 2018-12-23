package net.driftingsouls.ds2.server.namegenerator;

import net.driftingsouls.ds2.server.ships.ShipClasses;

/**
 * Generatoren fuer das Kuerzel fuer die Schiffsklasse am Anfang des Schiffsnamens.
 */
public enum SchiffsKlassenNamenGenerator
{
	KEIN_KUERZEL("Nichts") {
		@Override
		public String generiere(ShipClasses schiffsKlasse)
		{
			return "";
		}
	},
	TERRANISCH("Terranisch") {
		@Override
		public String generiere(ShipClasses schiffsKlasse)
		{
			switch(schiffsKlasse) {
				case TRANSPORTER:
					return "GTT";
				case ZERSTOERER:
					return "GTD";
				case JUGGERNAUT:
					return "GTJ";
				case KORVETTE:
					return "CTCv";
				case SCHWERER_KREUZER:
				case KREUZER:
					return "GTC";
				case FREGATTE:
					return "GTFg";
				case STATION:
					return "GTI";
				case JAEGER:
					return "GTF";
				case GESCHUETZ:
					return "GTSG";
				case FORSCHUNGSKREUZER:
					return "GTFC";
				case AWACS:
					return "GTA";
				case TRAEGER:
					return "GTCa";
				case BOMBER:
					return "GTB";
				default:
					return "";
			}
		}
	},
	VASUDANISCH("Vasudanisch") {
		@Override
		public String generiere(ShipClasses schiffsKlasse)
		{
			switch(schiffsKlasse) {
				case TRANSPORTER:
					return "GVT";
				case ZERSTOERER:
					return "GVD";
				case JUGGERNAUT:
					return "GVJ";
				case KORVETTE:
					return "GVCv";
				case SCHWERER_KREUZER:
				case KREUZER:
					return "GVC";
				case FREGATTE:
					return "GVFg";
				case STATION:
					return "GVI";
				case JAEGER:
					return "GVF";
				case GESCHUETZ:
					return "GVSG";
				case FORSCHUNGSKREUZER:
					return "GVFC";
				case AWACS:
					return "GVA";
				case TRAEGER:
					return "GVCa";
				case BOMBER:
					return "GVB";
				default:
					return "";
			}
		}
	};

	private final String label;

	SchiffsKlassenNamenGenerator(final String label)
	{
		this.label = label;
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
	 * Generiert ein Kuerzel.
	 *
	 * @return Das Kuerzel
	 */
	public abstract String generiere(ShipClasses schiffsKlasse);
}
