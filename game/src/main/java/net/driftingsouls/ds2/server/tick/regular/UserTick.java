/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Handel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.util.List;

/**
 * Tick fuer Aktionen, die sich auf den gesamten Account beziehen.
 *
 * @author Sebastian Gift
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserTick extends TickController
{
	private EntityManager em;

	@Override
	protected void prepare()
	{
		this.em = getEM();
	}

	@Override
	protected void tick()
	{
		final long deleteThreshould = Common.time() - 60*60*24*14;
		log("DeleteThreshould is " + deleteThreshould);

		List<Integer> users = em.createQuery("select id from User", Integer.class).getResultList();
		new EvictableUnitOfWork<Integer>("User Tick")
		{

			@Override
			public void doWork(Integer userID) {
				EntityManager em = getEM();

				User user = em.find(User.class, userID);
				//Im Kampagnentick brauchen die Inserate etc nicht bearbeitet zu werden
				//wir muesse nur den TickReady-Status zuruecksetzen
				if(isCampaignTick()){
					user.setTickReady(false);
					return;
				}
				if(user.isInVacation())
				{
					//Set vacation points
					int costsPerTick = new ConfigService().getValue(WellKnownConfigValue.VAC_POINTS_PER_VAC_TICK);
					user.setVacpoints(user.getVacpoints() - costsPerTick);
				}
				else
				{
					int pointsPerTick = new ConfigService().getValue(WellKnownConfigValue.VAC_POINTS_PER_PLAYED_TICK);
					user.setVacpoints(user.getVacpoints() + pointsPerTick);

					//Delete all pms older than 14 days from inbox
					Ordner trashCan = Ordner.getTrash(user);
					if(trashCan != null)
					{
						int trash = trashCan.getId();
						em.createQuery("update PM set gelesen=2, ordner= :trash where empfaenger= :user and ordner= :ordner and time < :time")
						  .setParameter("trash", trash)
						  .setParameter("user", user)
						  .setParameter("ordner", 0)
						  .setParameter("time", deleteThreshould)
						  .executeUpdate();
					}
					else
					{
						log("User hat keinen Muelleimer.");
					}

					//Subtract costs for trade ads
					TypedQuery<Long> query = em.createQuery("select count(*) from Handel where who=:who", Long.class);
					query.setParameter("who", user);
					long adCount = query.getSingleResult();

					int adCost = new ConfigService().getValue(WellKnownConfigValue.AD_COST);

					BigInteger account = user.getKonto();

					log("Ads: " + adCount);
					log("Costs: " + adCost);

					if(adCount > 0)
					{
						//Not enough money in account
						BigInteger costs = BigInteger.valueOf(adCost*adCount);
						if(account.compareTo(costs) < 0)
						{
							log("Spieler kann nicht alle Rechnungen zahlen, loesche Handelsinserate.");
							BigInteger adCountBI = BigInteger.valueOf(adCount);
							BigInteger adCostBI = BigInteger.valueOf(adCost);
							int wasteAdCount = adCountBI.subtract(account.divide(adCostBI)).intValue();

							TypedQuery<Handel> wasteQuery = em.createQuery("from Handel where who=:who order by time asc", Handel.class);
							wasteQuery.setParameter("who", user);
							wasteQuery.setMaxResults(wasteAdCount);
							List<Handel> wasteAds = wasteQuery.getResultList();

							log(wasteAdCount + " ads zu loeschen.");

							costs = BigInteger.valueOf((adCount - wasteAdCount)*adCost);

							for(Handel wasteAd: wasteAds)
							{
								em.remove(wasteAd);
							}

							PM.send(user, user.getId(), "Handelsinserate gel&ouml;scht", wasteAdCount + " Ihrer Handelsinserate wurden gel&ouml;scht, weil Sie die Kosten nicht aufbringen konnten.");
						}

						log("Geld f&uuml;r Handelsinserate " + costs);
						User nobody = em.find(User.class, -1);
						nobody.transferMoneyFrom(user.getId(), costs,
								"Kosten f&uuml;r Handelsinserate - User: " + user.getName() + " (" + user.getId() + ")",
								false, UserMoneyTransfer.Transfer.AUTO);
					}
				}

			}

		}
		.setFlushSize(10)
		.executeFor(users);
	}
}
