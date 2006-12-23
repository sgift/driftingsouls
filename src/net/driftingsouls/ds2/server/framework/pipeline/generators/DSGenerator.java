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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren
 * @author Christopher Jung
 *
 */
public abstract class DSGenerator extends Generator {
	public enum ActionType {
		DEFAULT("Action"),
		AJAX("AjaxAct");
		
		private String type;
		
		private ActionType(String type) {
			this.type = type;
		}
		
		public String getActionExt() {
			return type;
		}
	}
	
	protected abstract class FWOutputHelper {
		public abstract void printHeader();
		public abstract void printFooter();
		public abstract void printErrorList();
	}
	
	protected class FWHtmlOutputHelper extends FWOutputHelper {
		public void printHeader() {
			if( !getParameter("_style").equals("xml") ) {
				StringBuffer sb = getResponse().getContent();
				String url = Configuration.getSetting("URL")+"/";
				boolean usegfxpak = false;
				if( getUser() != null ) {
					if( !getUser().getUserImagePath().equals(User.getDefaultImagePath(getDatabase())) ) {
						usegfxpak = true;
					}
					url = getUser().getUserImagePath();
				}
				
				sb.append("<head>\n");
				sb.append("<title>Drifting Souls 2</title>\n");
				sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
				if( !getDisableDefaultCSS() ) { 
					sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+Configuration.getSetting("URL")+"format.css\" />\n");
				}
				sb.append("<!--[if IE]>\n");
   				sb.append("<style type=\"text/css\">@import url("+Configuration.getSetting("URL")+"format_fuer_den_dummen_ie.css);</style>\n");
  				sb.append("<![endif]-->\n");

				sb.append(getTemplateEngine().getVar( "__HEADER" ) );
				
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
				sb.append("// -->\n");
				sb.append("</script>\n");
			}
		}
		
		public void printFooter() {
			if( !template.equals("") ) {
				getTemplateEngine().parse( "OUT", masterTemplateID );
					
				getTemplateEngine().p("OUT");
			}
			if( !getParameter("_style").equals("xml") ) {
				StringBuffer sb = getResponse().getContent();
				if( !getDisableDebugOutput() ) {
					sb.append("<div style=\"text-align:center; font-size:11px;color:#c7c7c7; font-family:'BankGothic Md BT','Bank Gothic Medium BT','Bank Gothic','BankGothic',courier;\">\n");
					sb.append("<br /><br /><br />\n");
					sb.append("QCount: "+getDatabase().getQCount()+"<br />\n");
					sb.append("Execution-Time: "+(System.currentTimeMillis()-getStartTime())/1000d+"s<br />\n");
					//echo "<a class=\"forschinfo\" target=\"none\" style=\"font-size:11px\" href=\"http://ds2.drifting-souls.net/mantis/\">Zum Bugtracker</a><br />\n";
					sb.append("</div>\n");
				}
				sb.append("</body>");
			}
		}

