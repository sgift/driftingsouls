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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.driftingsouls.ds2.server.framework.pipeline.GeneratorPipeline;
import net.driftingsouls.ds2.server.framework.pipeline.HttpRequest;
import net.driftingsouls.ds2.server.framework.pipeline.HttpResponse;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.pipeline.ReaderPipeline;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import net.driftingsouls.ds2.server.framework.pipeline.actions.Action;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Das Hauptservlet von Driftingsouls
 * @author Christopher Jung
 *
 */
public class DriftingSoulsServlet extends HttpServlet {
	private static final long serialVersionUID = -6026961401478799134L;
	
	private Log LOG = null;
	private Map<String,ModuleSetting> modules = new HashMap<String,ModuleSetting>();
	private ModuleSetting defaultModule = null;
	private List<Rule> rules = new ArrayList<Rule>();
	
	private class ModuleSetting implements Cloneable {
		Class<?> generator = null;
		Class<?> transformer = null;
		Class<?> serializer = null;
		
		ModuleSetting(String generator, String transformer, String serializer) throws ClassNotFoundException {
			if( (generator != null) && !"".equals(generator.trim()) ) {
				this.generator = Class.forName(generator);
			}
			if( (transformer != null) && !"".equals(transformer.trim()) ) {
				this.transformer = Class.forName(transformer);
			}
			if( (serializer != null) && !"".equals(serializer.trim()) ) {
				this.serializer = Class.forName(serializer);
			}
		}
		
