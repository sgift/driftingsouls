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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Loggable;

/**
 * Repraesentiert eine Verbindung mit der Datenbank. Eine Verbindung kann entweder inidividuell
 * oder aus einem Pool heraus erstellt werden.
 * 
 * @author Christopher Jung
 *
 */
public class Database implements Loggable {
	private Connection connection;
	private int affectedRows = 0;
	private int insertid = -1;
	private boolean tStatus = false;
	private boolean transaction = false;
	private boolean debugTransaction;
	private boolean error = false;
	private int qcount = 0;
	private boolean queryLog = false;
	private StringBuffer queryLogBuffer = null;
	
	/**
	 * Erstellt eine neue Datenbank-Verbindung aus dem Verbindungspool
	 *
	 */
	public Database() {
		try {
			connection = DBConnectionPool.getConnection();
		}
		catch( Exception e ) {
			e.printStackTrace();
			error("Unable to connect to Database via DBConnectionPool\n"+e, null);
		}
	}
	
	/**
	 * Erstellt eine neue Datenbankverbindung zu einem bestimmten Server/Datenbank
	 * @param server Der Name des Servers (z.B. localhost)
	 * @param database Der Name der Datenbank (z.B. 'ds2')
	 * @param username Der Benutzername
	 * @param password Das Passwort
	 */
	public Database( String server, String database, String username, String password ) {			
		String url = "jdbc:mysql://"+server+"/"+database+"?autoReconnect=TRUE";
		try {
			connection = DriverManager.getConnection(url, username, password);
		}
		catch( SQLException e ) {
			e.printStackTrace();
			error("Unable to connect to "+url+"\n"+e, null);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * (De)aktiviert das QueryLog
	 * @param value <code>true</code>, falls das QueryLog aktiviert werden soll
	 */
	public void setQueryLogStatus( boolean value ) {
		this.queryLog = value;
		
		if( this.queryLog && (queryLogBuffer == null) ) {
			this.queryLogBuffer = new StringBuffer();
		}
	}
	
	/**
	 * Gibt zurueck, ob das QueryLog aktiviert ist
	 * @return <code>true</code>, falls das QueryLog aktiviert ist
	 */
	public boolean getQueryLogStatus() {
		return this.queryLog;
	}
	
	/**
	 * Gibt die geloggten Queries zurueck, jeweils getrennt durch zwei Zeilenumbrueche
	 * @return Die geloggten Queries
	 */
	public String getQueryLog() {
		return this.queryLogBuffer.toString();
	}
	
	/**
	 * Loggt die angegebene Query im QueryLog
	 * @param query Die zu loggende Query
	 */
	protected void logQuery(String query) {
		if( queryLog ) {
			queryLogBuffer.append(query+"\n\n");
		}
	}
	
	/**
	 * Liefert die Anzahl der SQL-Querys, die mittels dieser Datenbankverbindung getaetigt wurden
	 * @return Anzahl der SQL-Querys
	 */
	public int getQCount() {
		return qcount;
	}
	
	/**
	 * Schliesst die Datenbankverbindung und gibt (falls die Verbindung mittels 
	 * eines DB-Pools erstellt wurde) die phyische Verbindung fuer andere wieder frei
	 */
	public void close() {
		try {
			connection.close();
		}
		catch( SQLException e ) {
			error("Unable to close sql connection\n"+e, null);
			e.printStackTrace();
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Erstellt ein neues Prepared-SQL-Statement aus der angegebenen Query.
	 * Zurueckgegeben wird die vorbereitete SQL-Query, welche dann ausgefuehrt werden kann
	 * 
	 * @param queryList Die SQL-Query
	 * @return Die vorbereitete SQL-Query
	 */
	public PreparedQuery prepare(String ... queryList) {
		StringBuilder query = new StringBuilder(50);
		for( int i=0; i < queryList.length; i++ ) {
			query.append(queryList[i]);
		}
		
		PreparedStatement stmt = null;
		try {
			stmt = connection.prepareStatement(query.toString());
			return new PreparedQuery(this, stmt, query.toString());
		}
		catch( SQLException e ) {
			error("Query faild: "+query+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Fuehrt eine Query auf der Datenbank aus. Die Query muss eine
	 * rein lesende Query sein. Schreibende Querys sollten die update-Funktion
	 * benutzen.
	 * 
	 * @param queryList Die SQL-Query. Falls es sich um mehrere Elemente handelt, werden diese zu einem String verbunden
	 * @return Das Ergebnis der SQL-Query
	 */
	public SQLQuery query( Object ... queryList ) {
		StringBuilder query = new StringBuilder(50);
		for( int i=0; i < queryList.length; i++ ) {
			query.append(queryList[i]);
		}
				
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			qcount++;
			if( this.queryLog ) {
				logQuery(query.toString());
			}
			return new SQLQuery(this, stmt.executeQuery(query.toString()), stmt);
		}
		catch( SQLException e ) {
			error("Query faild: "+query+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Fuert eine Query auf der Datenbank aus. Die Query muss die Datenbank aktuallisieren (schreiben).
	 * Ein SELECT oder vergleichbares ist nicht moeglich.
	 * 
	 * @param queryList Die SQL-Query. Falls es sich um mehrere Elemente handelt, werden diese zu einem String verbunden
	 * @return Die Anzahl der von der Query betroffenen Zeilen
	 */
	public int update( Object ... queryList ) {
		StringBuilder query = new StringBuilder(50);
		for( int i=0; i < queryList.length; i++ ) {
			query.append(queryList[i]);
		}
		
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			qcount++;
			if( this.queryLog ) {
				logQuery(query.toString());
			}
			affectedRows = stmt.executeUpdate(query.toString(), Statement.RETURN_GENERATED_KEYS);
			ResultSet genkeys = stmt.getGeneratedKeys();
			if( genkeys.next() ) {
				insertid = genkeys.getInt(1);
			}
			genkeys.close();
			return affectedRows;
		}
		catch( SQLException e ) {
			error("Update faild: "+query+"\n"+e, stmt);
			throw new SQLRuntimeException(e);
		}
		finally {
			if( stmt != null ) {
				try {
					stmt.close();
				}
				catch( SQLException e ) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Fuehrt eine Query auf der Datenbank aus. Die Query muss eine
	 * rein lesende Query sein. Schreibende Querys sollten die update-Funktion
	 * benutzen. Die erste Ergebniszeile wird anschliessend zurueckgegeben.
	 * Sollten keine Ergebnisse vorliegen, ist die Ergebniszeile leer
	 * 
	 * @param query Die SQL-Query. Falls es sich um mehrere Elemente handelt, werden diese zu einem String verbunden
	 * @return Die erste Ergebniszeile der SQL-Query.
	 */
	public SQLResultRow first( Object ... query ) {
		SQLQuery q = query(query);
		if( q == null ) {
			return new SQLResultRow();
		}
		q.next();
		SQLResultRow row = q.getRow();
		q.free();
		return row;
	}
	
	/**
	 * Liefert die Anzahl der von der vom 
	 * letzten Aufruf der Update-Methode betroffenen 
	 * Zeilen
	 * @return Die Anzahl der Zeilen
	 */
	public int affectedRows() {
		return affectedRows;
	}
	
	/**
	 * Liefert den vom SQL-Server generierten 
	 * Wert (bzw ID) der zuletzt eingefuegten Zeile
	 * mit einem auto_increment 
	 * @return die letzte eingefuegte ID/Wert
	 */
	public int insertID() {
		return insertid;
	}
	
	/**
	 * Beginnt eine Transaktion. Waehrend der Transaktion auftretende Fehler
	 * werden nicht gemeldet
	 */
	public void tBegin() {
		tBegin(false);
	}
	
	/**
	 * Beginnt eine Transaktion. Auf Wunsch werden Fehler, die zu einem Abbruch der 
	 * Transaktion fuehren, gemeldet. 
	 * @param debugtransact true, falls Fehler gemeldet werden sollen.
	 */
	public void tBegin(boolean debugtransact) {
		if( !transaction ) {
			transaction = true;
			tStatus = true;
			debugTransaction = debugtransact;
			
			update("START TRANSACTION");
		}
	}
	
	/**
	 * Fuert eine Query auf der Datenbank innerhalb einer Transaktion aus. 
	 * Die Query muss die Datenbank aktuallisieren (schreiben).
	 * Ein SELECT oder vergleichbares ist nicht moeglich.
	 * Zudem wird ueberprueft, ob die Anzahl der aktuallisierten Zeilen mit der erwarteten Anzahl uebereinstimmt.
	 * Wenn dies nicht der Fall ist schlaegt die Transaktion fehl. 
	 * Sollte die Transaktion bereits fehlgeschlagen sein, wird die Query nicht ausgefuehrt. 
	 * 
	 * @param count Die erwartete Anzahl an aktuallisierten Zeilen 
	 * @param query Die SQL-Query. Falls es sich um mehrere Elemente handelt, werden diese zu einem String verbunden
	 */
	public void tUpdate( int count, Object ... query ) {
		if( !transaction || tStatus ) {
			update(query);
			if( transaction && (affectedRows() != count) ) {
				tStatus = false;
				if( debugTransaction ) {
					LOG.warn("Transaktion fehlgeschlagen: "+Common.implode("", query));
				}
			}
		}
	}
	
	/**
	 * Fuehrt ein Rollback auf der aktuellen Transaktion aus
	 *
	 */
	public void tRollback() {
		update("ROLLBACK");
		transaction = false;
	}
	
	/**
	 * Beendet die aktuelle Transaktion. Wenn keine Fehler aufgetreten sind,
	 * werden die Aenderungen geschrieben. Wenn Fehler aufgetreten sind, wird ein 
	 * Rollback durchgefuehrt und die Transaktion war nicht erfolgreich.
	 * @return true, falls die Transaktion erfolgreich war
	 */
	public boolean tCommit() {
		if( tStatus == true ) {
			update("COMMIT");
		}
		else {
			tRollback();	
		}
		
		transaction = false;
		
		return tStatus;
	}
	
	/**
	 * Liefert die Status der letzten Transaktion zurueck.
	 * 
	 * @return true, falls der Status der letzten Transaktion "ok" war/ist
	 */
	public boolean tStatus() {
		return tStatus;
	}
	
	protected void setTStatus(boolean status) {
		tStatus = status;
	}
	
	protected void incQCount() {
		qcount++;
	}
	
	/**
	 * Escaped einen String so, dass er gefahrlos in die Datenbank geschrieben werden kann
	 * (verhindern von SQL-Injection usw).
	 * Achtung! Die Funktion kann keinen 100%-igen Schutz bieten. Sicherer ist es daher, auf
	 * Prepared-SQL-Statements zu setzen.
	 * 
	 * @param str der zu escapende String
	 * @return der escapte String
	 */
	public String prepareString( String str ) {
		StringBuilder buffer = new StringBuilder(str.length()+5);
		for( int i=0; i < str.length(); i++ ) {
			char chr = str.charAt(i);
			
			char escape = 0;
			switch( chr ) {
			case '\n':
				escape = 'n';
				break;
			case '\r':
				escape = 'r';
				break;
			case '\'':
				escape = '\'';
				break;
			case '"':
				escape = '"';
				break;
			case '\032':
				escape = 'Z';
				break;
			case '\\':
				escape = '\\';
				break;
			case 0:
				escape = '0';
				break;
			}
			if( escape != 0 ) {
				buffer.append('\\');
				buffer.append(escape);
			}
			else {
				buffer.append(chr);
			}
		}
		return buffer.toString();
	}
	
	/**
	 * Entfernt die mittels {@link #prepareString(String)} eingefuegten Escape-Zeichen wieder
	 * von einen String.
	 * 
	 * @param str Der zu bearbeitende String
	 * @return der String ohne Escapezeichen
	 */
	public String checkoutString( String str ) {
		Common.stub();
		return str;
	}
	
	protected void error( String text, Statement stmt ) {
		error = true;
		tStatus = false;
		LOG.error(text);
		try {
			if( (stmt != null) && (stmt.getWarnings() != null) ) {
				LOG.error("SQL-WARNING: "+stmt.getWarnings().getMessage());
			}
		}
		catch( SQLException e ) {
			e.printStackTrace();
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Liefert den Fehlerstatus der Datenbankverbindung
	 * @return true, falls ein Fehler aufgetreten ist
	 */
	public boolean getErrorStatus() {
		return error;
	}

	/**
	 * @return Returns the transaction.
	 */
	public boolean isTransaction() {
		return transaction;
	}

	/**
	 * @return Returns the debugTransaction.
	 */
	protected boolean isDebugTransaction() {
		return debugTransaction;
	}
}
