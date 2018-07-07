package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.DBTest;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigServiceTest extends DBTest
{
	@Test
	public void gegebenEineLeereDatenbank_get_sollteDenDefaultWertZurueckgebenUndEinenEintragInDerDBAnlegen()
	{
		mitTransaktion(() -> {
			// setup
			ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy", "1234");
			ConfigService configService = new ConfigService();

			// run
			ConfigValue value = configService.get(desc);

			// assert
			assertNotNull(value);
			assertEquals("1234", value.getValue());
			assertEquals("dummy", value.getName());
		});

		getEM().clear();

		ConfigValue dbValue = getEM().find(ConfigValue.class, "dummy");
		assertNotNull(dbValue);
		assertEquals("1234", dbValue.getValue());
		assertEquals("dummy", dbValue.getName());
	}

	@Test
	public void gegebenEineDatenbankMitGesetztemConfigWert_get_sollteGenauDiesenWertZurueckgeben()
	{
		// setup
		mitTransaktion(() -> {
			ConfigValue val = new ConfigValue("dummy", "5678");
			getEM().persist(val);
		});

		ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy", "1234");
		ConfigService configService = new ConfigService();

		mitTransaktion(() -> {
			// run
			ConfigValue value = configService.get(desc);

			// assert
			assertNotNull(value);
			assertEquals("5678", value.getValue());
			assertEquals("dummy", value.getName());
		});
	}

	@Test
	public void gegebenEineLeereDatenbank_getValue_sollteDenDefaultWertZurueckgeben()
	{
		mitTransaktion(() -> {
			// setup
			ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy", "1234");
			ConfigService configService = new ConfigService();

			// run
			String value = configService.getValue(desc);

			// assert
			assertEquals("1234", value);
		});

		getEM().clear();

		ConfigValue dbValue = getEM().find(ConfigValue.class, "dummy");
		assertNull(dbValue);
	}

	@Test
	public void gegebenEineDatenbankMitGesetztemConfigWert_getValue_sollteGenauDiesenWertZurueckgeben()
	{
		// setup
		mitTransaktion(() -> {
			ConfigValue val = new ConfigValue("dummy", "5678");
			getEM().persist(val);
		});

		ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy", "1234");
		ConfigService configService = new ConfigService();

		mitTransaktion(() -> {
			// run
			String value = configService.getValue(desc);

			// assert
			assertEquals("5678", value);
		});
	}

	private static class DummyConfigValueDescriptor<T> implements ConfigValueDescriptor<T> {
		private final Class<T> type;
		private final String name;
		private final String defaultValue;

		private DummyConfigValueDescriptor(Class<T> type, String name, String defaultValue)
		{
			this.type = type;
			this.name = name;
			this.defaultValue = defaultValue;
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public String getDescription()
		{
			return "Dummy-Description";
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
	}
}