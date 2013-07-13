package net.driftingsouls.ds2.server.battles;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.ks.BasicKSAction;
import net.driftingsouls.ds2.server.modules.ks.KSAttackAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * Erlaubt das automatische Feuern in einer Schlacht.
 * AutoFire feuert immer einen User in einer Schlacht. Wenn beide Seiten feuern sollen werden zwei AutoFire Instanzen benoetigt.
 */
public class AutoFire
{
    public AutoFire(Battle battle)
    {
        if(battle == null)
        {
            throw new IllegalArgumentException("Battle may not be null.");
        }

        this.battle = battle;
    }

    public void fireShips()
    {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        List<BattleShip> firingShips = new ArrayList<>(battle.getShips(Side.OWN));
        List<BattleShip> attackedShips = new ArrayList<>(battle.getShips(Side.ENEMY));

        for(BattleShip firingShip: firingShips)
        {
            log.info("Firing with ship: " + firingShip.getShip().getId());
            log.info("------");
            int energy = firingShip.getShip().getEnergy();
            if(energy == 0)
            {
                continue;
            }

            Map<Weapon, Integer> shipWeapons = getShipWeapons(firingShip);
            if(shipWeapons.size() == 0)
            {
                continue;
            }

            if(firingShip.getWeapons() == 0)
            {
                continue;
            }

            int navSkill = firingShip.getNavigationalValue();
            int defensiveSkill = firingShip.getDefensiveValue();

            for(BattleShip attackedShip: attackedShips)
            {
                if(attackedShip.getHull() == 0)
                {
                    continue;
                }

                log.info("\tAttacking ship: " + attackedShip.getShip().getId());
                battle.load(battle.getCommander(0), firingShip.getShip(), attackedShip.getShip(), 0);

                Map<Integer, KSAttackAction> firingOptions = new TreeMap<>(new Comparator<Integer>()
                {
                    @Override
                    public int compare(Integer firstDamage, Integer secondDamage)
                    {
                        return (firstDamage - secondDamage) * -1;
                    }
                });
                
                for(Map.Entry<Weapon, Integer> weapon: shipWeapons.entrySet())
                {
                    if(weapon.getKey().getECost() > energy)
                    {
                        continue;
                    }

                    Set<Integer> ammos = new HashSet<>();
                    if(weapon.getKey().getAmmoType().length > 0)
                    {
                        Cargo mycargo = firingShip.getCargo();
                        List<ItemCargoEntry> itemList = mycargo.getItemsWithEffect(ItemEffect.Type.AMMO);
                        if(weapon.getKey().hasFlag(Weapon.Flags.AMMO_SELECT))
                        {
                            for(int i=0; i < itemList.size(); i++)
                            {
                                ammos.add(itemList.get(i).getItemID());
                            }
                        }
                        else
                        {
                            for(int i=0; i < itemList.size(); i++)
                            {
                                IEAmmo effect = (IEAmmo)itemList.get(i).getItemEffect();
                                if(Common.inArray(effect.getAmmo().getType(), weapon.getKey().getAmmoType()))
                                {
                                    ammos.add(effect.getAmmo().getItemId());
                                    break;
                                }
                            }
                        }
                    }

                    Set<Ammo> filteredAmmos = new HashSet<>();
                    if(ammos.isEmpty())
                    {
                        filteredAmmos.add(null);
                    }
                    else
                    {
                        for(int ammoId: ammos)
                        {
                            Iterator<Object> iterator = ContextMap.getContext().getDB().createQuery("from Ammo where itemid=:id and type in (:ammo)")
                                     .setInteger("id", ammoId)
                                     .setParameterList("ammo", weapon.getKey().getAmmoType()).iterate();
                            if(iterator.hasNext())
                            {
                                log.info("Ammo type allowed by weapon.");
                                Ammo ammo = (Ammo)iterator.next();
                                filteredAmmos.add(ammo);
                            }
                            else
                            {
                                log.info("Ammo type not allowed by weapon.");
                            }
                        }
                    }

                    for(Ammo ammo: filteredAmmos)
                    {
                        firingOptions.putAll(loadWeapon(battle.getCommander(0), weapon.getKey(), weapon.getValue(), ammo, attackedShip, firingShip, navSkill, defensiveSkill));
                    }
                }

                //Ship has no energy to fire any of its weapons
                if(firingOptions.isEmpty())
                {
                    break;
                }

                boolean destroyed = false;
                boolean damageable = true;
                for(Map.Entry<Integer, KSAttackAction> firingOption: firingOptions.entrySet())
                {
                    log.info("\t\tFiring weapon: " + firingOption.getValue().getWeaponName());
                    int possibleDamage = attackedShip.calcPossibleDamage();
                    if(possibleDamage == 0)
                    {
                        log.info("\t\tShip no longer damageable.");
                        damageable = false;
                        break;
                    }

                    boolean continueFiring = true;
                    int fired = 0;
                    while(continueFiring)
                    try
                    {
                        BasicKSAction.Result result = firingOption.getValue().attack(battle);
                        if(result != BasicKSAction.Result.OK)
                        {
                            log.info("\t\tCannot continue firing weapon: " + firingOption.getValue().getWeaponName());
                            continueFiring = false;
                            log.info("\t\tWeapon fired: " + ++fired + " times.");
                        }
                    }
                    catch (IOException e)
                    {}

                    destroyed = attackedShip.getHull() == 0;
                    if(destroyed)
                    {
                        log.info("\t\tShip has been destroyed.");
                        break;
                    }
                }

                if(destroyed)
                {
                    break;
                }
                
                if(!damageable)
                {
                    break;
                }
            }
        }
    }
    
