package net.driftingsouls.ds2.server.framework.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader fuer Template-Objekte. Laedt zu einem Dateinamen (Templatedatei)
 * ein passendes Template-Objekt.
 */
@Service
public class TemplateLoader
{
	private static final Log log = LogFactory.getLog(TemplateLoader.class);
	private static final String PACKAGE = "net.driftingsouls.ds2.server.framework.templates";

	private Map<String, Template> templateMap = new ConcurrentHashMap<>();

	/**
	 * Laedt das Templateobjekt zur angegebenen Templatedatei. Falls kein Templateobjekt
	 * geladen werden konnte wird <code>null</code> zurueckgegeben. Die mittels dieser Methode
	 * geladene Template-Instanz sollte nicht gecachet werden, da dies Aufgabe dieses Service ist.
	 * @param filename Die Templatedatei
	 * @return Das Templateobjekt oder <code>null</code>
	 */
	public Template load(String filename)
	{
		loadFromClassPath(filename, Thread.currentThread().getContextClassLoader());
		return templateMap.get(filename);
	}

	private void loadFromClassPath(String filename, ClassLoader loader) {
		String className = convertFileNameToClassName(filename);

		try {
			@SuppressWarnings("unchecked")
			Class<Template> templateClass = (Class<Template>) loader.loadClass(className);
			templateMap.put(filename, templateClass.getDeclaredConstructor().newInstance());
		} catch (Exception e) {
			log.debug("FAILED: Loading class " + className, e);
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("Loaded class " + className);
		}
	}

	private String convertFileNameToClassName(String filename)
	{
		String fname = new File(filename).getName();
		fname = fname.substring(0,fname.length()-".html".length()).replace(".", "");
		return PACKAGE + "." + fname;
	}
}
