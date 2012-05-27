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
package net.driftingsouls.ds2.server.framework.pipeline.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.pipeline.actions.Action;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// TODO: Behandlung Module, Reader und Servlet in eigene Klassen auslagern
abstract class AbstractRule implements Rule {
	private final Node config;
	
	// actions
	private final List<Action> actions = new ArrayList<Action>();
	private final List<Map<String,Parameter>> actionParams = new ArrayList<Map<String,Parameter>>();
	
	private final Executer executer;

	private final ParameterMap parameterMap;
	
	private final PipelineConfig pipelineConfig;
	
	AbstractRule( PipelineConfig pipelineConfig, Node matchNode ) throws Exception {
		this.pipelineConfig = pipelineConfig;
		
		NodeList nodes = XMLUtils.getNodesByXPath(matchNode, "actions/*");
		if( nodes != null ) {
			setupActions(nodes);
		}
		
		Node config = XMLUtils.getNodeByXPath(matchNode, "config");
		if( config != null )  {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			doc.appendChild(doc.importNode(config, true));
			this.config = doc.getFirstChild();
		}
		else {
			this.config = null;
		}
		
		Node paramMap = XMLUtils.getNodeByXPath(matchNode, "parameter-map");
		if( paramMap != null )  {
			this.parameterMap = new ParameterMap(paramMap);
		}
		else {
			this.parameterMap = null;
		}
		
		Node node = XMLUtils.getNodeByXPath(matchNode, "execute-module");
		if( node != null ) {
			this.executer = new ModuleExecuter(this.pipelineConfig, node);
			return;
		}

		node = XMLUtils.getNodeByXPath(matchNode, "execute-reader");
		if( node != null ) {
			this.executer = new ReaderExecuter(node);
			return;
		}
		
		node = XMLUtils.getNodeByXPath(matchNode, "execute-servlet");
		if( node != null ) {
			this.executer = new ServletExecuter();
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
	
	@Override
	public boolean executeable(Context context) throws Exception {
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
	
	@Override
	public Pipeline execute(Context context) throws Exception {
		if( !executeable(context) ) {
			return null;
		}
		
		if( parameterMap != null ) {
			parameterMap.apply(context);
		}
		
		Pipeline pipe = this.executer.execute(context);
		
		if( pipe != null ) {
			pipe.setConfiguration(config);
		}
		
		return pipe;
	}
}
