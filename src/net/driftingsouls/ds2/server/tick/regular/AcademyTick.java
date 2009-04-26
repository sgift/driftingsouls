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

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.namegenerator.NameGenerator;
import net.driftingsouls.ds2.server.tick.TickController;

import org.apache.commons.lang.math.RandomUtils;

/**
 * <h1>Berechnung des Ticks fuer Akademien.</h1>
 * Der Ausbildungscountdown wird reduziert und, wenn dieser abgelaufen ist,
 * die Aus- bzw. Weiterbildung durchgefuehrt.
 * Abschliessend werden die Raenge der Offiziere aktuallsiert.
 * @author Christopher Jung
 *
 */
public class AcademyTick extends TickController {
	private int maxid;
	private Map<Integer,Offizier.Ability> dTrain;

	@Override
	protected void prepare() {
		org.hibernate.Session db = getDB();

		// Max-ID berechnen
		maxid = ((Number)db.createQuery("select max(id) from Offizier")
				.iterate().next()
		).intValue();
		log("maxid: "+maxid);
		maxid++;

		dTrain = new HashMap<Integer,Offizier.Ability>();
		dTrain.put(1, Offizier.Ability.ING);
		dTrain.put(2, Offizier.Ability.WAF);
		dTrain.put(3, Offizier.Ability.NAV);
		dTrain.put(4, Offizier.Ability.SEC);
		dTrain.put(5, Offizier.Ability.COM);
		
		//Reset all hanging tasks to one tick
		db.createQuery("update Academy as a set a.remain=:remain where a.remain=0 and (a.upgrade!=:upgrade or a.train!=:train)")
		  .setParameter("remain", 1)
		  .setParameter("upgrade", "")
		  .setParameter("train", 0)
		  .executeUpdate();
	}

	private String getNewOffiName(int race) {
		String offiname = "Offizier "+maxid;

		NameGenerator generator = Rassen.get().rasse(race).getNameGenerator(Rasse.GeneratorType.PERSON);
		if( generator != null ) {
			offiname = generator.generate(1)[0];
		}

		return offiname;
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();

		final User sourceUser = (User)db.get(User.class, -1);

		List<?> accList = db.createQuery("from Academy " +
		"where remain=1 and (base.owner.vaccount=0 or base.owner.wait4vac!=0)").list();
		for( Iterator<?> iter=accList.iterator(); iter.hasNext(); ) {
			Academy acc = (Academy)iter.next();

			try {
				Base base = acc.getBase();

				log("Akademie "+acc.getBaseId()+":");

				// Einen neuen Offizier ausbilden?
				if( acc.getTrain() != 0 ) {
					log("\tAusbildung abgeschlossen");
					String offiname = getNewOffiName(base.getOwner().getRace());

					Offizier offizier = new Offizier(base.getOwner(), offiname);

					int train = acc.getTrain();
					if( !Offiziere.LIST.containsKey(train) ) {
						train = Offiziere.LIST.keySet().iterator().next();
					}

					SQLResultRow offi = Offiziere.LIST.get(train);

					offizier.setAbility(Offizier.Ability.ING, offi.getInt("ing"));
					offizier.setAbility(Offizier.Ability.WAF, offi.getInt("waf"));
					offizier.setAbility(Offizier.Ability.NAV, offi.getInt("nav"));
					offizier.setAbility(Offizier.Ability.SEC, offi.getInt("sec"));
					offizier.setAbility(Offizier.Ability.COM, offi.getInt("com"));

					int spec = RandomUtils.nextInt(((int[])offi.get("specials")).length);
					spec = ((int[])offi.get("specials"))[spec];

					offizier.setSpecial(Offizier.Special.values()[spec-1]);

					offizier.setDest("b", base.getId());

					int newid = (Integer)db.save(offizier);

					if( maxid < newid ) {
						maxid = newid;
					}

					maxid++;
				}
				// Einen bestehenden Offizier weiterbilden?
				else if( acc.getUpgrade().length() > 0 ) {
					log("\tWeiterbildung abgeschlossen");
					int[] dat = Common.explodeToInt(" ", acc.getUpgrade());
					final Offizier.Ability ability = dTrain.get(dat[1]);

					final Offizier offi = Offizier.getOffizierByID(dat[0]);
					offi.setAbility(ability, offi.getAbility(ability)+2);
					offi.setDest("b", base.getId());
				}
				acc.setRemain(0);
				acc.setTrain(0);
				acc.setUpgrade("");

				// Nachricht versenden
				String msg = "Die Flottenakademie auf dem Asteroiden "+base.getName()+" hat die Ausbildung abgeschlossen";
				PM.send(sourceUser,base.getOwner().getId(), "Ausbildung abgeschlossen", msg);

				getContext().commit();
				db.evict(acc);
				HibernateFacade.evictAll(db, Offizier.class);
			}
			catch( RuntimeException e ) {
				this.log("Bearbeitung der Akademie "+acc.getBaseId()+" fehlgeschlagen: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "Academy Tick Exception", "Academy: "+acc.getBaseId());

				throw e;
			}
		}

		int count = db.createQuery("update Academy as a " +
				"set a.remain=a.remain-1 " +
		"where a.remain!=0 and a.base in (from Base where id=a.col and (owner.vaccount=0 or owner.wait4vac!=0))")
		.executeUpdate();
		log("Offiziere in der Aus/Weiterbildung: "+count);
		
		//
		// Raenge der Offiziere neu berechnen
		//
		count = 0;
		for( int i = Offiziere.MAX_RANG; i > 0; i-- ) {
			count += db.createQuery("update Offizier " +
					"set rang= :rang " +
			"where rang < :rang and (ing+waf+nav+sec+com)/125 >= :rang")
			.setInteger("rang", i)
			.executeUpdate();
		}

		log(count+" Offizier(e) befoerdert");
	}
}
