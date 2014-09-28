package net.driftingsouls.ds2.server.framework.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loader fuer Template-Objekte. Laedt zu einem Dateinamen (Templatedatei)
 * ein passendes Template-Objekt.
 */
@Service
public class TemplateLoader
{
	private static final Log log = LogFactory.getLog(TemplateLoader.class);
	private static final String PACKAGE = "net.driftingsouls.ds2.server.templates";
	private static Map<String,Template> templateMap = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Laedt das Templateobjekt zur angegebenen Templatedatei. Falls kein Templateobjekt
	 * geladen werden konnte wird <code>null</code> zurueckgegeben.
	 * @param filename Die Templatedatei
	 * @return Das Templateobjekt oder <code>null</code>
	 */
	public Template load(String filename)
	{
		if( !templateMap.containsKey(filename) ) {
			String fname = new File(filename).getName();
			fname = fname.substring(0,fname.length()-".html".length()).replace(".", "");

			try {
				Class<?> tmpl = Class.forName(PACKAGE+"."+fname);
				Template t = (Template)tmpl.newInstance();
				templateMap.put(filename, t);
			}
			catch( Exception e ) {
				log.debug("FAILED: Loading class "+PACKAGE+"."+fname, e);
				return null;
			}

			if( log.isDebugEnabled() ) {
				log.debug("Loaded class "+PACKAGE+"."+filename);
			}
		}

		return templateMap.get(filename);
	}
}
