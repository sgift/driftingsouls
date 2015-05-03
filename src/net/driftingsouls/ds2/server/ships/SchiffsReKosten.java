package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.entities.User;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class SchiffsReKosten
{
	// 15000^3/10000
	private static final BigDecimal DIVISOR = BigDecimal.valueOf(15000).pow(3).movePointLeft(4);
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

	public static BigInteger berecheKosten(long summeSchiffskosten, long summeEinheitenkosten) {
		BigDecimal powSchiffskosten = BigDecimal.valueOf(summeSchiffskosten).pow(3);
		return powSchiffskosten.divide(DIVISOR, 0, BigDecimal.ROUND_HALF_UP).add(BigDecimal.valueOf(summeEinheitenkosten)).toBigInteger();
	}
}
