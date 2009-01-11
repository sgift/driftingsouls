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

import java.io.IOException;

import net.driftingsouls.ds2.server.framework.BasicContext;
import net.driftingsouls.ds2.server.framework.CmdLineRequest;
import net.driftingsouls.ds2.server.framework.SimpleResponse;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.scheduling.quartz.QuartzJobBean;

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
		BasicContext basicContext = new BasicContext(request, response);

		try
		{
			if( context.getMergedJobDataMap().containsKey("onlyTick") ) {
				request.setParameter("only", ((Class<?>)context.getMergedJobDataMap().get("onlyTick")).getName());
			}
			
			Class<? extends AbstractTickExecuter> cls = Class
				.forName(tick)
				.asSubclass(AbstractTickExecuter.class);
			
			AbstractTickExecuter tick = cls.newInstance();
			tick.addLogTarget(TickController.STDOUT, false);
			tick.execute();
			tick.dispose();
		}
		catch( IOException e )
		{
			throw new JobExecutionException(e);
		}
		catch( ClassNotFoundException e )
		{
			throw new JobExecutionException(e);
		}
		catch( InstantiationException e )
		{
			throw new JobExecutionException(e);
		}
		catch( IllegalAccessException e )
		{
			throw new JobExecutionException(e);
		}
		finally
		{
			basicContext.free();
		}
	}

}
