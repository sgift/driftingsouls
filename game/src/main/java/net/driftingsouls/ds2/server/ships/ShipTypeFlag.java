package net.driftingsouls.ds2.server.ships;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Flags fuer Schiffstypen.
 */
public enum ShipTypeFlag
{
	/**
	 * Kennzeichnet das Schiff als Jaeger.
	 */
	JAEGER("jaeger", "Jäger", "Die Jägereigenschaft ermöglicht es diesem Schiff, auf Trägern zu landen"),

	/**
	 * Das angegebene Schiff verfuegt ueber eine Zerstoererpanzerung (Schadensabsorption).
	 */
	ZERSTOERERPANZERUNG("zerstoererpanzerung", "Schadensabsorption", "Die Schadensabsorption sorgt dafür, dass pro Kampfrunde dem Schiff maximal 25 % Schaden gemessen an an der maximalen Hüllenstärke zugefügt werden kann"),

	/**
	 * Das angegebene Schiff kann Asteroiden kolonisieren.
	 */
	COLONIZER("colonizer", "Kolonieschiff", "Kolonieschiffe können unbewohnte Asteroiden besiedeln"),

	/**
	 * Das angegebene Schiff kann in Kaempfen fluechtende Schiffe abfangen.
	 */
	ABFANGEN("abfangen", "Abfangen", "Schiffe mit der Eigenschaft 'Abfangen' können im Kampf auf flüchtende Schiffe feuern"),

	/**
	 * Das angegebene Schiff ist nicht kaperbar.
	 */
	NICHT_KAPERBAR("nicht_kaperbar", "nicht kaperbar", "Dieses Schiff kann nicht gekapert, wohl aber geplündert werden"),

	/**
	 * Das Schiff wird nach einem Transfervorgang beim naechsten Tick zerstoert.
	 */
	INSTABIL("instabil", "instabil", "Ein Objekt mit der Eigenschaft 'instabil' zerfällt, sobald es geplündert wurde"),

	/**
	 * Das Schiff ist nur sichtbar, wenn man sich im selben Sektor befindet.
	 */
	SEHR_KLEIN("sehr_klein", "sehr klein", "Ein Objekt mit der Eigenschaft 'sehr klein' kann aufgrund seiner Größe nicht auf den Langstreckensensoren geortet werden"),

	/**
	 * Transfers von und zu dem Schiff sind nicht moeglich.
	 */
	KEIN_TRANSFER("kein_transfer", "kein Transfer möglich", "Es können keine Waren zu oder von dem Objekt transferiert werden, da der Laderaum versiegelt ist. Dies gilt auch für Plünderungen"),

	/**
	 * Das Schiff verfuegt ueber erweiterte SRS-Sensoren (mehr Informationen).
	 */
	SRS_AWAC("srs_awac", "AWACS-SRS", "Mit AWACS-Kurzstreckensensoren kann das Schiff die Antriebsüberhitzung anderer Schiffe im Sektor analysieren"),

	/**
	 * Das Schiff verfuegt ueber zusaetzliche Erweiterungen zuzueglich zu den erweiterten SRS-Sensoren.
	 * (Nur wirksam in Kombination mit {@link #SRS_AWAC}
	 */
	SRS_EXT_AWAC("srs_ext_awac", "erweiterte AWACS-SRS", "Mit den Verbesserten AWACS-Kurzstreckensensoren ist eine detailierte Analyse von Antriebsüberhitzung, Crew und Enegiereserven eines anderen Schiffes im selben Sektor möglich"),

	/**
	 * Das Schiff verfuegt ueber einen shivanischen Sprungantrieb.
	 */
	JUMPDRIVE_SHIVAN("jumpdrive_shivan", "shivanischer Sprungantrieb", "Dieses Schiff verfügt über einen shivanischen Sprungantrieb"),

	/**
	 * Das Schiff kann direkt einer Schlacht beitreten ohne eine Runde "aussetzen" zu muessen.
	 */
	INSTANT_BATTLE_ENTER("instant_battle_enter", "schnelle Kampfbereitschaft", "Dieses Schiff ist in der Runde, in der es einer Schlacht beitritt, bereits einsatzbereit"),

	/**
	 * Das Schiff kann nicht gepluendert werden.
	 */
	NICHT_PLUENDERBAR("nicht_pluenderbar", "nicht plünderbar", "Sie können keine Waren von diesem Schiff plündern"),

	/**
	 * Das Schiff kann nicht zerstoert werden.
	 */
	GOD_MODE("god_mode", "nicht zerstörbar (God Mode)", "Dieses Schiff kann nicht zerstört werden"),

	/**
	 * Das Schiff kann Drohnen kontrollieren.
	 */
	DROHNEN_CONTROLLER("drohnen_controller", "Drohnen-Kontrollschiff", "Dieses Schiff kann Drohnen kontrollieren"),

