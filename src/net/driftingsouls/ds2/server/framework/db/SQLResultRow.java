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

import java.math.BigInteger;
import java.util.HashMap;

import net.driftingsouls.ds2.server.framework.Loggable;

/**
 * Repraesentiert eine SQL-Ergebniszeile, die nicht mehr direkt an die Datenbank
 * gekoppelt ist (Keine offenen Resourcen usw).
 * Bei <code>null</code>-Werten oder nicht vorhandenen Spalten wird ein default-Wert
 * zurueckgegeben. (Ja, dass ist vieleicht nicht so ganz schoen, vereinfacht aber so manch
 * eine Sache beim portieren)
 * @author Christopher Jung
 *
 */
public class SQLResultRow extends HashMap<String,Object> implements Loggable,Cloneable {
	private static final long serialVersionUID = 1L;

	/**
	 * Gibt den Wert einer Spalte als <code>String</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public String getString(String column) {
		Object val = get(column);
		if( val != null ) {
			return val.toString();
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte String-Spalte '"+column+"'", new Throwable());
		}
		return "";
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>int</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public int getInt(String column) {
		Object val = get(column);
		if( val != null ) {
			if( val instanceof Number ) {
				return ((Number)val).intValue();
			}
			return Integer.parseInt(val.toString());
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte int-Spalte '"+column+"'", new Throwable());
		}
		return 0;
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>boolean</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public boolean getBoolean(String column) {
		Object val = get(column);
		if( val != null ) {
			if( val instanceof Boolean ) {
				return ((Boolean)val).booleanValue();
			}
			return Boolean.parseBoolean(val.toString());
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte boolean-Spalte '"+column+"'", new Throwable());
		}
		return false;
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>long</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public long getLong(String column) {
		Object val = get(column);
		if( val != null ) {
			if( val instanceof Number ) {
				return ((Number)val).longValue();
			}
			return Long.parseLong(val.toString());
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte long-Spalte '"+column+"'", new Throwable());
		}
		return 0;
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>BitInteger</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public BigInteger getBigInteger(String column) {
		Object val = get(column);
		if( val != null ) {
			if( val instanceof BigInteger ) {
				return (BigInteger)val;
			}
			
			return new BigInteger(val.toString());
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte BigInteger-Spalte '"+column+"'", new Throwable());
		}
		return BigInteger.ZERO;
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>double</code> zurueck
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public double getDouble(String column) {
		Object val = get(column);
		if( val != null ) {
			if( val instanceof Number ) {
				return ((Number)val).doubleValue();
			}
			return Double.parseDouble(val.toString());
		}
		else if( !containsKey(column) ) {
			LOG.warn("SQLResultRow: Unbekannte double-Spalte '"+column+"'", new Throwable());
		}
		return 0d;
	}
	
	@Override
	public Object clone() {
		return super.clone();
	}
}
