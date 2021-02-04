package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.DismantlingService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    TestAppConfig.class
})
public class NebulaTest {
    @Autowired
    private ShipService shipService;
    @Autowired
    private DismantlingService dismantlingService;

    @Test
    public void damage() {
        User user = Mockito.mock(User.class);
        ShipType type = Mockito.mock(ShipType.class);
        Ship ship = new Ship(user, type, 1, 1, 1);

        ConfigService config = Mockito.mock(ConfigService.class);
        Mockito.when(config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_SHIELD)).thenReturn(10);
        Mockito.when(config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_ABLATIVE)).thenReturn(15);
        Mockito.when(config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_HULL)).thenReturn(20);
        Mockito.when(config.getValue(WellKnownConfigValue.NEBULA_DAMAGE_SUBSYSTEM)).thenReturn(30);

        Mockito.when(type.getShields()).thenReturn(10000);
        Mockito.when(type.getAblativeArmor()).thenReturn(1000);
        Mockito.when(type.getHull()).thenReturn(100);


        ship.setShields(1100);
        ship.setAblativeArmor(1000);
        ship.setHull(100);
        Nebel.Typ.DAMAGE.damageShip(ship, config, shipService, dismantlingService);

        Assert.assertEquals(100, ship.getShields());
        Assert.assertEquals(1000, ship.getAblativeArmor());
        Assert.assertEquals(100, ship.getHull());


        ship.setShields(900);
        ship.setAblativeArmor(1000);
        ship.setHull(100);
        Nebel.Typ.DAMAGE.damageShip(ship, config, shipService, dismantlingService);

        Assert.assertEquals(0, ship.getShields());
        Assert.assertEquals(985, ship.getAblativeArmor());
        Assert.assertEquals(100, ship.getHull());


        ship.setShields(900);
        ship.setAblativeArmor(10);
        ship.setHull(100);
        ship.setComm(100);
        ship.setEngine(100);
        ship.setSensors(100);
        ship.setWeapons(100);
        Nebel.Typ.DAMAGE.damageShip(ship, config, shipService, dismantlingService);

        Assert.assertEquals(0, ship.getShields());
        Assert.assertEquals(0, ship.getAblativeArmor());
        Assert.assertEquals(94, ship.getHull());
        Assert.assertTrue("Too much damage to comm", ship.getComm() > 70);
        Assert.assertTrue("Too much damage to engine", ship.getEngine() > 70);
        Assert.assertTrue("Too much damage to sensors", ship.getSensors() > 70);
        Assert.assertTrue("Too much damage to weapons", ship.getWeapons() > 70);


        Mockito.when(type.getShields()).thenReturn(0);
        Mockito.when(type.getAblativeArmor()).thenReturn(1000);
        Mockito.when(type.getHull()).thenReturn(100);

        ship.setShields(0);
        ship.setAblativeArmor(1000);
        ship.setHull(100);
        Nebel.Typ.DAMAGE.damageShip(ship, config, shipService, dismantlingService);

        Assert.assertEquals(0, ship.getShields());
        Assert.assertEquals(850, ship.getAblativeArmor());
        Assert.assertEquals(100, ship.getHull());
    }
}
