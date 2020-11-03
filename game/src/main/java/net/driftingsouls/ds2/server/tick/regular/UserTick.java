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
import net.driftingsouls.ds2.server.entities.Handel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserMoneyTransfer;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.services.FolderService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Tick fuer Aktionen, die sich auf den gesamten Account beziehen.
 * 
 * @author Sebastian Gift
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserTick extends TickController
{
	@PersistenceContext
	private EntityManager em;

	private final ConfigService configService;
	private final PmService pmService;
	private final FolderService folderService;

	public UserTick(ConfigService configService, PmService pmService, FolderService folderService) {
		this.configService = configService;
		this.pmService = pmService;
		this.folderService = folderService;
	}

	@Override
	protected void prepare()
	{
		//Nothing to do
	}

	@Override
	protected void tick()
	{
		final long deleteThreshould = Common.time() - TimeUnit.DAYS.toSeconds(14);
		log("DeleteThreshould is " + deleteThreshould);
		
		List<Integer> users = em.createQuery("select id from User", Integer.class).getResultList();
		new EvictableUnitOfWork<Integer>("User Tick")
		{

			@Override
			public void doWork(Integer userID) {
				User user = em.find(User.class, userID);
				
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
					Ordner trashCan = folderService.getTrash(user);
					int trashId = trashCan.getId();
					em.createQuery("update PM set gelesen=2, ordner= :trash where empfaenger= :user and ordner= :ordner and time < :time")
						.setParameter("trash", trashId)
						.setParameter("user", user)
						.setParameter("ordner", 0)
						.setParameter("time", deleteThreshould)
						.executeUpdate();
					
					//Subtract costs for trade ads
					long adCount = em.createQuery("select count(*) from Handel where who=:who", Long.class)
									 	   .setParameter("who", user)
									 	   .getSingleResult();
					
					int adCost = configService.getValue(WellKnownConfigValue.AD_COST);
					
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
												
							List<Handel> wasteAds = em.createQuery("from Handel where who=:who order by time asc", Handel.class)
																  .setParameter("who", user)
																  .setMaxResults(wasteAdCount)
																  .getResultList();
							
							log(wasteAdCount + " ads zu loeschen.");
							
							costs = BigInteger.valueOf((adCount - wasteAdCount)*adCost);

							wasteAds.forEach(em::remove);

							pmService.send(user, user.getId(), "Handelsinserate gel&ouml;scht", wasteAdCount + " Ihrer Handelsinserate wurden gel&ouml;scht, weil Sie die Kosten nicht aufbringen konnten.");
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
