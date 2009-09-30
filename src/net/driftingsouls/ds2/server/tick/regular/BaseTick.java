/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WeaponFactory;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.hibernate.StaleObjectStateException;

/**
 * <h1>Berechnung des Ticks fuer Basen.</h1>
 * @author Christopher Jung
 *
 */
public class BaseTick extends TickController 
{
	private StringBuilder pmcache;
	private int lastowner;
	
	@Override
	protected void prepare() 
	{
		this.pmcache = new StringBuilder();
		this.lastowner = 0;
	}

	private void tickBases() 
	{
		org.hibernate.Session db = getDB();
		
		User sourceUser = (User)db.get(User.class, -1);
		
		// Da wir als erstes mit dem Usercargo rumspielen -> sichern der alten Nahrungswerte
		List<?> users = db.createQuery("from User where id!=0 and (vaccount=0 or wait4vac!=0)").list();
		for( Iterator<?> iter = users.iterator(); iter.hasNext(); ) {
			User auser = (User)iter.next();
			
			auser.setNahrungsStat(Long.toString(new Cargo(Cargo.Type.STRING, auser.getCargo()).getResourceCount(Resources.NAHRUNG)));
		}
		
		getContext().commit();
		
		// Nun holen wir uns mal die Basen...
		List<?> bases = db.createQuery("from Base b join fetch b.owner where b.owner!=0 and (b.owner.vaccount=0 or b.owner.wait4vac!=0) order by b.owner").list();
			
		log("Kolonien: "+bases.size());
		log("");
		
		String messages = "";
		for( Iterator<?> iter = bases.iterator(); iter.hasNext(); ) {
			Base base = (Base)iter.next();
			
			// Muessen ggf noch alte Userdaten geschrieben und neue geladen werden?
			if( base.getOwner().getId() != this.lastowner ) {
				HibernateFacade.evictAll(db, 
						WerftObject.class, 
						WerftQueueEntry.class, 
						Forschungszentrum.class,
						Academy.class,
						WeaponFactory.class);
				
				log(base.getOwner().getId()+":");
				if( this.pmcache.length() != 0 ) {
					this.pmcache.setLength(0);
				}
				
				try {
					getContext().commit();
				}
				catch( Exception e ) {
					getContext().rollback();
					if( e instanceof StaleObjectStateException ) {
						evictStaleObjects(db, (StaleObjectStateException)e);
					}
					HibernateFacade.evictAll(db, PM.class);
					db.evict(db.get(User.class, this.lastowner));
					
					this.log("Base Tick - User #"+this.lastowner+" failed: "+e);
					e.printStackTrace();
					Common.mailThrowable(e, "BaseTick - User #"+this.lastowner+" failed: "+e, "");
				}
			}
			
			if(this.lastowner != base.getOwner().getId() && !messages.trim().equals(""))
			{
				PM.send(sourceUser, this.lastowner, "Basis-Tick", messages);
				messages = "";
			}
			
			this.lastowner = base.getOwner().getId();
			try 
			{
				messages += base.tick();
				getContext().commit();
			}
			catch( Exception e ) 
			{
				getContext().rollback();
				
				if( e instanceof StaleObjectStateException ) 
				{
					evictStaleObjects(db, (StaleObjectStateException)e);
				}
				
				this.log("Base Tick - Base #"+base.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "BaseTick - Base #"+base.getId()+" Exception", "");
			}
			finally 
			{
				db.evict(base);
			}
		}
		
		if(!messages.isEmpty())
		{
			PM.send(sourceUser, this.lastowner, "Basis-Tick", messages);
		}
		
		try 
		{
			getContext().commit();
		}
		catch( Exception e ) 
		{
			getContext().rollback();
			db.clear();
			
			this.log("Base Tick - User #"+this.lastowner+" failed: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "BaseTick - User #"+this.lastowner+" failed: "+e, "");
		}
	}

	private void evictStaleObjects(org.hibernate.Session db, StaleObjectStateException e)
	{
		// Nicht besonders schoen, da moeglicherweise unangenehme Nebeneffekte auftreten koennen
		// Eine solche Entity kann aber ebenso negative Effekte auf den Tick haben, wenn diese
		// nicht entfernt wird
		Object entity = db.get(e.getEntityName(), e.getIdentifier());
		if( entity != null ) {
			db.evict(entity);
		}
	}

	@Override
	protected void tick() 
	{
		try {
			tickBases();
			getContext().commit();
		}
		catch( Exception e ) {			
			this.log("Base Tick failed: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "BaseTick Exception", "");
		}
	}
}
