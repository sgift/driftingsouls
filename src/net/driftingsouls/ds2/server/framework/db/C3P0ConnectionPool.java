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

import com.mchange.v2.c3p0.ComboPooledDataSource;

class C3P0ConnectionPool implements ConnectionPool {
	private ComboPooledDataSource cpds = null;
	
	public void setup(String driver, String user, String password, String url) throws Exception {
		cpds = new ComboPooledDataSource();
		cpds.setDriverClass( driver ); //loads the jdbc driver            
		cpds.setJdbcUrl( url );
		cpds.setUser(user);                                  
		cpds.setPassword(password);
		cpds.setMinPoolSize(5);                                     
		cpds.setAcquireIncrement(5);
		cpds.setMaxPoolSize(30);
		cpds.setMaxStatements(180);
		cpds.setMaxStatementsPerConnection(20);
		cpds.setPreferredTestQuery("SELECT ticks FROM config LIMIT 1");
		cpds.setIdleConnectionTestPeriod(30);
		cpds.setMaxIdleTime(600);
	}

	public Connection getConnection() throws Exception {
		return cpds.getConnection();
	}

}
