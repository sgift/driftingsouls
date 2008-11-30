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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren
 * @author Christopher Jung
 *
 */
public abstract class DSGenerator extends Generator {
	private static final Log log = LogFactory.getLog(DSGenerator.class);
	
	protected static abstract class OutputHelper {
		private Context context = null;
		private Map<String,Object> attributes = new HashMap<String,Object>();
		
		/**
		 * Konstruktor
		 *
		 */
		public OutputHelper() {
			context = ContextMap.getContext();
		}
		
		/**
		 * Gibt den Header aus
		 * @throws IOException 
		 *
		 */
		public abstract void printHeader() throws IOException;
		/**
		 * Gibt den Footer aus
		 * @throws IOException 
		 *
		 */
		public abstract void printFooter() throws IOException;
		/**
		 * Gibt die Fehlerliste aus
		 * @throws IOException 
		 *
		 */
		public abstract void printErrorList() throws IOException;
		
		/**
		 * Setzt ein Attribut
		 * @param key Der Schluessel
		 * @param value Der Wert
		 */
		public final void setAttribute(String key, Object value) {
			this.attributes.put(key, value);
		}
		
		/**
		 * Gibt das Attribut mit dem angegebenen Schluessel zurueck
		 * @param key Der Schluessel
		 * @return Der Wert
		 */
		public final Object getAttribute(String key) {
			return this.attributes.get(key);
		}
		
		/**
		 * Gibt den aktuellen Kontext zurueck
		 * @return Der Kontext
		 */
		protected final Context getContext() {
			return this.context;
		}
	}
	
	/**
	 * <p>Ausgabehilfe fuer HTML</p>
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
		
		/**
		 * Injiziert die DS-Konfiguration
		 * @param config Die DS-Konfiguration
		 */
		@Autowired
		public void setConfiguration(Configuration config) {
			this.config = config;
		}
		
