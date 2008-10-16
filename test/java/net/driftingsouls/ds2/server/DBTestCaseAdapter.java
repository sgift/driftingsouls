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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.Configuration;

import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;

/**
 * Adapter fuer Junit 3 DB-Testcases
 * @author Christopher Jung
 *
 */
class DBTestCaseAdapter extends DBTestCase {
	private DBTestable testable;
	
	/**
	 * Konstruktor
	 * @param testable Der Testcase
	 */
	public DBTestCaseAdapter(DBTestable testable) {
		this.testable = testable;
		try {
			Configuration.init("./test/cfg/");
			
			System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, "com.mysql.jdbc.Driver" );
			System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, Configuration.getSetting("db_url") );
			System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, Configuration.getSetting("db_user") );
			System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, Configuration.getSetting("db_password") );
		}
		catch( Exception e ) {
			if( e instanceof RuntimeException ) {
				throw (RuntimeException)e;
			}

			throw new RuntimeException("Fehler beim Initalisieren des Testcases", e);
		}
	}
	
	@Override
	protected DatabaseOperation getTearDownOperation() throws Exception {
		return DatabaseOperation.DELETE_ALL;
	}

	@Override
	protected IDataSet getDataSet() throws Exception {
		return testable.getDataSet();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected IDatabaseConnection getConnection() throws Exception {
		return super.getConnection();
	}
}
