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

import java.util.List;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.tick.TickController;

import org.hibernate.FlushMode;
import org.hibernate.Transaction;

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
		Transaction transaction = db.beginTransaction();
		User sourceUser = (User)db.get(User.class, -1);
		
		// Get all bases, take everything with them - we need it all.
		List<Base> bases = Common.cast(db.createQuery("from Base b fetch all properties where b.owner!=0 and (b.owner.vaccount=0 or b.owner.wait4vac!=0) order by b.owner").setFetchSize(5000).list());
		transaction.commit();	
		
		String messages = "";
		int count = 0;
		for(Base base: bases)
		{
			transaction = db.beginTransaction();
			try
			{
				// Muessen ggf noch alte Userdaten geschrieben und neue geladen werden?
				if( base.getOwner().getId() != this.lastowner ) 
				{
					log(base.getOwner().getId()+":");
					if( this.pmcache.length() != 0 ) 
					{
						this.pmcache.setLength(0);
					}
				}
				
				if(this.lastowner != base.getOwner().getId() && !messages.trim().equals(""))
				{
					PM.send(sourceUser, this.lastowner, "Basis-Tick", messages);
					messages = "";
				}
				
				this.lastowner = base.getOwner().getId();
				messages += base.tick();
				transaction.commit();
				count++;
				final int UNFLUSHED_OBJECTS_SIZE = 50;
				if(count%UNFLUSHED_OBJECTS_SIZE == 0)
				{
					//No db.clear here or the session cannot lazy-load the user objects
					db.flush();
				}
			}
			catch(Exception e)
			{
				transaction.rollback();
				e.printStackTrace();
				Common.mailThrowable(e, "BaseTick Exception", "Base: " + base.getId());
			}
		}
		
		transaction = db.beginTransaction();
		if(!messages.isEmpty())
		{
			PM.send(sourceUser, this.lastowner, "Basis-Tick", messages);
		}
		transaction.commit();
	}

	@Override
	protected void tick() 
	{
		FlushMode oldMode = getDB().getFlushMode();
		getDB().setFlushMode(FlushMode.MANUAL);
		tickBases();
		getDB().flush();
		getDB().clear();
		getDB().setFlushMode(oldMode);
	}
}