	/**
	 * Das Schiff ist eine Drohne und kann daher nur im selben Sektor wie ein Drohnenkontroller agieren ({@link #DROHNEN_CONTROLLER}).
	 * Wenn kein Drohnenkontroller vorhanden ist, ist es handlungsunfaehig.
	 */
	DROHNE("drohne", "Drohne", "Dieses Schiff ist eine Drohne. Es benötigt ein Drohnen-Kontrollschiff, um funktionieren zu können"),

	/**
	 * Das Schiff kann in einer Schlacht in die zweite Reihe fliegen.
	 */
	SECONDROW("secondrow", "zweite Reihe", "Dieses Schiff kann in einem Kampf in die zweite Reihe wechseln, wo es vor Angriffen des Gegners besser geschützt ist"),

	/**
	 * Das Schiff kann Offiziere in Hoehe der eigenen max. Crew aufnehmen.
	 */
	OFFITRANSPORT("offitransport", "Offizierstransporter", "Dieses Schiff kann Offiziere in der Höhe seiner maximalen Besatzungsplätze transportieren"),

	/**
	 * Das Schiff kann sich mit anderen Werften zu Werftkomplexen zusammenschliessen.
	 */
	WERFTKOMPLEX("werftkomplex", "Werftkomplex", "Diese Werft kann sich mit anderen Werften, welche die ebenfalls Eigenschaft Werftkomplex besitzen, zusammenschließen. Die Ressourcen der Einzelwerften werden dabei gemeinsam genutzt"),

	/**
	 * Das Schiff kann sich mit anderen Werften zu Werftkomplexen zusammenschliessen.
	 */
	TRADEPOST("tradepost", "Handelsposten", "Bei diesem Schiff handelt es sich um einen Handelsposten"),

	/**
	 * Das Schiff kann keine selbstzerstoerung.
	 */
	NICHT_ZERSTOERBAR("nosuicide", "keine Selbstzerstörung", "Dieses Schiff verfügt über keine Selbstzerstörungsanlage"),

	/**
	 * Das Schiff ist ein Versorger.
	 */
	VERSORGER("versorger", "Versorger", "Dieses Schiff ist ein Versorger und kann eigene wie verbündete Schiffe im selben Sektor mit Nahrung versorgen"),
	/**
	 * Übergabe von und zu dem Schiff sind nicht moeglich.
	 */
	NICHT_UEBERGEBBAR("nicht_uebergebbar", "nicht übergebbar", "Der Inhaber dieses Schiffes kann es nicht an andere Kolonisten übergeben"),
	/**
	 * Muss im Kampf als erstes zerstört werden.
	 */
	SCHUTZSCHILD("schutzschild", "Schutzschild", "Dieses Schiff schützt verbündete Schiffe vor Angriffen, bis es zerstört worden ist. Im Kampf muss es als erstes zerstört werden, ehe andere Schiffe 'hinter dem Schild' angegriffen werden können");

	private final String flag;
	private final String label;
	private final String description;

	ShipTypeFlag(String flag, String label, String description)
	{
		this.flag = flag;
		this.label = label;
		this.description = description;
	}

	/**
	 * Gibt die ID des internen Flags in der Datenbank zurueck.
	 * @return Das Flag
	 */
	public String getFlag()
	{
		return flag;
	}

	/**
	 * Gibt das Anzeigelabel des Flags zurueck.
	 * @return Das Label
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Gibt die Beschreibung des Flags zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Gibt das Flag mit der angebenen Datenbank-ID zurueck.
	 * @param flag Die Datenbank-ID des Flags
	 * @return Das Flag
	 * @throws IllegalArgumentException Falls die ID unbekannt ist
	 * @see #getFlag()
	 */
	public static Optional<ShipTypeFlag> byFlag(@Nonnull String flag) throws IllegalArgumentException
	{
		for (ShipTypeFlag shipTypeFlag : values())
		{
			if( shipTypeFlag.getFlag().equals(flag) )
			{
				return Optional.of(shipTypeFlag);
			}
		}

		return Optional.empty();
	}

	/**
	 * Parst einen mit Leerzeichen separierten String von Datenbank-IDs von Flags und
	 * gibt die zugehoerigen Objekte als Set zurueck.
	 * @param flagString Der String
	 * @return Die Enum-Objekte
	 * @throws java.lang.IllegalArgumentException Falls die ID eines Flags unbekannt ist
	 */
	public static @Nonnull EnumSet<ShipTypeFlag> parseFlags(String flagString) throws IllegalArgumentException
	{
		EnumSet<ShipTypeFlag> flagSet = EnumSet.noneOf(ShipTypeFlag.class);

		if( flagString == null )
		{
			return flagSet;
		}

		String[] flagArray = StringUtils.split(flagString, ' ');
		for (String aflag : flagArray)
		{
			if( aflag.trim().isEmpty() )
			{
				continue;
			}
			ShipTypeFlag.byFlag(aflag).ifPresent(flagSet::add);
		}
		return flagSet;
	}
}
