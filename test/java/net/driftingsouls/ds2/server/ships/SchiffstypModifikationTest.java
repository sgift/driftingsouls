package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.entities.Weapon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

public class SchiffstypModifikationTest
{
	@Test
	public void gegebenEinSchiffOhneWaffenUndEinModulMitWaffen_applyTo_sollteDemSchiffDieseWaffenHinzufuegen()
	{
		// setup
		SchiffstypModifikation mod = new SchiffstypModifikation();
		Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
		waffen.add(new Schiffswaffenkonfiguration(new Weapon("Test"), 2, 4, 0));
		mod.setWaffen(waffen);

		ShipType type = new ShipType();

		// run
		ShipTypeData modded = mod.applyTo(type, new String[0]);

		// assert
		assertEquals(1, modded.getWeapons().size());
		assertEquals(1, modded.getMaxHeat().size());
		assertEquals(Integer.valueOf(2), modded.getWeapons().get("Test"));
		assertEquals(Integer.valueOf(4), modded.getMaxHeat().get("Test"));
	}

	@Test
	public void gegebenEinSchiffMitWaffenUndEinModulMitWaffen_applyTo_sollteDemSchiffDieseWaffenHinzufuegen()
	{
		// setup
		SchiffstypModifikation mod = new SchiffstypModifikation();
		Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
		waffen.add(new Schiffswaffenkonfiguration(new Weapon("Test"), 2, 4, 0));
		mod.setWaffen(waffen);

		ShipType type = new ShipType();
		Map<String,Integer> typeWeapons = new HashMap<>();
		typeWeapons.put("Test2", 3);
		type.setWeapons(typeWeapons);

		Map<String,Integer> typeWeaponHeat = new HashMap<>();
		typeWeaponHeat.put("Test2", 6);
		type.setMaxHeat(typeWeaponHeat);

		// run
		ShipTypeData modded = mod.applyTo(type, new String[0]);

		// assert
		assertEquals(2, modded.getWeapons().size());
		assertEquals(2, modded.getMaxHeat().size());
		assertEquals(Integer.valueOf(2), modded.getWeapons().get("Test"));
		assertEquals(Integer.valueOf(3), modded.getWeapons().get("Test2"));
		assertEquals(Integer.valueOf(4), modded.getMaxHeat().get("Test"));
		assertEquals(Integer.valueOf(6), modded.getMaxHeat().get("Test2"));
	}

	@Test
	public void gegebenEinSchiffMitWaffenUndEinModulMitGleichenWaffen_applyTo_sollteDieWaffenDesSchiffsModifizieren()
	{
		// setup
		SchiffstypModifikation mod = new SchiffstypModifikation();
		Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
		waffen.add(new Schiffswaffenkonfiguration(new Weapon("Test"), 2, 4, 0));
		mod.setWaffen(waffen);

		ShipType type = new ShipType();
		Map<String,Integer> typeWeapons = new HashMap<>();
		typeWeapons.put("Test", 3);
		type.setWeapons(typeWeapons);

		Map<String,Integer> typeWeaponHeat = new HashMap<>();
		typeWeaponHeat.put("Test", 6);
		type.setMaxHeat(typeWeaponHeat);

		// run
		ShipTypeData modded = mod.applyTo(type, new String[0]);

		// assert
		assertEquals(1, modded.getWeapons().size());
		assertEquals(1, modded.getMaxHeat().size());
		assertEquals(Integer.valueOf(5), modded.getWeapons().get("Test"));
		assertEquals(Integer.valueOf(10), modded.getMaxHeat().get("Test"));
	}

	@Test
	public void gegebenEinSchiffMitWaffenUndEinModulMitZuEntfernendenWaffen_applyTo_sollteDieWaffenDesSchiffsModifizieren()
	{
		// setup
		SchiffstypModifikation mod = new SchiffstypModifikation();
		Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
		waffen.add(new Schiffswaffenkonfiguration(new Weapon("Test"), -3, 0, 0));
		mod.setWaffen(waffen);

		ShipType type = new ShipType();
		Map<String,Integer> typeWeapons = new HashMap<>();
		typeWeapons.put("Test", 3);
		type.setWeapons(typeWeapons);

		Map<String,Integer> typeWeaponHeat = new HashMap<>();
		typeWeaponHeat.put("Test", 6);
		type.setMaxHeat(typeWeaponHeat);

		// run
		ShipTypeData modded = mod.applyTo(type, new String[0]);

		// assert
		assertEquals(0, modded.getWeapons().size());
		assertEquals(0, modded.getMaxHeat().size());
	}

	@Test
	public void gegebenEinSchiffMitWaffenUndEinModulMitAnderenWaffenUndEinSlotDerWaffenErsetzt_applyTo_sollteDieWaffenDesSchiffsErsetzen()
	{
		// setup
		SchiffstypModifikation mod = new SchiffstypModifikation();
		Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
		waffen.add(new Schiffswaffenkonfiguration(new Weapon("Test"), 3, 6, 0));
		mod.setWaffen(waffen);

		ShipType type = new ShipType();
		Map<String,Integer> typeWeapons = new HashMap<>();
		typeWeapons.put("Test2", 3);
		type.setWeapons(typeWeapons);

		Map<String,Integer> typeWeaponHeat = new HashMap<>();
		typeWeaponHeat.put("Test2", 6);
		type.setMaxHeat(typeWeaponHeat);

		// run
		ShipTypeData modded = mod.applyTo(type, new String[] {"Test2"});

		// assert
		assertEquals(1, modded.getWeapons().size());
		assertEquals(1, modded.getMaxHeat().size());
		assertEquals(Integer.valueOf(3), modded.getWeapons().get("Test"));
		assertEquals(Integer.valueOf(6), modded.getMaxHeat().get("Test"));
	}
}
