package net.driftingsouls.ds2.server.install;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.map.TileCache;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;


public class StarSystemContentGenerator
{
	private static final Logger LOG = LogManager.getLogger(StarSystemContentGenerator.class);

	public void generiereSysteme(int anzahl)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		String[] prefixe = new String[] {"Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Sigma", "Yalon", "Nuevo"};
		String[] namen = new String[] {"Serpentis", "Regulus", "Pegasi", "Chyron", "Boreolus", "Draconis", "Tarh", "Castello", "Centauri", "Cygni", "Aquilae", "Djemm"};
		String[] suffixe = new String[] {"Rift", "Nebulae"};

		for( int i=0; i < anzahl; i++ )
		{
			StarSystem sys = new StarSystem();
			sys.setWidth(50+(int)(Math.random()*75));
			sys.setHeight(50 + (int) (Math.random() * 75));
			sys.setAccess(StarSystem.Access.NORMAL);
			sys.setStarmapVisible(true);
			sys.setOrderLocations("25/25");

			String prefix = prefixe[ThreadLocalRandom.current().nextInt(prefixe.length)];
			String name = namen[ThreadLocalRandom.current().nextInt(namen.length)];
			String suffix = suffixe[ThreadLocalRandom.current().nextInt(suffixe.length)];

			sys.setName((ThreadLocalRandom.current().nextInt(10)>1 ? prefix+" " : "")+name+(ThreadLocalRandom.current().nextInt(10) > 7 ? " "+suffix : ""));

			db.persist(sys);
			sys.setDropZone(new Location(sys.getID(), 25, 25));
		}

        // Standard-DropZone setzen
		ConfigService config = new ConfigService();
		ConfigValue value = config.get(WellKnownConfigValue.GTU_DEFAULT_DROPZONE);

		StarSystem sys = (StarSystem)db.createQuery("from StarSystem where starmap!=:nichtSichtbar")
				.setParameter("nichtSichtbar", StarSystem.Access.NICHT_SICHTBAR)
				.setMaxResults(1)
				.uniqueResult();
		value.setValue(Integer.toString(sys.getID()));
	}

	public void generiereSprungpunkte()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<StarSystem> list = Common.cast(db.createCriteria(StarSystem.class).list());
		list = list.stream().filter((s) -> s.getAccess() != StarSystem.Access.NICHT_SICHTBAR).collect(Collectors.toList());

		// Alle Sternensysteme einmal miteinander verbinden
		StarSystem last = null;
		for (StarSystem starSystem : list)
		{
			if( last != null )
			{
				generiereSprungpunkt(starSystem, last);
			}

			last = starSystem;
		}

		// Noch ein paar zufaellige Verbindungen ergaenzen
		for( int i=0; i < list.size()/2; i++ )
		{
			StarSystem sys1 = list.get(ThreadLocalRandom.current().nextInt(list.size()));
			StarSystem sys2 = list.get(ThreadLocalRandom.current().nextInt(list.size()));
			generiereSprungpunkt(sys1, sys2);
		}
	}

	private void generiereSprungpunkt(StarSystem sys1, StarSystem sys2)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		int x1 = ThreadLocalRandom.current().nextInt(sys1.getWidth() + 1);
		int y1 = ThreadLocalRandom.current().nextInt(sys1.getHeight() + 1);

		int x2 = ThreadLocalRandom.current().nextInt(sys2.getWidth() + 1);
		int y2 = ThreadLocalRandom.current().nextInt(sys2.getHeight() + 1);

		JumpNode node1 = new JumpNode();
		node1.setSystem(sys1.getID());
		node1.setX(x1);
		node1.setY(y1);
		node1.setSystemOut(sys2.getID());
		node1.setXOut(x2);
		node1.setYOut(y2);
		node1.setName(sys2.getName() + " (" + sys2.getID() + ":" + x2 + "/" + y2 + ")");
		db.persist(node1);

		JumpNode node2 = new JumpNode();
		node2.setSystemOut(sys1.getID());
		node2.setXOut(x1);
		node2.setYOut(y1);
		node2.setSystem(sys2.getID());
		node2.setX(x2);
		node2.setY(y2);
		node2.setName(sys1.getName() + " (" + sys1.getID() + ":" + x1 + "/" + y1 + ")");

		db.persist(node2);
	}

	public void fuelleSystem(StarSystem sys)
	{
		if( sys.getAccess() == StarSystem.Access.NICHT_SICHTBAR )
		{
			return;
		}
		
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
			int x = ThreadLocalRandom.current().nextInt(sys.getWidth() + 1);
			int y = ThreadLocalRandom.current().nextInt(sys.getHeight() + 1);
			if (nebelMap[x][y])
			{
				continue;
			}
			nebelMap[x][y] = true;
			Nebel.Typ nebelTyp = Nebel.Typ.values()[ThreadLocalRandom.current().nextInt(Nebel.Typ.values().length)];

			Nebel nebel = new Nebel(new MutableLocation(sys.getID(), x, y), nebelTyp);
			db.persist(nebel);
		}
	}

	private void fuelleSystemMitAsteroiden(StarSystem sys, List<BaseType> baseTypes, Function<Integer,Integer> anzahlBerechnung)
	{
		int size = sys.getWidth() * sys.getHeight();
		for (int i = 0; i < anzahlBerechnung.apply(size); i++)
		{
			int x = ThreadLocalRandom.current().nextInt(1, sys.getWidth());
			int y = ThreadLocalRandom.current().nextInt(1,sys.getHeight());
			BaseType type = baseTypes.get(ThreadLocalRandom.current().nextInt(baseTypes.size()));

			erzeugeBasisBei(sys, x, y, type);
		}
	}

	private void erzeugeBasisBei(StarSystem sys, int x, int y, BaseType type)
	{
		Session db = ContextMap.getContext().getDB();
		final User nullUser = (User) db.get(User.class, 0);

		Base base = new Base(new Location(sys.getID(), x, y), nullUser, type);
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
