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
package net.driftingsouls.ds2.server.tick;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.CmdLineRequest;
import net.driftingsouls.ds2.server.framework.EmptyPermissionResolver;
import net.driftingsouls.ds2.server.framework.SimpleResponse;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Quartz-Scheduler fuer DS-Ticks. Die Konfiguration erfolgt durch
 * Spring.
 * @author Christopher Jung
 *
 */
public class ScheduledTick extends QuartzJobBean implements StatefulJob
{
	private String tick;

	/**
	 * Injiziert den auszufuehrenden Tick (als Klassennamen).
	 *
	 * @param tick Der Klassenname des auszufuehrenden Ticks
	 */
	public void setTickExecuter(String tick)
	{
		this.tick = tick;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException
	{
		CmdLineRequest request = new CmdLineRequest(new String[0]);
		SimpleResponse response = new SimpleResponse();
		ApplicationContext applicationContext = getApplicationContext(context);
		BasicContext basicContext = new BasicContext(request, response, new EmptyPermissionResolver(), applicationContext);
		try
		{
			if( context.getMergedJobDataMap().containsKey("onlyTick") ) {
				request.setParameter("only", ((Class<?>)context.getMergedJobDataMap().get("onlyTick")).getName());
			}

			Class<? extends AbstractTickExecuter> cls = Class
				.forName(tick)
				.asSubclass(AbstractTickExecuter.class);

			AbstractTickExecuter tick = cls.getDeclaredConstructor().newInstance();
			basicContext.autowireBean(tick);
			tick.addLogTarget(TickController.STDOUT, false);
			tick.execute();
			tick.dispose();
		}
		catch( IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e )
		{
			throw new JobExecutionException(e);
		}
		finally
		{
			basicContext.free();
			HibernateUtil.removeCurrentEntityManager();
		}
	}

	private ApplicationContext getApplicationContext(JobExecutionContext context )
			throws JobExecutionException {
		ApplicationContext appCtx;
		try
		{
			appCtx = (ApplicationContext)context.getScheduler().getContext().get("applicationContext");
			if (appCtx == null) {
				throw new JobExecutionException(
						"Kein ApplicationContext unter dem Key \"applicationContext\" konfiguriert");
			}
			return appCtx;
		}
		catch (SchedulerException e)
		{
			throw new JobExecutionException("Konnte ApplicationContext nicht laden", e);
		}
	}
}
