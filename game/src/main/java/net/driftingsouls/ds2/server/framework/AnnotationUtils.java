package net.driftingsouls.ds2.server.framework;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.annotation.Annotation;

/**
 * Hilfsmethoden zum Umgang mit Annotations.
 */
public class AnnotationUtils
{
	public static final AnnotationUtils INSTANCE = new AnnotationUtils();

	private AnnotationUtils() {}

	/**
	 * Loads a scanner for all ds packages
	 */
	public ScanResult scanDsClasses()
	{
		return new ClassGraph()
			.enableAllInfo()
			.acceptPackages("net.driftingsouls.ds2.server")
			.scan();
	}
}
