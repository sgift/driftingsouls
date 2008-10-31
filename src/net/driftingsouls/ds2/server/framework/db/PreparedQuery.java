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

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.ArrayUtils;

/**
 * <h1>Ein PreparedStatement</h1>
 * @author Christopher Jung
 *
 */
public class PreparedQuery {
	private PreparedStatement stmt;
	private Database db;
	private String query;
	private int insertid;
	private int affectedRows;
	
	protected PreparedQuery(Database db, PreparedStatement stmt, String query) {
		this.stmt = stmt;
		this.db = db;
		this.query = query;
	}
	
	/**
	 * Fuehrt eine Query auf der Datenbank (<code>SELECT</code> usw)
	 * mit dem PreparedStatement aus 
	 * @param values Die Parameter des Statements in der Reihenfolge in der Query
	 * @return Das Ergebnis der SQL-Query
	 */
	public synchronized SQLQuery query(Object ... values) {
		try {
			if( values != null ) {
				stmt.clearParameters();
				for( int i=0; i < values.length; i++ ) {
					stmt.setObject(i+1, values[i]);
				}
			}
			db.incQCount();
			if( db.getQueryLogStatus() ) {
				db.logQuery("PS: "+query+" Parameter: "+ArrayUtils.toString(values, "null"));
			}
			return new SQLQuery(db, stmt.executeQuery(), stmt);
		}
		catch( SQLException e ) {
			db.error("Prepared Query faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}

	/**
	 * Fuehrt das Statement als Query (<code>SELECT</code> usw) 
	 * auf der Datenbank aus.
	 * @return Das Ergebnis der SQL-Query
	 */
	public SQLQuery query() {
		return query((Object[])null);
	}
	
	/**
	 * Fuehrt ein Update der Datenbank (<code>UPDATE</code>,<code>INSERT</code> usw)
	 * mit dem PreparedStatement aus. Das Update wird als Transaktion ausgefuert.
	 * Wenn die Anzahl der vom Update betroffenen Zeilen nicht mit der Erwartung uebereinstimmt,
	 * schlaegt die Transaktion fehl.
	 * @param count Die Anzahl der erwarteten betroffenen Zeilen
	 */
	public void tUpdate( int count ) {
		tUpdate(count, (Object[])null);
	}
	
	/**
	 * Fuehrt ein Update der Datenbank (<code>UPDATE</code>,<code>INSERT</code> usw)
	 * mit dem PreparedStatement aus. Das Update wird als Transaktion ausgefuert.
	 * Wenn die Anzahl der vom Update betroffenen Zeilen nicht mit der Erwartung uebereinstimmt,
	 * schlaegt die Transaktion fehl.
	 * @param count Die Anzahl der erwarteten betroffenen Zeilen
	 * @param values Die Parameter des Statements in der Reihenfolge in der Query
	 */
	public void tUpdate( int count, Object ... values ) {
		if( update(values) != count ) {
			throw new RuntimeException("Inkonsistenter DB-Status");
		}
	}
	
	/**
	 * Fuehrt das Statement als Update (<code>UPDATE</code>,<code>INSERT</code> usw) 
	 * auf der Datenbank aus und liefert die Anzahl der betroffenen Zeilen zurueck.
	 * 
	 * @return Die Anzahl der betroffenen Zeilen
	 */
	public int update() {
		return update((Object[])null);
	}
	
	/**
	 * Fuehrt ein Update der Datenbank (<code>UPDATE</code>,<code>INSERT</code> usw)
	 * mit dem PreparedStatement aus 
	 * @param values Die Parameter des Statements in der Reihenfolge in der Query
	 * @return die Anzahl der von der Datenbankveraenderung betroffenen Zeilen
	 */
	public synchronized int update(Object ... values) {
		try {
			if( values != null ) {
				stmt.clearParameters();
				for( int i=0; i < values.length; i++ ) {
					stmt.setObject(i+1, values[i]);
				}
			}
			db.incQCount();
			if( db.getQueryLogStatus() ) {
				db.logQuery("PS: "+query+" Parameter: "+ArrayUtils.toString(values, "null"));
			}
			affectedRows = stmt.executeUpdate();
			ResultSet genkeys = stmt.getGeneratedKeys();
			if( genkeys.next() ) {
				insertid = genkeys.getInt(1);
			}
			genkeys.close();
			return affectedRows;
		}
		catch( SQLException e ) {
			db.error("Prepared Update faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Fuehrt das Prepared-SQL-Statement aus und liefert die erste Zeile als
	 * Ergebnis zurueck. <b>Das Statement wird anschliessend geschlossen</b>.
	 * Wenn das Ergebnis leer ist, wird eine leere Zeile zurueckgegeben.
	 * 
	 * @return Die erste Zeile des Ergebnisses
	 */
	public SQLResultRow first() {
		return first(false, (Object[])null);
	}
	
	/**
	 * Setzt die angegebenen Werte in das Statement ein (die Reihenfolge
	 * entspricht der Nummerierung der Parameter des Statements), fuehrt dieses aus,
	 * gibt die erste Zeile zurueck und <b>schliesst anschliessend das Statement</b>.
	 * Wenn das Ergebnis leer ist, wird eine leere Zeile zurueckgegeben.
	 * 
	 * @param values Die in das Statement einzusetzenden Werte
	 * @return Die erste Zeile des Ergebnisses
	 */
	public SQLResultRow first(Object ... values) {
		return first(false, values);
	}
	
	/**
	 * Fuehrt das Prepared-SQL-Statement aus und liefert die erste Zeile als
	 * Ergebnis zurueck. <b>Das Statement wird nicht geschlossen</b>.
	 * Wenn das Ergebnis leer ist, wird eine leere Zeile zurueckgegeben.
	 * 
	 * @return Die erste Zeile des Ergebnisses
	 */
	public SQLResultRow pfirst() {
		return first(true, (Object[])null);
	}
	
	/**
	 * Setzt die angegebenen Werte in das Statement ein (die Reihenfolge
	 * entspricht der Nummerierung der Parameter des Statements), fuehrt dieses aus,
	 * gibt die erste Zeile zurueck. <b>Das Statement wird nicht geschlosssen</b>.
	 * Wenn das Ergebnis leer ist, wird eine leere Zeile zurueckgegeben.
	 * 
	 * @param values Die in das Statement einzusetzenden Werte
	 * @return Die erste Zeile des Ergebnisses
	 */
	public SQLResultRow pfirst(Object ... values) {
		return first(true, values);
	}
	
	private SQLResultRow first(boolean preserveStatement, Object ... values) {
		SQLQuery q = query(values);
		if( q == null ) {
			return new SQLResultRow();
		}
		q.next();
		SQLResultRow row = q.getRow();
		q.free(preserveStatement);
		
		return row;
	}
	
	/**
	 * Liefert die Anzahl der vom letzten Update-Befehl betroffenen Zeilen
	 * @return Die Anzahl der betroffenen Zeilen
	 */
	public int affectedRows() {
		return affectedRows;
	}
	
	/**
	 * Liefert die mittels auto_increment gesetzte ID der zuletzt eingefuegten Zeile
	 * @return Die ID
	 * @see Database#insertID()
	 */
	public int insertID() {
		return insertid;
	}
	
	/**
	 * Setzt alle Parameter der Query zurueck
	 *
	 */
	public void clear() {
		try {
			stmt.clearParameters();
		}
		catch( SQLException e ) {
			db.error("clearParameters faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setInt(int index, int value) {
		try {
			stmt.setInt(index, value);
		}
		catch( SQLException e ) {
			db.error("setInt faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setString(int index, String value) {
		try {
			stmt.setString(index, value);
		}
		catch( SQLException e ) {
			db.error("setString faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setDouble(int index, double value) {
		try {
			stmt.setDouble(index, value);
		}
		catch( SQLException e ) {
			db.error("setDouble faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setBoolean(int index, boolean value) {
		try {
			stmt.setBoolean(index, value);
		}
		catch( SQLException e ) {
			db.error("setBoolean faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setBlob(int index, Blob value) {
		try {
			stmt.setBlob(index, value);
		}
		catch( SQLException e ) {
			db.error("setBlob faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Setzt den Parameter mit dem angegebenen Index auf den angegebenen Wert
	 * @param index Der Parameter-Index
	 * @param value Der Wert
	 * @return Dieses Objekt
	 */
	public PreparedQuery setObject(int index, Object value) {
		try {
			stmt.setObject(index, value);
		}
		catch( SQLException e ) {
			db.error("setObject faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		return this;
	}
	
	/**
	 * Schliesst das Prepared-SQL-Statement.
	 * Anschliessend ist das Statement nicht mehr verwendbar!
	 *
	 */
	public void close() {
		try {
			stmt.close();
		}
		catch( SQLException e ) {
			db.error("close faild: "+stmt+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}
}
