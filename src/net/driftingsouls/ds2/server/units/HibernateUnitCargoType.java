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
package net.driftingsouls.ds2.server.units;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Hibernate-Typdaten fuer DS-UnitCargos.
 * <p>Hinweis: <code>null</code>-Werte werden automatisch zu leeren UnitCargos konvertiert.</p>
 *
 */
public class HibernateUnitCargoType implements UserType {
	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		if( cached == null ) {
			return null;
		}
		return new UnitCargo((String)cached);
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		if( value == null ) {
			return null;
		}
		
		return ((UnitCargo)value).clone();
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		if( value == null ) {
			return null;
		}
		UnitCargo cargo = (UnitCargo)value;
		return cargo.save();
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if( x==null)
		{
			return false;
		}
		return x.equals(y);
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		if( x == null)
		{
			return 0;
		}
		return x.hashCode();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		String value = (String)Hibernate.STRING.nullSafeGet(rs, names[0]);
		
		if( (value == null) || value.isEmpty() ) {
			return new UnitCargo();
		}
		return new UnitCargo(value);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		if( value == null ) {
			value = new UnitCargo();
		}
		Hibernate.STRING.nullSafeSet(st, ((UnitCargo)value).save(), index);
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return ((UnitCargo)original).clone();
	}
	
	@Override
	public Class<?> returnedClass() {
		return UnitCargo.class;
	}

	@Override
	public int[] sqlTypes() {
		return new int[] {java.sql.Types.VARCHAR};
	}
}
