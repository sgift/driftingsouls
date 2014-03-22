package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class StarSystemContentGenerator
{
	private static final Logger LOG = LogManager.getLogger(StarSystemContentGenerator.class);

	public void fuelleSystem(StarSystem sys)
	{
		LOG.info("Fuelle System " + sys.getName() + " (" + sys.getID() + ")");

		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<BaseType> baseTypes = Common.cast(db.createCriteria(BaseType.class).list());
		TileCache.forSystem(sys).resetCache();
		fuelleSystemMitAsteroiden(sys, baseTypes.stream().filter((bt) -> bt.getSize() == 0).collect(Collectors.toList()), (size) -> (int) Math.pow(size, 0.6));
		fuelleSystemMitAsteroiden(sys, baseTypes.stream().filter((bt) -> bt.getSize() > 0).collect(Collectors.toList()), (size) -> (int) Math.pow(size, 0.2));
		fuelleSystemMitNebeln(sys);
		fuelleSystemMitFelsbrockenKonfigurationen(sys);
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

	private void fuelleSystemMitFelsbrockenKonfigurationen(StarSystem sys)
	{
		if( Math.random() < 0.5 )
		{
			return;
		}
		Session db = ContextMap.getContext().getDB();
		ShipType felsbrockenType = (ShipType)db.createQuery("from ShipType where shipClass=:cls")
				.setParameter("cls", ShipClasses.FELSBROCKEN)
				.setMaxResults(1)
				.uniqueResult();
		if( felsbrockenType == null )
		{
			LOG.warn("Es existiert kein Schiffstyp der Klasse "+ShipClasses.FELSBROCKEN.getSingular());
			return;
		}

		ConfigFelsbrockenSystem cfs = new ConfigFelsbrockenSystem(sys, (int)Math.pow(sys.getWidth()*sys.getHeight(),0.4));

		for( int i=0; i < 3; i++ )
		{
			Cargo cargo = new Cargo();
			for (ResourceID resId : Arrays.asList(Resources.ADAMATIUM, Resources.PLATIN, Resources.ERZ, Resources.SILIZIUM, Resources.SHIVARTE, Resources.TITAN, Resources.URAN))
			{
				int anzahl = (int)(Math.random()*40)-20;
				if( anzahl < 0 )
				{
					continue;
				}
				cargo.addResource(resId, anzahl);
			}
			if( cargo.isEmpty() )
			{
				continue;
			}

			cfs.getFelsbrocken().add(new ConfigFelsbrocken(cfs, felsbrockenType, (int) (Math.random() * 100), cargo));
		}
		db.persist(cfs);
	}

}
