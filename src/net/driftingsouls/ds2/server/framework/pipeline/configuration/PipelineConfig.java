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

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <h1>Repraesentiert die Pipeline-Konfiguration.</h1>
 * <p>Eine Pipeline entscheidet darueber wie, unter welchen Bedingungen und von wem
 * eine Request verarbeitet wird.</p> 
 * @author Christopher Jung
 *
 */
@Component
@Lazy
public class PipelineConfig {
	private static final Log log = LogFactory.getLog(PipelineConfig.class);
	
	private Map<String,ModuleSetting> modules = new ConcurrentHashMap<>();
	private ModuleSetting defaultModule = null;
	private List<Rule> rules = new CopyOnWriteArrayList<>();
	private Configuration configuration;
	
	ModuleSetting getModuleSettingByName(String name) throws Exception {
		ModuleSetting moduleSetting = (ModuleSetting)defaultModule.clone();
		if( name != null && modules.containsKey(name) ) {
			moduleSetting.use(modules.get(name));
		}
		
		return moduleSetting;
	}
	
	/**
	 * Injiziert die DS-Konfiguration.
	 * @param config Die DS-Konfiguration
	 */
	@Autowired
	public void setConfiguration(Configuration config) {
		this.configuration = config;
	}
	
	private void scanForModules() throws IOException {
		SortedSet<Class<?>> entityClasses = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
		for( Class<?> cls : entityClasses )
		{
			Module modConfig = cls.getAnnotation(Module.class);
			if( modConfig.defaultModule() )
			{
				if( this.defaultModule != null )
				{
					log.error("Multiple Default-Modules detected: "+this.defaultModule.getClass().getName()+" and "+cls);
				}
				this.defaultModule = new ModuleSetting(cls);
			}
			else
			{
				this.modules.put(modConfig.name(), new ModuleSetting(cls));
			}
		}
	}
	
	/**
	 * Liesst die Konfiguration aus der Pipeline-Konfigurationsdatei ein.
	 * @throws Exception
	 */
	@PostConstruct
	public void readConfiguration() throws Exception {
		if( !this.rules.isEmpty() ) {
			throw new IllegalStateException("Die Pipeline wurde bereits geladen");
		}
		log.info("Reading pipeline.xml");

		try( InputStream stream = getClass().getResourceAsStream("/META-INF/pipeline.xml") )
		{
			if( stream == null )
			{
				throw new FileNotFoundException("Konnte pipeline.xml nicht finden");
			}
			Document doc = XMLUtils.readStream(stream);

			// Module
			scanForModules();

			// Regeln
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "/pipeline/rules/*");
			for (int i = 0; i < nodes.getLength(); i++)
			{
				if ("match".equals(nodes.item(i).getNodeName()))
				{
					rules.add(new MatchRule(this, nodes.item(i)));
				}
				else
				{
					throw new Exception("Unhandled pipeline rule '" + nodes.item(i).getNodeName() + "'");
				}
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
	public Pipeline getPipelineForContext(Context context) throws Exception {
		Pipeline pipeline;
		for( Rule rule : rules ) {
			if( (pipeline = rule.execute(context)) != null ) {
				return pipeline;
			}
		}
		
		return null;
	}
}
