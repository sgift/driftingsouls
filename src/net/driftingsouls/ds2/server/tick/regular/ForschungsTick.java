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
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.entities.Forschungszentrum;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * <h1>Der Forschungstick</h1>
 * Bearbeitet die Forschungszentren und markiert erforschte Techs bei den 
 * Spielern als erforscht.
 * @author Christopher Jung
 *
 */
public class ForschungsTick extends TickController {
	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();
		final User sourceUser = (User)db.get(User.class, -1);
		
		List fzList = db.createQuery("from Forschungszentrum where dauer=1 and (base.owner.vaccount=0 or base.owner.wait4vac!=0)").list();
		for( Iterator iter=fzList.iterator(); iter.hasNext(); ) {
			Forschungszentrum fz = (Forschungszentrum)iter.next();
			
			try {
				Base base = fz.getBase();
				User user = base.getOwner();

				log("fz "+fz.getBaseId());
				log("\tforschung: "+fz.getForschung());
				Forschung forschung = Forschung.getInstance(fz.getForschung());
					
				log("\t"+forschung.getName()+" ("+forschung.getID()+") erforscht");
					
				user.addResearch( forschung.getID() );
					
				String msg = "Das Forschungszentrum auf "+base.getName()+" hat die Forschungen an "+forschung.getName()+" abgeschlossen";
					
				if( forschung.hasFlag( Forschung.FLAG_DROP_NOOB_PROTECTION) && user.isNoob() ) {
					msg += "\n\n[color=red]Durch die Erforschung dieser Technologie stehen sie nicht l&auml;nger unter GCP-Schutz.\nSie k&ouml;nnen nun sowohl angreifen als auch angegriffen werden![/color]";
					user.setFlag( User.FLAG_NOOB, false );
					
					log("\t"+user.getId()+" steht nicht laenger unter gcp-schutz");
				}
					
				PM.send(sourceUser, base.getOwner().getId(), "Forschung abgeschlossen", msg);
				
				fz.setForschung(0);
				fz.setDauer(0);
				
				getContext().commit();
				db.evict(fz);
			}
			catch( RuntimeException e ) {
				this.log("Forschungszentrum "+fz.getBaseId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "ForschungsTick Exception", "Forschungszentrum: "+fz.getBaseId());
				
				throw e;
			}
		}

		int count = db.createQuery("update Forschungszentrum as f " +
				"set f.dauer=f.dauer-1 " +
				"where f.dauer!=0 and f.col in (select id from Base where id=f.col and (owner.vaccount=0 or owner.wait4vac!=0))")
			.executeUpdate();
		
		log("Laufende Forschungen: "+count);
	}

}
