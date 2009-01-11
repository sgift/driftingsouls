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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Das Ergebnis einer Datenbank-Query.</h1>
 * <p>Eine Datenbank-Query erlaubt es Daten aus einem Result-Set einer SQL-Query
 * abzurufen.</p>
 * Da das Ergebnis Resourcen der Datenbank belegt, sollte das Objekt nach Ermittlung
 * aller notwendigen Daten mittels {@link #free()} wieder freigegeben werden.
 * @author Christopher Jung
 *
 */
public class SQLQuery {
	private Database db;
	private ResultSet result;
	private Boolean empty = null;
	private Statement stmt;
	private boolean end = false;;
	private SQLResultRow row = null;
	private List<String> rowNames = null;
	private boolean first = true;

	protected SQLQuery( Database db, ResultSet result, Statement stmt ) {
		this.db = db;
		this.result = result;
		this.stmt = stmt;
	}
	
	/**
	 * Ermittelt die Anzahl der Ergebniszeilen der SQL-Query.
	 * Diese Operation ist Zeitaufwendig da sie nicht nativ von JDBC
	 * unterstuetzt wird.
	 * @return Anzahl der Ergebniszeilen
	 */
	public int numRows() {
		try {
			if( isEnd() ) {
				return result.getRow();
			}
			int currentRow = result.getRow();
			result.last();
			int row = result.getRow();
			if( currentRow > 0 ) {
				result.absolute(currentRow);
			}
			else {
				result.first();
				result.previous();
			}
			return row;
		}
		catch( SQLException e ) {
			db.error("Couldn't calculate total row count\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Bewegt den Zeiger auf die vorherige Ergebniszeile. Sollte es keine 
	 * vorherige Ergebniszeile geben, so wird <code>false</code> zurueckgegeben.
	 * @return <code>false</code>, falls es keine vorherige Ergebniszeile gibt
	 */
	public boolean previous() {
		try {
			return result.previous();
		}
		catch( SQLException e ) {
			db.error("Couldn't iterate to next result row\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Bewegt den Zeiger auf die naechste Ergebniszeile. Sollte es keine 
	 * naechste Ergebniszeile geben, so wird <code>false</code> zurueckgegeben.
	 * @return <code>false</code>, falls es keine naechste Ergebniszeile gibt
	 */
	public boolean next() {
		try {
			end = !result.next();
			row = null;
			return !end;
		}
		catch( SQLException e ) {
			db.error("Couldn't iterate to next result row\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>Object</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public Object get(String column) {
		try {
			return result.getObject(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch object column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>Object</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public Blob getBlob(String column) {
		try {
			return result.getBlob(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch blob column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>boolean</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public boolean getBoolean(String column) {
		try {
			return result.getBoolean(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch boolean column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>int</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public int getInt(String column) {
		try {
			return result.getInt(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch int column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>long</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public long getLong(String column) {
		try {
			return result.getLong(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch long column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>String</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public String getString(String column) {
		try {
			return result.getString(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch String column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Wert einer Spalte als <code>Double</code> zurueck.
	 * @param column Der Spaltenname
	 * @return Der Wert
	 */
	public double getDouble(String column) {
		try {
			return result.getDouble(column);
		}
		catch( SQLException e ) {
			db.error("Couldn't fetch double column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Wert einer <code>boolean</code>-Spalte auf einen neuen Wert.
	 * Die unterliegende Datenbank wird erst nach Aufruf von {@link #update()} aktuallisiert.
	 * @param column Der Spaltenname
	 * @param value Der neue Wert
	 */
	public void setBoolean( String column, boolean value ) {
		try {
			result.updateBoolean(column, value);
		}
		catch( SQLException e ) {
			db.error("Couldn't update boolean column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Wert einer <code>int</code>-Spalte auf einen neuen Wert.
	 * Die unterliegende Datenbank wird erst nach Aufruf von {@link #update()} aktuallisiert.
	 * @param column Der Spaltenname
	 * @param number Der neue Wert
	 */
	public void setInt( String column, int number ) {
		try {
			result.updateInt(column, number);
		}
		catch( SQLException e ) {
			db.error("Couldn't update int column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Wert einer <code>long</code>-Spalte auf einen neuen Wert.
	 * Die unterliegende Datenbank wird erst nach Aufruf von {@link #update()} aktuallisiert.
	 * @param column Der Spaltenname
	 * @param number Der neue Wert
	 */
	public void setLong( String column, long number ) {
		try {
			result.updateLong(column, number);
		}
		catch( SQLException e ) {
			db.error("Couldn't update long column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Wert einer <code>String</code>-Spalte auf einen neuen Wert.
	 * Die unterliegende Datenbank wird erst nach Aufruf von {@link #update()} aktuallisiert.
	 * @param column Der Spaltenname
	 * @param text Der neue Wert
	 */
	public void setString( String column, String text ) {
		try {
			result.updateString(column, text);
		}
		catch( SQLException e ) {
			db.error("Couldn't update String column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Setzt den Wert einer <code>double</code>-Spalte auf einen neuen Wert.
	 * Die unterliegende Datenbank wird erst nach Aufruf von {@link #update()} aktuallisiert.
	 * @param column Der Spaltenname
	 * @param value Der neue Wert
	 */
	public void setDouble( String column, double value ) {
		try {
			result.updateDouble(column, value);
		}
		catch( SQLException e ) {
			db.error("Couldn't update double column "+column+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Aktuallisiert die Datenbank-Zeile mit den neuen, ueber die Set-Methoden gesetzen, Werten.
	 *
	 */
	public void update() {
		try {
			result.updateRow();
		}
		catch( SQLException e ) {
			db.error("Couldn't update row\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Prueft, ob das Ergebnis-Set der Datenbank-Query leer ist.
	 * @return <code>true</code>, falls es leer ist
	 */
	public boolean isEmpty() {
		if( first && (empty == null) ) {
			empty = !next();
			previous();
			first = false;
		}
		return empty;
	}
	
	/**
	 * Prueft, ob bereits die letzte Ergebnis-Zeile ereicht wurde.
	 * @return <code>true</code>, falls dies die letzte Zeile ist
	 */
	public boolean isEnd() {
		return end;
	}
	
	/**
	 * Gibt die Datenbankresourcen der Query wieder frei.
	 * Nach Aufruf dieser Methode koennen keine weiteren Operationen
	 * auf dem Ergebnis-Set mehr ausgefuehrt werden. 
	 *
	 */
	public void free() {
		free(false);
	}
	
	/**
	 * Gibt die Datenbankresourcen der Query wieder frei.
	 * Nach Aufruf dieser Methode koennen keine weiteren Operationen 
	 * auf dem Ergebnis-Set mehr ausgefuehrt werden. 
	 * Auf Wunsch wird die zu Grunde liegende Datenbank-Query selbst nicht
	 * freigegeben (Sinnvoll bei PreparedStatements).
	 * @param preserveStatement <code>true</code>, falls die unterliegende Query nicht freigegeben werden soll
	 *
	 */
	public void free(boolean preserveStatement) {
		try {
			result.close();
			if( !preserveStatement ) {
				stmt.close();
			}
		}
		catch( SQLException e ) {
			db.error("Couldn't close result set\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Gibt den Inhalt der aktuellen Ergebniszeile zurueck.
	 * Das daraus resultierende {@link SQLResultRow}-Objekt greift nicht mehr
	 * direkt auf Datenbank-Resourcen zu. Es funktioniert folglich auch noch nach der
	 * Freigabe der Datenbank-Resourcen.<br>
	 * Es wird in jedem Fall ein Objekt zurueckgegeben.
	 * @return Die aktuelle Ergebniszeile
	 */
	public SQLResultRow getRow() {
		SQLResultRow row = new SQLResultRow();
		if( end ) {
			return row;
		}
		if( this.row != null ) {
			row.putAll(this.row);
			return row;
		}
		try {
			if( rowNames == null ) {
				ResultSetMetaData data = result.getMetaData();
				rowNames = new ArrayList<String>(data.getColumnCount());
	
				for( int i=1; i <= data.getColumnCount(); i++ ) {
					rowNames.add(data.getColumnName(i));
				}
			}
			for( int i=0; i < rowNames.size(); i++ ) {
				row.put( rowNames.get(i), result.getObject(i+1) );
			}
		}
		catch(SQLException e) {
			db.error("Couldn't fetch row data\n"+e, null);
			throw new SQLRuntimeException(e);
		}
		
		this.row = row;
		
		return row;
	}
}
