package net.driftingsouls.ds2.server.battles;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.modules.ks.BasicKSAction;
import net.driftingsouls.ds2.server.modules.ks.KSAttackAction;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Erlaubt das automatische Feuern in einer Schlacht.
 * AutoFire feuert immer einen User in einer Schlacht. Wenn beide Seiten feuern sollen werden zwei AutoFire Instanzen benoetigt.
 */
public class AutoFire
{
    public AutoFire(Session db, Battle battle)
    {
        if(db == null)
        {
            throw new IllegalArgumentException("Db may not be null.");
        }

        if(battle == null)
        {
            throw new IllegalArgumentException("Battle may not be null.");
        }

        this.db = db;
        this.battle = battle;
    }

    public void fireShips()
    {
        List<BattleShip> firingShips = new ArrayList<>(battle.getShips(Side.OWN));
        List<BattleShip> attackedShips = new ArrayList<>(battle.getShips(Side.ENEMY));

        for(BattleShip firingShip: firingShips)
        {
            log.info("Firing with ship: " + firingShip.getShip().getId());
            log.info("------");
            battle.setFiringShip(firingShip.getShip());
            int energy = firingShip.getShip().getEnergy();
            if(energy == 0)
            {
                log.info("\tShip has no energy. Stopping.");
                continue;
            }

            Map<Weapon, Integer> shipWeapons = getShipWeapons(firingShip);
            if(shipWeapons.size() == 0)
            {
                log.info("\tShip has no weapons. Stopping.");
                continue;
            }

            if(firingShip.getWeapons() == 0)
            {
                log.info("\tWeapon system is down. Stopping.");
                continue;
            }

            Map<String, String> maxHeats = Weapons.parseWeaponList(firingShip.getTypeData().getMaxHeat());
            Map<String, String> heats = Weapons.parseWeaponList(firingShip.getWeaponHeat());
            
            for(Map.Entry<String, String> heatLog: maxHeats.entrySet())
            {
                log.info("Weapon: " + heatLog.getKey() + " Max heat: " + heatLog.getValue());
            }

            for(Map.Entry<String, String> heatLog: heats.entrySet())
            {
                log.info("Weapon: " + heatLog.getKey() + " Heat: " + heatLog.getValue());
            }
            
            for(Map.Entry<Weapon, Integer> weapon: shipWeapons.entrySet())
            {
                log.info("\tFiring weapon: " + weapon.getKey().getId());
                int maxHeat = Integer.valueOf(maxHeats.get(weapon.getKey().getId()));
                int heat =  0;
                if(heats.containsKey(weapon.getKey().getId()))
                {
                    heat = Integer.valueOf(heats.get(weapon.getKey().getId()));
                }
                
                Set<Munitionsdefinition> ammunition = getPossibleAmmunition(firingShip.getShip(), weapon.getKey());
                for(Munitionsdefinition ammo: ammunition)
                {
                    if(firingShip.getShip().getEnergy() == 0)
                    {
                        break;
                    }

                    log.info("\t\tFiring ammunition: " + (ammo != null ? ammo.getName() : "No Ammo"));
                    boolean continueFire = true;
                    while(continueFire)
                    {
                        if(heat + weapon.getValue() > maxHeat)
                        {
                            log.info("\t\tMaximum heat reached for Weapon. Stopping.");
                            break;
                        }

                        KSAttackAction firingAction = findTarget(battle.getCommander(0), firingShip, weapon, ammo, attackedShips);
                        if(firingAction != null)
                        {
                            BasicKSAction.Result result = null;
                            try
                            {
                                log.info("\t\tFiring at ship: " + firingAction.getAttackedShip().getId() + " Shields: " + firingAction.getAttackedShip().getShields() + " Ablative Armor: " + firingAction.getAttackedShip().getAblativeArmor() + " Hull: " + firingAction.getAttackedShip().getHull());
                                result = firingAction.attack(battle);
                                heat += weapon.getValue();
                                db.merge(firingAction.getAttackedShip());
                                db.merge(firingShip);
                                log.info("\t\tFired at ship: " + firingAction.getAttackedShip().getShip().getId() + " Shields: " + firingAction.getAttackedShip().getShields() + " Ablative Armor: " + firingAction.getAttackedShip().getAblativeArmor() + " Hull: " + firingAction.getAttackedShip().getHull());
                            }
                            catch (IOException e)
                            {
                                log.info("Exception while firing weapon", e);
                            }

                            if(result != BasicKSAction.Result.OK)
                            {
                                continueFire = false;
                            }
                        }
                        else
                        {
                            log.info("\t\tNo possible target found. Stopping.");
                            continueFire = false;
                        }

                        if(firingShip.getShip().getEnergy() == 0)
                        {
                            continueFire = false;
                        }
                    }
                }

                if(firingShip.getShip().getEnergy() == 0)
                {
                    log.info("\tShip has no more energy. Stopping.");
                    break;
                }
            }
        }

        battle.writeLog();
    }
    
