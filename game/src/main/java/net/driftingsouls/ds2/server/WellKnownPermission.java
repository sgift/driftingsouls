package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.PermissionDescriptor;

/**
 * Allgemeine Berechtigungen fuer Gameplay-Elemente von DS.
 */
public enum WellKnownPermission implements PermissionDescriptor
{
	COMM_ADMIN_PM("comm", "adminPM"),
	COMM_OFFIZIELLE_PM("comm", "offiziellePM"),
	COMNET_ALLES_LESBAR("comnet", "allesLesbar"),
	COMNET_ALLES_SCHREIBBAR("comnet", "allesSchreibbar"),
	FRAKTIONEN_BIETERNAME("fraktionen", "bietername"),
	FRAKTIONEN_ANBIETERNAME("fraktionen", "anbietername"),
	FORSCHUNG_ALLES_SICHTBAR("forschung", "allesSichtbar"),
	HANDEL_ANGEBOTE_LOESCHEN("handel", "angeboteLoeschen"),
	ITEM_MODULESETMETA_SICHTBAR("item", "modulSetMetaSichtbar"),
	ITEM_UNBEKANNTE_SICHTBAR("item", "unbekannteSichtbar"),
	SCHIFF_SCRIPT("schiff", "script"),
	SCHIFF_STATUSFELD("schiff", "statusFeld"),
	SCHIFFSTYP_NPCKOSTEN_SICHTBAR("schiffstyp", "npckostenSichtbar"),
	SCHIFFSTYP_VERSTECKTE_SICHTBAR("schiffstyp", "versteckteSichtbar"),
	SCHLACHT_ALLE_AUFRUFBAR("schlacht", "alleAufrufbar"),
	SCHLACHT_LISTE("schlacht", "liste"),
	STATISTIK_ERWEITERTE_SPIELERLISTE("statistik", "erweiterteSpielerliste"),
	UNIT_VERSTECKTE_SICHTBAR("unit", "versteckteSichtbar"),
	USER_VERSTECKTE_SICHTBAR("user", "versteckteSichtbar");

	private final String category;
	private final String action;

	WellKnownPermission(String category, String action)
	{
		this.category = category;
		this.action = action;
	}

	@Override
	public String getCategory()
	{
		return this.category;
	}

	@Override
	public String getAction()
	{
		return this.action;
	}

}
