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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * <h1>Berechnung des Ticks fuer Akademien.</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Aus- bzw. Weiterbildung durchgefuehrt.
 * Abschliessend werden die Raenge der Offiziere aktuallsiert.
 * @author Christopher Jung
 * @author Bernhard Ludemann
 *
 */
public class AcademyTick extends TickController {
	private Map<Integer,Offizier.Ability> dTrain;
	private static final Map<Integer,String> offis = new HashMap<Integer,String>();

	@Override
	protected void prepare() 
	{
		dTrain = new HashMap<Integer,Offizier.Ability>();
		dTrain.put(1, Offizier.Ability.ING);
		dTrain.put(2, Offizier.Ability.WAF);
		dTrain.put(3, Offizier.Ability.NAV);
		dTrain.put(4, Offizier.Ability.SEC);
		dTrain.put(5, Offizier.Ability.COM);
		
		offis.put(1, "Ingenieur");
		offis.put(2, "Navigator");
		offis.put(3, "Sicherheitsexperte");
		offis.put(4, "Captain");
	}

	@Override
	protected void tick() 
	{
		org.hibernate.Session db = getDB();

		Transaction transaction = db.beginTransaction();
		final User sourceUser = (User)db.get(User.class, -1);

		List<?> accList = db.createQuery("from Academy " +
		"where train=1 and (base.owner.vaccount=0 or base.owner.wait4vac!=0)").list();
		for( Iterator<?> iter=accList.iterator(); iter.hasNext(); ) 
		{
			Academy acc = (Academy)iter.next();

			try 
			{
				Base base = acc.getBase();

				log("Akademie "+acc.getBaseId()+":");
				
				boolean build;
				
				String msg = "";
				
				// Einen neuen Offizier ausbilden?
				if( acc.getTrain() ) 
				{
					log("\tAusbildung laeuft");
					
					AcademyQueueEntry[] entries = acc.getScheduledQueueEntries();
					
					msg = "Die Ausbildung von<br />"; 
					
					build = false;
					
					for( AcademyQueueEntry entry : entries )
					{	
						entry.decRemainingTime();

						if( entry.getRemainingTime() <= 0 )
						{	
							build = true;

							if( entry.getTraining() < 0 )
							{
								msg = msg+"einem Neuen Offizier ("+offis.get(-entry.getTraining())+")<br />";
							}
							else
							{
								msg = msg+Offizier.getOffizierByID(entry.getTraining()).getName()+" ("+dTrain.get(entry.getTrainingType())+")<br />";
							}
							entry.finishBuildProcess();
							
							log("\tOffizier Aus-/Weitergebildet");
						}
					}
					msg = msg+" auf dem Asteroiden "+base.getName()+" wurde abgeschlossen.";
					
					if( build )
					{
						acc.rescheduleQueue();
						// Nachricht versenden
						PM.send(sourceUser,base.getOwner().getId(), "Ausbildung abgeschlossen", msg);
					}
					
					if( acc.getNumberScheduledQueueEntries() == 0 )
					{
						acc.setTrain(false);
					}
				}
				else
				{
					log("\tKeine Ausbildung vorhanden");
				}

				transaction.commit();
				transaction = db.beginTransaction();
			}
			catch( RuntimeException e ) 
			{
				transaction.rollback();
				this.log("Bearbeitung der Akademie "+acc.getBaseId()+" fehlgeschlagen: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "Academy Tick Exception", "Academy: "+acc.getBaseId());

				throw e;
			}
		}

		//
		// Raenge der Offiziere neu berechnen
		//
		int count = 0;
		for( int i = Offiziere.MAX_RANG; i > 0; i-- ) {
			count += db.createQuery("update Offizier " +
					"set rang= :rang " +
			"where rang < :rang and (ing+waf+nav+sec+com)/125 >= :rang")
			.setInteger("rang", i)
			.executeUpdate();
		}
		transaction.commit();
		log(count+" Offizier(e) befoerdert");
	}
}