    private Set<Munitionsdefinition> getPossibleAmmunition(Ship ship, Weapon weapon)
    {
        //Load all ammunition from cargo
        Set<Integer> ammos = new HashSet<>();
        if(!weapon.getMunitionstypen().isEmpty())
        {
            Cargo mycargo = ship.getCargo();
            List<ItemCargoEntry<Munition>> itemList = mycargo.getItemsOfType(Munition.class);
            if(weapon.hasFlag(Weapon.Flags.AMMO_SELECT))
            {
				ammos.addAll(itemList.stream().map(ItemCargoEntry::getItemID).collect(Collectors.toList()));
            }
            else
            {
				for (ItemCargoEntry<Munition> item : itemList)
				{
					IEAmmo effect = item.getItem().getEffect();
					if (weapon.getMunitionstypen().contains(effect.getAmmo().getType()))
					{
						ammos.add(item.getItemID());
						break;
					}
				}
            }
        }

        //Filter ammunition the weapon cannot fire
        Set<Munitionsdefinition> filteredAmmos = new HashSet<>();
        if(ammos.isEmpty())
        {
            filteredAmmos.add(null);
        }
        else
        {
            for(int ammoId: ammos)
            {
                Iterator<?> iterator = db.createQuery("from Munitionsdefinition where itemid=:id and type in (:ammo)")
                                                .setInteger("id", ammoId)
                                                .setParameterList("ammo", weapon.getMunitionstypen()).iterate();
                if(iterator.hasNext())
                {
                    log.info("Ammo type " + ammoId + " allowed by weapon.");
                    Munitionsdefinition ammo = (Munitionsdefinition)iterator.next();
                    filteredAmmos.add(ammo);
                }
                else
                {
                    log.info("Ammo type " + ammoId + " not allowed by weapon.");
                }
            }
        }

        return filteredAmmos;
    }