		void use(ModuleSetting module) {
			if( (module.generator != null) && !"".equals(module.generator) ) {
				generator = module.generator;
			}
			if( (module.transformer != null) && !"".equals(module.transformer) ) {
				transformer = module.transformer;
			}
			if( (module.serializer != null) && !"".equals(module.serializer) ) {
				serializer = module.serializer;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#clone()
		 */
		@Override
		public Object clone() throws CloneNotSupportedException {
			try {
				return new ModuleSetting(generator.getName(), transformer.getName(), serializer.getName());
			}
			catch( Exception e ) {
				// Should not happen
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		public String toString() {
			return "Generator: "+generator.getName()+", Transformer: "+transformer.getName()+", Serializer: "+serializer.getName();
		}
	}
	
	/**
	 * Repraesentiert einen Regel innerhalb der Pipeline-Konfiguration.
	 * Aus allgemeinen Regeln werden die konkreten, fuer den jeweiligen Kontext
	 * angepassten Pipelines generiert
	 * 
	 * @author Christopher Jung
	 *
	 */
	private interface Rule {
		/**
		 * Prueft, ob die Regel ausgefuehrt werden kann
		 * @param context Der Kontext, in dem geprueft werden soll
		 * @return true, falls die Regel ausgefuehrt werden kann
		 * @throws Exception
		 */
		public boolean executeable( Context context ) throws Exception;
		
		/**
		 * Fuehrt die Regel aus und liefert die sich daraus ergebende Pipeline zurueck.
		 * 
		 * @param context Der Kontext, in dem die Regel ausgefuehrt werden soll
		 * @return Die aus der Regel abgeleitete Pipeline
		 * @throws Exception
		 */
		public Pipeline execute( Context context ) throws Exception;
	}
	
	private abstract class AbstractRule implements Rule {		
		private class Parameter {
			private static final int PLAIN = 0;
			private static final int URL_PARAMETER = 1;
			private static final int URL_DIRECTORY = 2;
			
			private int type = PLAIN;
			private String data = "";
			
			/**
			 * Erstellt aus einem Elternknoten, welcher Parameterinformationen enthaelt, ein
			 * Parameterobjekt.
			 * 
			 * @param masternode Der Elternknoten mit Parameterinformationen
			 * @throws Exception
			 */
			public Parameter(Node masternode) throws Exception {
				Node node = XMLUtils.getNodeByXPath(masternode, "plain | urlparameter | urldirectory");
				if( node == null ) {
					throw new Exception("Keine Parameter in der Match-Rule vorhanden");
				}
				
				if( "plain".equals(node.getNodeName()) ) {
					type = PLAIN;
					data = XMLUtils.getStringByXPath(node, "@name").trim();
				}
				else if( "urlparameter".equals(node.getNodeName()) ) {
					type = URL_PARAMETER;
					data = XMLUtils.getStringByXPath(node, "@name").trim();
				}
				else if( "urldirectory".equals(node.getNodeName()) ) {
					type = URL_DIRECTORY;
					data = XMLUtils.getStringByXPath(node, "@number").trim();
					Integer.parseInt(data); // Check, ob das Konvertieren ohne Probleme geht
				}
			}
			
			/**
			 * Liefert den sich aus dem Parameter und den aktuellen Kontext ergebenden Wert
			 * @param context Der aktuelle Kontext
			 * @return Der Wert
			 * @throws Exception
			 */
			public String getValue(Context context) throws Exception {
				switch( type ) {
				case PLAIN:
					return data;

				case URL_PARAMETER:
					return context.getRequest().getParameter(data);

				case URL_DIRECTORY:
					String[] dirs = context.getRequest().getPath().substring(1).split("\\/");
					
					int number = Integer.parseInt(data);
					if( (Math.abs(number) > dirs.length) || (number == 0) ) {
						throw new Exception("Match-Rule: Directory index out of bounds");
					}
					if( number > 0 ) {
						return dirs[number-1];
					}

					return dirs[dirs.length+number];					
				}
				return null;
			}
		}
		private static final int EXECUTE_MODULE = 0;
		private static final int EXECUTE_READER = 1;
		
		// actions
		private List<Action> actions = new ArrayList<Action>();
		private List<HashMap<String,Parameter>> actionParams = new ArrayList<HashMap<String,Parameter>>();
		
		// execute-module
		private Parameter parameter = null;
		private String moduleExecMode = "default";
		private Class<?> serializerClass = null;
		private Class<?> transformerClass = null;
		
		// execute-reader
		private String file = "";
		private Pattern pattern = null;
		private Class<?> readerClass = null;
		
		private int executionType = -1;
		
		AbstractRule( Node matchNode ) throws Exception {
			NodeList nodes = XMLUtils.getNodesByXPath(matchNode, "actions/*");
			if( nodes != null ) {
				setupActions(nodes);
			}
			
			Node node = XMLUtils.getNodeByXPath(matchNode, "execute-module");
			if( node != null ) {
				setupModuleExecuter(node);
				return;
			}

			node = XMLUtils.getNodeByXPath(matchNode, "execute-reader");
			if( node != null ) {
				setupReaderExecuter(node);
				return;
			}
			
			throw new Exception("Unable to determine execution type of rule");
		}
		
		private void setupActions(NodeList nodes) throws Exception {
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				if( !"action".equals(node.getNodeName()) ) {
					continue;
				}
				
				Class<?> cls = Class.forName(XMLUtils.getStringByXPath(node, "@class"));
				actions.add((Action)cls.newInstance());
				HashMap<String,Parameter> map = new HashMap<String,Parameter>();
				
				NodeList paramNodes = XMLUtils.getNodesByXPath(node, "parameter");
				for( int j=0; j < paramNodes.getLength(); j++ ) {
					Node paramNode = paramNodes.item(j);
					String name = XMLUtils.getStringByXPath(paramNode, "@name");
					if( (name == null) || "".equals(name) ) {
						continue;
					}
					Parameter param = new Parameter(paramNode);
					map.put(name, param);
				}
				
				actionParams.add(map);
			}
		}
		
		private void setupReaderExecuter(Node node) throws Exception {
			executionType = EXECUTE_READER;

			readerClass = Class.forName(XMLUtils.getStringByXPath(node, "reader/@class"));
			file = XMLUtils.getStringByXPath(node, "file/@file");
			String pattern = XMLUtils.getStringByXPath(node, "file/@pattern");

			if( (pattern != null) && !"".equals(pattern.trim()) ) {
				this.pattern = Pattern.compile(pattern);
			}
		}
		
		private void setupModuleExecuter(Node node) throws Exception {
			executionType = EXECUTE_MODULE;
			String execMode = XMLUtils.getStringByXPath(node, "@exec-mode");
			if( (execMode != null) && !"".equals(execMode) ) {
				if( "default".equals(execMode) || "ajax".equals(execMode) ) {
					moduleExecMode = execMode;
				}
				else {
					throw new Exception("Illegaler Modul-Exec-Mode '"+execMode+"'");
				}
			}
			
			parameter = new Parameter(node);
			
			String transformer = XMLUtils.getStringByXPath(node, "transformer/@class");
			if( (transformer != null) && !"".equals(transformer) ) {
				transformerClass = Class.forName(transformer);
			}
			
			String serializer = XMLUtils.getStringByXPath(node, "serializer/@class");
			if( (serializer != null) && !"".equals(serializer) ) {
				serializerClass = Class.forName(serializer);
			}
		}
		
		public boolean executeable(Context context) throws Exception{
			if( actions.size() == 0 ) {
				return true;
			}
			for( int i=0; i < actions.size(); i++ ) {
				Action act = actions.get(i);
				act.reset();
				for( String paramName : actionParams.get(i).keySet() ) {
					act.setParameter(paramName, actionParams.get(i).get(paramName).getValue(context) );
				}
				if( !act.action(context) ) {
					return false;
				}
			}
			return true;
		}
		
		public Pipeline execute(Context context) throws Exception {
			if( !executeable(context) ) {
				return null;
			}
			if( executionType == EXECUTE_MODULE ) {
				return executeModule(context);
			}
			if( executionType == EXECUTE_READER ) {
				return executeReader(context);
			}
			
			return null;
		}
		
		private Pipeline executeReader(Context context) throws Exception {
			String file = this.file;
			if( pattern != null ) {
				file = pattern.matcher(context.getRequest().getPath()).replaceFirst(file);
			}

			return new ReaderPipeline(readerClass, file);
		}
		
		private Pipeline executeModule(Context context) throws Exception {
			String module = parameter.getValue(context);
			
			ModuleSetting moduleSetting = (ModuleSetting)defaultModule.clone();
			if( modules.get(module) != null ) {
				moduleSetting.use(modules.get(module));
			}
			
			return new GeneratorPipeline(moduleExecMode, 
					moduleSetting.generator, 
					(transformerClass != null ? transformerClass : moduleSetting.transformer), 
					(serializerClass != null ? serializerClass : moduleSetting.serializer) );
		}
	}
	
	private class MatchRule extends AbstractRule {		
		private Pattern match = null;
		
		MatchRule( Node matchNode ) throws Exception {
			super(matchNode);
			match = Pattern.compile( XMLUtils.getStringByXPath(matchNode, "@pattern") );
		}
		
		@Override
		public boolean executeable(Context context) throws Exception {
			if( !match.matcher(context.getRequest().getPath()).matches() ) {
				return false;
			}
			return super.executeable(context);
		}
	}
	
	private ModuleSetting readModuleSetting( Node moduleNode ) throws Exception {
		return new ModuleSetting( 
				XMLUtils.getStringByXPath(moduleNode, "generator/@class"),
				XMLUtils.getStringByXPath(moduleNode, "transformer/@class"),
				XMLUtils.getStringByXPath(moduleNode, "serializer/@class"));
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if( config.getServletContext().getInitParameter("logger") == null ) {
			System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		}
		else {
			System.getProperties().setProperty("org.apache.commons.logging.Log",config.getServletContext().getInitParameter("logger"));
		}
		
		LOG = new LogFactoryImpl().getInstance("DS2");
		LOG.info("Booting DS...");
		
		try {
			new DriftingSouls(LOG,  config.getServletContext().getInitParameter("configdir"));
		}
		catch( Exception e ) {
			LOG.fatal(e, e);
			throw new ServletException(e);
		}
		
		// Pipeline lesen
		LOG.info("Reading "+Configuration.getSetting("configdir")+"pipeline.xml");
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"pipeline.xml");
			// Module
			Node moduleNode = XMLUtils.getNodeByXPath(doc, "/pipeline/modules");
			defaultModule = readModuleSetting(moduleNode);
			
			NodeList nodes = XMLUtils.getNodesByXPath(moduleNode, "module");
			for( int i=0; i < nodes.getLength(); i++ ) {
				String moduleName = nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
				modules.put( moduleName, readModuleSetting(nodes.item(i)) );
			}
			
			// Regeln
			nodes = XMLUtils.getNodesByXPath(doc, "/pipeline/rules/*");
			for( int i=0; i < nodes.getLength(); i++ ) {
				if( nodes.item(i).getNodeName() == "match" ) {
					rules.add(new MatchRule(nodes.item(i)));
				}
				else {
					throw new Exception("Unhandled pipeline rule '"+nodes.item(i).getNodeName()+"'");
				}
			}
		}
		catch( Exception e ) {
			LOG.fatal(e, e);
			throw new ServletException(e);
		}
		
		LOG.info("DS is now ready for service");
	}

