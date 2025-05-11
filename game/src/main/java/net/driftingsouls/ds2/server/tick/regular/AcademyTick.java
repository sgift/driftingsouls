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

import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;
import net.driftingsouls.ds2.server.framework.db.batch.SingleUnitOfWork;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Berechnung des Ticks fuer Akademien.</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Aus- bzw. Weiterbildung durchgefuehrt.
 * Abschliessend werden die Raenge der Offiziere aktuallsiert.
 * @author Christopher Jung
 * @author Bernhard Ludemann
 *
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AcademyTick extends TickController {
	private Map<Integer,Offizier.Ability> dTrain;
	private static final Map<Integer,String> offis = new HashMap<>();

	@Override
	protected void prepare()
	{
		dTrain = new HashMap<>();
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
		var db = getEM();

		List<Academy> accList;
		if(isCampaignTick()) {

			accList = db.createQuery("from Academy a " +
					"where a.train=true and (a.base.owner.vaccount=0 or a.base.owner.wait4vac!=0) and a.base.system in (:systeme)", Academy.class)
					.setParameter("systeme", affectedSystems)
					.getResultList();
		}
		else{
			accList = db.createQuery("from Academy a " +
					"where a.train=true and (a.base.owner.vaccount=0 or a.base.owner.wait4vac!=0)", Academy.class)
					.getResultList();
		}
		
		new EvictableUnitOfWork<Academy>("Academy Tick")
		{
			@Override
			public void doWork(Academy acc) {
				getEM().merge(acc);
				Base base = acc.getBase();

				log("Akademie "+acc.getId()+":");

				// Einen neuen Offizier ausbilden?
				if( acc.getTrain() )
				{
					log("\tAusbildung laeuft");

					List<AcademyQueueEntry> entries = acc.getScheduledQueueEntries();

					StringBuilder msg = new StringBuilder("Die Ausbildung von\n");

					boolean build = false;

					for( AcademyQueueEntry entry : entries )
					{
						entry.decRemainingTime();

						if( entry.getRemainingTime() <= 0 )
						{
							build = true;

							if( entry.getTraining() < 0 )
							{
								msg.append("einem Neuen Offizier (").append(offis.get(-entry.getTraining())).append(")\n");
							}
							else
							{
								Offizier offi = Offizier.getOffizierByID(entry.getTraining());
								if( offi != null )
								{
									msg.append(offi.getName()).append(" (").append(dTrain.get(entry.getTrainingType())).append(")\n");
								}
							}
							entry.finishBuildProcess();

							log("\tOffizier Aus-/Weitergebildet");
						}
					}
					msg.append(" auf dem Asteroiden [base=").append(base.getId()).append("]").append(base.getName()).append("[/base] wurde abgeschlossen.");

					if( build )
					{
						acc.rescheduleQueue();
						// Nachricht versenden
						final User sourceUser = db.find(User.class, -1);
                        User accUser = base.getOwner();
                        if(accUser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_OFFICER_BUILD_PM)) {
                            PM.send(sourceUser, base.getOwner().getId(), "Ausbildung abgeschlossen", msg.toString());
                        }
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
			}
		}
		.setFlushSize(10)
		.executeFor(accList);

		//
		// Raenge der Offiziere neu berechnen
		//
		new SingleUnitOfWork("Academy Tick - Offiziere befoerdern") {
			@Override
			public void doWork() {
				var db = getEM();
				int count = 0;
				for( int i = Offiziere.MAX_RANG; i > 0; i-- ) {
					count += db.createQuery("update Offizier " +
							"set rang= :rang " +
					"where rang < :rang and (ing+waf+nav+sec+com)/125 >= :rang")
					.setParameter("rang", i)
					.executeUpdate();
				}
				log(count+" Offizier(e) befoerdert");
			}
		}
		.execute();

		//da es immer mal wieder zu Problemem mit Offizieren kommt, die gefunden werden, setzen wir den Trainingsstatus des Offiziers zurueck, wenn wir ihn nicht mehr in einem Trainingseintrag finden
		List<Offizier> offiziere = db.createQuery("from Offizier where id not in (select training from AcademyQueueEntry)", Offizier.class)
				.getResultList();
		for(Offizier offizier : offiziere){
				offizier.setTraining(false);
		}
	}
}
