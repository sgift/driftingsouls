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
package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

import javax.persistence.EntityManager;

/**
 * Repraesentiert einen Kontext. Bei einem Kontext handelt es sich um einen
 * Aufruf des Systems z.B. ueber HTTP. Die mit dem Aufruf verbundenen Daten koennen
 * hier abgerufen werden. Zudem kann die Ausgabe hier getaetigt werden.
 * Zudem verfuegt der Kontext noch ueber Caches fuer diverse Dinge, die direkt
 * an einen Aufruf gebunden sein muessen und nicht dauerhaft gecached werden koennen.
 *
 * @author Christopher Jung
 *
 */
public interface Context extends PermissionResolver {
	/**
	 * Gibt die passende Instanz des EntityManagers zurueck.
	 * @return Die EntityManager-Instanz
	 * @see #getDB()
	 */
    EntityManager getEM();

	/**
	 * Liefert eine Instanz der berwendeten DB-Session zurueck.
	 * @return Die DB-Session
	 */
    org.hibernate.Session getDB();

	/**
	 * Liefert den gerade aktiven User.
	 *
	 * @return Das zum gerade aktiven User gehoerende User-Objekt
	 */
    BasicUser getActiveUser();

	/**
	 * Setzt den gerade aktiven User auf das angebene User-Objekt.
	 *
	 * @param user Der neue aktive User
	 */
    void setActiveUser(BasicUser user);

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu.
	 *
	 * @param error Die Beschreibung des Fehlers
	 */
    void addError(String error);

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu und bietet zudem eine Ausweich-URL an.
	 *
	 * @param error Die Beschreibung des Fehlers
	 * @param link Die Ausweich-URL
	 */
    void addError(String error, String link);

	/**
	 * Liefert den letzten Fehler zurueck.
	 *
	 * @return Der letzte Fehlers
	 *
	 * @see #addError(String, String)
	 * @see #addError(String)
	 */
    Error getLastError();

	/**
	 * Liefert eine Liste aller Fehler zurueck.
	 *
	 * @return Eine Liste aller Fehlerbeschreibungen
	 */
    Error[] getErrorList();

	/**
	 * Liefert die Request fuer diesen Aufruf.
	 * @return Die Request des Aufrufs
	 */
    Request getRequest();

	/**
	 * Liefert die zum Aufruf gehoerende Response.
	 * @return Die Response des Aufrufs
	 */
    Response getResponse();

	/**
	 * Setzt das zum Aufruf gehoerende Response-Objekt.
	 * @param response Das Response-Objekt
	 */
    void setResponse(Response response);

	/**
	 * Liefert eine unter einem bestimmten Scope einmalige Instanz einer Klasse.
	 * Sollte keine Instanz dieser Klasse im Scope vorhanden sein,
	 * wird dieses erstellt.
	 *
	 * @param <T> Eine Klasse
	 * @param cls Die gewuenschte Klasse
	 * @return Eine Instanz der Klase
	 */
    <T> T get(Class<T> cls);

	/**
	 * Entfernt die unter einem bestimmten Scope gueltige Instanz dieser Klasse.
	 * @param cls Die Klasse
	 */
    void remove(Class<?> cls);

	/**
	 * Setzt eine Kontext-lokale Variable auf einen angegebenen Wert.
	 * @param cls Die Klasse, welche die Variable setzen moechte - fungiert als zusaetzlicher Schluessel
	 * @param varname Der Name der Variablen
	 * @param value Der neue Wert der Variablen
	 */
    void putVariable(Class<?> cls, String varname, Object value);

	/**
	 * Liefert eine Kontext-lokale Variable zurueck.
	 * @param cls Die Klasse, welche die Variable abrufen moechte - fungiert als zusaetzlicher Schluessel
	 * @param varname Der Name der Variablen
	 * @return Die Variable oder <code>null</code>, falls die Variable nicht existiert
	 */
    Object getVariable(Class<?> cls, String varname);

	/**
	 * Registriert einen Kontext-Observer im Kontextobjekt. Der Observer wird
	 * fortan ueber Ereignisse des Kontexts informiert
	 * @param listener Der Listener
	 */
    void registerListener(ContextListener listener);

	/**
	 * Setzt den vom Kontext verwendeten {@link PermissionResolver}.
	 * @param permissionResolver Der PermissionResolver
	 */
    void setPermissionResolver(PermissionResolver permissionResolver);

	/**
	 * Fuehrt ein Autowiring auf der angegebenen Beaninstanz durch. Alle in Spring
	 * hinterlegten Beans werden, sofern die entsprechende Property/Methode
	 * mit {@link org.springframework.beans.factory.annotation.Autowired} markiert ist,
	 * automatisch injiziert. Sofern die Bean als {@link org.springframework.context.ApplicationContextAware}
	 * markiert ist wird auch der {@link org.springframework.context.ApplicationContext} injiziert.
	 * @param bean Die zu verarbeitende Bean
	 */
	void autowireBean(Object bean);

	/**
	 * Ermittelt die Spring-Bean mit dem angegebenen Namen und Typ.
	 * @param cls Der Typ der Bean
	 * @param name Der Name der Bean
	 * @param <T> Der Typ der Bean
	 * @return Die Bean
	 * @throws IllegalArgumentException Falls die angegebene Bean nicht gefunden werden konnte
	 */
	<T> T getBean(Class<T> cls, String name) throws IllegalArgumentException;
}
