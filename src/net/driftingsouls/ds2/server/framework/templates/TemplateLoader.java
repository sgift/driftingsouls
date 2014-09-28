package net.driftingsouls.ds2.server.framework.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
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

	private static class LoadedTemplate {
		final long lastModified;
		final Template template;

		LoadedTemplate(long lastModified, Template template)
		{
			this.lastModified = lastModified;
			this.template = template;
		}
	}

	private Map<String,LoadedTemplate> templateMap = Collections.synchronizedMap(new HashMap<>());

	private String templateDir;

	public TemplateLoader()
	{
		templateDir = System.getProperty("DS_TEMPLATE_DIR");
	}

	/**
	 * Laedt das Templateobjekt zur angegebenen Templatedatei. Falls kein Templateobjekt
	 * geladen werden konnte wird <code>null</code> zurueckgegeben. Die mittels dieser Methode
	 * geladene Template-Instanz sollte nicht gecachet werden, da dies Aufgabe dieses Service ist.
	 * @param filename Die Templatedatei
	 * @return Das Templateobjekt oder <code>null</code>
	 */
	public Template load(String filename)
	{
		if( templateDir != null )
		{
			loadFromFile(filename);
		}
		else if( !templateMap.containsKey(filename) )
		{
			loadFromClassPath(filename, Thread.currentThread().getContextClassLoader(), -1);
		}

		LoadedTemplate loadedTemplate = templateMap.get(filename);
		return loadedTemplate != null ? loadedTemplate.template : null;
	}

	private void loadFromClassPath(String filename, ClassLoader loader, long lastModified)
	{
		String className = convertFileNameToClassName(filename);

		try {
			Class<?> tmpl = loader.loadClass(className);
			Template t = (Template)tmpl.newInstance();
			templateMap.put(filename, new LoadedTemplate(lastModified,t));
		}
		catch( Exception e ) {
			log.debug("FAILED: Loading class "+className, e);
			return;
		}

		if( log.isDebugEnabled() ) {
			log.debug("Loaded class "+className);
		}
	}

	private String convertFileNameToClassName(String filename)
	{
		String fname = new File(filename).getName();
		fname = fname.substring(0,fname.length()-".html".length()).replace(".", "");
		return PACKAGE + "." + fname;
	}

	private static class TemplateClassLoader extends ClassLoader
	{
		private TemplateClassLoader(ClassLoader parent)
		{
			super(parent);
		}

		public Class<?> loadTemplateClass(String name, byte[] bytes)
		{
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	private void loadFromFile(String filename)
	{
		File file = new File(templateDir+"/"+filename);
		if( !file.canRead() )
		{
			log.warn("Konnte Templatedatei " + file.getAbsolutePath() + " nicht finden");
			return;
		}

		LoadedTemplate loadedTemplate = templateMap.get(filename);
		if( loadedTemplate != null && loadedTemplate.lastModified >= file.lastModified() )
		{
			return;
		}

		TemplateClassLoader loader = new TemplateClassLoader(Thread.currentThread().getContextClassLoader());

		TemplateCompiler compiler = new TemplateCompiler(
				file.getAbsolutePath(),
				null,
				javaFile -> loader.loadTemplateClass(javaFile.getClassName(), javaFile.getClassBytes()));

		try
		{
			compiler.compile();
		}
		catch( IOException e )
		{
			log.error("Konnte Template "+filename+" nicht kompilieren", e);
			return;
		}

		loadFromClassPath(filename, loader, file.lastModified());
	}
}
