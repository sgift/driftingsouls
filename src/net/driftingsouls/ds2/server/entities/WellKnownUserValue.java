package net.driftingsouls.ds2.server.entities;

/**
 * Ein Enum der bekannten Benutzereinstellungen ({@link net.driftingsouls.ds2.server.framework.UserValue}).
 */
public class WellKnownUserValue<T>
{
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_BATTLE_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/battle_pm", "false");
	public static final WellKnownUserValue<String> TBLORDER_BASEN_ORDER = new WellKnownUserValue<>(String.class, "TBLORDER/basen/order", "name");
	public static final WellKnownUserValue<Integer> TBLORDER_BASEN_ORDER_MODE = new WellKnownUserValue<>(Integer.class, "TBLORDER/basen/order_mode", "0");
	public static final WellKnownUserValue<Integer> TBLORDER_BASEN_SHOWCARGO = new WellKnownUserValue<>(Integer.class, "TBLORDER/basen/showcargo", "1");
	public static final WellKnownUserValue<Integer> GAMEPLAY_BASES_MAXTILES = new WellKnownUserValue<>(Integer.class, "GAMEPLAY/bases/maxtiles", "500");
	public static final WellKnownUserValue<String> PMS_SIGNATURE = new WellKnownUserValue<>(String.class, "PMS/signature", "");
	public static final WellKnownUserValue<Integer> TBLORDER_FACTIONS_KONTO_MAXTYPE = new WellKnownUserValue<>(Integer.class, "TBLORDER/factions/konto_maxtype", "2");
	public static final WellKnownUserValue<String> TBLORDER_MAIN_NOTIZEN = new WellKnownUserValue<>(String.class, "TBLORDER/main/notizen", "");
	public static final WellKnownUserValue<Integer> TBLORDER_SCHIFF_WRAPFACTOR = new WellKnownUserValue<>(Integer.class, "TBLORDER/schiff/wrapfactor", "1");
	public static final WellKnownUserValue<Integer> TBLORDER_UEBERSICHT_INTTUTORIAL = new WellKnownUserValue<>(Integer.class, "TBLORDER/uebersicht/inttutorial", "5");
	public static final WellKnownUserValue<Integer> TBLORDER_SCHIFF_TOOLTIPS = new WellKnownUserValue<>(Integer.class, "TBLORDER/schiff/tooltips", "1");
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_RESEARCH_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/research_pm", "true");
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_SHIP_BUILD_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/ship_build_pm", "true");
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_BASE_DOWN_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/base_down_pm", "true");
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_OFFICER_BUILD_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/officer_build_pm", "true");
	public static final WellKnownUserValue<Boolean> GAMEPLAY_USER_UNIT_BUILD_PM = new WellKnownUserValue<>(Boolean.class, "GAMEPLAY/user/unit_build_pm", "true");
	public static final WellKnownUserValue<String> TBLORDER_SCHIFFE_ORDER = new WellKnownUserValue<>(String.class, "TBLORDER/schiffe/order", "id");
	public static final WellKnownUserValue<Integer> TBLORDER_SCHIFFE_SHOWJAEGER = new WellKnownUserValue<>(Integer.class, "TBLORDER/schiffe/showjaeger", "0");
	public static final WellKnownUserValue<Boolean> TBLORDER_SCHIFFE_SHOWHANDELSPOSTEN = new WellKnownUserValue<>(Boolean.class, "TBLORDER/schiffe/showhandelsposten", "false");
	public static final WellKnownUserValue<String> TBLORDER_UEBERSICHT_BOX = new WellKnownUserValue<>(String.class, "TBLORDER/uebersicht/box", "bookmarks");
	public static final WellKnownUserValue<String> TBLORDER_KS_ATTACKMODE = new WellKnownUserValue<>(String.class, "TBLORDER/ks/attackmode", "");
	public static final WellKnownUserValue<String> TBLORDER_SCHIFF_SENSORORDER = new WellKnownUserValue<>(String.class, "TBLORDER/schiff/sensororder", "id");
	public static final WellKnownUserValue<Integer> GTU_AUCTION_USER_COST = new WellKnownUserValue<>(Integer.class, "GTU_AUCTION_USER_COST", "10");
	public static final WellKnownUserValue<Integer> TBLORDER_PMS_FORWARD = new WellKnownUserValue<>(Integer.class, "TBLORDER/pms/forward", "0");
	public static final WellKnownUserValue<String> TBLORDER_SCHIFFE_MODE = new WellKnownUserValue<>(String.class, "TBLORDER/schiffe/mode", "carg");

	private Class<T> type;
	private final String name;
	private final String defaultValue;

	private WellKnownUserValue(Class<T> type, String name, String defaultValue)
	{
		this.type = type;
		this.name = name;
		this.defaultValue = defaultValue;
	}

	/**
	 * Gibt den internen Namen der Einstellung zurueck.
	 * @return Der interne Name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt den Typ der Einstellung zurueck.
	 * @return Der Typ
	 */
	public Class<T> getType()
	{
		return type;
	}

	/**
	 * Gibt den Standardwert der Einstellung zurueck.
	 * @return Der Standardwert
	 */
	public String getDefaultValue()
	{
		return defaultValue;
	}
}
