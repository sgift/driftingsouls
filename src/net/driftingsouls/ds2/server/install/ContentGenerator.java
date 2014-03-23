package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.CmdLineRequest;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DriftingSouls;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.SimpleResponse;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.criterion.Restrictions;
import org.quartz.impl.StdScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContentGenerator
{
	private static final Logger LOG = LogManager.getLogger(ContentGenerator.class);

	public void generiereContent()
	{
		try
		{
			new DriftingSouls("web/WEB-INF/cfg/", true);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Konnte DS nicht starten", e);
		}
		mitHibernateSession((session) -> mitContext((context) -> {
			Session db = context.getDB();

			mitTransaktion("Setze Konfigurationseinstellungen", () -> {
				new ConfigService().get(WellKnownConfigValue.ENABLE_CHEATS).setValue("true");
			});

			mitTransaktion("Erzeuge ComNet-Kanaele", this::erzeugeComNetKanaele);

			mitTransaktion("Erzeuge NPC-Bestelldaten", () -> {
				db.persist(new OrderableOffizier("Ingenieur", 1, 0, 25, 20, 10, 5, 5));
				db.persist(new OrderableOffizier("Navigator", 1, 0, 5, 10, 30, 5, 10));
				db.persist(new OrderableOffizier("Sicherheitsexperte", 1, 0, 10, 25, 5, 35, 5));
				db.persist(new OrderableOffizier("Captain", 1, 0, 10, 10, 15, 5, 35));

				Rasse rasse = (Rasse) db.get(Rasse.class, 0);
				List<ShipType> shipTypes = Common.cast(db.createQuery("from ShipType order by rand()")
						.setMaxResults(10)
						.list());
				for (ShipType shipType : shipTypes)
				{
					db.persist(new OrderableShip(shipType, rasse, (int) (10 * Math.random())));
				}
			});

			mitTransaktion("Fuelle Systeme mit Inhalt",
					() -> Common.cast(db.createCriteria(StarSystem.class).add(Restrictions.gt("id", 0)).list()),
					(StarSystem sys) -> new StarSystemContentGenerator().fuelleSystem(sys)
			);

			mitTransaktion("Kolonisiere Asteroiden mit bestehenden Benutzern",
					() -> Common.cast(db.createCriteria(User.class).add(Restrictions.ne("id", 0)).list()),
					(User user) -> kolonisiereAsteroidenFuer(user, RandomUtils.nextInt(8) + 1));

			mitTransaktion("Erzeuge Newseintrag", () -> {
				NewsEntry news = new NewsEntry(
						"Willkommen bei DS2",
						"Installationsprogramm",
						Common.time(),
						"Willkommen bei DS2",
						"Willkommen bei DS2\r\nDu hasst den Demodatensatz erfolgreich installiert.\r\nViel Spass beim Testen und Patchen!");

				db.persist(news);
			});
		}));
	}

	private void erzeugeComNetKanaele()
	{
		Session db = ContextMap.getContext().getDB();

		ComNetChannel standard = new ComNetChannel("Standard");
		standard.setReadAll(true);
		standard.setWriteAll(true);
		db.persist(standard);

		ComNetChannel notfrequenz = new ComNetChannel("Notfrequenz");
		notfrequenz.setReadAll(true);
		notfrequenz.setWriteAll(true);
		db.persist(notfrequenz);

		ComNetChannel gnn = new ComNetChannel("GNN News Network");
		gnn.setReadAll(true);
		gnn.setWriteNpc(true);
		db.persist(gnn);

		ComNetChannel nonRpg = new ComNetChannel("Non-RPG");
		nonRpg.setReadAll(true);
		nonRpg.setWriteNpc(true);
		db.persist(nonRpg);
	}

	private <T> void mitTransaktion(final String name, final Supplier<? extends Collection<T>> jobDataGenerator, final Consumer<T> job)
	{
		LOG.info(name);
		new EvictableUnitOfWork<T>(name) {
			@Override
			public void doWork(T object) throws Exception
			{
				job.accept(object);
			}
		}
		.setFlushSize(1).setErrorReporter((uow, fo, e) -> LOG.error("Fehler bei "+uow.getName()+": Objekte "+fo+" fehlgeschlagen", e)).executeFor(jobDataGenerator.get());
	}

	private void mitTransaktion(final String name, final Runnable job)
	{
		LOG.info(name);
		new SingleUnitOfWork(name) {
			@Override
			public void doWork() throws Exception
			{
				job.run();
			}
		}.setErrorReporter((uow, fo, e) -> LOG.error("Fehler bei "+uow.getName(), e)).execute();
	}

	private void kolonisiereAsteroidenFuer(User user, int anzahl)
	{
		Session db = ContextMap.getContext().getDB();
		List<Base> basen = Common.cast(db.createQuery("from Base where owner.id=0 order by rand()")
				.setMaxResults(anzahl)
				.list());
		basen.forEach((b) -> {
			Integer[] bebauung = b.getBebauung();
			Integer[] active = b.getActive();
			bebauung[0] = 1;
			active[0] = 1;
			b.setBebauung(bebauung);
			b.setActive(active);

			b.setOwner(user);
			BaseStatus status = Base.getStatus(b);
			b.setEnergy(status.getEnergy());
			b.setBewohner(status.getLivingSpace());

			Cargo cargo = status.getProduction();
			for (ResourceEntry resourceEntry : cargo.getResourceList())
			{
				cargo.addResource(resourceEntry.getId(), Math.abs(resourceEntry.getCount1()*10000));
			}

			b.setCargo(cargo);
		});
	}

	private void mitContext(Consumer<Context> contextConsumer)
	{
		try
		{
			ApplicationContext springContext = new FileSystemXmlApplicationContext("web/WEB-INF/cfg/spring.xml");

			// Ticks provisorisch deaktivieren
			StdScheduler quartzSchedulerFactory = (StdScheduler) springContext.getBean("quartzSchedulerFactory");
			quartzSchedulerFactory.shutdown();

			BasicContext context = new BasicContext(new CmdLineRequest(new String[0]), new SimpleResponse(), new EmptyPermissionResolver(), springContext);
			try
			{
				ContextMap.addContext(context);

				contextConsumer.accept(context);
			}
			finally
			{
				context.free();
			}
		}
		catch (Exception e)
		{
			LOG.error("Konnte Content nicht generieren", e);
		}
	}

	private void mitHibernateSession(Consumer<Session> handler)
	{
		SessionFactory sf = HibernateUtil.getSessionFactory();
		Session session = sf.openSession();
		try
		{
			ManagedSessionContext.bind(session);

			// Call the next filter (continue request processing)
			handler.accept(session);

			// Commit and cleanup
			LOG.debug("Committing the database transaction");

			ManagedSessionContext.unbind(sf);

		}
		catch (RuntimeException e)
		{
			LOG.error("", e);
		}
		finally
		{
			session.close();
		}
	}
}
