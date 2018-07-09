package net.driftingsouls.ds2.server.framework.db;

import org.hibernate.dialect.DerbyTenSevenDialect;

import java.sql.Types;

public class FixedDerbyDialect extends DerbyTenSevenDialect
{
	public FixedDerbyDialect()
	{
		// Hibernate neigt dazu String-Lobs als normale VARCHAR-Spalten der Laenge 255 anzulegen.
		// Da String-Lobs aber explizit fuer Spalten gedacht sind mit einer Laenge groesser 255 gibt es hier zwangslaeufig Probleme.
		// Workaround: String-Lobs als CLOB anlegen.
		registerColumnType( Types.CLOB, "clob" );
	}
}