	private void doSomething(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException,
			ServletException {
		try {
			Pipeline pipeline = null;
			Request request = new HttpRequest(httpRequest); 
			Response response = new HttpResponse(httpResponse);
			BasicContext context = new BasicContext(request, response);
			
			for( Rule rule : rules ) {
				if( (pipeline = rule.execute(context)) != null ) {
					break;
				}
			}
			
			if( pipeline != null ) {
				pipeline.execute(context);
			}
			else {
				throw new Exception("Unable to find a suitable rule for URL '"+request.getRequestURL()+(request.getQueryString() != null ? "?"+request.getQueryString() : "")+"'");
			}
			
			response.send();
			context.free();
		}
		catch( Exception e ) {
			e.printStackTrace();
			httpResponse.setContentType("text/html");
			PrintWriter writer = httpResponse.getWriter();
			writer.append("<html><head><title>Drifting Souls Server Framework</title></head>");
			writer.append("<body>");
			writer.append("<table border=\"0\"><tr><td>\n");
			writer.append("<div align=\"center\">\n");
			writer.append("<h1>Drifting Souls Server Framework</h1>");
			writer.append("Unhandled Exception "+e.getClass().getName()+" during pipeline execution detected<br />\n");
			writer.append("Reason: "+e.getMessage()+"</div>\n");
			writer.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />");
			StackTraceElement[] st = e.getStackTrace();
			for( int i=0; i < st.length; i++ ) {
				writer.append(st[i].toString()+"<br />\n");
			}
			writer.append("</td></tr></table></body></html>");
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException {
		doSomething(request,response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException {
		doSomething(request,response);		
	}
}
