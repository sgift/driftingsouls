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

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <h1>Repraesentiert die Pipeline-Konfiguration</h1>
 * <p>Eine Pipeline entscheidet darueber wie, unter welchen Bedingungen und von wem
 * eine Request verarbeitet wird.</p> 
 * @author Christopher Jung
 *
 */
public class PipelineConfig {
	private static Map<String,ModuleSetting> modules = new HashMap<String,ModuleSetting>();
	private static ModuleSetting defaultModule = null;
	private static List<Rule> rules = new ArrayList<Rule>();
	
	private static ModuleSetting readModuleSetting( Node moduleNode ) throws Exception {
		return new ModuleSetting( 
				XMLUtils.getStringByXPath(moduleNode, "generator/@class"));
	}
	
	static ModuleSetting getModuleSettingByName(String name) throws Exception {
		ModuleSetting moduleSetting = (ModuleSetting)defaultModule.clone();
		if( modules.get(name) != null ) {
			moduleSetting.use(modules.get(name));
		}
		
		return moduleSetting;
	}
	
	/**
	 * Liesst die Konfiguration aus der Pipeline-Konfigurationsdatei ein
	 * @throws Exception
	 */
	public static void readConfiguration() throws Exception {
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
	
	/**
	 * Gibt die Pipeline fuer die mit dem aktuellen Kontext verbundene Request zurueck.
	 * Sollte keine passende Pipeline existieren, so wird <code>null</code> zurueckgegeben.
	 * @param context Der aktuelle Kontext
	 * @return Die Pipeline oder <code>null</code>
	 * @throws Exception
	 */
	public static Pipeline getPipelineForContext(Context context) throws Exception {
		Pipeline pipeline = null;
		for( Rule rule : rules ) {
			if( (pipeline = rule.execute(context)) != null ) {
				return pipeline;
			}
		}
		
		return null;
	}
}
