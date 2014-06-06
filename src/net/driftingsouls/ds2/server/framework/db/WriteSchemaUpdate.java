package net.driftingsouls.ds2.server.framework.db;

import net.driftingsouls.ds2.server.framework.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;

public class WriteSchemaUpdate
{
	public static void main(String[] args) throws Exception
	{
		if( args.length != 2 ) {
			System.err.println("WriteSchemaUpdate <configdir> <output-file>");
			return;
		}
		Configuration.init(args[0]);

		String dbUrl = Configuration.getDbUrl();
		String dbUser = Configuration.getDbUser();
		String dbPassword = Configuration.getDbPassword();

		HibernateUtil.initConfiguration(Configuration.getConfigPath()+"hibernate.xml", dbUrl, dbUser, dbPassword);
		HibernateUtil.createFactories();

		try( Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPassword) )
		{
			HibernateUtil.writeSchemaUpdateToDisk(con, args[1]);
		}
	}
}