		public void printErrorList() {
			StringBuffer sb = getResponse().getContent();
			sb.append("<div align=\"center\">\n");
			sb.append(Common.tableBegin(430,"left"));
			sb.append("<div style=\"text-align:center; font-size:14px; font-weight:bold\">Es sind Fehler aufgetreten:</div><ul>\n");
					
			for( Error error : getErrorList() ) {
				if( error.getUrl() == null ) {
					sb.append("<li><span style=\"font-size:14px; color:red\">"+error.getDescription().replaceAll("\n","<br />")+"</span></li>\n");
				}
				else {
					sb.append("<li><a class=\"error\" style=\"font-size:14px; font-weight:normal\" href=\""+error.getUrl()+"\">"+error.getDescription().replaceAll("\n","<br />")+"</a></li>\n");
				}
			}	
					
			sb.append("<ul>\n");
			sb.append(Common.tableEnd());
			sb.append("</div>\n");
		}
	}
	
	protected class FWAjaxOutputHelper extends FWOutputHelper {
		public void printHeader() {
			// Moegliche Templates vorerst deaktivieren 
			// (bis wir sie fuer ajax wirklich mal brauchen sollten)
			// TODO
			//$cntl->setTemplate('');
		}
		public void printFooter() {}
		public void printErrorList() {}
	}
	
	private ActionType actionType;
	private FWOutputHelper actionTypeHandler;
	
	private String template;
	private TemplateEngine templateEngine;
	private String masterTemplateID;
	
	private boolean noActionBlocking;
	private boolean updateLastAction;
	
	private boolean disableDefaultCSS;
	private boolean disableDebugOutput;
	private String browser;
	private long startTime;
	private boolean requireValidSession;
	private List<String> onLoadFunctions;
	private Map<String, String> bodyParameters;
	private Map<String, Object> parameter;
	private String subParameter;
	private List<String> preloadUserValues;
	
	
	public DSGenerator(Context context) {
		super(context);
		
		setDisableActionBlocking(false);

		parameter = new HashMap<String,Object>();
		subParameter = "";
		
		parameterString("sess");
		parameterString("module");
		parameterString("action");
		parameterString("_style");
		
		startTime = System.currentTimeMillis();
		
		disableDebugOutput = false;
		requireValidSession = true;
		disableDefaultCSS = false;
		
		onLoadFunctions = new ArrayList<String>();
		bodyParameters = new HashMap<String,String>();
		
		preloadUserValues = new ArrayList<String>();
		preloadUserValues.add("id");
		
		template = "";
		templateEngine = null;
		masterTemplateID = "";
			
		updateLastAction = true;
		setActionType(ActionType.DEFAULT);

		String browser = getRequest().getHeader("user-agent").toLowerCase();
		
		if( browser.indexOf("opera") > -1  ) {
			browser = "opera";
		}
		else if( browser.indexOf("msie") > -1 ) {
			browser = "msie";
		}
		else {
			browser = "mozilla";
		}
		this.browser = browser;
	}
	
	@Deprecated
	protected void requireUserProperty( String value ) {
		/*if( !preloadUserValues.contains(value) ) {
			preloadUserValues.add(value);
		}*/
	}
	
	@Deprecated
	protected void requireUserProperty( String ... values ) {
		for( String value : values ) {
			requireUserProperty(value);	
		}
	}
	
	public User getUser() {
		return getActiveUser();
	}
	
	public Object getParameter( String parameter ) {
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
				this.parameter.put(parameter, Common.getNumberFormat().parse(val));
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
	
	public void parameterArray( String parameter, String[] subparams, String[] types ) {
		HashMap<String,Object> map = new HashMap<String,Object>();
		
		for( int i=0; i < subparams.length; i++ ) {
			if( "number".equals(types[i]) ) {
				String val = getRequest().getParameter(parameter+"["+subparams[i]+"]");
				if( val != null ) {
					try {
						map.put(subparams[i], Common.getNumberFormat().parse(val));
					}
					catch( ParseException e ) {
						addError("Parameter "+parameter+"["+subparams[i]+"] ist keine g&uuml;ltige Zahl");
						map.put(subparams[i], 0d);
					}
				}
				else {
					map.put(subparams[i], 0d);
				}
			}
			else if( "string".equals(types[i]) ) {
				map.put(subparams[i], getRequest().getParameter(parameter+"["+subparams[i])+"]");
			}
		}
		this.parameter.put(parameter, map);
	}
	
	private void createTemplateEngine() {
		if( templateEngine != null ) {
			return;
		}
				
		templateEngine = new TemplateEngine(getContext());

		String style = (String)getParameter("_style");
		if( !style.equals("") ) {
			templateEngine.setOverlay(style);	
		}
				
		if( getBrowser().equals("opera") ) {
			templateEngine.set_var("_BROWSER_OPERA",1);
		}
		else if( getBrowser().equals("msie") ) {
			templateEngine.set_var("_BROWSER_MSIE",1);
		}
		else {
			templateEngine.set_var("_BROWSER_MOZILLA",1);
		}
		
		templateEngine.set_var(	"global.sess",	getString("sess"),
								"global.module", getString("module") );
	}
	
	/**
	 * Gibt die mit dem Generator verknuepfte Instanz des Template-Engines zurueck
	 * @return Das Template-Engine
	 */
	public TemplateEngine getTemplateEngine() {
		if( templateEngine == null ) {
			createTemplateEngine();
		}
		return templateEngine;
	}
	
	/**
	 * Gibt die ID der im System registrierten Template-File zurueck. 
	 * Sollte noch kein Template-File registriert sein, so wird ein leerer
	 * String zurueckgegeben
	 * @return die ID der Template-File oder <code>""</code>
	 */
	public String getTemplateID() {
		return masterTemplateID;
	}
	
	/**
	 * Setzt das vom Generator verwendete Template-File auf die angegebene Datei. Die Datei muss
	 * in kompilierter Form im System vorliegen (das vorhandensein der unkompilierten Variante ist nicht
	 * erforderlich).
	 * @param file Der Dateiname der unkompilierten Template-Datei
	 */
	public void setTemplate( String file ) {
		if( !file.equals("") ) {
			template = file;
		
			if( templateEngine == null ) {
				createTemplateEngine();
			}
		
			String mastertemplate = new File(file).getName();
			if( mastertemplate.indexOf(".html") > -1 ) {
				mastertemplate = mastertemplate.substring(0,mastertemplate.lastIndexOf(".html"));
			}
			mastertemplate = "_"+mastertemplate.toUpperCase();
		
			masterTemplateID = mastertemplate;

			if( !templateEngine.set_file( masterTemplateID, file ) ) {
				masterTemplateID = "";
				template = "";	
			}
		}
		else {
			template = "";
			masterTemplateID = "";	
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
			e.printStackTrace();
			addError("Es ist ein Fehler beim Aufruf der Action '"+action+"' aufgetreten:\n"+e.toString());
		}
	}
	
	/**
	 * Ruft die Standardaktion auf
	 *
	 */
	protected void redirect() {
		redirect("default");
	}
	
	public void handleAction( String action, ActionType actionType ) {
		setActionType( actionType );

		if( (getRequest().getParameter("action") == null) || "".equals(getRequest().getParameter("action")) ) {
			action = "default";
		}
		
		if( actionType != ActionType.DEFAULT ) {
			setDisableActionBlocking(true);
		}
		
		String content = "";
		if( requireValidSession && (getActiveUser() == null) && "".equals(getString("sess")) ) {
			addError( "FATAL ERROR: Es wurde keine session-id &uuml;bergeben" );
		}
		
		if( getErrorList().length != 0 ) {
			template = "";
			
			actionTypeHandler.printHeader();
			
			if( getErrorList().length > 0 ) {
				actionTypeHandler.printErrorList();
			}

			actionTypeHandler.printFooter();
			
			return;
		}
		if( templateEngine != null ) {
			if( getActiveUser() != null ) {
				getActiveUser().setTemplateVars( templateEngine );	
			}	
		}
		
		if( (getErrorList().length == 0) && validateAndPrepare(action) ) {
			String callAction = action + actionType.getActionExt();
			
			try {
				Method method = getClass().getMethod(callAction);
				method.setAccessible(true);
				method.invoke(this);
			}
			catch( InvocationTargetException e ) {
				Throwable t = e.getCause();
				t.printStackTrace();
				StackTraceElement[] st = t.getStackTrace();
				String stacktrace = "";
				for( StackTraceElement s : st ) {
					stacktrace += s.toString()+"\n";
				}
					
				addError("Es ist ein Fehler in der Action '"+action+"' aufgetreten:\n"+t.toString()+"\n\n"+stacktrace);
				
				Common.mailThrowable(e, "DSGenerator Invocation Target Exception", "Action: "+action+"\nActionType: "+actionType+"\nUser: "+(getActiveUser() != null ? getActiveUser().getID() : "none"));
			}
			catch( NoSuchMethodException e ) {
				addError("Die Aktion '"+action+"' existiert nicht!");
			}
			catch( Exception e ) {
				addError("Es ist ein Fehler beim Aufruf der Action '"+action+"' aufgetreten:\n"+e.toString());
				Common.mailThrowable(e, "DSGenerator Exception", "Action: "+action+"\nActionType: "+actionType+"\nUser: "+(getActiveUser() != null ? getActiveUser().getID() : "none"));
			}
		}
		else {				
			template = "";	
		}
		parseSubParameter("");
		content = getResponse().getContent().toString();
		content = StringUtils.replace(content,"{{{__SESSID__}}}", getString("sess"));
		getResponse().resetContent();
			
		printHeader( action );
		
		if( getErrorList().length > 0 ) {
			actionTypeHandler.printErrorList();
		}
		
		getResponse().getContent().append(content);
		
		printFooter( action );
	}
	
	protected void printHeader( String action ) {
		actionTypeHandler.printHeader();
	}
	
	protected void printFooter( String action ) {
		actionTypeHandler.printFooter();
	}
	
	public void requireValidSession( boolean value ) {
		requireValidSession = value;
	}
	
	public void setDisableDebugOutput( boolean value ) {
		disableDebugOutput = value;
	}
	
	public boolean getDisableDebugOutput() {
		return disableDebugOutput;	
	}
	
	public void setDisableDefaultCSS( boolean value ) {
		disableDefaultCSS = value;
	}
	
	public boolean getDisableDefaultCSS() {
		return disableDefaultCSS;	
	}
	
	@Deprecated
	public void setDisableActionBlocking( boolean value ) {
		noActionBlocking = value;	
	}
	
	@Deprecated
	public void setDisableLastActionUpdate( boolean value ) {
		updateLastAction = !value;	
	}
	
	public long getStartTime() {
		return startTime;
	}

	public String getOnLoadText() {
		if( onLoadFunctions.size() > 0 ) {
			StringBuilder sb = new StringBuilder("onLoad=\"");
			sb.append(Common.implode(" ", onLoadFunctions));
			sb.append("\"");
			
			return sb.toString();
		}
		
		return "";	
	}
	
	public void addOnLoadFunction( String func ) {
		onLoadFunctions.add(func);
	}
	
	public String getBodyParameters() {
		StringBuilder text = new StringBuilder();
		
		if( bodyParameters.size() > 0 ) {
			for( String key : bodyParameters.keySet() ) {
				text.append(key+"=\""+bodyParameters.get(key)+"\" ");
			}
		}
		
		return text.toString();	
	}
	
	public void addBodyParameter( String parameter, String value ) {
		bodyParameters.put(parameter,value);
	}
	
	public String getBrowser() {
		return browser;
	}
	
	protected void setActionType( ActionType type ) {
		if( type == ActionType.DEFAULT ) {
			actionTypeHandler = new FWHtmlOutputHelper();
		}
		else if( type == ActionType.AJAX ) {
			actionTypeHandler = new FWAjaxOutputHelper();
		}	
		
		actionType = type;
	}
	
	protected abstract boolean validateAndPrepare(String action);
	
	public void defaultAjaxAct() {
		defaultAction();	
	}
	
	public void defaultAction() {
		getResponse().getContent().append("DEFAULT");
	}
}
