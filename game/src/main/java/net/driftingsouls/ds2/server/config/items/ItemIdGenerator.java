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
package net.driftingsouls.ds2.server.config.items;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Generiert IDs fuer Items. Die IDs werden entweder von der DB generiert falls keine ID gesetzt wurde.
 * @author Christopher Jung
 *
 */
public class ItemIdGenerator implements IdentifierGenerator, Configurable {
	private String targetColumn;
	private String targetTable;
	private int maxId;


	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) {
		Item item = (Item)object;

		if( item.getID() > 0 ) {
			return item.getID();
		}
		synchronized (this) {
			int generatedMaxId = getMaxId(session);
			if( generatedMaxId < this.maxId )
			{
				generatedMaxId = this.maxId;
			}

			this.maxId = generatedMaxId+1;

			return this.maxId;
		}
	}

	private int getMaxId( SharedSessionContractImplementor session ) {
		final String sql = "SELECT max( "+this.targetColumn+" ) FROM "+this.targetTable;
		try {
			try (PreparedStatement st = session.connection().prepareStatement(sql))
			{
				try (ResultSet rs = st.executeQuery())
				{
					if (rs.next())
					{
						return rs.getInt(1);
					}
					throw new HibernateException("Konnte max(id) nicht berechnen");
				}
			}

		}
		catch (SQLException sqle) {
			SqlExceptionHelper helper = new SqlExceptionHelper(true);
			throw helper.convert(
								sqle,
								"Konnte max(id) nicht berechnen",
								sql
			);
		}
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
		this.targetColumn = params.getProperty("target_column");
		this.targetTable = params.getProperty("target_table");
	}
}
