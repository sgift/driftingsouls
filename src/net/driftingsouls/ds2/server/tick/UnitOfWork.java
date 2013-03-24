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
package net.driftingsouls.ds2.server.tick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.exception.GenericJDBCException;

/**
 * <p>Klasse zur Durchfuehrung isolierter Arbeitsaufgaben (= 1 Transaktion) im Tick.
 * Sorgt dafuer, dass das Flushing sowie die Transaktionen entsprechend behandelt werden.
 * Der Fehlerfall ist beruecksichtigt. Gesteuert werden kann die Flushgroesse (default 50).</p>
 * <p>Bei der Implementierung dieser Klasse ist die Methode {@link #doWork(Object)} zu ueberschreiben.
 * Diese Methode realisiert dabei die jeweilige Arbeitsaufgabe fuer genau ein Objekt.</p>
 * @author Christopher Jung
 * @param <T> Der Typ der Arbeitsobjekte
 *
 */
public abstract class UnitOfWork<T>
{
	private static final Logger LOG = LogManager.getLogger(UnitOfWork.class);

	private String name;
	private int flushSize = 50;
	private org.hibernate.Session db;
	
	/**
	 * Konstruktor.
	 * @param name Der Name der Arbeitsaufgabe
	 */
	public UnitOfWork(String name)
	{
		this.name = name;
		this.db = ContextMap.getContext().getDB();
	}
	
	/**
	 * Setzt die Flushgroesse, d.h. die Anzahl der abzuarbeitenden Arbeitsaufgaben
	 * bevor ein Flush durchgefuehrt wird.
	 * @param size Die Groesse
	 * @return Die Instanz
	 */
	public UnitOfWork<T> setFlushSize(int size)
	{
		this.flushSize = size;
		return this;
	}
	
	/**
	 * Gibt die aktuelle Instanz der Hibernate-Session zurueck.
	 * @return Die Session;
	 */
	public final org.hibernate.Session getDB()
	{
		return db;
	}
	
	/**
	 * Fuehrt die Verarbeitung fuer eine Menge von Objekten aus. Jedes Objekt
	 * stellt dabei eine isoliert zu verarbeitende Instanz dar.
	 * @param work Die Menge der Objekte
	 */
	public void executeFor(Collection<T> work)
	{
		FlushMode oldMode = db.getFlushMode();
		try {
			db.setFlushMode(FlushMode.MANUAL);
			Transaction transaction = db.beginTransaction();

			List<T> unflushedObjects = new ArrayList<T>();

			int count = 0;
			for( Iterator<T> iter=work.iterator(); iter.hasNext(); ) 
			{
				final T workObject = iter.next();
				if( !tryWork(db, transaction, workObject) ) {
					transaction = db.beginTransaction();
				}

				unflushedObjects.add(workObject);
				
				count++;
				if(count%this.flushSize == 0)
				{
					try {
						db.flush();
						transaction.commit();
					}
					catch( Exception e ) {
						transaction.rollback();
						
						if( e instanceof StaleObjectStateException ) {
							StaleObjectStateException sose = (StaleObjectStateException)e;
							final Object staleObject = db.get(sose.getEntityName(), sose.getIdentifier());
							db.evict(staleObject);
						}
						else if( e instanceof GenericJDBCException ) {
							GenericJDBCException ge = (GenericJDBCException)e;
							final String msg = ge.getMessage();
							try {
								// Im Falle von z.B. Locking-Fehlermeldungen versuchen die entsprechende Entity zu entfernen,
								// damit zumindest der Rest weiterlaufen kann
								if( msg != null && msg.startsWith("could not update: [") ) {
									String entity = msg.substring(msg.indexOf('[')+1, msg.length()-1);
									String id = entity.substring(entity.indexOf('#')+1);
									entity = entity.substring(0, entity.indexOf('#'));
									if( NumberUtils.isNumber(id) ) {
										Object entityObj = db.get(entity, NumberUtils.toInt(id));
										if( entityObj != null ) {
											db.evict(entityObj);
										}
									}
								}
							}
							catch( RuntimeException e2 ) {
								// Parsen der Fehlermeldung nicht moeglich - normal weiter machen 
							}
						}
						LOG.warn(name+" - on flush von "+unflushedObjects, e);
						Common.mailThrowable(e, this.name + " Exception", "on flush von "+unflushedObjects);
					}
					
					onFlushed();

					unflushedObjects.clear();

					transaction = db.beginTransaction();
				}
			}
			db.flush();
			transaction.commit();
			
			onFlushed();
		}
		finally {		
			db.setFlushMode(oldMode);
		}
	}
	
	/**
	 * Callback nach einem Flush. Kann u.a. zum Aufraeumen
	 * der Hibernate-Session eingesetzt werden.
	 */
	public void onFlushed()
	{
		// EMPTY
	}

	private boolean tryWork(org.hibernate.Session db, Transaction transaction, T workObject)
	{
		try
		{
			doWork(workObject);
		}
		catch(Exception e)
		{
			transaction.rollback();
			
			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				final Object staleObject = db.get(sose.getEntityName(), sose.getIdentifier());
				db.evict(staleObject);
			}
			
			if( db.contains(workObject) )
			{
				db.evict(workObject);
			}

			LOG.warn(name+" - Object: "+workObject, e);
			e.printStackTrace();
			Common.mailThrowable(e, name+" Exception", "Object: " + workObject);
			
			return false;
		}
		return true;
	}
	
	/**
	 * Fuehrt den isolierten Verarbeitunsschritt fuer das angegebene Objekt aus.
	 * Diese Methode ist von entsprechenden Unterklassen zu implementieren.
	 * @param object Das Objekt
	 * @throws Exception Generelle Verarbeitungsfehler, die zu einem Abbruch der Transaktion fuehren sollen
	 */
	public abstract void doWork(T object) throws Exception;
}
