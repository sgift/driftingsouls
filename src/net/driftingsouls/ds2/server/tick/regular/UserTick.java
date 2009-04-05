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

import java.math.BigInteger;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Handel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.tick.TickController;

import org.hibernate.Session;

/**
 * Tick fuer Aktionen, die sich auf den gesamten Account beziehen.
 * 
 * @author Sebastian Gift
 */
public class UserTick extends TickController
{
	private Session db;
	
	@Override
	protected void prepare()
	{
		this.db = getDB();
	}
	
	private List<User> getActiveUserList()
	{
		return Common.cast(db.createQuery("from User where vaccount=0 or wait4vac>0").list());
	}

	@Override
	protected void tick()
	{
		final double foodPoolDegeneration = getGlobalFoodPoolDegeneration();
		
		
		long deleteThreshould = Common.time() - 60*60*24*14;
		log("DeleteThreshould is " + deleteThreshould);
		List<User> users = getActiveUserList();
		for(User user: users)
		{
			try {
				Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo());
				
				//Rot food
				double rottenFoodPercentage = foodPoolDegeneration + user.getFoodpooldegeneration();
				long food = usercargo.getResourceCount(Resources.NAHRUNG);
				long rottenFood = (long)(food*(rottenFoodPercentage/100.0));
				
				log(user.getId()+": "+rottenFood);
				
				usercargo.setResource(Resources.NAHRUNG, food - rottenFood);
				
				user.setCargo(usercargo.save());
				
				
				//Set vacation points
				if(user.isInVacation())
				{
					ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "vacpointspervactick");
					int costsPerTick = Integer.valueOf(value.getValue());
					user.setVacpoints(user.getVacpoints() - costsPerTick);
				}
				else
				{
					ConfigValue value = (ConfigValue)db.get(ConfigValue.class, "vacpointsperplayedtick");
					int pointsPerTick = Integer.valueOf(value.getValue());
					user.setVacpoints(user.getVacpoints() + pointsPerTick);
				}
				

				//Delete all pms older than 14 days from inbox
				Ordner trashCan = Ordner.getTrash(user);
				if(trashCan != null)
				{
					int trash = trashCan.getId();
					db.createQuery("update PM set gelesen=2, ordner= :trash where empfaenger= :user and ordner= :ordner and time < :time")
					  .setInteger("trash", trash)
					  .setEntity("user", user)
					  .setInteger("ordner", 0)
					  .setLong("time", deleteThreshould)
					  .executeUpdate();
				}
				else
				{
					log("User hat keinen Muelleimer.");
				}
				
				//Subtract costs for trade ads
				long adCount = (Long)db.createQuery("select count(*) from Handel where who=:who")
								 	   .setParameter("who", user)
								 	   .uniqueResult();
				
				ConfigValue adCostValue = (ConfigValue)db.get(ConfigValue.class, "adcost");
				int adCost = Integer.parseInt(adCostValue.getValue());
				
				BigInteger account = user.getKonto();
				
				//Not enough money in account
				BigInteger costs = BigInteger.valueOf(adCost*adCount);
				if(account.compareTo(costs) < 0)
				{
					int wasteAdCount = BigInteger.valueOf(adCount).subtract(account.divide(BigInteger.valueOf(adCount))).intValue();
					
					account = BigInteger.ZERO;
					user.setKonto(account);
					
					List<Handel> wasteAds = Common.cast(db.createQuery("from Handel where who=:who sort by time asc")
														  .setMaxResults(wasteAdCount)
														  .list());
					
					for(Handel wasteAd: wasteAds)
					{
						db.delete(wasteAd);
					}
					
					PM.send(user, user.getId(), "Handelsinserate gel&oml;scht", wasteAdCount + " Ihrer Handelsinserate wurden gel&oml;scht, weil Sie die Kosten nicht aufbringen konnten.");
				}
				else
				{
					account = account.subtract(costs);
					user.setKonto(account);
				}

				getContext().commit();
			}
			catch( Exception e )
			{
				getContext().rollback();
				
				this.log("User Tick - User #"+user.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "UserTick - User #"+user.getId()+" Exception", "");
			}
			finally
			{
				db.evict(user);
			}
		}
	}

	private double getGlobalFoodPoolDegeneration()
	{
		ConfigValue foodpooldegenerationConfig = (ConfigValue)db.get(ConfigValue.class, "foodpooldegeneration");
		return Double.valueOf(foodpooldegenerationConfig.getValue());
	}
}
