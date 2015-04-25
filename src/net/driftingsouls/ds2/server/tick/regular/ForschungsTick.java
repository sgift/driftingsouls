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
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h1>Der Forschungstick.</h1>
 * Bearbeitet die Forschungszentren und markiert erforschte Techs bei den
 * Spielern als erforscht.
 * @author Christopher Jung
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ForschungsTick extends TickController {
	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getDB();

		List<Integer> fzList = Common.cast(db
			.createQuery("select id from Forschungszentrum " +
				"where (base.owner.vaccount=0 or base.owner.wait4vac!=0) and forschung!=null")
			.list());
		new EvictableUnitOfWork<Integer>("Forschungstick")
		{
			@Override
			public void doWork(Integer fzId)
			{
				org.hibernate.Session db = getDB();
				Forschungszentrum fz = (Forschungszentrum)db.get(Forschungszentrum.class, fzId);

				if( fz.getDauer() > 1 )
				{
					fz.setDauer(fz.getDauer()-1);
					return;
				}

				Base base = fz.getBase();
				User user = base.getOwner();

				log("fz "+fz.getId());
				log("\tforschung: "+fz.getForschung());
				Forschung forschung = fz.getForschung();

				log("\t"+forschung.getName()+" ("+forschung.getID()+") erforscht");

				user.addResearch( forschung );

				String msg = "Das Forschungszentrum auf [base="+base.getId()+"]"+base.getName()+"[/base] hat die Forschungen an "+forschung.getName()+" abgeschlossen";

				if( forschung.hasFlag( Forschung.FLAG_DROP_NOOB_PROTECTION) && user.isNoob() )
				{
					msg += "\n\n[color=red]Durch die Erforschung dieser Technologie stehen sie nicht l&auml;nger unter GCP-Schutz.\nSie k&ouml;nnen nun sowohl angreifen als auch angegriffen werden![/color]";
					user.setFlag( UserFlag.NOOB, false );

					log("\t"+user.getId()+" steht nicht laenger unter gcp-schutz");
				}

				final User sourceUser = (User)db.get(User.class, -1);
                if(base.getOwner().getUserValue(WellKnownUserValue.GAMEPLAY_USER_RESEARCH_PM)) {
                    PM.send(sourceUser, base.getOwner().getId(), "Forschung abgeschlossen", msg);
                }

				fz.setForschung(null);
				fz.setDauer(0);
			}
		}
		.setFlushSize(10)
		.executeFor(fzList);
	}
}
