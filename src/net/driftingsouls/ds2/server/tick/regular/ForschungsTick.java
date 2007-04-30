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

import java.util.HashSet;

import net.driftingsouls.ds2.server.Forschung;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * <h1>Der Forschungstick</h1>
 * Bearbeitet die Forschungszentren und markiert erforschte Techs bei den 
 * Spielern als erforscht.
 * @author Christopher Jung
 *
 */
public class ForschungsTick extends TickController {
	private HashSet<Integer> vaclist;
	
	@Override
	protected void prepare() {
		vaclist = new HashSet<Integer>();
		vaclist.add(0);
	}

	@Override
	protected void tick() {
		Database db = getDatabase();
		
		SQLQuery fzdRow = db.query("SELECT id,forschung,dauer,col FROM fz WHERE dauer!=0");
		while( fzdRow.next() ) {
			try {
				SQLResultRow base = db.first("SELECT name,owner FROM bases WHERE id="+fzdRow.getInt("col"));
				
				User user = getContext().createUserObject(base.getInt("owner"));
			
				if( (user.getVacationCount() != 0) && (user.getWait4VacationCount() == 0) ) {
					log("Ueberspringe Forschungszentrum "+fzdRow.getInt("id")+" [VAC]");
					vaclist.add(fzdRow.getInt("id"));
					continue;
				}
				
				if( fzdRow.getInt("dauer") != 1 ) {
					continue;
				}
			
				log("fz "+fzdRow.getInt("id"));
				log("\tforschung: "+fzdRow.getInt("forschung"));
				Forschung forschung = Forschung.getInstance(fzdRow.getInt("forschung"));
					
				log("\t"+forschung.getName()+" ("+forschung.getID()+") erforscht");
					
				user.addResearch( forschung.getID() );
					
				String msg = "Das Forschungszentrum auf "+base.getString("name")+" hat die Forschungen an "+forschung.getName()+" abgeschlossen";
					
				if( forschung.hasFlag( Forschung.FLAG_DROP_NOOB_PROTECTION) && user.isNoob() ) {
					msg += "\n\n[color=red]Durch die Erforschung dieser Technologie stehen sie nicht l&auml;nger unter GCP-Schutz.\nSie k&ouml;nnen nun sowohl angreifen als auch angegriffen werden![/color]";
					user.setFlag( User.FLAG_NOOB, false );
					
					log("\t"+user.getID()+" steht nicht laenger unter gcp-schutz");
				}
					
				PM.send(getContext(), -1, base.getInt("owner"), "Forschung abgeschlossen", msg);
				
				db.update("UPDATE fz SET forschung=0,dauer=0 WHERE id=",fzdRow.getInt("id"));
			}
			catch( Exception e ) {
				this.log("Forschungszentrum "+fzdRow.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "ForschungsTick Exception", "Forschungszentrum: "+fzdRow.getInt("id"));
			}
		}
		fzdRow.free();
		
		db.update("UPDATE fz SET dauer=dauer-1 WHERE dauer!=0 AND NOT (id IN (",Common.implode(",",vaclist.toArray()),"))");
		log("Laufende Forschungen: "+db.affectedRows());
	}

}
