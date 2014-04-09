package net.driftingsouls.ds2.server.framework;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Hilfsmethoden zum Umgang mit Annotations.
 */
public class AnnotationUtils
{
	private static final Logger LOG = LogManager.getLogger(AnnotationUtils.class);

	public static final AnnotationUtils INSTANCE = new AnnotationUtils();
	private final Reflections reflections;

	private AnnotationUtils()
	{
		Set<URL> urls = ClasspathHelper.forPackage("net.driftingsouls.ds2.server");
		LOG.info("Scanning urls " + urls);
		reflections = new Reflections(new ConfigurationBuilder().setUrls(urls).setScanners(new TypeAnnotationsScanner()));
	}

	/**
	 * Findet alle Klassen die mit der angegebenen Annotation annotiert wurden. Die vollstaendigen Namen
	 * Klassen werden anschliessend in einem Set zurueckgegeben.
	 *
	 * @param annotationCls Die Annotation
	 * @return Das Set der Klassennamen
	 */
	public SortedSet<Class<?>> findeKlassenMitAnnotation(Class<? extends Annotation> annotationCls)
	{
		Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(annotationCls);
		SortedSet<Class<?>> result = new TreeSet<>((c1, c2) -> c1.getName().compareTo(c2.getName()));
		result.addAll(typesAnnotatedWith);
		return result;
	}
}
