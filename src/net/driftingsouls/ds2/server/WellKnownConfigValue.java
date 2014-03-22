package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.ConfigValueDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Die bekannten Konfigurationseinstellungen in DS.
 * @param <T> Der Zieldatentyp
 */
public class WellKnownConfigValue<T> implements ConfigValueDescriptor<T>
{
	private static final List<WellKnownConfigValue> VALUES = new ArrayList<>();

	// Hinweis: Keine Enum, da Enums keine Type Parameter unterstuetzen und
	// es Ziel ist, dass ein Nutzer dieser Klasse den Datentyp nicht extra
	// angeben braucht beim Aufruf von ConfigService.

	/**
	 * Der aktuelle Tick.
	 */
	public static final ConfigValueDescriptor<Integer> TICKS = new WellKnownConfigValue<>(Integer.class, "ticks", "1", "Der aktuelle Tick");
	/**
	 * Der Faktor für Nahrungskosten bei Offiziersweiterbildung.
	 */
	public static final ConfigValueDescriptor<Double> OFFIZIER_NAHRUNG_FACTOR = new WellKnownConfigValue<>(Double.class, "offnahrungfactor", "1.5", "Der Faktor für Nahrungskosten bei Offiziersweiterbildung");
	/**
	 * Der Faktor für Siliziumkosten bei Offiziersweiterbildung.
	 */
	public static final ConfigValueDescriptor<Double> OFFIZIER_SILIZIUM_FACTOR = new WellKnownConfigValue<>(Double.class, "offsiliziumfactor", "1", "Der Faktor für Siliziumkosten bei Offiziersweiterbildung");
	/**
	 * Der Faktor für die Dauer bei Offiziersweiterbildung.
	 */
	public static final ConfigValueDescriptor<Double> OFFIZIER_DAUER_FACTOR = new WellKnownConfigValue<>(Double.class, "offdauerfactor", "0.25", "Der Faktor für die Dauer bei Offiziersweiterbildung ");
	/**
	 * Die Siliziumkosten eines neu gebauten Offiziers.
	 */
	public static final ConfigValueDescriptor<Integer> NEW_OFF_SILIZIUM_COSTS = new WellKnownConfigValue<>(Integer.class, "newoffsiliziumcosts", "25", "Die Siliziumkosten eines neu gebauten Offiziers");
	/**
	 * Die Nahrungskosten eines neu gebauten Offiziers.
	 */
	public static final ConfigValueDescriptor<Integer> NEW_OFF_NAHRUNG_COSTS = new WellKnownConfigValue<>(Integer.class, "newoffnahrungcosts", "35", "Die Nahrungskosten eines neu gebauten Offiziers");
	/**
	 * Die Dauer eines neu gebauten Offiziers.
	 */
	public static final ConfigValueDescriptor<Integer> OFF_DAUER_COSTS = new WellKnownConfigValue<>(Integer.class, "offdauercosts", "8", "Die Dauer eines neu gebauten Offiziers");
	/**
	 * Laenge der Offiziersbauschlange.
	 */
	public static final ConfigValueDescriptor<Integer> MAX_OFFS_TO_TRAIN = new WellKnownConfigValue<>(Integer.class, "maxoffstotrain", "5", "Laenge der Offiziersbauschlange");
	/**
	 * Wert zwischen 0 und 1, der angibt welcher Anteil des freien Platzes maximal von neuen Einwanderern belegt wird.
	 */
	public static final ConfigValueDescriptor<Double> IMMIGRATION_FACTOR = new WellKnownConfigValue<>(Double.class, "immigrationfactor", "1", "Wert zwischen 0 und 1, der angibt welcher Anteil des freien Platzes maximal von neuen Einwanderern belegt wird.");
	/**
	 * Zufallswert zwischen 0 und dem Maximum an Einwanderern waehlen.
	 */
	public static final ConfigValueDescriptor<Boolean> RANDOMIZE_IMMIGRATION = new WellKnownConfigValue<>(Boolean.class, "randomizeimmigration", "true", "Zufallswert zwischen 0 und dem Maximum an Einwanderern waehlen.");
	/**
	 * Standard-Dropzone der GTU. Kann von jedem genutzt werden, egal, ob er Asteroiden im System hat.
	 */
	public static final ConfigValueDescriptor<Integer> GTU_DEFAULT_DROPZONE = new WellKnownConfigValue<>(Integer.class, "gtudefaultdropzone", "75", "Standard-Dropzone der GTU. Kann von jedem genutzt werden, egal, ob er Asteroiden im System hat.");
	/**
	 * Vacationpunkte, die ein Tick im Vac kostet.
	 */
	public static final ConfigValueDescriptor<Integer> VAC_POINTS_PER_VAC_TICK = new WellKnownConfigValue<>(Integer.class, "vacpointspervactick", "7", "Vacationpunkte, die ein Tick im Vac kostet");
	/**
	 * Faktor fuer die Groesse bei der Battle Value Formel.
	 */
	public static final ConfigValueDescriptor<Integer> BATTLE_VALUE_SIZE_MODIFIER = new WellKnownConfigValue<>(Integer.class, "bvsizemodifier", "4", "Faktor fuer die Groesse bei der Battle Value Formel");
	/**
	 * Faktor fuer die Dockanzahl bei der Battle Value Formel.
	 */
	public static final ConfigValueDescriptor<Integer> BATTLE_VALUE_DOCK_MODIFIER = new WellKnownConfigValue<>(Integer.class, "bvdockmodifier", "4", "Faktor fuer die Dockanzahl bei der Battle Value Formel");
	/**
	 * Preis, den ein Handelsinserat pro Tick kostet.
	 */
	public static final ConfigValueDescriptor<Integer> AD_COST = new WellKnownConfigValue<>(Integer.class, "adcost", "10", "Preis, den ein Handelsinserat pro Tick kostet.");
	/**
	 * Die ID des Comnet-Kanals in dem Kopfgeldmeldungen erscheinen sollen.
	 */
	public static final ConfigValueDescriptor<Integer> BOUNTY_CHANNEL = new WellKnownConfigValue<>(Integer.class, "bountychannel", "1", "Die ID des Comnet-Kanals in dem Kopfgeldmeldungen erscheinen sollen");
	/**
	 * Begruendung, weshalb man sich nicht registrieren kann (Leeres Feld == registrieren moeglich).
	 */
	public static final ConfigValueDescriptor<String> DISABLE_REGISTER = new WellKnownConfigValue<>(String.class, "disableregister", "", "Begruendung, weshalb man sich nicht registrieren kann (Leeres Feld == registrieren moeglich)");
	/**
	 * Begruendung, weshalb der Login abgeschalten ist (Leeres Feld == Login moeglich).
	 */
	public static final ConfigValueDescriptor<String> DISABLE_LOGIN = new WellKnownConfigValue<>(String.class, "disablelogin", "", "Begruendung, weshalb der Login abgeschalten ist (Leeres Feld == Login moeglich)");
	/**
	 * Begruendung, weshalb der Login abgeschalten ist (Leeres Feld == Login moeglich).
	 */
	public static final ConfigValueDescriptor<Integer> END_TIE_MODIFIER = new WellKnownConfigValue<>(Integer.class, "disablelogin", "5", "Faktor fuer die Anzahl der Schiffe, die man mehr haben muss, um einen Kampf unentschieden zu beenden");
	/**
	 * Die maximale Anzahl an Gegenstaenden (Items/Waren) pro Schiffs-Truemmerteil.
	 */
	public static final ConfigValueDescriptor<Integer> TRUEMMER_MAX_ITEMS = new WellKnownConfigValue<>(Integer.class, "truemmer_maxitems", "4", "Die maximale Anzahl an Gegenstaenden (Items/Waren) pro Schiffs-Truemmerteil");
	/**
	 * Sperrt Accounts waehrend des Ticks. 0 fuer keine Sperre, 1 fuer Sperre.
	 */
	public static final ConfigValueDescriptor<Integer> TICK = new WellKnownConfigValue<>(Integer.class, "tick", "0", "Sperrt Accounts waehrend des Ticks. 0 fuer keine Sperre, 1 fuer Sperre.");
	/**
	 * Automatisches Feuern fuer NPCs im Tick ein- oder ausschalten.
	 */
	public static final ConfigValueDescriptor<Boolean> AUTOFIRE = new WellKnownConfigValue<>(Boolean.class, "autofire", "false", "Automatisches Feuern fuer NPCs im Tick ein- oder ausschalten");
	/**
	 * Prozentsatz der Crew die maximal pro Tick verhungert.
	 */
	public static final ConfigValueDescriptor<Integer> MAX_VERHUNGERN = new WellKnownConfigValue<>(Integer.class, "maxverhungern", "10", "Prozentsatz der Crew die maximal pro Tick verhungert");
	/**
	 * Skalierungsfaktor fuer den Huellenschaden durch zu wenig Crew.
	 */
	public static final ConfigValueDescriptor<Double> NO_CREW_HULL_DAMAGE_SCALE = new WellKnownConfigValue<>(Double.class, "nocrewhulldamagescale", "10", "Skalierungsfaktor fuer den Huellenschaden durch zu wenig Crew");
	/**
	 * Vacationpunkte, die ein gespielter Tick bringt.
	 */
	public static final ConfigValueDescriptor<Integer> VAC_POINTS_PER_PLAYED_TICK = new WellKnownConfigValue<>(Integer.class, "vacpointsperplayedtick", "1", "Vacationpunkte, die ein gespielter Tick bringt");
	/**
	 * Limitiert die Reperaturkosten auf die Baukosten * faktor.
	 */
	public static final ConfigValueDescriptor<Double> REPAIR_COST_DAMPENING_FACTOR = new WellKnownConfigValue<>(Double.class, "repaircostdampeningfactor", "0.3", "Limitiert die Reperaturkosten auf die Baukosten * faktor");
	/**
	 * Schluessel mit denen man sich registrieren kann, wenn der Wert * ist braucht man keinen Schluessel zum registrieren.
	 */
	public static final ConfigValueDescriptor<String> KEYS = new WellKnownConfigValue<>(String.class, "keys", "*", "Schluessel mit denen man sich registrieren kann, wenn der Wert * ist braucht man keinen Schluessel zum registrieren");

	private String name;
	private String description;
	private String defaultValue;
	private Class<T> type;

	private WellKnownConfigValue(Class<T> type, String name, String defaultValue, String description)
	{
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.type = type;

		VALUES.add(this);
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getDefaultValue()
	{
		return defaultValue;
	}

	@Override
	public Class<T> getType()
	{
		return type;
	}

	/**
	 * Gibt alle Konfigurationseinstellungen zurueck.
	 * @return Die Konfigurationseinstellungen
	 */
	public WellKnownConfigValue[] values()
	{
		return VALUES.toArray(new WellKnownConfigValue[VALUES.size()]);
	}
}
