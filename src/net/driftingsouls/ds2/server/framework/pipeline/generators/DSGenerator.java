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
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Version;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren.
 * @author Christopher Jung
 *
 */
public abstract class DSGenerator extends Generator {
	private static final Log log = LogFactory.getLog(DSGenerator.class);

	/**
	 * Basisklasse fuer Objekte zur Ausgabe von Header, Footer und Fehlern.
	 *
	 */
	protected abstract static class OutputHelper {
		private Context context = null;
		private Map<String,Object> attributes = new HashMap<String,Object>();

		/**
		 * Konstruktor.
		 *
		 */
		public OutputHelper() {
			context = ContextMap.getContext();
		}

		/**
		 * Gibt den Header aus.
		 * @throws IOException
		 *
		 */
		public abstract void printHeader() throws IOException;
		/**
		 * Gibt den Footer aus.
		 * @throws IOException
		 *
		 */
		public abstract void printFooter() throws IOException;
		/**
		 * Gibt die Fehlerliste aus.
		 * @throws IOException
		 *
		 */
		public abstract void printErrorList() throws IOException;

		/**
		 * Setzt ein Attribut.
		 * @param key Der Schluessel
		 * @param value Der Wert
		 */
		public final void setAttribute(String key, Object value) {
			this.attributes.put(key, value);
		}

		/**
		 * Gibt das Attribut mit dem angegebenen Schluessel zurueck.
		 * @param key Der Schluessel
		 * @return Der Wert
		 */
		public final Object getAttribute(String key) {
			return this.attributes.get(key);
		}

		/**
		 * Gibt den aktuellen Kontext zurueck.
		 * @return Der Kontext
		 */
		protected final Context getContext() {
			return this.context;
		}
	}

	/**
	 * <p>Ausgabehilfe fuer HTML.</p>
	 * Attribute:
	 * <ul>
	 * <li><code>header</code> - String mit weiteren Header-Text
	 * <li><code>module</code> - Das gerade ausgefuehrte Modul
	 * <li><code>pagetitle</code> - Der Titel der Seite
	 * <li><code>pagemenu</code> - Eine Liste von Menueeintraegen fuer die Seite
	 * </ul>
	 *
	 */
	@Configurable
	protected class HtmlOutputHelper extends OutputHelper {
		private Configuration config;
		private Version version;

		/**
		 * Injiziert die DS-Konfiguration.
		 * @param config Die DS-Konfiguration
		 */
		@Autowired
		public void setConfiguration(Configuration config) {
			this.config = config;
		}

		/**
		 * Injiziert die momentane DS-Version.
		 * @param version Die DS-Version
		 */
		@Autowired
		public void setVersion(Version version) {
			this.version = version;
		}

		@Override
		public void printHeader() throws IOException {
			Response response = getContext().getResponse();

			response.setContentType("text/html", "UTF-8");

			if( getContext().getRequest().getParameterString("_style").equals("xml") ) {
				return;
			}
			Writer sb = response.getWriter();

			final boolean devMode = !"true".equals(this.config.get("PRODUCTION"));

			sb.append("<!DOCTYPE html>\n");
			sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">\n");
			sb.append("<head>\n");
			sb.append("<title>Drifting Souls 2</title>\n");
			sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
			sb.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=9\">\n");
			sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(config.get("URL")).append("data/css/ui-darkness/jquery-ui-1.8.20.css\" />\n");
			if( devMode )
			{
				appendDevModeCss(sb);
			}
			else
			{
				sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(config.get("URL")).append("data/css/v").append(version.getHgVersion()).append("/format.css\" />\n");
			}

			sb.append("<!--[if IE]>\n");
			sb.append("<style type=\"text/css\">@import url(").append(config.get("URL")).append("data/css/v").append(version.getHgVersion()).append("/format_fuer_den_dummen_ie.css);</style>\n");
			sb.append("<![endif]-->\n");

			if( this.getAttribute("header") != null ) {
				sb.append(this.getAttribute("header").toString());
			}

			sb.append("</head>\n");

			sb.append("<body ").append(getOnLoadText()).append(" ").append(getBodyParameters()).append(" >\n");
			sb.append("<input type='hidden' name='currentDsModule' id='currentDsModule' value='"+this.getAttribute("module")+"' />");

