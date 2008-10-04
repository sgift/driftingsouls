/**
 * 
 */
package net.driftingsouls.ds2.server.tick.regular;

import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.User;
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
	
	@SuppressWarnings("unchecked")
	private List<User> getActiveUserList()
	{
		return db.createQuery("from User where vaccount=0 or wait4vac>0").list();
	}

	@Override
	protected void tick()
	{
		final double foodPoolDegeneration = getGlobalFoodPoolDegeneration();
		
		List<User> users = getActiveUserList();
		for(User user: users)
		{
			Cargo usercargo = new Cargo( Cargo.Type.STRING, user.getCargo());
			
			//Rot food
			double rottenFoodPercentage = foodPoolDegeneration + user.getFoodpooldegeneration();
			long food = usercargo.getResourceCount(Resources.NAHRUNG);
			long rottenFood = (long)(food*(rottenFoodPercentage/100.0));
			
			log(user.getId()+": "+rottenFood);
			
			usercargo.setResource(Resources.NAHRUNG, food - rottenFood);
			
			user.setCargo(usercargo.save());
		}
	}

	private double getGlobalFoodPoolDegeneration()
	{
		ConfigValue foodpooldegenerationConfig = (ConfigValue)db.get(ConfigValue.class, "foodpooldegeneration");
		return Double.valueOf(foodpooldegenerationConfig.getValue());
	}
}
