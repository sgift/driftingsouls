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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.tick.TickController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.quartz.CronTriggerBean;

/**
 * Implementiert Admin-Kommandos rund um das Ticksystem.
 * @author Christopher Jung
 *
 */
public class TickAdminCommand
{
	private static final Log log = LogFactory.getLog(TickAdminCommand.class);
	
	private CronTriggerBean regularTick;
	private CronTriggerBean rareTick;
	private Scheduler scheduler;
	
	/**
	 * Injiziert den Quartz-Trigger fuer den normalen DS-Tick.
	 * @param regularTick Der Trigger
	 */
	@Required
	public void setRegularTickCronTrigger(CronTriggerBean regularTick) {
		this.regularTick = regularTick;
	}
	
	/**
	 * Injiziert den Quartz-Trigger fuer den seltenen DS-Tick.
	 * @param rareTick Der Trigger
	 */
	@Required
	public void setRareTickCronTrigger(CronTriggerBean rareTick) {
		this.rareTick = rareTick;
	}
	
	/**
	 * Injiziert den Quartz-Scheduler zum Ausfuehren von Quartz-Jobs.
	 * @param bean Der Scheduler
	 */
	@Required
	public void setScheduler(Scheduler bean) {
		this.scheduler = bean;
	}
	
	/**
	 * Fuehrt den normalen DS-Tick aus.
	 */
	public void runRegularTick() {
		try
		{
			log.info("RegularTick wird manuell gestartet");
			scheduler.triggerJob(this.regularTick.getJobName(), this.regularTick.getJobGroup());
		}
		catch( SchedulerException e )
		{
			log.error("Konnte regulartick nicht ausfuehren", e);
		}
	}
	
	/**
	 * Fuehrt den seltenen DS-Tick aus.
	 */
	public void runRareTick() {
		try
		{
			log.info("RareTick wird manuell gestartet");
			scheduler.triggerJob(this.rareTick.getJobName(), this.rareTick.getJobGroup());
		}
		catch( SchedulerException e )
		{
			log.error("Konnte raretick nicht ausfuehren", e);
		}
	}
	
	/**
	 * Fuehrt einen Teil des normalen DS-Tick aus.
	 * @param tickPart der auszufuehrende Teiltick
	 */
	public void runRegularTick(Class<? extends TickController> tickPart) {
		try
		{
			JobDataMap map = new JobDataMap();
			map.put("onlyTick", tickPart);
			
			log.info("RegularTick '"+tickPart+"' wird manuell gestartet");
			scheduler.triggerJob(this.regularTick.getJobName(), this.regularTick.getJobGroup(), map);
		}
		catch( SchedulerException e )
		{
			log.error("Konnte regulartick nicht ausfuehren", e);
		}
	}
	
	/**
	 * Fuehrt einen Teil des seltenen DS-Tick aus.
	 * @param tickPart der auszufuehrende Teiltick
	 */
	public void runRareTick(Class<? extends TickController> tickPart) {
		try
		{
			ContextMap.getContext().autowireBean(tickPart);

			JobDataMap map = new JobDataMap();
			map.put("onlyTick", tickPart);
			
			log.info("RegularTick '"+tickPart+"' wird manuell gestartet");
			scheduler.triggerJob(this.regularTick.getJobName(), this.regularTick.getJobGroup(), map);
		}
		catch( SchedulerException e )
		{
			log.error("Konnte regulartick nicht ausfuehren", e);
		}
	}
}
