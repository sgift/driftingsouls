package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.entities.User;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class SchiffsReKosten
{
	private long summeSchiffskosten;
	private long summeEinheitenkosten;

	public SchiffsReKosten()
	{
		summeSchiffskosten = 0;
		summeEinheitenkosten = 0;
	}

	public void verbucheSchiff(Ship ship) {
		summeEinheitenkosten += ship.getUnitBalance();
		summeSchiffskosten += ship.getBalance();
	}

	public boolean isKostenZuHoch(Ship ship, User user) {
		long tmpSummeEinheitenkosten = summeEinheitenkosten + ship.getUnitBalance();
		long tmpSummeSchiffskosten = summeSchiffskosten + ship.getBalance();

		BigInteger kosten = berecheKosten(tmpSummeSchiffskosten, tmpSummeEinheitenkosten);

		return user.getKonto().compareTo(kosten) < 0;
	}

	public boolean isWartungsKostenZuHoch(Ship ship, User user) {
		BigInteger kosten = berecheKosten(summeSchiffskosten + ship.getBalance(), summeEinheitenkosten);

		return user.getKonto().compareTo(kosten) < 0;
	}

	public long berechneVerbleibendeReOhneSold(Ship ship, User user) {
		BigInteger kosten = berecheKosten(summeSchiffskosten + ship.getBalance(), summeEinheitenkosten);

		if( user.getKonto().compareTo(kosten) < 0 ) {
			return 1;
		}
		return user.getKonto().subtract(kosten).longValue();
	}

	public BigInteger getGesamtkosten() {
		return berecheKosten(summeSchiffskosten, summeEinheitenkosten);
	}

	// Schiffskosten = x/3 + x^3 / (73*10^7) + x^11 / (5*10^45)
	public static BigInteger berecheKosten(long summeSchiffskosten, long summeEinheitenkosten) {
		BigDecimal Schiffskosten = BigDecimal.valueOf(summeSchiffskosten).divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP)
														 .add(BigDecimal.valueOf(summeSchiffskosten).pow(3).divide(BigDecimal.valueOf(73).movePointRight(7), 0, RoundingMode.HALF_UP))
														 .add(BigDecimal.valueOf(summeSchiffskosten).pow(11).divide(BigDecimal.valueOf(5).movePointRight(45), 0, RoundingMode.HALF_UP));
		return Schiffskosten.add(BigDecimal.valueOf(summeEinheitenkosten)).toBigInteger();
	}
}
