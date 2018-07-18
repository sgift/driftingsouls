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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.Kaserne;
import net.driftingsouls.ds2.server.entities.KaserneEntry;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.UnitType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h1>Berechnung des Ticks fuer Kasernen.</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Ausbildung durchgefuehrt.
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class KaserneTick extends TickController {

	@Override
	protected void prepare()
	{}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getDB();

		final User sourceUser = (User)db.get(User.class, -1);

		List<Integer> kasernen = Common.cast(
				db.createQuery("select k.id from Kaserne k where k.entries is not empty").list()
		);
		new EvictableUnitOfWork<Integer>("Kasernen Tick")
		{
			@Override
			public void doWork(Integer object) throws Exception
			{
				org.hibernate.Session db = getDB();
				Kaserne kaserne = (Kaserne)db.get(Kaserne.class, object);
				Base base = kaserne.getBase();

				log("Kaserne "+base.getId()+":");

				boolean build = false;

				String msg = "";

				if(kaserne.isBuilding())
				{
					log("\tAusbildung laeuft");

					msg = "Die Ausbildung von\n";

					for(KaserneEntry entry : kaserne.getQueueEntries())
					{
						entry.setRemaining(entry.getRemaining()-1);
						if(entry.getRemaining() <= 0)
						{
							UnitType unittype = entry.getUnit();
							msg = msg+entry.getCount()+" "+unittype.getName()+"\n";
							entry.finishBuildProcess(base);
							build = true;
						}
					}
					msg = msg + "auf der Basis [base="+base.getId()+"]"+base.getName()+"[/base] ist abgeschlossen.";

				}

				if( build )
				{
					// Nachricht versenden
                    if(base.getOwner().getUserValue(WellKnownUserValue.GAMEPLAY_USER_UNIT_BUILD_PM)) {
                        PM.send(sourceUser, base.getOwner().getId(), "Ausbildung abgeschlossen", msg);
                    }
				}
			}
		}
		.setFlushSize(10)
		.executeFor(kasernen);
	}
}
