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
	public static final ConfigValueDescriptor<Integer> END_TIE_MODIFIER = new WellKnownConfigValue<>(Integer.class, "endtiemodifier", "5", "Faktor fuer die Anzahl der Schiffe, die man mehr haben muss, um einen Kampf unentschieden zu beenden");
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
	/**
	 * Schiffstyp, der fuer Truemmerobjekte (Loot) verwendet wird.
	 */
	public static final ConfigValueDescriptor<Integer> TRUEMMER_SHIPTYPE = new WellKnownConfigValue<>(Integer.class, "truemmer_shiptype", "49", "Schiffstyp, der fuer Truemmerobjekte (Loot) verwendet wird");
	/**
	 * Ist das zerstoeren von Schiffen in einer Schlacht erlaubt?
	 */
	public static final ConfigValueDescriptor<Boolean> DESTROYABLE_SHIPS = new WellKnownConfigValue<>(Boolean.class, "destroyable_ships", "true", "Ist das zerstoeren von Schiffen in einer Schlacht erlaubt?");
	/**
	 * Ist der Noob-Schutz fuer neue Spieler aktiviert?
	 */
	public static final ConfigValueDescriptor<Boolean> NOOB_PROTECTION = new WellKnownConfigValue<>(Boolean.class, "noob_protection", "true", "Ist der Noob-Schutz fuer neue Spieler aktiviert?");
	/**
	 * Sind Cheats im Kampfsystem erlaubt?
	 */
	public static final ConfigValueDescriptor<Boolean> ENABLE_CHEATS = new WellKnownConfigValue<>(Boolean.class, "enable_cheats", "false", "Sind Cheats im Kampfsystem erlaubt?");
	/**
	 * Die ID des NPCs, der die Begruessungs-PMs bei der Registrierung versendet.
	 */
	public static final ConfigValueDescriptor<Integer> REGISTER_PM_SENDER = new WellKnownConfigValue<>(Integer.class, "register_pm_sender", "-16", "Absender der Begruessungs-PMs");
	/**
	 * Absender der PM bei einer zwangsweisen Allianzaufloesung wegen zu weniger Mitglieder.
	 */
	public static final ConfigValueDescriptor<Integer> ALLIANZAUFLOESUNG_PM_SENDER = new WellKnownConfigValue<>(Integer.class, "allianzaufloesung_pm_sender", "0", "Absender der PM bei einer zwangsweisen Allianzauflösung wegen zu weniger Mitglieder");
    /**
     * Der Rabatt Faktor fuer DI Auftraege
     */
    public static final ConfigValueDescriptor<Double> DI_FAKTOR_RABATT = new WellKnownConfigValue<>(Double.class, "rabattfaktordi", "1", "Der Rabatt den DI bei mehreren Upgrades gleichzeitig gibt.");
    /**
     * Der Zeit Faktor fuer DI Auftraege
     */
    public static final ConfigValueDescriptor<Double> DI_FAKTOR_ZEIT = new WellKnownConfigValue<>(Double.class, "zeitfaktordi", "1", "Der Zeitfaktor bei mehreren DI Ausbauten gleichzeitig.");
	/**
	 * Die Accounts, die administrative PMs (z.B. Loeschantraege) erhalten sollen. Mehrere IDs sind mit einem Komma zu separieren.
	 */
	public static final ConfigValueDescriptor<String> ADMIN_PMS_ACCOUNT = new WellKnownConfigValue<>(String.class, "admin_pms_account", "-2", "Die Accounts, die administrative PMs (z.B. Loeschantraege) erhalten sollen. Mehrere IDs sind mit einem Komma zu separieren.");
	/**
	 * Die URL zu den Smilie-Grafiken.
	 */
	public static final ConfigValueDescriptor<String> SMILIE_PATH = new WellKnownConfigValue<>(String.class, "smilie_path", "http://ds.rnd-it.de/images/smilies", "Die URL zu den Smilie-Grafiken.");
	/**
	 * Das Gebaeudelayout der beim Registrieren automatisch zugewiesenen Basis.
	 */
	public static final ConfigValueDescriptor<String> REGISTER_BASELAYOUT = new WellKnownConfigValue<>(String.class, "register_baselayout", "1,0,2,6,17,0,0,2,0,0,0,0,0,0,0,7,0,0,0,0,7,0,0,0,0,4,0,0,0,3,4,0,0,0,5,4,0,0,0,5", "Das Gebaeudelayout der beim Registrieren automatisch zugewiesenen Basis.");
	/**
	 * Der Cargo der beim Registrieren automatisch zugewiesenen Basis.
	 */
	public static final ConfigValueDescriptor<String> REGISTER_BASECARGO = new WellKnownConfigValue<>(String.class, "register_basecargo", "16|3500|0|0;17|1500|0|0;18|2500|0|0;19|3500|0|0;20|3500|0|0;22|1000|0|0;23|2500|0|0;24|2500|0|0;25|500|0|0", "Der Cargo der beim Registrieren automatisch zugewiesenen Basis.");
	/**
	 * Die URL zum Stash-Repository.
	 */
	public static final ConfigValueDescriptor<String> STASH_URL = new WellKnownConfigValue<>(String.class, "stash_url", "http://dev.drifting-souls.net/stash/", "Die URL zum Stash-Repository");
	/**
	 * Der Name des Projekts im Stash-Repository.
	 */
	public static final ConfigValueDescriptor<String> STASH_PROJECT_NAME = new WellKnownConfigValue<>(String.class, "stash_project", "DS", "Der Name des Projekts im Stash-Repository");
	/**
	 * Der Name des DS-Repositories im Stash-Repository.
	 */
	public static final ConfigValueDescriptor<String> STASH_REPO_NAME = new WellKnownConfigValue<>(String.class, "stash_repo", "rds", "Der Name des DS-Repositories im Stash-Repository");
	/**
	 * Die URL zur Bamboo-Installation.
	 */
	public static final ConfigValueDescriptor<String> BAMBOO_URL = new WellKnownConfigValue<>(String.class, "bamboo_url", "http://dev.drifting-souls.net/bamboo/", "Die URL zur Bamboo-Installation.");
    /**
     * Die maximale Anzahl gleichzeitiger dynamischer JumpNodes.
     */
    public static final ConfigValueDescriptor<Integer> MAX_DYN_JN = new WellKnownConfigValue<>(Integer.class, "max_dyn_jn", "0", "Die maximale Anzahl gleichzeitig auftretender dynamischer JumpNodes.");
	/**
	 * Wartung von Schiffen (de)aktivieren
	 */
	public static final ConfigValueDescriptor<Boolean> REQUIRE_SHIP_COSTS = new WellKnownConfigValue<>(Boolean.class, "require_ship_costs", "true", "Schiffs-Wartungskosten");

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
	public static WellKnownConfigValue[] values()
	{
		return VALUES.toArray(new WellKnownConfigValue[VALUES.size()]);
	}
}
