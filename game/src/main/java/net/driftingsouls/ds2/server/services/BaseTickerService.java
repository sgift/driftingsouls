package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseStatus;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Faction;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BaseTickerService
{
	private void respawnRess(Base base, int itemid)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		User sourceUser = (User)db.get(User.class, -1);

		base.setSpawnableRessAmount(itemid, 0);

		Base.SpawnableRessMap spawnableress = base.getSpawnableRessMap();
		if(spawnableress == null || spawnableress.isEmpty())
		{
			return;
		}
		Base.SpawnableRess spawnress = spawnableress.newRandomRess();
		int item = spawnress.itemId;
		int maxvalue = ThreadLocalRandom.current().nextInt(1,spawnress.maxValue);

		base.setSpawnableRessAmount(item, maxvalue);

		Item olditem = (Item)db.get(Item.class, itemid);
		Item newitem = (Item)db.get(Item.class, item);
		String message = "Kolonie: " + base.getName() + " (" + base.getId() + ")\n";
		message = message + "Ihre Arbeiter melden: Die Ressource " + olditem.getName() + " wurde aufgebraucht!\n";
		message = message + "Erfreulich ist: Ihre Geologen haben " + newitem.getName() + " gefunden!";

		PM.send(sourceUser, base.getOwner().getId(), "Ressourcen aufgebraucht!", message);
	}

	/**
	 * Ueberprueft alle Gebaeude und schaltet bei nicht vorhandenen Voraussetzungen ab.
	 * @return Gibt eine Meldung mit allen abgeschalteten Gebaeuden zurueck
	 */
	private String proofBuildings(Base base)
	{
		User owner = base.getOwner();
		String msg = "";

		if( (base.getCore() != null) && base.isCoreActive() ) {
			if( base.getCore().isShutDown() && !owner.hasResearched(base.getCore().getTechRequired()) )
			{
				base.setCoreActive(false);
				msg += "Der Core wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n";
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

			Building building = Building.getBuilding(bebauung[o]);

			if( bebon[o] == 0 )
			{
				continue;
			}

			if( building.isShutDown() &&
					(!owner.hasResearched(building.getTechRequired())
							|| (owner.getRace() != building.getRace() && building.getRace() != 0)))
			{
				bebon[o] = 0;
				msg += "Das Geb&auml;ude "+building.getName()+" wurde wegen unzureichenden Voraussetzungen abgeschaltet.\n";
			}
		}

		base.setActive(bebon);

		return msg;
	}

	private String produce(Base base, BaseStatus state, int newenergy)
	{
		String msg = "";
		Cargo baseCargo = (Cargo)base.getCargo().clone();
		Cargo nettoproduction = state.getNettoProduction();
		Cargo nettoconsumption = state.getNettoConsumption();
		org.hibernate.Session db = ContextMap.getContext().getDB();
		boolean ok = true;

		if(state.getArbeiter() > base.getBewohner())
		{
			return "Sie haben mehr Arbeiter als (maximal)Bev&ouml;lkerung. Die Produktion f&auml;llt aus.";
		}

		baseCargo.addResource(Resources.RE, base.getOwner().getKonto().longValue());

		Base.SpawnableRessMap ressMap = base.getSpawnableRessMap();

		for(ResourceEntry entry : nettoproduction.getResourceList())
		{
			// Auf Spawn Resource pruefen und ggf Produktion anpassen
			Item item = (Item)db.get(Item.class,entry.getId().getItemID());
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
					msg += "Ihre Arbeiter konnten keine Vorkommen der Ressource "+item.getName()+" finden.\n";
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
				msg += "Zu wenig "+entry.getPlainName()+" vorhanden. Die Produktion f&auml;llt aus.\n";
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
			msg += "Wegen einer Hungersnot fliehen ihre Einwohner. Die Produktion f&auml;llt aus.\n";
			ok = false;
		}

		// Zuerst sollen die Marines verhungern danach die Bevoelkerung.
		if(!base.feedMarines(baseCargo))
		{
			msg += "Wegen Unterern&auml;hrung desertieren ihre Truppen.\n";
		}

		// Ja, Marines futtern erstmal bevor Sie abhauen ...
		if(!base.payMarines(baseCargo))
		{
			msg += "Wegen fehlendem Sold desertieren ihre Truppen.\n";
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
		return msg;
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

		BaseStatus state = Base.getStatus(base);

		base.immigrate(state);

		int newenergy = base.rebalanceEnergy(state);
		if(newenergy < 0)
		{
			message += "Zu wenig Energie. Die Produktion f&auml;llt aus.\n";
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
				long money = base.automaticSale();
				boolean overfullCargo = base.clearOverfullCargo(state);
				if(money > 0)
				{
					base.getOwner().transferMoneyFrom(Faction.GTU, money, "Automatischer Warenverkauf Asteroid " + base.getName(), false, UserMoneyTransfer.Transfer.AUTO);
				}

				if(money > 0)
				{
					message += "Ihnen wurden " + money + " RE f&uuml;r automatische Verk&auml;ufe gut geschrieben.\n";
					usefullMessage = true;
				}

				if(overfullCargo)
				{
					message += "Wegen uuml;berfuuml;llten Lagerr&auml;umen wurde ein Teil der Produktion vernichtet.\n";
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
}
