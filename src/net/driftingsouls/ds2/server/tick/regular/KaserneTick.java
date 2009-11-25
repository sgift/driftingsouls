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
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * <h1>Berechnung des Ticks fuer Kasernen.</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Ausbildung durchgefuehrt.
 * 
 */
public class KaserneTick extends TickController {
	
	@Override
	protected void prepare() 
	{
		
	}

	@Override
	protected void tick() 
	{
		org.hibernate.Session db = getDB();

		final User sourceUser = (User)db.get(User.class, -1);

		List<?> kaserneList = db.createQuery("from Kaserne").list();
		for( Iterator<?> iter=kaserneList.iterator(); iter.hasNext(); ) {
			Kaserne kaserne = (Kaserne)iter.next();

			try {
				Base base = kaserne.getBase();

				log("Kaserne "+base.getId()+":");
				
				boolean build = false;
				
				String msg = "";
				
				if(kaserne.isBuilding())
				{
					log("\tAusbildung laeuft");
					KaserneEntry[] entries = kaserne.getQueueEntries();
					
					msg = "Die Ausbildung von<br />";
					
					for(KaserneEntry entry : entries)
					{
						entry.setRemaining(entry.getRemaining()-1);
						if(entry.getRemaining() <= 0)
						{
							UnitType unittype = (UnitType)db.get(UnitType.class, entry.getUnitId());
							msg = msg+entry.getCount()+" "+unittype.getName()+"<br />";
							entry.finishBuildProcess(base);
							build = true;
						}
					}
					msg = msg + "auf der Basis "+base.getName()+" ist abgeschlossen.";
					
				}
				
				if( build )
				{
					// Nachricht versenden
					PM.send(sourceUser,base.getOwner().getId(), "Ausbildung abgeschlossen", msg);
				}
				
				getContext().commit();
				db.evict(kaserne);
			}
			catch( RuntimeException e ) {
				this.log("Bearbeitung der Kaserne "+kaserne.getBase().getId()+" fehlgeschlagen: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "Kaserne Tick Exception", "Kaserne: "+kaserne.getBase().getId());

				throw e;
			}
		}
	}
}