		@Override
		public void printHeader() throws IOException {
			Response response = getContext().getResponse();
			
			response.setContentType("text/html", "UTF-8");
			
			if( getContext().getRequest().getParameterString("_style").equals("xml") ) {
				return;
			}
			Writer sb = response.getWriter();
			String url = config.get("URL")+"/";
			boolean usegfxpak = false;
			final BasicUser user = getContext().getActiveUser();
			if( user != null ) {
				if( !user.getUserImagePath().equals(BasicUser.getDefaultImagePath()) ) {
					usegfxpak = true;
				}
				url = user.getImagePath();
			}
			sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
			sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\" lang=\"de\">\n");
			sb.append("<head>\n");
			sb.append("<title>Drifting Souls 2</title>\n");
			sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
			if( !getDisableDefaultCSS() ) { 
				sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+config.get("URL")+"format.css\" />\n");
			}
			sb.append("<!--[if IE]>\n");
			sb.append("<style type=\"text/css\">@import url("+config.get("URL")+"format_fuer_den_dummen_ie.css);</style>\n");
			sb.append("<![endif]-->\n");

			if( this.getAttribute("header") != null ) {
				sb.append(this.getAttribute("header").toString());
			}
			
			sb.append("</head>\n");
			sb.append("<body "+getOnLoadText()+" "+getBodyParameters()+" >\n");
			sb.append("<div id=\"overDiv\" style=\"position:absolute; visibility:hidden; z-index:1000;\"></div>\n");
			sb.append("<script type=\"text/javascript\" src=\""+url+"data/javascript/overlibmws.js\"><!-- overLIB (c) Erik Bosrup -->\n");
			sb.append("</script>");
			if( usegfxpak ) {
				sb.append("<script src=\""+url+"data/javascript/gfxpakversion.js\" type=\"text/javascript\"></script>\n");
			}
			sb.append("<script src=\""+url+"data/javascript/prototype.js\" type=\"text/javascript\"></script>\n");
			sb.append("<script src=\""+url+"data/javascript/scriptaculous.js\" type=\"text/javascript\"></script>\n");
			sb.append("<script type=\"text/javascript\">\n");
			sb.append("<!--\n");
			sb.append("OLpageDefaults(TEXTPADDING,0,TEXTFONTCLASS,'tooltip',FGCLASS,'tooltip',BGCLASS,'tooltip');");
			sb.append("function ask(text,url) {\n");
			sb.append("if( confirm(text) ) {\n");
			sb.append("window.location.href = url;\n");
			sb.append("}\n");
			sb.append("}\n");
			sb.append("function getDsUrl() {\n");
			sb.append("var url = location.href;\n");
			sb.append("if( url.indexOf('?') > -1 ) {\n");
			sb.append("url = url.substring(0,url.indexOf('?'));\n");
			sb.append("}\n");
			sb.append("return url;\n");
			sb.append("}\n");
			
			if( this.getAttribute("module") != null ) {
				sb.append("if( parent && parent.setCurrentPage ) {\n");
				sb.append("parent.setCurrentPage('"+this.getAttribute("module")+"','"+this.getAttribute("pagetitle")+"');\n");
				PageMenuEntry[] entries = (PageMenuEntry[])this.getAttribute("pagemenu");
				if( (entries != null) && (entries.length > 0) ) {
					for( int i=0; i < entries.length; i++ ) {
						sb.append("parent.addPageMenuEntry('"+entries[i].title+"','"+entries[i].url.replace("&amp;", "&")+"');");
					}
				}
				sb.append("parent.completePage();");
				sb.append("}\n");
			}

			sb.append("// -->\n");
			sb.append("</script>\n");
			sb.append("<div id=\"error-placeholder\" />\n");
		}
		
		@Override
		public void printFooter() throws IOException {
			if( getContext().getRequest().getParameterString("_style").equals("xml") ) {
				return;
			}
			Writer sb = getContext().getResponse().getWriter();
			if( !getDisableDebugOutput() ) {
				sb.append("<div style=\"text-align:center; font-size:11px;color:#c7c7c7; font-family:arial, helvetica;\">\n");
				sb.append("<br /><br /><br />\n");
				sb.append("Execution-Time: "+(System.currentTimeMillis()-getStartTime())/1000d+"s<br />\n");
				//echo "<a class=\"forschinfo\" target=\"none\" style=\"font-size:11px\" href=\"http://ds2.drifting-souls.net/mantis/\">Zum Bugtracker</a><br />\n";
				sb.append("</div>\n");
			}
			sb.append("</body>");
			sb.append("</html>");
		}

		@Override
		public void printErrorList() throws IOException {
			Writer sb = getContext().getResponse().getWriter();
			sb.append("<div id=\"error-box\" align=\"center\">\n");
			sb.append(Common.tableBegin(430,"left"));
			sb.append("<div style=\"text-align:center; font-size:14px; font-weight:bold\">Es sind Fehler aufgetreten:</div><ul>\n");
					
			for( Error error : getContext().getErrorList() ) {
				if( error.getUrl() == null ) {
					sb.append("<li><span style=\"font-size:14px; color:red\">"+error.getDescription().replaceAll("\n","<br />")+"</span></li>\n");
				}
				else {
					sb.append("<li><a class=\"error\" style=\"font-size:14px; font-weight:normal\" href=\""+error.getUrl()+"\">"+error.getDescription().replaceAll("\n","<br />")+"</a></li>\n");
				}
			}
					
			sb.append("</ul>\n");
			sb.append(Common.tableEnd());
			sb.append("</div>\n");
			sb.append("<script type=\"text/javascript\">\n");
			sb.append("var error = document.getElementById('error-box');\n");
			sb.append("var errorMarker = document.getElementById('error-placeholder');\n");
			sb.append("error.parentNode.removeChild(error);\n");
			sb.append("errorMarker.appendChild(error);\n");
			sb.append("</script>");
		}
	}
	
	protected static class AjaxOutputHelper extends OutputHelper {
		@Override
		public void printHeader() {}
		@Override
		public void printFooter() {}
		@Override
		public void printErrorList() throws IOException {
			Writer sb = getContext().getResponse().getWriter();

			for( Error error : getContext().getErrorList() ) {
				sb.append("ERROR: "+error.getDescription().replaceAll("\n"," ")+"\n");
			}
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
	
	private boolean disableDefaultCSS;
	private boolean disableDebugOutput;
	private long startTime;
	private boolean requireValidSession;
	private List<String> onLoadFunctions;
	private Map<String, String> bodyParameters;
	private Map<String, Object> parameter;
	private String subParameter;
	private List<String> preloadUserValues;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private boolean disablePageMenu;
	
	/**
	 * Konstruktor
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
		this.requireValidSession = true;
		this.disableDefaultCSS = false;
		
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
	 * Gibt einen als String registrierten parameter zurueck
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
				addError("Parameter "+parameter+" ist keine g&uuml;ltige Zahl");
				this.parameter.put(parameter, 0d);
			}
			catch( ParseException e ) {
				addError("Parameter "+parameter+" ist keine g&uuml;ltige Zahl");
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
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen 
	 * @param action Der Name der Aktion
	 */
	protected void redirect( String action ) {
		String callAction = action + actionType.getActionExt();
		try {
			Method method = getClass().getMethod(callAction);
			method.setAccessible(true);
			method.invoke(this);
		}
		catch( Exception e ) {
			log.error("Es ist ein Fehler beim Aufruf der Action '"+action+"' aufgetreten", e);
			addError("Es ist ein Fehler beim Aufruf der Action '"+action+"' aufgetreten:\n"+e.toString());
			
			Common.mailThrowable(e, "DSGenerator Invocation Target Exception", 
					"Redirect-Action: "+action+"\n" +
					"ActionType: "+actionType+"\n" +
					"User: "+(getContext().getActiveUser() != null ? getContext().getActiveUser().getId() : "none")+"\n" +
					"Query-String: "+getContext().getRequest().getQueryString());
			
			getContext().rollback();
		}
	}
	
	/**
	 * Ruft die Standardaktion auf
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
	
	@Override
	public void handleAction( String action ) throws IOException {
		if( (action == null) || action.isEmpty() ) {
			action = "default";
		}
			
		// Ungueltige Sessions brauchen nicht extra abgefangen zu werden,
		// da fuer diese Bereits ein Fehler eingetragen wurde
		if( requireValidSession && (getContext().getActiveUser() == null) ) {
			addError( "Sie muessen sich einloggen um die Aktion durchfuehren zu koennen" );
		}
		
		if( getErrorList().length != 0 ) {
			printErrorListOnly();
			
			return;
		}
	
		try {
			Method method = getMethodForAction(action);
			setActionType(method.getAnnotation(Action.class).value());
			
			if( (getErrorList().length != 0) || !validateAndPrepare(action) ) {
				printErrorListOnly();
				
				return;
			}
			
			printHeader( action );
			
			method.setAccessible(true);
			method.invoke(this);
		}
		catch( InvocationTargetException e ) {
			Throwable t = e.getCause();
			log.error("", t);
			StackTraceElement[] st = t.getStackTrace();
			String stacktrace = "";
			for( StackTraceElement s : st ) {
				stacktrace += s.toString()+"\n";
			}
			
			if( t instanceof LockAcquisitionException ) {
				addError("Die gew&uuml;nschte Aktion konnte nicht erfolgreich durchgef&uuml;hrt werden. Bitte versuchen sie es erneut.");
			}
			else if( t instanceof StaleObjectStateException ) {
				addError("Die gew&uuml;nschte Aktion konnte nicht erfolgreich durchgef&uuml;hrt werden. Bitte versuchen sie es erneut.");
			}
			else if( (t instanceof GenericJDBCException) && 
					(((GenericJDBCException)t).getSQLException().getMessage() != null) &&
					((GenericJDBCException)t).getSQLException().getMessage().startsWith("Beim Warten auf eine Sperre wurde die") ) {
				addError("Die gew&uuml;nschte Aktion konnte nicht erfolgreich durchgef&uuml;hrt werden. Bitte versuchen sie es sp&auml;ter erneut.");
			}
			else {
				addError("Es ist ein Fehler in der Action '"+action+"' aufgetreten:\n"+t.toString()+"\n\n"+stacktrace);
			}
			
			Common.mailThrowable(e, "DSGenerator Invocation Target Exception", 
					"Action: "+action+"\n" +
					"ActionType: "+actionType+"\n" +
					"User: "+(getContext().getActiveUser() != null ? getContext().getActiveUser().getId() : "none")+"\n" +
					"Query-String: "+getContext().getRequest().getQueryString());
			
			getContext().rollback();
		}
		catch( NoSuchMethodException e ) {
			log.error("", e);
			addError("Die Aktion '"+action+"' existiert nicht!");
		}
		catch( Exception e ) {
			log.error("", e);
			addError("Es ist ein Fehler beim Aufruf der Action '"+action+"' aufgetreten:\n"+e.toString());
			Common.mailThrowable(e, "DSGenerator Exception", 
					"Action: "+action+"\n" +
					"ActionType: "+actionType+"\n"+
					"User: "+(getContext().getActiveUser() != null ? getContext().getActiveUser().getId() : "none")+"\n" +
					"Query-String: "+getContext().getRequest().getQueryString());
			getContext().rollback();
		}
		
		parseSubParameter("");
		
		printErrorList();
			
		printFooter( action );
	}

	protected void printErrorList() throws IOException
	{
		if( getErrorList().length > 0 ) {
			actionTypeHandler.printErrorList();
		}
	}

	private void printErrorListOnly() throws IOException
	{
		actionTypeHandler.printHeader();
		
		printErrorList();
		
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
	 * Gibt an, ob fuer die Ausfuehrung einer Aktion eine gueltige Session
	 * erforderlich ist (also, dass der Benutzer angemeldet ist)
	 * @param value <code>true</code>, falls eine gueltige Session erforderlich ist
	 */
	public void requireValidSession( boolean value ) {
		requireValidSession = value;
	}
	
	/**
	 * (De)aktiviert die Debug-Ausgaben
	 * @param value <code>true</code> zur Deaktivierung
	 */
	public void setDisableDebugOutput( boolean value ) {
		disableDebugOutput = value;
	}
	
	/**
	 * Gibt zurueck, ob die Debugausgabe deaktiviert ist
	 * @return <code>true</code>, falls sie deaktiviert ist
	 */
	public boolean getDisableDebugOutput() {
		return disableDebugOutput;	
	}
	
	/**
	 * (De)aktiviert die Default-CSS-Stile
	 * @param value <code>true</code> zur Deaktivierung
	 */
	public void setDisableDefaultCSS( boolean value ) {
		disableDefaultCSS = value;
	}
	
	/**
	 * Gibt zurueck, ob die Default-CSS-Stile deaktiviert sind
	 * @return <code>true</code>, falls sie deaktiviert sind
	 */
	public boolean getDisableDefaultCSS() {
		return disableDefaultCSS;	
	}
	
	/**
	 * Setzt die Bezeichnung der aktuellen Seite
	 * @param title Die Bezeichnung
	 */
	public void setPageTitle(String title) {
		this.pageTitle = title;
	}
	
	/**
	 * Fuegt dem Seitenmenue einen Eintrag hinzu
	 * @param title Die Titel des Eintrags
	 * @param url Die URL
	 */
	public void addPageMenuEntry(String title, String url) {
		this.pageMenuEntries.add(new PageMenuEntry(title, url));
	}
	
	/**
	 * Setzt, ob das Seitenmenue nicht verwendet werden soll
	 * @param value <code>true</code>, falls es nicht verwendet werden soll
	 */
	public void setDisablePageMenu(boolean value) {
		this.disablePageMenu = value;
	}
	
	/**
	 * Gibt den Startzeitpunkt der Verarbeitung zurueck
	 * @return Der Startzeitpunkt der Verarbeitung
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Gibt das <code>onLoad</code>-Attribut des HTML-Body-Tags zurueck
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
	 * Fuegt eine Javascript-Funktion zum <code>onLoad</code>-Aufruf des Body-Tags hinzu
	 * @param func Der Javascript-Funktionsaufruf
	 */
	public void addOnLoadFunction( String func ) {
		onLoadFunctions.add(func);
	}
	
	/**
	 * Gibt weitere HTML-Body-Tag-Attribute zurueck
	 * @return Weitere HTML-Body-Tag-Attribute
	 * @see #getOnLoadText()
	 */
	public String getBodyParameters() {
		StringBuilder text = new StringBuilder();
		
		if( bodyParameters.size() > 0 ) {
			for( String key : bodyParameters.keySet() ) {
				text.append(key+"=\""+bodyParameters.get(key)+"\" ");
			}
		}
		
		return text.toString();	
	}
	
	/**
	 * Fuegt ein weiteres HTML-Body-Tag-Attribut hinzu.
	 * Sollte das Attribut bereits gesetzt seit, so wird es
	 * ueberschrieben
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
		
		actionType = type;
	}
	
	/**
	 * Gibt den aktuellen Aktionstyp zurueck
	 * @return Der Aktionstyp
	 */
	protected ActionType getActionType() {
		return actionType;
	}
	
	/**
	 * Gibt die Ausgabehilfe zurueck
	 * @return Die Ausgabehilfe
	 */
	protected OutputHelper getOutputHelper() {
		return actionTypeHandler;
	}
	
	protected abstract boolean validateAndPrepare(String action);
	
	/**
	 * Die Default-Ajax-Aktion
	 * @throws IOException 
	 *
	 */
	public void defaultAjaxAct() throws IOException {
		defaultAction();	
	}
	
	/**
	 * Die Default-HTML-Aktion
	 * @throws IOException 
	 *
	 */
	public void defaultAction() throws IOException {
		getResponse().getWriter().append("DEFAULT");
	}
}
