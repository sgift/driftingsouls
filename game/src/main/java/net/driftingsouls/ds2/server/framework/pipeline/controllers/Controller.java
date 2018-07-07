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
package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren.
 *
 * @author Christopher Jung
 */
public abstract class Controller implements PermissionResolver
{
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private Context context;

	/**
	 * Konstruktor.
	 *
	 */
	public Controller()
	{
		this.context = ContextMap.getContext();

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<>();
	}

	/**
	 * Ruft die angegebene Methode des angegebenen Objekts als verschachtelte Actionmethode (SubAction) auf.
	 *
	 * @param subparam Der Prefix fuer die URL-Parameter zwecks Schaffung eines eigenen Namensraums. Falls <code>null</code> oder Leerstring wird kein Prefix verwendet
	 * @param objekt Das Objekt dessen Methode aufgerufen werden soll
	 * @param methode Der Name der Actionmethode
	 * @param args Die zusaetzlich zu uebergebenden Argumente (haben vorrang vor URL-Parametern)
	 * @return Das Ergebnis der Methode
	 * @throws ReflectiveOperationException Falls die Reflection-Operation schief laeuft
	 */
	protected final Object rufeAlsSubActionAuf(String subparam, Object objekt, String methode, Map<String,Object> args) throws ReflectiveOperationException
	{
		return new ActionMethodInvoker().rufeAlsSubActionAuf(subparam, objekt, methode, args);
	}

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu.
	 *
	 * @param error Die Beschreibung des Fehlers
	 */
	public final void addError( String error ) {
		context.addError(error);
	}

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu und bietet zudem eine Ausweich-URL an.
	 *
	 * @param error Die Beschreibung des Fehlers
	 * @param link Die Ausweich-URL
	 */
	public final void addError( String error, String link ) {
		context.addError(error, link);
	}

	/**
	 * Liefert die Request fuer diesen Aufruf.
	 * @return Die Request des Aufrufs
	 */
	public final Response getResponse() {
		return context.getResponse();
	}

	/**
	 * Liefert die zum Aufruf gehoerende Response.
	 * @return Die Response des Aufrufs
	 */
	public final Request getRequest() {
		return context.getRequest();
	}

	/**
	 * Gibt den aktuellen Kontext zurueck.
	 * @return Der Kontext
	 */
	public final Context getContext() {
		return context;
	}

	/**
	 * Gibt die aktuelle Hibernate-Session zurueck.
	 * @return Die aktuelle Hibernate-Session
	 */
	public final Session getDB() {
		return context.getDB();
	}

	/**
	 * Gibt den aktuellen Hibernate-EntityManager zurueck.
	 * @return Der aktuelle Hibernate-EntityManager
	 */
	public final EntityManager getEM() {
		return context.getEM();
	}

	/**
	 * Gibt den aktiven User zurueck. Falls kein User eingeloggt ist
	 * wird <code>null</code> zurueckgegeben.
	 * @return Der User oder <code>null</code>
	 */
	public final BasicUser getUser() {
		return getContext().getActiveUser();
	}

	@Override
	public final boolean hasPermission(PermissionDescriptor permission)
	{
		return this.context.hasPermission(permission);
	}

	protected final void printFooter(OutputHandler handler) throws IOException
	{
		handler.setAttribute("pagetitle", this.pageTitle);
		handler.setAttribute("pagemenu", this.pageMenuEntries.toArray(new PageMenuEntry[this.pageMenuEntries.size()]));
		handler.printFooter();
	}

	/**
	 * Setzt die Bezeichnung der aktuellen Seite.
	 *
	 * @param title Die Bezeichnung
	 */
	public final void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	/**
	 * Fuegt dem Seitenmenue einen Eintrag hinzu.
	 *
	 * @param title Die Titel des Eintrags
	 * @param url Die URL
	 */
	public final void addPageMenuEntry(String title, String url)
	{
		this.pageMenuEntries.add(new PageMenuEntry(title, url));
	}

	protected boolean validateAndPrepare()
	{
		return true;
	}
}
