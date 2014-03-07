package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.tick.EvictableUnitOfWork;
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
			LOG.info("Fuelle Systeme mit Inhalt");
			Session db = context.getDB();
			List<StarSystem> list = Common.cast(db.createCriteria(StarSystem.class).add(Restrictions.gt("id", 0)).list());

			new EvictableUnitOfWork<StarSystem>("Fuelle Systeme") {
				@Override
				public void doWork(StarSystem sys) throws Exception
				{
					LOG.info("Fuelle System "+sys.getName()+" ("+sys.getID()+")");

					List<BaseType> baseTypes = Common.cast(db.createCriteria(BaseType.class).list());
					TileCache.forSystem(sys).resetCache();
					fuelleSystemMitAsteroiden(sys, baseTypes.stream().filter((bt) -> bt.getSize() == 0).collect(Collectors.toList()), (size) -> (int)Math.pow(size, 0.6));
					fuelleSystemMitAsteroiden(sys, baseTypes.stream().filter((bt) -> bt.getSize() > 0).collect(Collectors.toList()), (size) -> (int) Math.pow(size, 0.2));
					fuelleSystemMitNebeln(sys);
				}
			}.setFlushSize(1).executeFor(list);

			LOG.info("Kolonisiere Asteroiden mit bestehenden Benutzern");

			List<User> users = Common.cast(db.createCriteria(User.class).add(Restrictions.ne("id", 0)).list());
			new EvictableUnitOfWork<User>("Kolonisiere Asteroiden") {
				@Override
				public void doWork(User user) throws Exception
				{
					kolonisiereAsteroidenFuer(user, RandomUtils.nextInt(8)+1);
				}
			}
			.setFlushSize(1).executeFor(users);
		}));
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

	private void fuelleSystemMitNebeln(StarSystem sys)
	{
		Session db = ContextMap.getContext().getDB();
		boolean[][] nebelMap = new boolean[sys.getWidth() + 1][sys.getHeight() + 1];
		int size = sys.getWidth() * sys.getHeight();

		for (int i = 0; i < Math.pow(size,0.7); i++)
		{
			int x = RandomUtils.nextInt(sys.getWidth() + 1);
			int y = RandomUtils.nextInt(sys.getHeight() + 1);
			if (nebelMap[x][y])
			{
				continue;
			}
			nebelMap[x][y] = true;
			Nebel.Typ nebelTyp = Nebel.Typ.values()[RandomUtils.nextInt(Nebel.Typ.values().length)];

			Nebel nebel = new Nebel(new MutableLocation(sys.getID(), x, y), nebelTyp);
			db.persist(nebel);
		}
	}

	private void fuelleSystemMitAsteroiden(StarSystem sys, List<BaseType> baseTypes, Function<Integer,Integer> anzahlBerechnung)
	{
		int size = sys.getWidth() * sys.getHeight();
		for (int i = 0; i < anzahlBerechnung.apply(size); i++)
		{
			int x = RandomUtils.nextInt(sys.getWidth() + 1);
			int y = RandomUtils.nextInt(sys.getHeight() + 1);
			BaseType type = baseTypes.get(RandomUtils.nextInt(baseTypes.size()));

			erzeugeBasisBei(sys, x, y, type);
		}
	}

	private void erzeugeBasisBei(StarSystem sys, int x, int y, BaseType type)
	{
		Session db = ContextMap.getContext().getDB();
		final User nullUser = (User) db.get(User.class, 0);

		Base base = new Base(new Location(sys.getID(), x, y), nullUser);
		base.setKlasse(type.getId());
		base.setWidth(type.getWidth());
		base.setHeight(type.getHeight());
		base.setMaxTiles(type.getMaxTiles());
		base.setMaxCargo(type.getCargo());
		base.setMaxEnergy(type.getEnergy());
		base.setAvailableSpawnableRess(type.getSpawnableRess());
		base.setSize(type.getSize());
		base.setBebauung(new Integer[0]);
		base.setActive(new Integer[0]);
		base.setTerrain(new Integer[0]);
		db.persist(base);
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
