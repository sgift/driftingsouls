/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.type.Type;

/**
 * Generiert IDs fuer die Schiffsklasse. Die IDs werden entweder von der DB generiert (falls keine ID gesetzt wurde)
 * oder via der Stored Procedure <code>newIntelliShipId</code>. Diese erhaelt als Initialwert die gesetzte ID.
 * @author Christopher Jung
 *
 */
public class ShipIdGenerator implements PostInsertIdentifierGenerator, Configurable {
	private IdentityGenerator ident;

	public synchronized Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		Ship ship = (Ship)object;
		
		if( ship.getId() > 0 ) {
			return getIntelliId( session, ship.getId() );
		}
		return IdentifierGeneratorFactory.POST_INSERT_INDICATOR;
	}
	
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
	        Dialect dialect,
	        boolean isGetGeneratedKeysEnabled) throws HibernateException {
		return ident.getInsertGeneratedIdentifierDelegate(persister, dialect, isGetGeneratedKeysEnabled);
	}

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		this.ident = new IdentityGenerator();
	}

	private int getIntelliId( SessionImplementor session, int startId ) {
		final String sql = "SELECT newIntelliShipId( ? )";
		try {
			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			st.setInt(1, startId);
			try {
				ResultSet rs = st.executeQuery();
				try {
					if ( rs.next() ) {
						return rs.getInt(1);
					}
					throw new HibernateException("Stored Procedure newIntelliShipId failed");
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
			
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
				session.getFactory().getSQLExceptionConverter(),
				sqle,
				"Stored Procedure newIntelliShipId failed",
				sql
			);
		}
	}
}