    private Map<Integer, KSAttackAction> loadWeapon(User user, Weapon weapon, int weaponCount, Ammo ammo, BattleShip attackedShip, BattleShip firingShip, int navSkill, int defensiveSkill)
    {
        Map<Integer, KSAttackAction> firingOptions = new TreeMap<>(new Comparator<Integer>()
        {
            @Override
            public int compare(Integer firstDamage, Integer secondDamage)
            {
                return (firstDamage - secondDamage) * -1;
            }
        });

        KSAttackAction firingAction = new KSAttackAction(user, weapon, "single", 1);
        int ammoId = 0;
        if(ammo != null)
        {
            ammoId = ammo.getItemId();
        }

        if(!firingAction.init(battle, weapon.getInternalName(), ammoId))
        {
            return firingOptions;
        }

        int torpedoDefense = firingAction.getAntiTorpTrefferWS(attackedShip.getTypeData(), attackedShip);
        int fighterDefense = firingAction.getFighterDefense(battle);

        int weaponHitPercentage = firingAction.calculateTrefferWS(battle, attackedShip.getTypeData(), fighterDefense, torpedoDefense, navSkill, defensiveSkill);
        int damage = 0;
        //Ignore subsystem damage weapons for the moment
        for(int i = 0; i < weaponCount; i++)
        {
            if(ammo != null)
            {
                if(weapon.getSubDamage(firingShip.getTypeData()) <= 0)
                {
                    damage += ammo.getDamage() * weaponHitPercentage;
                }
            }
            else
            {

                if(weapon.getSubDamage(firingShip.getTypeData()) <= 0)
                {
                    damage += weapon.getBaseDamage(firingShip.getTypeData()) * weaponHitPercentage;
                }
            }
        }

        while(firingOptions.containsKey(damage) && damage > 0)
        {
            damage -= 1;
        }

        if(damage != 0)
        {
            firingOptions.put(damage, firingAction);
        }

        return firingOptions;
    }
    
    private Map<Weapon, Integer> getShipWeapons(BattleShip ship)
    {
        Map<Weapon, Integer> shipWeapons = new HashMap<>();

        Map<String,String> weaponList = Weapons.parseWeaponList(ship.getTypeData().getWeapons());
        for(Map.Entry<String, String> entry: weaponList.entrySet())
        {
            Weapon weapon = Weapons.get().weapon(entry.getKey());
            weapon.setInternalName(entry.getKey());
            shipWeapons.put(weapon, Integer.valueOf(entry.getValue()));
        }

        return shipWeapons;
    }

    private final Battle battle;

    private static final Log log = LogFactory.getLog(AutoFire.class);
}
