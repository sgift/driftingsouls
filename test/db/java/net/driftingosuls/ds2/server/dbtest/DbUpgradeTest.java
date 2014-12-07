package net.driftingosuls.ds2.server.dbtest;

import net.driftingsouls.ds2.server.framework.db.DatabaseMigrator;
import org.junit.Test;

import java.io.IOException;

public class DbUpgradeTest
{
	@Test
	public void gegebenEineDatenbankMitDemInitialenSchema_upgrade_sollteAlleUpgradeScripteFehlerfreiAnwenden() throws IOException
	{
		// Die Verbindungsinformationen sind fest da der Testfall _nur_ in einer speziell
		// dafuer gedachten VM ausgefuert werden sollte
		new DatabaseMigrator().upgradeDatabase("jdbc:mysql://localhost:3306/ds2", "root", "12345", true);
	}
}
