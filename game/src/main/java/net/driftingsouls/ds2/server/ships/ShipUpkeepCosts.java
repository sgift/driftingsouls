package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.entities.User;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class ShipUpkeepCosts
{
	private long totalShipCosts;
	private long totalUnitCosts;

	public ShipUpkeepCosts()
	{
		totalShipCosts = 0;
		totalUnitCosts = 0;
	}

	public void addShip(Ship ship) {
		totalUnitCosts += ship.getUnitBalance();
		totalShipCosts += ship.getBalance();
	}

	public boolean isTooExpensive(Ship ship, User user) {
		long tmpTotalUnitCosts = totalUnitCosts + ship.getUnitBalance();
		long tmpTotalShipCosts = totalShipCosts + ship.getBalance();

		BigInteger costs = calculateCosts(tmpTotalShipCosts, tmpTotalUnitCosts);

		return user.getKonto().compareTo(costs) < 0;
	}

	public boolean isUpkeepTooExpensive(Ship ship, User user) {
		BigInteger costs = calculateCosts(totalShipCosts + ship.getBalance(), totalUnitCosts);

		return user.getKonto().compareTo(costs) < 0;
	}

	public long calculateRemainingReWithoutPay(Ship ship, User user) {
		BigInteger costs = calculateCosts(totalShipCosts + ship.getBalance(), totalUnitCosts);

		if( user.getKonto().compareTo(costs) < 0 ) {
			return 1;
		}
		return user.getKonto().subtract(costs).longValue();
	}

	public BigInteger getTotalCosts() {
		return calculateCosts(totalShipCosts, totalUnitCosts);
	}

	// Schiffskosten = x/3 + x^3 / (73*10^7) + x^11 / (5*10^45)
	public static BigInteger calculateCosts(long totalShipCosts, long totalUnitCosts) {
		BigDecimal shipCosts = BigDecimal.valueOf(totalShipCosts).divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP)
														 .add(BigDecimal.valueOf(totalShipCosts).pow(3).divide(BigDecimal.valueOf(73).movePointRight(7), 0, RoundingMode.HALF_UP))
														 .add(BigDecimal.valueOf(totalShipCosts).pow(11).divide(BigDecimal.valueOf(5).movePointRight(45), 0, RoundingMode.HALF_UP));
		return shipCosts.add(BigDecimal.valueOf(totalUnitCosts)).toBigInteger();
	}
}
