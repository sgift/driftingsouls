package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.PermissionDescriptor;

/**
 * Admin-Berechtigungen von DS.
 */
public enum WellKnownAdminPermission implements PermissionDescriptor
{
	CONSOLE("AdminConsole"),
	SICHTBAR("sichtbar"),
	STARMAP_VIEW("starmapView"),
	STARMAP_SYSTEMAUSWAHL("starmapSystemauswahl"),
	ADD_GTU("AddGtu"),
	ADD_SHIPS("AddShips"),
	BASES_MAP("BasesMap"),
	BATTLE_END("BattleEnd"),
	CREATE_OBJECTS("CreateObjects"),
	CREATE_OBJECTS_FROM_IMAGE("CreateObjectsFromImage"),
	EDIT_AMMO("EditAmmo"),
	EDIT_BASES("EditBases"),
	EDIT_BASE_TYPE("EditBaseType"),
	EDIT_BUILDING("EditBuilding"),
	EDIT_BUILDING_PICTURE("EditBuildingPicture"),
	EDIT_COMNET_CHANNEL("EditComnetChannel"),
	EDIT_CONFIG_FELSBROCKEN("EditConfigFelsbrocken"),
	EDIT_CONFIG_FELSBROCKEN_SYSTEM("EditConfigFelsbrockenSystem"),
	EDIT_CONFIG_VALUES("EditConfigValues"),
	EDIT_CORE("EditCore"),
    EDIT_DI("EditDI"),
    EDIT_DYN_JN("EditDynJN"),
	EDIT_FACTORY_ENTRY("EditFactoryEntry"),
	EDIT_FRAKTIONS_ANGEBOT("EditFraktionsAngebot"),
	EDIT_FRAKTIONS_GUI_EINTRAG("EditFraktionsGuiEintrag"),
	EDIT_GROUP("EditGroup"),
	EDIT_GUI_HELP_TEXT("EditGuiHelpText"),
	EDIT_INT_TUTORIAL("EditIntTutorial"),
	EDIT_ITEM("EditItem"),
	EDIT_ITEM_PICTURE("EditItemPicture"),
	EDIT_JUMPNODE("EditJumpNode"),
	EDIT_MEDAL("EditMedal"),
	EDIT_MODULE_SLOT("EditModuleSlot"),
	EDIT_NEWS_ENTRY("EditNewsEntry"),
	EDIT_ORDERABLE_SHIPS("EditOrderableShips"),
	EDIT_RANG("EditRang"),
	EDIT_RASSE("EditRasse"),
	EDIT_RESEARCH("EditResearch"),
	EDIT_RESEARCH_PICTURE("EditResearchPicture"),
	EDIT_SCHIFFSTYPMODIFIKATION("EditSchiffstypModifikation"),
	EDIT_SHIP("EditShip"),
	EDIT_SHIP_COSTS("EditShipCosts"),
	EDIT_SHIPTYPES("EditShiptypes"),
	EDIT_SHIPTYPE_PICTRUE("EditShiptypePicture"),
	EDIT_SYSTEM("EditSystem"),
	EDIT_UNIT_PICTURE("EditUnitPicture"),
	EDIT_UNITS("EditUnits"),
	EDIT_USER("EditUser"),
	EDIT_USER_PERMISSIONS("EditUserPermissions"),
	EDIT_WEAPON("EditWeapon"),
	GTU_PRICES("GtuPrices"),
	GTU_VERKAEUFE("GtuVerkaeufe"),
	PLAYER_DELETE("PlayerDelete"),
	PLAYER_DELETE_ACTIVE("PlayerDeleteActive"),
	PLAYER_LOGIN_SUPER("PlayerLoginSuper"),
	PLAYER_STATISTICS("PlayerStatistics"),
	PORTAL_NEWS("PortalNews"),
	QUESTS_STM("QuestsSTM");

	private final String action;

	WellKnownAdminPermission(String action)
	{
		this.action = action;
	}

	@Override
	public String getCategory()
	{
		return "admin";
	}

	@Override
	public String getAction()
	{
		return this.action;
	}

}
