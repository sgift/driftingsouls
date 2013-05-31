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
package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren.
 * @author Christopher Jung
 *
 */
public abstract class DSGenerator extends Generator {
	private static final Log log = LogFactory.getLog(DSGenerator.class);

	private ActionType actionType;
	private OutputHelper actionTypeHandler;

	private boolean disableDebugOutput;
	private long startTime;
	private Map<String, String> bodyParameters;
	private Map<String, Object> parameter;
	private String subParameter;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private boolean disablePageMenu;

	/**
	 * Konstruktor.
	 * @param context Der Kontext
	 */
	public DSGenerator(Context context) {
		super(context);

		this.parameter = new HashMap<>();
		this.subParameter = "";

		parameterString("module");
		parameterString("action");

		this.startTime = System.currentTimeMillis();

		this.disableDebugOutput = false;

		this.bodyParameters = new HashMap<>();

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<>();
		this.disablePageMenu = false;

		setActionType(ActionType.DEFAULT);
	}

	private Object getParameter( String parameter ) {
		if( subParameter.equals("") ) {
			return this.parameter.get(parameter);
		}
		return this.parameter.get(subParameter+"["+parameter+"]");
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>int</code> zurueck.
	 * @param parameter Der Parametername
	 * @return Der Wert
	 */
	public int getInteger(String parameter) {
		return ((Number)getParameter(parameter)).intValue();
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>double</code> zurueck.
	 * @param parameter Der Parametername
	 * @return Der Wert
	 */
	public double getDouble(String parameter) {
		return ((Number)getParameter(parameter)).doubleValue();
	}

	/**
	 * Gibt einen als String registrierten parameter zurueck.
	 * @param parameter Der Name des Parameters
	 * @return Der Wert
	 */
	public String getString(String parameter) {
		return (String)getParameter(parameter);
	}

	/**
	 * Entfernt einen Parameter. Bei einer anschliessenden
	 * Registrierung des Parameters, ist der Wert leer.
	 * @param parameter Der Parametername
	 */
	public void unsetParameter( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		getRequest().setParameter(parameter,null);
		this.parameter.remove(parameter);
	}

	protected void parseSubParameter( String subparam ) {
		subParameter = subparam;
	}

	/**
	 * Registriert einen Parameter im System als Zahl. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 * @param parameter Der Name des Parameters
	 */
	public void parameterNumber( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		if( (getRequest().getParameter(parameter) != null) && !"".equals(getRequest().getParameter(parameter)) ) {
			String val = getRequest().getParameter(parameter);
			try {
				this.parameter.put(parameter, Common.getNumberFormat().parse(val.trim()));
			}
			catch( NumberFormatException | ParseException e ) {
				this.parameter.put(parameter, 0d);
			}
		}
		else {
			this.parameter.put(parameter, 0d);
		}
	}

	/**
	 * Registriert einen Parameter im System als String. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 * @param parameter Der Name des Parameters
	 */
	public void parameterString( String parameter ) {
		if( !subParameter.equals("") ) {
			parameter = subParameter+"["+parameter+"]";
		}
		if( getRequest().getParameter(parameter) != null ) {
			this.parameter.put(parameter, getRequest().getParameter(parameter));
		}
		else {
			this.parameter.put(parameter,"");
		}
	}

	/**
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen.
	 * @param action Der Name der Aktion
	 */
	protected void redirect( String action )
	{
		try
		{
			Method method = getMethodForAction(action);

			final Action actionDescriptor = method.getAnnotation(Action.class);
			doActionOptimizations(actionDescriptor);

			method.setAccessible(true);
			method.invoke(this);
		}
		catch( Exception e )
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Ruft die Standardaktion auf.
	 *
	 */
	protected void redirect() {
		redirect("default");
	}

	private Method getMethodForAction(String action) throws NoSuchMethodException {
		Method[] methods = getClass().getMethods();
		for (Method method : methods)
		{
			Action actionAnnotation = method.getAnnotation(Action.class);
			if (actionAnnotation == null)
			{
				continue;
			}

			if (method.getName().equals(action + "Action"))
			{
				return method;
			}

			if (method.getName().equals(action + "AjaxAct"))
			{
				return method;
			}

			if (method.getName().equals(action))
			{
				return method;
			}
		}

		throw new NoSuchMethodException();
	}

	private void prepareUrlParams(Annotation[] annotations)
	{
		for( Annotation an : annotations )
		{
			if( an instanceof UrlParam )
			{
				prepareUrlParam((UrlParam)an);
			}
			else if( an instanceof UrlParams )
			{
				for( UrlParam param : ((UrlParams)an).value() )
				{
					prepareUrlParam(param);
				}
			}
		}
	}

	private void prepareUrlParam(UrlParam an)
	{
		switch (an.type()) {

			case NUMBER:
				parameterNumber(an.name());
				break;
			case STRING:
				parameterString(an.name());
				break;
		}
	}

	@Override
	public void handleAction( String action ) throws IOException {
		if( (action == null) || action.isEmpty() ) {
			action = "default";
		}

		try {
			prepareUrlParams(this.getClass().getAnnotations());

			Method method = getMethodForAction(action);
			prepareUrlParams(method.getAnnotations());

			Action actionDescriptor = method.getAnnotation(Action.class);
			setActionType(actionDescriptor.value());

			if( (getErrorList().length != 0) || !validateAndPrepare(action) ) {
				printErrorListOnly(actionDescriptor.value());

				return;
			}

			if( actionDescriptor.value() == ActionType.DEFAULT )
			{
				printHeader( action );
			}

			doActionOptimizations(actionDescriptor);

			method.setAccessible(true);
			Object result = method.invoke(this);
			writeResultObject(result, actionDescriptor.value());
		}
		catch( NoSuchMethodException e )
		{
			log.error("", e);
			addError("Die Aktion '"+action+"' existiert nicht!");
		}
		catch( Exception e )
		{
			throw new RuntimeException(e);
		}

		parseSubParameter("");

		printErrorList(this.actionType);

		printFooter( action );
	}

	protected void writeResultObject(Object result, ActionType value) throws IOException
	{
		if( result != null )
		{
			getResponse().getWriter().append(result.toString());
		}
	}

	private void doActionOptimizations(final Action actionDescriptor)
	{
		final Session db = this.getDB();
		if( actionDescriptor.readOnly() )
		{
			// Nur lesender Zugriff -> flushes deaktivieren
			db.flush();
			db.setFlushMode(FlushMode.MANUAL);
		}
		else
		{
			db.setFlushMode(FlushMode.AUTO);
		}
	}

	protected void printErrorList(ActionType type) throws IOException
	{
		if( getErrorList().length > 0 ) {
			actionTypeHandler.printErrorList();
		}
	}

	protected void printErrorListOnly(ActionType type) throws IOException
	{
		actionTypeHandler.printHeader();

		printErrorList(type);

		actionTypeHandler.printFooter();
	}

	protected void printHeader( String action ) throws IOException {
		if( !this.disablePageMenu ) {
			actionTypeHandler.setAttribute("module", getString("module"));
			actionTypeHandler.setAttribute("pagetitle", this.pageTitle);
			actionTypeHandler.setAttribute("pagemenu", this.pageMenuEntries.toArray(new PageMenuEntry[this.pageMenuEntries.size()]));
		}

		actionTypeHandler.setAttribute("bodyParameters", this.getBodyParameters());
		actionTypeHandler.setAttribute("enableDebugOutput", !this.disableDebugOutput ? true : null);
		actionTypeHandler.setAttribute("startTime", this.startTime);
		actionTypeHandler.printHeader();
	}

	protected void printFooter( String action ) throws IOException {
		actionTypeHandler.printFooter();
	}

	/**
	 * (De)aktiviert die Debug-Ausgaben.
	 * @param value <code>true</code> zur Deaktivierung
	 */
	public void setDisableDebugOutput( boolean value ) {
		disableDebugOutput = value;
	}

	/**
	 * Setzt die Bezeichnung der aktuellen Seite.
	 * @param title Die Bezeichnung
	 */
	public void setPageTitle(String title) {
		this.pageTitle = title;
	}

	/**
	 * Fuegt dem Seitenmenue einen Eintrag hinzu.
	 * @param title Die Titel des Eintrags
	 * @param url Die URL
	 */
	public void addPageMenuEntry(String title, String url) {
		this.pageMenuEntries.add(new PageMenuEntry(title, url));
	}

	/**
	 * Setzt, ob das Seitenmenue nicht verwendet werden soll.
	 * @param value <code>true</code>, falls es nicht verwendet werden soll
	 */
	public void setDisablePageMenu(boolean value) {
		this.disablePageMenu = value;
	}

	/**
	 * Gibt weitere HTML-Body-Tag-Attribute zurueck.
	 * @return Weitere HTML-Body-Tag-Attribute
	 */
	private String getBodyParameters() {
		StringBuilder text = new StringBuilder();

		if( bodyParameters.size() > 0 ) {
			for( String key : bodyParameters.keySet() ) {
				text.append(key).append("=\"").append(bodyParameters.get(key)).append("\" ");
			}
		}

		return text.toString();
	}

	/**
	 * Fuegt ein weiteres HTML-Body-Tag-Attribut hinzu.
	 * Sollte das Attribut bereits gesetzt seit, so wird es
	 * ueberschrieben.
	 * @param parameter Der Name des Attributs
	 * @param value Der Wert
	 */
	public void addBodyParameter( String parameter, String value ) {
		bodyParameters.put(parameter,value);
	}

	protected void setActionType( ActionType type ) {
		if( type == ActionType.DEFAULT ) {
			actionTypeHandler = new HtmlOutputHelper();
		}
		else if( type == ActionType.AJAX ) {
			actionTypeHandler = new AjaxOutputHelper();
		}
		else if( type == ActionType.BINARY ) {
			actionTypeHandler = new BinaryOutputHelper();
		}

		getContext().autowireBean(actionTypeHandler);

		actionType = type;
	}

	/**
	 * Gibt den aktuellen Aktionstyp zurueck.
	 * @return Der Aktionstyp
	 */
	protected ActionType getActionType() {
		return actionType;
	}

	/**
	 * Gibt die Ausgabehilfe zurueck.
	 * @return Die Ausgabehilfe
	 */
	protected OutputHelper getOutputHelper() {
		return actionTypeHandler;
	}

	protected abstract boolean validateAndPrepare(String action);

	/**
	 * Die Default-Ajax-Aktion.
	 * @throws IOException
	 *
	 */
	public void defaultAjaxAct() throws IOException {
		defaultAction();
	}

	/**
	 * Die Default-HTML-Aktion.
	 * @throws IOException
	 *
	 */
	public void defaultAction() throws IOException {
		getResponse().getWriter().append("DEFAULT");
	}
}
