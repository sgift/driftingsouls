/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.db;

import java.sql.Connection;

/**
 * Verwaltungsklasse fuer den JDBC-Connection-Pool
 * @author Christopher Jung
 *
 */
public class DBConnectionPool {
	private static ConnectionPool ds = null;
	
	private DBConnectionPool() {
	}

	/**
	 * Initalisiert einen MySQL-Connection-Pool
	 * @param server Der MySQL-Server
	 * @param database Der Datenbank-Name
	 * @param username Der Benutzername
	 * @param password Das Passwort
	 * @throws Exception
	 */
	public static void setupMySQLPool( String server, String database, String username, String password ) throws Exception {
		ds = new C3P0ConnectionPool();
		ds.setup("com.mysql.jdbc.Driver", username, password, "jdbc:mysql://"+server+"/"+database+"?autoReconnect=TRUE");		
	}
	
	/**
	 * Gibt eine DB-Verbindung aus dem Pool zurueck
	 * @return Eine DB-Verbindung aus dem Pool
	 * @throws Exception
	 */
	public static Connection getConnection() throws Exception {
		if( ds == null ) {
			throw new Exception("DBConnectionPool wurde nicht initalisiert");
		}
		
		return ds.getConnection();
	}
}
