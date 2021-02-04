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
package net.driftingsouls.ds2.server.tick.rare;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.statistik.StatAktiveSpieler;
import net.driftingsouls.ds2.server.entities.statistik.StatCargo;
import net.driftingsouls.ds2.server.entities.statistik.StatItemLocations;
import net.driftingsouls.ds2.server.entities.statistik.StatUserCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 *
 * @author Christopher Jung
 */
@Service("rareRestTick")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestTick extends TickController {
    @PersistenceContext
    private EntityManager em;

    private int tick;

    @Override
    protected void prepare() {
        this.tick = new ConfigService().getValue(WellKnownConfigValue.TICKS);
    }

    @Override
    protected void tick() {
        this.log("Berechne Gesamtcargo:");
        Cargo cargo = new Cargo();
        Map<User, Cargo> usercargos = new HashMap<>();
        Map<User, Map<Integer, Set<String>>> useritemlocations = new HashMap<>();

        ermittleCargoStatistiken(cargo, usercargos, useritemlocations);

        StatCargo stat = new StatCargo(this.tick, cargo);
        em.persist(stat);

        this.log("\t" + cargo.save());
        this.log("Speichere User-Cargo-Stats");
        speicherUserCargoStatistik(usercargos);

        this.log("Speichere Module-Location-Stats");
        speicherItemLocations(useritemlocations);

        this.log("Erfasse Spieleraktivitaet");
        erstelleSpielerStatistik();
    }

    private void erstelleSpielerStatistik() {
        StatAktiveSpieler lastStatSpieler = em.createQuery("from StatAktiveSpieler order by tick DESC", StatAktiveSpieler.class)
            .setMaxResults(1)
            .getSingleResult();
        StatAktiveSpieler statSpieler = new StatAktiveSpieler();
        statSpieler.setTick(this.tick);

        List<User> spielerList = em.createQuery("from User u", User.class).getResultList();
        for (User user : spielerList) {
            statSpieler.erfasseSpieler(user);
            if (lastStatSpieler != null) {
                if (lastStatSpieler.getMaxUserId() < user.getId()) {
                    statSpieler.erfasseRegistrierung(user);
                }
            }
        }
        em.persist(statSpieler);
    }

    private void ermittleCargoStatistiken(Cargo cargo, Map<User, Cargo> usercargos, Map<User, Map<Integer, Set<String>>> useritemlocations) {
        em.createQuery("from Base as b inner join fetch b.owner where b.owner.id!=0", Base.class)
            .getResultStream().forEach(base -> {
            Cargo bcargo = base.getCargo();
            if (base.getOwner().getId() > 0) {
                cargo.addCargo(bcargo);
            }

            if (!usercargos.containsKey(base.getOwner())) {
                usercargos.put(base.getOwner(), new Cargo(bcargo));
            } else {
                usercargos.get(base.getOwner()).addCargo(bcargo);
            }


            List<ItemCargoEntry<Item>> itemlist = bcargo.getItemEntries();
            for (ItemCargoEntry<Item> aitem : itemlist) {
                if (aitem.getItem() instanceof Munition) {
                    continue;
                }

                if (!useritemlocations.containsKey(base.getOwner())) {
                    useritemlocations.put(base.getOwner(), new HashMap<>());
                }
                Map<Integer, Set<String>> itemlocs = useritemlocations.get(base.getOwner());
                if (!itemlocs.containsKey(aitem.getItemID())) {
                    itemlocs.put(aitem.getItemID(), new HashSet<>());
                }
                itemlocs.get(aitem.getItemID()).add("b" + base.getId());
            }

            em.detach(base);
        });

        em.createQuery("from Ship as s left join fetch s.modules where s.id>0", Ship.class)
            .getResultStream()
            .forEach(ship -> {
                Cargo scargo = ship.getCargo();
                if (ship.getOwner().getId() > 0) {
                    cargo.addCargo(scargo);
                }

                if (!usercargos.containsKey(ship.getOwner())) {
                    usercargos.put(ship.getOwner(), new Cargo(scargo));
                } else {
                    usercargos.get(ship.getOwner()).addCargo(scargo);
                }

                List<ItemCargoEntry<Item>> itemlist = scargo.getItemEntries();
                for (ItemCargoEntry<Item> aitem : itemlist) {
                    if (aitem.getItem() instanceof Munition) {
                        continue;
                    }
                    if (!useritemlocations.containsKey(ship.getOwner())) {
                        useritemlocations.put(ship.getOwner(), new HashMap<>());
                    }
                    Map<Integer, Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
                    if (!itemlocs.containsKey(aitem.getItemID())) {
                        itemlocs.put(aitem.getItemID(), new HashSet<>());
                    }
                    itemlocs.get(aitem.getItemID()).add("s" + ship.getId());
                }

                ModuleEntry[] modulelist = ship.getModuleEntries();

                for (ModuleEntry amodule : modulelist) {
                    Module shipmodule = amodule.createModule();
                    if (shipmodule instanceof ModuleItemModule) {
                        ModuleItemModule itemmodule = (ModuleItemModule) shipmodule;
                        if (ship.getOwner().getId() > 0) {
                            cargo.addResource(itemmodule.getItemID(), 1);
                        }
                        usercargos.get(ship.getOwner()).addResource(itemmodule.getItemID(), 1);
                        if (!useritemlocations.containsKey(ship.getOwner())) {
                            useritemlocations.put(ship.getOwner(), new HashMap<>());
                        }
                        Map<Integer, Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
                        if (!itemlocs.containsKey(itemmodule.getItemID().getItemID())) {
                            itemlocs.put(itemmodule.getItemID().getItemID(), new HashSet<>());
                        }
                        itemlocs.get(itemmodule.getItemID().getItemID()).add("s" + ship.getId());
                    }
                }
                em.clear();
            });

        this.log("\tLese Zwischenlager ein");
        em.createQuery("from GtuZwischenlager", GtuZwischenlager.class)
            .getResultStream().forEach(entry -> {
            Cargo acargo = entry.getCargo1();
            if (entry.getUser1().getId() > 0) {
                cargo.addCargo(acargo);
            }

            if (!usercargos.containsKey(entry.getUser1())) {
                usercargos.put(entry.getUser1(), new Cargo(acargo));
            } else {
                usercargos.get(entry.getUser1()).addCargo(acargo);
            }

            List<ItemCargoEntry<Item>> itemlist = acargo.getItemEntries();
            for (ItemCargoEntry<Item> aitem : itemlist) {
                if (aitem.getItem() instanceof Munition) {
                    continue;
                }
                if (!useritemlocations.containsKey(entry.getUser1())) {
                    useritemlocations.put(entry.getUser1(), new HashMap<>());
                }
                Map<Integer, Set<String>> itemlocs = useritemlocations.get(entry.getUser1());
                if (!itemlocs.containsKey(aitem.getItemID())) {
                    itemlocs.put(aitem.getItemID(), new HashSet<>());
                }
                itemlocs.get(aitem.getItemID()).add("g" + entry.getPosten().getId());
            }

            acargo = entry.getCargo2();
            if (entry.getUser2().getId() > 0) {
                cargo.addCargo(acargo);
            }
            if (!usercargos.containsKey(entry.getUser2())) {
                usercargos.put(entry.getUser2(), new Cargo(acargo));
            } else {
                usercargos.get(entry.getUser2()).addCargo(acargo);
            }

            itemlist = acargo.getItemEntries();
            for (ItemCargoEntry<Item> aitem : itemlist) {
                if (aitem.getItem() instanceof Munition) {
                    continue;
                }
                if (!useritemlocations.containsKey(entry.getUser2())) {
                    useritemlocations.put(entry.getUser2(), new HashMap<>());
                }
                Map<Integer, Set<String>> itemlocs = useritemlocations.get(entry.getUser2());
                if (!itemlocs.containsKey(aitem.getItemID())) {
                    itemlocs.put(aitem.getItemID(), new HashSet<>());
                }
                itemlocs.get(aitem.getItemID()).add("g" + entry.getPosten().getId());
            }
        });
    }

    private void speicherUserCargoStatistik(Map<User, Cargo> usercargos) {
        em.createQuery("delete from StatUserCargo").executeUpdate();

        for (Map.Entry<User, Cargo> entry : usercargos.entrySet()) {
            User owner = entry.getKey();
            Cargo userCargo = entry.getValue();
            StatUserCargo userstat = new StatUserCargo(owner, userCargo);
            this.log(owner.getId() + ":" + userCargo.save());
            em.persist(userstat);
        }
    }

    private void speicherItemLocations(Map<User, Map<Integer, Set<String>>> useritemlocations) {
        em.createQuery("delete from StatItemLocations").executeUpdate();

        for (Map.Entry<User, Map<Integer, Set<String>>> entry : useritemlocations.entrySet()) {
            User owner = entry.getKey();
            for (Map.Entry<Integer, Set<String>> innerEntry : entry.getValue().entrySet()) {
                Set<String> locations = innerEntry.getValue();
                int itemid = innerEntry.getKey();

                List<String> locationlist = new ArrayList<>();
                for (String loc : locations) {
                    locationlist.add(loc);

                    // Bei einer durchschnittlichen Zeichenkettenlaenge von 8 passen nicht mehr 10 Orte rein
                    if (locationlist.size() >= 10) {
                        break;
                    }
                }

                StatItemLocations itemstat = new StatItemLocations(owner, itemid, Common.implode(";", locationlist));
                em.persist(itemstat);
            }
        }
    }

}