			if( devMode )
			{
				appendDevModeJavascript(sb);
			}
			else
			{
				sb.append("<script src=\"").append(config.get("URL")).append("data/javascript/v").append(version.getHgVersion()).append("/ds.js\" type=\"text/javascript\"></script>\n");
			}
			if( this.getAttribute("module") != null ) {
				sb.append("<script type=\"text/javascript\">\n");
				sb.append("<!--\n");
				sb.append("if( parent && parent.setCurrentPage ) {\n");
				sb.append("parent.setCurrentPage('" + this.getAttribute("module") + "','" + this.getAttribute("pagetitle") + "');\n");
				PageMenuEntry[] entries = (PageMenuEntry[])this.getAttribute("pagemenu");
				if( (entries != null) && (entries.length > 0) ) {
					for( int i=0; i < entries.length; i++ ) {
						sb.append("parent.addPageMenuEntry('").append(entries[i].title).append("','").append(entries[i].url.replace("&amp;", "&")).append("');");
					}
				}
				sb.append("parent.completePage();");
				sb.append("}\n");
				sb.append("// -->\n");
				sb.append("</script>\n");
			}
			sb.append("<div id=\"error-placeholder\"></div>\n");
		}

		private void appendDevModeCss(Writer sb) throws IOException
		{
			File cssdir = new File(this.config.get("ABSOLUTE_PATH")+"data/css/common");

			for( String filename : new TreeSet<String>(Arrays.asList(cssdir.list())) )
			{
				sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(config.get("URL")).append("data/css").append("/common/").append(filename).append("\" />\n");
			}
		}

		private void appendDevModeJavascript(Writer sb) throws IOException
		{
			File jsdir = new File(this.config.get("ABSOLUTE_PATH")+"data/javascript/");
			File libdir = new File(jsdir.getAbsolutePath()+"/libs");
			File commondir = new File(jsdir.getAbsolutePath()+"/common");

			sb.append("<script src=\"").append(config.get("URL")).append("data/javascript").append("/libs/jquery-1.8.2.min.js\" type=\"text/javascript\"></script>\n");
			sb.append("<script src=\"").append(config.get("URL")).append("data/javascript").append("/libs/jquery-ui-1.9.1.min.js\" type=\"text/javascript\"></script>\n");
			for( String filename : new TreeSet<String>(Arrays.asList(libdir.list())) )
			{
				if( filename.startsWith("jquery-1") || filename.startsWith("jquery-ui-1") || !filename.endsWith(".js") )
				{
					continue;
				}
				sb.append("<script src=\"").append(config.get("URL")).append("data/javascript").append("/libs/").append(filename).append("\" type=\"text/javascript\"></script>\n");
			}

			for( String filename : new TreeSet<String>(Arrays.asList(commondir.list())) )
			{
				if( !filename.endsWith(".js") )
				{
					continue;
				}
				sb.append("<script src=\"").append(config.get("URL")).append("data/javascript").append("/common/").append(filename).append("\" type=\"text/javascript\"></script>\n");
			}
			if( new File(jsdir.getAbsolutePath()+"/modules/"+this.getAttribute("module")+".js").isFile() )
			{
				sb.append("<script src=\"").append(config.get("URL")).append("data/javascript").append("/modules/").append((String)this.getAttribute("module")).append(".js\" type=\"text/javascript\"></script>\n");
			}
		}

		@Override
		public void printFooter() throws IOException
		{
			if( getContext().getRequest().getParameterString("_style").equals("xml") )
			{
				return;
			}
			Writer sb = getContext().getResponse().getWriter();
			if( !getDisableDebugOutput() )
			{
				sb.append("<div style=\"text-align:center; font-size:11px;color:#c7c7c7; font-family:arial, helvetica;\">\n");
				sb.append("<br /><br /><br />\n");
				sb.append("Execution-Time: "+(System.currentTimeMillis()-getStartTime())/1000d+"s");
				if( this.version.getBuildTime() != null )
				{
					sb.append(" -- Version: ").append(this.version.getHgVersion()).append(", ").append(this.version.getBuildTime());
				}
				//	echo "<a class=\"forschinfo\" target=\"none\" style=\"font-size:11px\" href=\"http://ds2.drifting-souls.net/mantis/\">Zum Bugtracker</a><br />\n";
				sb.append("</div>\n");
			}
			sb.append("</body>");
			sb.append("</html>");
		}

		@Override
		public void printErrorList() throws IOException {
			Writer sb = getContext().getResponse().getWriter();
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append("<div class='gfxbox' style='width:470px'>");
			sb.append("<div style=\"text-align:center; font-size:14px; font-weight:bold\">Es sind Fehler aufgetreten:</div><ul>\n");

			for( Error error : getContext().getErrorList() ) {
				if( error.getUrl() == null ) {
					sb.append("<li><span style=\"font-size:14px; color:red\">").append(error.getDescription().replaceAll("\n", "<br />")).append("</span></li>\n");
				}
				else {
					sb.append("<li><a class=\"error\" style=\"font-size:14px; font-weight:normal\" href=\"").append(error.getUrl()).append("\">").append(error.getDescription().replaceAll("\n", "<br />")).append("</a></li>\n");
				}
			}

			sb.append("</ul>\n");
			sb.append("</div>");
			sb.append("</div>\n");
			sb.append("<script type=\"text/javascript\">\n");
			sb.append("var error = document.getElementById('error-box');\n");
			sb.append("var errorMarker = document.getElementById('error-placeholder');\n");
			sb.append("error.parentNode.removeChild(error);\n");
			sb.append("errorMarker.appendChild(error);\n");
			sb.append("</script>");
		}
	}

	/**
	 * Ausgabeklasse fuer AJAX-Antworten.
	 */
	protected static class AjaxOutputHelper extends OutputHelper {
		@Override
		public void printHeader() {}
		@Override
		public void printFooter() {}
		@Override
		public void printErrorList() throws IOException {}
	}

	/**
	 * Ausgabeklasse fuer Binary-Antworten.
	 */
	protected static class BinaryOutputHelper extends OutputHelper {
		@Override
		public void printHeader() {}
		@Override
		public void printFooter() {}
		@Override
		public void printErrorList() throws IOException {
		}
	}

	private static class PageMenuEntry {
		String title;
		String url;

		PageMenuEntry(String title, String url) {
			this.title = title;
			this.url = url;
		}
	}

	private ActionType actionType;
	private OutputHelper actionTypeHandler;

	private boolean disableDebugOutput;
	private long startTime;
	private List<String> onLoadFunctions;
	private Map<String, String> bodyParameters;
	private Map<String, Object> parameter;
	private String subParameter;
	private List<String> preloadUserValues;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private boolean disablePageMenu;

	/**
	 * Konstruktor.
	 * @param context Der Kontext
	 */
	public DSGenerator(Context context) {
		super(context);

		this.parameter = new HashMap<String,Object>();
		this.subParameter = "";

		parameterString("module");
		parameterString("action");
		parameterString("_style");

		this.startTime = System.currentTimeMillis();

		this.disableDebugOutput = false;

		this.onLoadFunctions = new ArrayList<String>();
		this.bodyParameters = new HashMap<String,String>();

		this.preloadUserValues = new ArrayList<String>();
		this.preloadUserValues.add("id");

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<PageMenuEntry>();
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
			catch( NumberFormatException e ) {
				this.parameter.put(parameter, 0d);
			}
			catch( ParseException e ) {
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
		for( int i=0; i < methods.length; i++ ) {
			Action actionAnnotation = methods[i].getAnnotation(Action.class);
			if( actionAnnotation == null ) {
				continue;
			}

			if( methods[i].getName().equals(action+"Action") ) {
				return methods[i];
			}

			if( methods[i].getName().equals(action+"AjaxAct") ) {
				return methods[i];
			}

			if( methods[i].getName().equals(action) ) {
				return methods[i];
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
	 * Gibt zurueck, ob die Debugausgabe deaktiviert ist.
	 * @return <code>true</code>, falls sie deaktiviert ist
	 */
	public boolean getDisableDebugOutput() {
		return disableDebugOutput;
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
	 * Gibt den Startzeitpunkt der Verarbeitung zurueck.
	 * @return Der Startzeitpunkt der Verarbeitung
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Gibt das <code>onLoad</code>-Attribut des HTML-Body-Tags zurueck.
	 * @return Das <code>onLoad</code>-Attribut
	 */
	public String getOnLoadText() {
		if( onLoadFunctions.size() > 0 ) {
			StringBuilder sb = new StringBuilder("onLoad=\"");
			sb.append(Common.implode(" ", onLoadFunctions));
			sb.append("\"");

			return sb.toString();
		}

		return "";
	}

	/**
	 * Fuegt eine Javascript-Funktion zum <code>onLoad</code>-Aufruf des Body-Tags hinzu.
	 * @param func Der Javascript-Funktionsaufruf
	 */
	public void addOnLoadFunction( String func ) {
		onLoadFunctions.add(func);
	}

	/**
	 * Gibt weitere HTML-Body-Tag-Attribute zurueck.
	 * @return Weitere HTML-Body-Tag-Attribute
	 * @see #getOnLoadText()
	 */
	public String getBodyParameters() {
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