    private KSAttackAction findTarget(User user, BattleShip firingShip, Map.Entry<Weapon, Integer> shipWeapon, Munitionsdefinition ammo, List<BattleShip> possibleTargets)
    {
        log.info("\t\tCalculating best target");
        if(possibleTargets.isEmpty())
        {
            return null;
        }

        int ammoId = 0;
        if(ammo != null)
        {
            ammoId = ammo.getItemId();
        }
        Weapon weapon = shipWeapon.getKey();

        //Always fire on the same kind of ship unless the weapon is a special big ship or small ship attack weapon (i.e. torpedos on bombers)
        boolean attackSmall = firingShip.getTypeData().getSize() < ShipType.SMALL_SHIP_MAXSIZE;
        if(weapon.getId().equals("torpedo") || weapon.getId().equals("mjolnir"))
        {
            attackSmall = false;
        }

        if(weapon.getId().contains("flak") || weapon.getId().contains("AAA"))
        {
            attackSmall = true;
        }

        //Find the ship we want to damage most
        KSAttackAction firingAction = null;
        Ship target = null;
        int killDesire = 0;
        int baseDamage = 0;
        if(ammo != null)
        {
            if(ammo.getSubDamage() <= 0)
            {
                baseDamage = ammo.getDamage();
            }
        }
        else
        {

            if(weapon.getSubDamage() <= 0)
            {
                baseDamage = weapon.getBaseDamage();
            }
        }

        for(BattleShip possibleTarget: possibleTargets)
        {
            log.info("\t\tChecking ship for kill: " + possibleTarget.getShip().getId());

            if(possibleTarget.getShip().isDocked() || possibleTarget.getShip().isLanded())
            {
                log.info("\t\tShip is landed or docked. Ignoring.");
                continue;
            }
            
            if(attackSmall && possibleTarget.getTypeData().getSize() > ShipType.SMALL_SHIP_MAXSIZE && firingAction != null)
            {
                log.info("\t\tShip doesn't match preferred kill type and preferred kill type ship found. Ignoring.");
                continue;
            }

            if(!attackSmall && possibleTarget.getTypeData().getSize() <= ShipType.SMALL_SHIP_MAXSIZE && firingAction != null)
            {
                log.info("\t\tShip doesn't match preferred kill type and preferred kill type ship found. Ignoring.");
                continue;
            }

            if(possibleTarget.getEngine() == 0 || possibleTarget.getWeapons() == 0)
            {
                log.info("\t\tShip has no engine and/or weapon. Possible target for seizing. Ignoring.");
                continue;
            }

            if(possibleTarget.getHull() == 0)
            {
                log.info("\t\tShip is already destroyed. Ignoring.");
                continue;
            }

            KSAttackAction currentFiringAction = new KSAttackAction(user, weapon, "single", 1);
            if(!currentFiringAction.init(battle, weapon.getId(), ammoId))
            {
                log.info("\t\tWeapon cannot attack Ship. Ignoring.");
                continue;
            }

            int possibleDamage = possibleTarget.calcPossibleDamage();
            log.info("\t\tMaximum possible damage on target: " + possibleDamage);

            int fighterDefense = currentFiringAction.getFighterDefense(battle);
            int torpedoDefense = currentFiringAction.getAntiTorpTrefferWS(possibleTarget.getTypeData(), possibleTarget);
            int hitChance = currentFiringAction.calculateTrefferWS(battle, possibleTarget.getTypeData(), fighterDefense, torpedoDefense, firingShip.getNavigationalValue(), possibleTarget.getDefensiveValue(), false);
            int currentDamage = 0;
            log.info("\t\tBase damage for weapon: " + baseDamage);
            log.info("\t\tHit chance: " + hitChance);

            for(int i = 0; i < shipWeapon.getValue(); i++)
            {
                int diceRoll = (int)(Math.random() * 100.0d);
                if(diceRoll <= hitChance)
                {
                    currentDamage += baseDamage;
                }
            }
            
            currentDamage = Math.min(currentDamage, possibleDamage);
            log.info("\t\tCalculated damage: " + currentDamage);

            int currentKillDesire = calculateKillDesirability(currentDamage);
            if(currentKillDesire > 0)
            {
                if(currentKillDesire > killDesire)
                {
                    log.info("\t\tKill desire: " + currentKillDesire + " better than current kill desire: " + killDesire + " - Switching target.");
                    killDesire = currentKillDesire;
                    firingAction = currentFiringAction;
                    target = possibleTarget.getShip();
                }
                else if(currentKillDesire == killDesire)
                {
                    if(target != null)
                    {
                        if(target.getTypeData().getSize() > ShipType.SMALL_SHIP_MAXSIZE && attackSmall)
                        {
                            log.info("\t\tKill desire: " + currentKillDesire + " equals best kill desire, but new target has preferred size - Switching target.");
                            killDesire = currentKillDesire;
                            firingAction = currentFiringAction;
                            target = possibleTarget.getShip();
                        }

                        if(target.getTypeData().getSize() <= ShipType.SMALL_SHIP_MAXSIZE && !attackSmall)
                        {
                            log.info("\t\tKill desire: " + currentKillDesire + " equals best kill desire, but new target has preferred size - Switching target.");
                            killDesire = currentKillDesire;
                            firingAction = currentFiringAction;
                            target = possibleTarget.getShip();
                        }
                    }
                }
            }
        }
        
        if(target != null)
        {
            battle.setAttackedShip(target);
        }

        return firingAction;
    }

    /**
     * Calculates how desirable it is to fire on the ship.
     *
     * @param damage The damage the weapons can do at the ship.
     * @return The desirability. Higher numbers are better.
     */
    private int calculateKillDesirability(int damage)
    {
        return damage;
    }

    private Map<Weapon, Integer> getShipWeapons(BattleShip ship)
    {
        Map<Weapon, Integer> shipWeapons = new HashMap<>();

        Map<String,String> weaponList = Weapons.parseWeaponList(ship.getTypeData().getWeapons());
        for(Map.Entry<String, String> entry: weaponList.entrySet())
        {
            Weapon weapon = Weapons.get().weapon(entry.getKey());
            shipWeapons.put(weapon, Integer.valueOf(entry.getValue()));
        }

        return shipWeapons;
    }

    private final Battle battle;
    private final Session db;

    private static final Log log = LogFactory.getLog(AutoFire.class);
}
