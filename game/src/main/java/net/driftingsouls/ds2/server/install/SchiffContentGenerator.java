package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.config.ModuleSlot;
import net.driftingsouls.ds2.server.framework.ContextMap;

public class SchiffContentGenerator
{
	private <T> T persist(T entity)
	{
		ContextMap.getContext().getDB().persist(entity);
		return entity;
	}

	public void erzeugeModulSlots()
	{
		persist(new ModuleSlot("armour", "Panzerung", null));
		persist(new ModuleSlot("boarding_cruiser_upgrade", "Rüstslot Kaperschiff", null));
		ModuleSlot destroyerDeffensive = persist(new ModuleSlot("destroyer_deffensive", "Zerstörer Deffensivwaffen", null));
		ModuleSlot destroyerEngine = persist(new ModuleSlot("destroyer_engine", "Zerstörer Antrieb", null));
		ModuleSlot destroyerHull = persist(new ModuleSlot("destroyer_hull", "Zerstörer Hülle", null));
		ModuleSlot destroyerOffensive = persist(new ModuleSlot("destroyer_offensive", "Zerstörer Offensivwaffen", null));
		ModuleSlot destroyerReactor = persist(new ModuleSlot("destroyer_reactor", "Zerstörer Reaktor", null));
		ModuleSlot destroyerSpecial = persist(new ModuleSlot("destroyer_special", "Zerstörer Spezial", null));
		ModuleSlot destroyerTech = persist(new ModuleSlot("destroyer_tech", "Zerstörer Technik", null));
		persist(new ModuleSlot("drone_cont", "Drohnen Kontrollslot", null));
		persist(new ModuleSlot("jprimary", "Primärwaffe [Jäger]", null));
		persist(new ModuleSlot("jsecondary", "Sekundärwaffe [Jäger]", null));
		ModuleSlot misc = persist(new ModuleSlot("misc", "Diverses", null));
		persist(new ModuleSlot("miscawacs", "Scanner", null));
		persist(new ModuleSlot("misctanker", "Diverses [Tanker]", misc));
		persist(new ModuleSlot("misc_ganystation", "Rüstslot Ganymed-Station", null));
		persist(new ModuleSlot("misc_kottos", "Diverses [Kottos]", misc));
		persist(new ModuleSlot("misc_large", "Diverses [grosse Schiffe]", misc));
		persist(new ModuleSlot("misc_scanner", "Diverses [Scanner]", misc));
		persist(new ModuleSlot("misc_shipyard", "Werftpack", null));
		persist(new ModuleSlot("misc_station", "Rüststlot Station", null));
		ModuleSlot miscWeapon = persist(new ModuleSlot("misc_weapon", "Bewaffnung", null));
		persist(new ModuleSlot("paladin_misc", "Rüstslot Paladin", null));
		persist(new ModuleSlot("spec_cruiser_upgrade", "Rüstslot Kreuzer z.b.V.", null));
		persist(new ModuleSlot("terran_corvette_upgrade", "Rüstslot terranische Korvette", null));
		ModuleSlot terranCruiserUpgrade = persist(new ModuleSlot("terran_cruiser_upgrade", "Rüstslot terranischer Kreuzer", null));
		persist(new ModuleSlot("vasudan_corvette_upgrade", "Rüstslot vasudanische Korvette", null));
		ModuleSlot vasudanCruiserUpgrade = persist(new ModuleSlot("vasudan_cruiser_upgrade", "Rüstslot vasudanischer Kreuzer", null));
		persist(new ModuleSlot("weapon_ganystation", "Bewaffnung Ganymed-Station", null));
		persist(new ModuleSlot("weapon_station", "Bewaffnung Station", null));
		persist(new ModuleSlot("misc_large_weapon", "Bewaffnung", miscWeapon));
		persist(new ModuleSlot("shivan_destroyer_deffensive", "Shivanische Zerstörer Deffensivwaffen", destroyerDeffensive));
		persist(new ModuleSlot("shivan_destroyer_engine", "Shivanische Zerstörer Antrieb", destroyerEngine));
		persist(new ModuleSlot("shivan_destroyer_hull", "Shivanische Zerstörer Hülle", destroyerHull));
		persist(new ModuleSlot("shivan_destroyer_offensive", "Shivanische Zerstörer Offensivwaffen", destroyerOffensive));
		persist(new ModuleSlot("shivan_destroyer_reactor", "Shivanische Zerstörer Reaktor", destroyerReactor));
		persist(new ModuleSlot("shivan_destroyer_special", "Shivanische Zerstörer Spezial", destroyerSpecial));
		persist(new ModuleSlot("shivan_destroyer_tech", "Shivanische Zerstörer Technik", destroyerTech));
		ModuleSlot terranDestroyerDeffensive = persist(new ModuleSlot("terran_destroyer_deffensive", "Terranische Zerstörer Deffensivwaffen", destroyerDeffensive));
		ModuleSlot terranDestroyerEngine = persist(new ModuleSlot("terran_destroyer_engine", "Terranische Zerstörer Antrieb", destroyerEngine));
		ModuleSlot terranDestroyerHull = persist(new ModuleSlot("terran_destroyer_hull", "Terranische Zerstörer Hülle", destroyerHull));
		ModuleSlot terranDestroyerOffensive = persist(new ModuleSlot("terran_destroyer_offensive", "Terranische Zerstörer Offensivwaffen", destroyerOffensive));
		ModuleSlot terranDestroyerReactor = persist(new ModuleSlot("terran_destroyer_reactor", "Terranische Zerstörer Reaktor", destroyerReactor));
		ModuleSlot terranDestroyerSpecial = persist(new ModuleSlot("terran_destroyer_special", "Terranische Zerstörer Spezial", destroyerSpecial));
		ModuleSlot terranDestroyerTech = persist(new ModuleSlot("terran_destroyer_tech", "Terranische Zerstörer Technik", destroyerTech));
		ModuleSlot vasudanDestroyerDeffensive = persist(new ModuleSlot("vasudan_destroyer_deffensive", "Vasudanische Zerstörer Deffensivwaffen", destroyerDeffensive));
		ModuleSlot vasudanDestroyerEngine = persist(new ModuleSlot("vasudan_destroyer_engine", "Vasudanische Zerstörer Antrieb", destroyerEngine));
		ModuleSlot vasudanDestroyerHull = persist(new ModuleSlot("vasudan_destroyer_hull", "Vasudanische Zerstörer Hülle", destroyerHull));
		ModuleSlot vasudanDestroyerOffensive = persist(new ModuleSlot("vasudan_destroyer_offensive", "Vasudanische Zerstörer Offensivwaffen", destroyerOffensive));
		ModuleSlot vasudanDestroyerReactor = persist(new ModuleSlot("vasudan_destroyer_reactor", "Vasudanische Zerstörer Reaktor", destroyerReactor));
		ModuleSlot vasudanDestroyerSpecial = persist(new ModuleSlot("vasudan_destroyer_special", "Vasudanische Zerstörer Spezial", destroyerSpecial));
		ModuleSlot vasudanDestroyerTech = persist(new ModuleSlot("vasudan_destroyer_tech", "Vasudanische Zerstörer Technik", destroyerTech));
		persist(new ModuleSlot("terran_hcruiser_upgrade", "Rüstslot terranischer schwerer Kreuzer", terranCruiserUpgrade));
		persist(new ModuleSlot("orion_terran_destroyer_deffensive", "Orion Deffensivwaffen", terranDestroyerDeffensive));
		persist(new ModuleSlot("orion_terran_destroyer_engine", "Orion Antrieb", terranDestroyerEngine));
		persist(new ModuleSlot("orion_terran_destroyer_hull", "Orion Hülle", terranDestroyerHull));
		persist(new ModuleSlot("orion_terran_destroyer_offensive", "Orion Offensivwaffen", terranDestroyerOffensive));
		persist(new ModuleSlot("orion_terran_destroyer_reactor", "Orion Reaktor", terranDestroyerReactor));
		persist(new ModuleSlot("orion_terran_destroyer_special", "Orion Spezial", terranDestroyerSpecial));
		persist(new ModuleSlot("orion_terran_destroyer_tech", "Orion Technik", terranDestroyerTech));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_deffensive", "Typhoon Deffensivwaffen", vasudanDestroyerDeffensive));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_engine", "Typhoon Antrieb", vasudanDestroyerEngine));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_hull", "Typhoon Hülle", vasudanDestroyerHull));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_offensive", "Typhoon Offensivwaffen", vasudanDestroyerOffensive));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_reactor", "Typhoon Reaktor", vasudanDestroyerReactor));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_special", "Typhoon Spezial", vasudanDestroyerSpecial));
		persist(new ModuleSlot("typhoon_vasudan_destroyer_tech", "Typhoon Technik", vasudanDestroyerTech));
		persist(new ModuleSlot("vasudan_hcruiser_upgrade", "Rüstslot vasudanischer schwerer Kreuzer", vasudanCruiserUpgrade));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_deffensive", "Hatshepsut Deffensivwaffen", vasudanDestroyerDeffensive));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_engine", "Hatshepsut Antrieb", vasudanDestroyerEngine));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_hull", "Hatshepsut Hülle", vasudanDestroyerHull));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_offensive", "Hatshepsut Offensivwaffen", vasudanDestroyerOffensive));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_reactor", "Hatshepsut Reaktor", vasudanDestroyerReactor));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_special", "Hatshepsut Spezial", vasudanDestroyerSpecial));
		persist(new ModuleSlot("hatshepsut_vasudan_destroyer_tech", "Hatshepsut Technik", vasudanDestroyerTech));
		persist(new ModuleSlot("hecate_terran_destroyer_deffensive", "Hecate Deffensivwaffen", terranDestroyerDeffensive));
		persist(new ModuleSlot("hecate_terran_destroyer_engine", "Hecate Antrieb", terranDestroyerEngine));
		persist(new ModuleSlot("hecate_terran_destroyer_hull", "Hecate Hülle", terranDestroyerHull));
		persist(new ModuleSlot("hecate_terran_destroyer_offensive", "Hecate Offensivwaffen", terranDestroyerOffensive));
		persist(new ModuleSlot("hecate_terran_destroyer_reactor", "Hecate Reaktor", terranDestroyerReactor));
		persist(new ModuleSlot("hecate_terran_destroyer_special", "Hecate Spezial", terranDestroyerSpecial));
		persist(new ModuleSlot("hecate_terran_destroyer_tech", "Hecate Technik", terranDestroyerTech));
	}
}
