package net.driftingsouls.ds2.server.framework;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Hilfsmethoden zum Umgang mit Annotations.
 */
public class AnnotationUtils
{
	public static final AnnotationUtils INSTANCE = new AnnotationUtils();

	private final AnnotationDB annotationDb = new AnnotationDB();

	private AnnotationUtils()
	{
		URL[] urls = ClasspathUrlFinder.findResourceBases("META-INF/ds.marker");
		try
		{
			annotationDb.scanArchives(urls);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Konnte Annotation-Datenbank nicht aufbauen");
		}
	}

	/**
	 * Findet alle Klassen die mit der angegebenen Annotation annotiert wurden. Die vollstaendigen Namen
	 * Klassen werden anschliessend in einem Set zurueckgegeben.
	 *
	 * @param annotationCls Die Annotation
	 * @return Das Set der Klassennamen
	 */
	public SortedSet<String> findeKlassenMitAnnotation(Class<? extends Annotation> annotationCls)
	{
		return new TreeSet<>(this.annotationDb.getAnnotationIndex().get(annotationCls.getName()));
	}
}
