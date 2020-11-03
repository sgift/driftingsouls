package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.TestAppConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.lang.NonNullApi;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
public class ConfigServiceTest
{
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private ConfigService configService;

	@Test
	public void gegebenEineLeereDatenbank_get_sollteDenDefaultWertZurueckgebenUndEinenEintragInDerDBAnlegen()
	{
		var transactionTemplate = new TransactionTemplate(transactionManager);
		var value = transactionTemplate.execute(status -> {
			ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy", "1234");

			return configService.get(desc);
		});


		// assert
		assertNotNull(value);
		assertEquals("1234", value.getValue());
		assertEquals("dummy", value.getName());

		ConfigValue dbValue = transactionTemplate.execute(status -> entityManager.find(ConfigValue.class, "dummy"));
 		assertNotNull(dbValue);
		assertEquals("1234", dbValue.getValue());
		assertEquals("dummy", dbValue.getName());
	}

	@Test
	public void gegebenEineDatenbankMitGesetztemConfigWert_get_sollteGenauDiesenWertZurueckgeben()
	{
		var transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult(transactionStatus -> {
			ConfigValue val = new ConfigValue("dummy2", "5678");
			entityManager.persist(val);
		});


		ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy2", "1234");

		// run
		ConfigValue value = configService.get(desc);

		// assert
		assertNotNull(value);
		assertEquals("5678", value.getValue());
		assertEquals("dummy2", value.getName());
	}

	@Test
	public void gegebenEineLeereDatenbank_getValue_sollteDenDefaultWertZurueckgeben()
	{
		// setup
		ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy3", "1234");

		// run
		String value = configService.getValue(desc);

		// assert
		assertEquals("1234", value);

		ConfigValue dbValue = entityManager.find(ConfigValue.class, "dummy3");
		assertNull(dbValue);
	}

	@Test
	public void gegebenEineDatenbankMitGesetztemConfigWert_getValue_sollteGenauDiesenWertZurueckgeben()
	{
		var transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.executeWithoutResult(transactionStatus -> {
			ConfigValue val = new ConfigValue("dummy4", "5678");
			entityManager.persist(val);
		});

		ConfigValueDescriptor<String> desc = new DummyConfigValueDescriptor<>(String.class, "dummy4", "1234");

		// run
		String value = configService.getValue(desc);

		// assert
		assertEquals("5678", value);
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