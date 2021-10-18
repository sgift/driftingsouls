package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.bases.AutoGTUAction;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuWarenKurse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BaseTickerService
{
	@PersistenceContext
	private EntityManager em;

	private final PmService pmService;
	private final BaseService baseService;
	private final BuildingService buildingService;

	public BaseTickerService(PmService pmService, BaseService baseService, BuildingService buildingService) {
		this.pmService = pmService;
		this.baseService = baseService;
		this.buildingService = buildingService;
	}

	private void respawnRess(Base base, int itemid)
	{
		User sourceUser = em.find(User.class, -1);

		base.setSpawnableRessAmount(itemid, 0);

		Base.SpawnableRessMap spawnableress = getSpawnableRessMap(base);
		if(spawnableress == null || spawnableress.isEmpty())
		{
			return;
		}
		Base.SpawnableRess spawnress = spawnableress.newRandomRess();
		int item = spawnress.itemId;
		int maxvalue = ThreadLocalRandom.current().nextInt(1,spawnress.maxValue);

		base.setSpawnableRessAmount(item, maxvalue);

		Item olditem = Item.getItem(itemid);
		Item newitem = Item.getItem(item);
		String message = "Kolonie: " + base.getName() + " (" + base.getId() + ")\n";
		message = message + "Ihre Arbeiter melden: Die Ressource " + olditem.getName() + " wurde aufgebraucht!\n";
		message = message + "Erfreulich ist: Ihre Geologen haben " + newitem.getName() + " gefunden!";

		pmService.send(sourceUser, base.getOwner().getId(), "Ressourcen aufgebraucht!", message);
	}

	/**
	 * Ueberprueft alle Gebaeude und schaltet bei nicht vorhandenen Voraussetzungen ab.
	 * @return Gibt eine Meldung mit allen abgeschalteten Gebaeuden zurueck
	 */
	private String proofBuildings(Base base)
	{
		User owner = base.getOwner();
		StringBuilder msg = new StringBuilder();

		if( (base.getCore() != null) && base.isCoreActive() ) {
			if( base.getCore().isShutDown() && !owner.hasResearched(base.getCore().getTechRequired()) )
			{
				base.setCoreActive(false);
				msg.append("Der Core wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n");
			}
		}

		Integer[] bebauung = base.getBebauung();
		Integer[] bebon = base.getActive();

		for( int o=0; o < base.getWidth() * base.getHeight(); o++ )
		{
			if( bebauung[o] == 0 )
			{
				continue;
			}

			Building building = buildingService.getBuilding(bebauung[o]);

			if( bebon[o] == 0 )
			{
				continue;
			}

			if( building.isShutDown() &&
					(!owner.hasResearched(building.getTechRequired())
							|| (owner.getRace() != building.getRace() && building.getRace() != 0)))
			{
				bebon[o] = 0;
				msg.append("Das Gebäude ").append(building.getName()).append(" wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n");
			}
		}

		base.setActive(bebon);

		return msg.toString();
	}

	private String produce(Base base, BaseStatus state, int newenergy)
	{
		StringBuilder msg = new StringBuilder();
		Cargo baseCargo = (Cargo)base.getCargo().clone();
		Cargo nettoproduction = state.getNettoProduction();
		Cargo nettoconsumption = state.getNettoConsumption();
		boolean ok = true;

		if(state.getArbeiter() > base.getBewohner())
		{
			return "Sie haben mehr Arbeiter als (Maximal)-Bevölkerung. Die Produktion fällt aus.";
		}

		baseCargo.addResource(Resources.RE, base.getOwner().getKonto().longValue());

		Base.SpawnableRessMap ressMap = getSpawnableRessMap(base);

		for(ResourceEntry entry : nettoproduction.getResourceList())
		{
			// Auf Spawn Resource pruefen und ggf Produktion anpassen
			Item item = Item.getItem(entry.getId().getItemID());
			if(item.isSpawnableRess()) {
				// Genug auf dem Asteroiden vorhanden
				// und abziehen
				if(base.getSpawnableRessAmount(item.getID()) > nettoproduction.getResourceCount(entry.getId())) {
					base.setSpawnableRessAmount(item.getID(), base.getSpawnableRessAmount(item.getID()) - nettoproduction.getResourceCount(entry.getId()));
				}
				// Ueberhaupt nichts auf dem Asteroiden vorhanden
				else if (base.getSpawnableRessAmount(item.getID()) < 0 && !ressMap.containsRess(item) ) {
					// Dann ziehen wir die Production eben ab
					nettoproduction.setResource(entry.getId(), 0);
					msg.append("Ihre Arbeiter konnten keine Vorkommen der Ressource ").append(item.getName()).append(" finden.\n");
				}
				// Es kann nicht mehr die volle Produktion gefoerdert werden
				else {
					// Produktion drosseln und neue Ressource spawnen
					nettoproduction.setResource(entry.getId(), base.getSpawnableRessAmount(item.getID()));
					respawnRess(base, item.getID());
				}
			}
		}
		Cargo fullproduction = (Cargo)nettoproduction.clone();
		fullproduction.substractCargo(nettoconsumption);

		ResourceList resources = baseCargo.compare(fullproduction, true);
		for(ResourceEntry entry: resources)
		{
			long stock = entry.getCount1();
			long production = entry.getCount2();

			long balance = stock + production;

			//Not enough resources for production
			if(balance < 0)
			{
				msg.append("Zu wenig ").append(entry.getPlainName()).append(" vorhanden. Die Produktion fällt aus.\n");
				ok = false;
			}

			if(production > 0)
			{
				baseCargo.addResource(entry.getId(), production);
			}
			else
			{
				production = Math.abs(production);
				baseCargo.substractResource(entry.getId(), production);
			}
		}

		if(!base.feedInhabitants(baseCargo))
		{
			msg.append("Wegen einer Hungersnot fliehen Ihre Einwohner. Die Produktion fällt aus.\n");
			ok = false;
		}

		// Zuerst sollen die Marines verhungern danach die Bevoelkerung.
		if(!base.feedMarines(baseCargo))
		{
			msg.append("Wegen Unterernährung desertieren Ihre Truppen.\n");
		}

		// Ja, Marines futtern erstmal bevor Sie abhauen ...
		if(!base.payMarines(baseCargo))
		{
			msg.append("Wegen fehlenden Solds desertieren Ihre Truppen.\n");
		}

		if(ok)
		{
			// Alles OK ggf müssen wir Konto anpassen und darauf achten das Produktion der Basis auch Bargeld liefert
			long baseRE = base.getCargo().getResourceCount(Resources.RE) + nettoproduction.getResourceCount(Resources.RE);

			long newRE = baseCargo.getResourceCount(Resources.RE);

			if(newRE > baseRE)
			{
				base.getOwner().setKonto(BigInteger.valueOf(newRE - baseRE));
				baseCargo.setResource(Resources.RE, baseRE);
			}
			else
			{
				base.getOwner().setKonto(BigInteger.ZERO);
			}
			base.setEnergy(newenergy);
			base.setCargo(baseCargo);
		}
		return msg.toString();
	}

	/**
	 * Laesst die Basis ticken.
	 *
	 * @return Die Ticknachrichten, wenn es welche gab.
	 */
	public String tick(Base base)
	{
		String message = "Basis [base="+base.getId()+"]" + base.getName() + "[/base]\n----------------------------------\n";
		boolean usefullMessage = false;

		String proof = proofBuildings(base);
		if(!proof.equals(""))
		{
			message += proof;
			usefullMessage = true;
		}

		BaseStatus state = baseService.getStatus(base);

		base.immigrate(state);

		int newenergy = base.rebalanceEnergy(state);
		if(newenergy < 0)
		{
			message += "Zu wenig Energie. Die Produktion fällt aus.\n";
			usefullMessage = true;
		}
		else
		{
			String prodmsg = produce(base, state, newenergy);
			if(!prodmsg.equals(""))
			{
				message += prodmsg;
				usefullMessage = true;
			}
			else
			{
				long money = automaticSale(base);
				boolean overfullCargo = baseService.clearOverfullCargo(base, state);
				if(money > 0)
				{
					base.getOwner().transferMoneyFrom(Faction.GTU, money, "Automatischer Warenverkauf Asteroid " + base.getName(), false, UserMoneyTransfer.Transfer.AUTO);
				}

				if(money > 0)
				{
					message += "Ihnen wurden " + money + " RE für automatische Verkäufe gutgeschrieben.\n";
					usefullMessage = true;
				}

				if(overfullCargo)
				{
					message += "Wegen überfüllten Lagerräumen wurde ein Teil oder die gesamte der Produktion vernichtet.\n";
					usefullMessage = true;
				}
			}
		}

		if(base.getBewohner() > state.getLivingSpace())
		{
			base.setBewohner(state.getLivingSpace());
		}

		if(usefullMessage)
		{
			message += "\n";
		}
		else
		{
			message = "";
		}

		return message;
	}

	/**
	 * Gibt die zum spawn freigegebenen Ressourcen zurueck.
	 * Beruecksichtigt ebenfalls die Systemvorraussetzungen.
	 * @return Die zum Spawn freigegebenen Ressourcen
	 */
	private Base.SpawnableRessMap getSpawnableRessMap(Base base)
	{
		StarSystem system = StarSystem.getSystem(base.getSystem());

		if(system == null) {
			return null;
		}

		if(base.getSpawnableRess() == null && system.getSpawnableRess() == null && base.getKlasse().getSpawnableRess() == null)
		{
			return null;
		}

		Base.SpawnableRessMap spawnMap = new Base.SpawnableRessMap();

		if( base.getSpawnableRess() != null )
		{
			String[] spawnableress = StringUtils.split(base.getSpawnableRess(), ";");
			for (String spawnableres : spawnableress)
			{
				String[] thisress = StringUtils.split(spawnableres, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (base.getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new Base.SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}
		if( system.getSpawnableRess() != null )
		{
			String[] spawnableresssystem = StringUtils.split(system.getSpawnableRess(), ";");
			for (String aSpawnableresssystem : spawnableresssystem)
			{
				String[] thisress = StringUtils.split(aSpawnableresssystem, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (base.getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new Base.SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}
		if( base.getKlasse().getSpawnableRess() != null && !base.getKlasse().getSpawnableRess().isEmpty() )
		{
			String[] spawnableresstype = StringUtils.split(base.getKlasse().getSpawnableRess(), ";");
			for (String aSpawnableresstype : spawnableresstype)
			{
				String[] thisress = StringUtils.split(aSpawnableresstype, ",");
				if( thisress.length != 3 )
				{
					continue;
				}
				int itemid = Integer.parseInt(thisress[0]);
				int chance = Integer.parseInt(thisress[1]);
				int maxvalue = Integer.parseInt(thisress[2]);

				// Er soll nur Ressourcen spawnen die noch nicht vorhanden sind
				if (base.getSpawnableRessAmount(itemid) <= 0)
				{
					spawnMap.addSpawnRess(new Base.SpawnableRess(itemid, chance, maxvalue));
				}
			}
		}

		spawnMap.buildChanceMap();

		return spawnMap;
	}

	/**
	 * Enforces the automatic sale rules of the base.
	 *
	 * @return The money for resource sales.
	 */
	private long automaticSale(Base base)
	{
		long money = 0;
		List<AutoGTUAction> actions = base.getAutoGTUActs();
		if(!actions.isEmpty() )
		{
			for(AutoGTUAction action: actions)
			{

				ResourceID resource = action.getResID();

				long sell;
				switch(action.getActID())
				{
					case AutoGTUAction.SELL_ALL:
						sell = action.getCount();
						if(sell > base.getCargo().getResourceCount(resource))
						{
							sell = base.getCargo().getResourceCount(resource);
						}
						break;
					case AutoGTUAction.SELL_TO_LIMIT:
						long maximum = action.getCount();
						sell = base.getCargo().getResourceCount(resource) - maximum;
						break;
					default:
						sell = 0;
				}

				if(sell > 0)
				{
					base.getCargo().substractResource(resource, sell);
					money += getSalePrice(resource, sell);
				}
			}
		}

		return money;
	}

	/**
	 * Calculates the money using the current gtu price for base sales.
	 * @param resource Die ID der Ressource
	 * @param count Die Anzahl
	 * @return The money for a base sale of the resource.
	 */
	private long getSalePrice(ResourceID resource, long count)
	{
		GtuWarenKurse kurs = em.find(GtuWarenKurse.class, "asti");
		Cargo prices = kurs.getKurse();
		double price = prices.getResourceCount(resource) / 1000d;

		return Math.round(price * count);
	}
}
