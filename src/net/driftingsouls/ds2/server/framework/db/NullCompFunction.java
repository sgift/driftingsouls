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
package net.driftingsouls.ds2.server.framework.db;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * <p>Vergleicht ob zwei Werte gleich sind unter Beruecksichtigung der <code>null</code>. Der Rueckgabewert
 * ist ein <code>Boolean</code>.</p>
 * <p>Syntax: <code>ncp(<i>&lt;zu pruefende Spalte&gt;</i>, <i>&lt;Wert der Spalte&gt;</i>)</code></p> 
 * @author Christopher Jung
 *
 */
// Wer auch immer einen besseren Weg findet: Nur zu...
public class NullCompFunction implements SQLFunction {
	private static String NULL_VALUE = NullCompFunction.class.getName()+"#render()#null-value";
	
	@Override
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.BOOLEAN;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	@Override
	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		final String param = (String)args.get(0);
		final String value = (String)args.get(1);
		return "case ifnull("+value+",'"+NULL_VALUE+"') when '"+NULL_VALUE+"' then "+param+" is null when "+param+" then 1 else 0 end";
	}

}
