/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.battles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Testet Schlachten
 * @author Christopher Jung
 *
 */
public class BattleTest extends DriftingSoulsDBTestCase {
	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(BattleTest.class.getResourceAsStream("BattleTest.xml"));
	}
	
	private static final int ATTACKER = 1;
	private static final int DEFENDER = 4;
	
	private User user1;
	private User user2;
	private User user3;
	private User user4;
	private Map<Integer,Ship> ships = new HashMap<Integer,Ship>();
	
	/**
	 * Laedt die beiden Spieler
	 */
	@Before
	public void loadUsers() {
		org.hibernate.Session db = context.getDB();
		user1 = (User)db.get(User.class, 1);
		user2 = (User)db.get(User.class, 2);
		user3 = (User)db.get(User.class, 3);
		user4 = (User)db.get(User.class, 4);
	}
	
	/**
	 * Laedt alle Schiffe
	 */
	@Before
	public void loadShips() {
		org.hibernate.Session db = context.getDB();
		for( int i=1; i < 11; i++ ) {
			ships.put(i, (Ship)db.get(Ship.class, i));
		}
	}
	
	/**
	 * Testet das Erstellen von Schlachten
	 * TODO: Fehlerverhalten
	 * TODO: Gelandete Schiffe
	 */
	@Test
	public void testCreateBattle() {
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		
		assertThat(battle, not(nullValue()));
		
		// Alle Schiffe sollten in der Schlacht sein
		for( Ship ship : this.ships.values() ) {
			if( ship.getOwner() == this.user1 || ship.getOwner() == this.user2 ) {
				assertThat(ship.getBattle(), is(battle));
			}
			else {
				assertThat(ship.getBattle(), is(nullValue()));
			}
		}
		
		// Kommandanten pruefen
		assertThat(battle.getCommander(0), is(this.user1));
		assertThat(battle.getCommander(1), is(this.user2));
		
		// Beziehungen pruefen
		assertThat(this.user1.getRelation(this.user2.getId()), is(User.Relation.ENEMY));
		assertThat(this.user1.getRelation(this.user3.getId()), is(User.Relation.NEUTRAL));
		assertThat(this.user1.getRelation(this.user4.getId()), is(User.Relation.NEUTRAL));
		
		assertThat(this.user2.getRelation(this.user1.getId()), is(User.Relation.ENEMY));
		assertThat(this.user2.getRelation(this.user3.getId()), is(User.Relation.NEUTRAL));
		assertThat(this.user2.getRelation(this.user4.getId()), is(User.Relation.NEUTRAL));
		
		// Korrekte Schiffe ausgewaehlt?
		assertThat(battle.getOwnShip().getShip(), is(ships.get(ATTACKER)));
		assertThat(battle.getEnemyShip().getShip(), is(ships.get(DEFENDER)));
		
		// Schiffslisten korrekt gefuellt?
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(2));
		
		// Schiffe korrekt in die Schlacht eingefuegt?
		for( BattleShip bs : battle.getOwnShips() ) {
			assertThat(bs.getOwner(), is(this.user1));
			assertThat(bs.getHull(), is(bs.getShip().getHull()));
			assertThat(bs.getShields(), is(bs.getShip().getShields()));
			assertThat(bs.getAction(), is(0));
		}
		
		for( BattleShip bs : battle.getEnemyShips() ) {
			assertThat(bs.getOwner(), is(this.user2));
			assertThat(bs.getHull(), is(bs.getShip().getHull()));
			assertThat(bs.getShields(), is(bs.getShip().getShields()));
			assertThat(bs.getAction(), is(0));
		}
	}
	
	/**
	 * Testet das Erstellen von Schlachten bei Allianzspielern
	 */
	@Test
	public void testCreateBattleAllies() {
		// Setup der Allianzen
		org.hibernate.Session db = this.context.getDB();
		
		Ally ally1 = new Ally("Testally1", this.user1);
		db.persist(ally1);
		this.user1.setAlly(ally1);
		this.user3.setAlly(ally1);
		
		Ally ally2 = new Ally("Testally2", this.user2);
		db.persist(ally2);
		this.user2.setAlly(ally2);
		this.user4.setAlly(ally2);
		
		this.context.commit();
		
		// Test
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		assertThat(battle, not(nullValue()));
		
		// Alle Schiffe sollten in der Schlacht sein
		for( Ship ship : this.ships.values() ) {
			assertThat(ship.getBattle(), is(battle));
		}
		
		// Kommandanten pruefen
		assertThat(battle.getCommander(0), is(this.user1));
		assertThat(battle.getCommander(1), is(this.user2));
		
		// Beziehungen pruefen - die Beziehungen in der Allianz sind neutral, da sie
		// von keiner Methode zuvor (beim Allybeitritt) auf FRIEND gesetzt wurden
		assertThat(this.user1.getRelation(this.user2.getId()), is(User.Relation.ENEMY));
		assertThat(this.user1.getRelation(this.user3.getId()), is(User.Relation.NEUTRAL));
		assertThat(this.user1.getRelation(this.user4.getId()), is(User.Relation.ENEMY));
		
		assertThat(this.user2.getRelation(this.user1.getId()), is(User.Relation.ENEMY));
		assertThat(this.user2.getRelation(this.user3.getId()), is(User.Relation.ENEMY));
		assertThat(this.user2.getRelation(this.user4.getId()), is(User.Relation.NEUTRAL));
		
		assertThat(this.user3.getRelation(this.user1.getId()), is(User.Relation.NEUTRAL));
		assertThat(this.user3.getRelation(this.user2.getId()), is(User.Relation.ENEMY));
		assertThat(this.user3.getRelation(this.user4.getId()), is(User.Relation.ENEMY));
		
		assertThat(this.user4.getRelation(this.user1.getId()), is(User.Relation.ENEMY));
		assertThat(this.user4.getRelation(this.user2.getId()), is(User.Relation.NEUTRAL));
		assertThat(this.user4.getRelation(this.user3.getId()), is(User.Relation.ENEMY));
		
		// Korrekte Schiffe ausgewaehlt?
		assertThat(battle.getOwnShip().getShip(), is(ships.get(ATTACKER)));
		assertThat(battle.getEnemyShip().getShip(), is(ships.get(DEFENDER)));
		
		// Schiffslisten korrekt gefuellt?
		assertThat(battle.getOwnShips().size(), is(5));
		assertThat(battle.getEnemyShips().size(), is(5));
		
		// Schiffe korrekt in die Schlacht eingefuegt?
		for( BattleShip bs : battle.getOwnShips() ) {
			assertThat(bs.getOwner(), anyOf(is(this.user1), is(this.user3)));
			assertThat(bs.getHull(), is(bs.getShip().getHull()));
			assertThat(bs.getShields(), is(bs.getShip().getShields()));
			assertThat(bs.getAction(), is(0));
		}
		
		for( BattleShip bs : battle.getEnemyShips() ) {
			assertThat(bs.getOwner(), anyOf(is(this.user2), is(this.user4)));
			assertThat(bs.getHull(), is(bs.getShip().getHull()));
			assertThat(bs.getShields(), is(bs.getShip().getShields()));
			assertThat(bs.getAction(), is(0));
		}
	}
	
	/**
	 * Testet, ob beim Rundenwechsel die Waffen korrekt entsperrt werden
	 */
	@Test
	public void testEndTurnUnblockWeapons() {
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		for( BattleShip bs : battle.getOwnShips() ) {
			bs.setAction(Battle.BS_BLOCK_WEAPONS);
		}
		for( BattleShip bs : battle.getEnemyShips() ) {
			bs.setAction(Battle.BS_BLOCK_WEAPONS);
		}
		
		battle.endTurn(false);
		
		for( BattleShip bs : battle.getOwnShips() ) {
			assertThat(bs.getAction(), is(0));
		}
		for( BattleShip bs : battle.getEnemyShips() ) {
			assertThat(bs.getAction(), is(0));
		}
	}
	
	/**
	 * Testet, ob alle zerstoerten Schiffe korrekt am Ende der Runde entfernt werden
	 */
	@Test
	public void testEndTurnRemoveDestroyed() {
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		// Alle bis auf eines zerstoeren
		for( int i=0; i < battle.getOwnShips().size()-1; i++ ) {
			battle.getOwnShips().get(i).setAction(Battle.BS_DESTROYED);
		}
		BattleShip surviving = battle.getOwnShips().get(battle.getOwnShips().size()-1);
		
		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(true));
		assertThat(battle.getOwnShips().size(), is(1));
		assertThat(battle.getOwnShips().get(0), is(battle.getOwnShip()));
		assertThat(battle.getOwnShip(), is(surviving));
		assertThat(battle.getEnemyShips().size(), is(2));
		
		long count = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user1")
			.setEntity("user1", user1)
			.iterate().next();
		
		assertThat(count, is(1L));
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
	}
	
	/**
	 * Testet, ob alle zerstoerten Schiffe korrekt am Ende der Runde entfernt werden
	 */
	@Test
	public void testEndTurnRemoveDestroyedWithDocked() {
		Ship ship = new Ship(this.user1, (ShipType)context.getDB().get(ShipType.class, 2), 1, 1, 1);
		ship.setName("Testjaeger");
		context.getDB().persist(ship);
		Ship masterShip = (Ship)context.getDB().get(Ship.class, 1);
		masterShip.setStartFighters(false);
		assertThat(masterShip.land(ship), is(false));
		context.commit();
		
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		// Alle bis auf eines zerstoeren
		for( BattleShip bship : battle.getOwnShips() )
		{
			if( bship.getShip().getId() == 1 || "l 1".equals(bship.getShip().getDocked()) )
			{
				bship.setAction(Battle.BS_DESTROYED);
			}
		}
		assertThat(battle.getOwnShips().size(), is(4));

		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(true));
		assertThat(battle.getOwnShips().size(), is(2));
		assertThat(battle.getEnemyShips().size(), is(2));
		
		long count = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user1")
			.setEntity("user1", user1)
			.iterate().next();
		
		assertThat(count, is(2L));
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
	}
	
	/**
	 * Testet, ob am Ende einer Runde eine Flotte korrekt entfernt wird, wenn alle ihre
	 * Schiffe zerstoert wurden
	 */
	@Test
	public void testEndTurnDestroyFleet()
	{
		ShipFleet fleet = new ShipFleet("Testflotte");
		context.getDB().persist(fleet);
		boolean first = true;
		for( Ship ship : this.ships.values() )
		{
			if( ship.getOwner() != this.user1 )
			{
				continue;
			}
			if( first )
			{
				first = false;
				continue;
			}
			fleet.addShip(ship);
		}
		context.commit();
		
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		
		// Alle bis auf eines zerstoeren
		BattleShip surviving = null;
		for( int i=0; i < battle.getOwnShips().size(); i++ )
		{
			if( battle.getOwnShips().get(i).getShip().getFleet() != null )
			{
				battle.getOwnShips().get(i).setAction(Battle.BS_DESTROYED);
			}
			else
			{
				surviving = battle.getOwnShips().get(i);
			}
		}
	
		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(true));
		assertThat(battle.getOwnShips().size(), is(1));
		assertThat(battle.getOwnShips().get(0), is(battle.getOwnShip()));
		assertThat(battle.getOwnShip(), is(surviving));
		assertThat(battle.getEnemyShips().size(), is(2));
		
		long count = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user1")
			.setEntity("user1", user1)
			.iterate().next();
		
		assertThat(count, is(1L));
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
		
		long fleetCount = (Long)context.getDB().createQuery("select count(*) from ShipFleet")
			.iterate().next();
		assertThat(fleetCount, is(0L));
	}
	
	/**
	 * Testet, ob eine Schlacht korrekt beendet, wenn auf der eigenen Seite alle
	 * Schiffe zerstoert wurden.
	 */
	@Test
	public void testEndTurnRemoveDestroyedAndEndBattle() {
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		// Alle bis auf eines zerstoeren
		for( int i=0; i < battle.getOwnShips().size(); i++ ) {
			battle.getOwnShips().get(i).setAction(Battle.BS_DESTROYED);
		}
		
		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(false));
		assertThat(0, is(battle.getOwnShips().size()));
		assertThat(0, is(battle.getEnemyShips().size()));
		
		assertThat((short)1, is(this.user1.getLostBattles()));
		assertThat((short)0, is(this.user1.getWonBattles()));
		
		assertThat((short)0, is(this.user2.getLostBattles()));
		assertThat((short)1, is(this.user2.getWonBattles()));
		
		long count = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user1")
			.setEntity("user1", user1)
			.iterate().next();
		
		assertThat(count, is(0L));
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2 and battle is null")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
		
		long battleCount = (Long)context.getDB().createQuery("select count(*) from Battle")
			.iterate().next();
		assertThat(battleCount, is(0L));
	}
	
	/**
	 * Testet, ob eine Schlacht korrekt beendet, wenn auf der Gegnerseite das letzte
	 * Schiff zerstoert wurden.
	 */
	@Test
	public void testEndTurnRemoveDestroyedLastShipAndEndBattle()
	{
		boolean first = true;
		for( Ship ship : this.ships.values() )
		{
			if( ship.getOwner() != this.user1 )
			{
				continue;
			}
			if( first )
			{
				first = false;
				continue;
			}
			ship.destroy();
		}
		Battle battle = Battle.create(user2.getId(), DEFENDER, ATTACKER);
		assertThat(battle.getEnemyShips().size(), is(1));
		
		// Alle bis auf eines zerstoeren
		for( int i=0; i < battle.getEnemyShips().size(); i++ )
		{
			battle.getEnemyShips().get(i).setAction(Battle.BS_DESTROYED);
		}
		
		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(false));
		assertThat(0, is(battle.getOwnShips().size()));
		assertThat(0, is(battle.getEnemyShips().size()));
		
		assertThat((short)1, is(this.user1.getLostBattles()));
		assertThat((short)0, is(this.user1.getWonBattles()));
		
		assertThat((short)0, is(this.user2.getLostBattles()));
		assertThat((short)1, is(this.user2.getWonBattles()));
		
		long count = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user1")
			.setEntity("user1", user1)
			.iterate().next();
		
		assertThat(count, is(0L));
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2 and battle is null")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
		
		long battleCount = (Long)context.getDB().createQuery("select count(*) from Battle")
			.iterate().next();
		assertThat(battleCount, is(0L));
	}
	
	/**
	 * Testet, ob alle fluechtenden Schiffe korrekt am Ende der Runde entfernt werden
	 */
	@Test
	public void testEndTurnRemoveEscaping() {
		Battle battle = Battle.create(user1.getId(), ATTACKER, DEFENDER);
		// Alle bis auf eines zerstoeren
		for( int i=0; i < battle.getOwnShips().size()-1; i++ ) {
			battle.getOwnShips().get(i).setAction(Battle.BS_FLUCHT);
		}
		BattleShip surviving = battle.getOwnShips().get(battle.getOwnShips().size()-1);
		
		boolean exists = battle.endTurn(false);
		
		assertThat(exists, is(true));
		assertThat(battle.getOwnShips().size(), is(1));
		assertThat(battle.getOwnShips().get(0), is(battle.getOwnShip()));
		assertThat(battle.getOwnShip(), is(surviving));
		assertThat(battle.getEnemyShips().size(), is(2));
		
		List<Ship> ships = Common.cast(context.getDB().createQuery("from Ship where owner= :user1")
			.setEntity("user1", user1)
			.list());
		
		assertThat(ships.size(), is(3));
		for( Ship ship : ships ) {
			if( ship == surviving.getShip() ) {
				continue;
			}
			assertThat(ship.getBattle(), is(nullValue()));
			assertThat(ship.getLocation().getSystem(), is(battle.getLocation().getSystem()));
		}
		
		long count2 = (Long)context.getDB().createQuery("select count(*) from Ship where owner= :user2")
			.setEntity("user2", user2)
			.iterate().next();
		
		assertThat(count2, is(2L));
	}
}
