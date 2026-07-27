package net.driftingsouls.ds2.server.framework.templates;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards the coupling between a controller's class name and its template file name.
 *
 * <p>{@link TemplateViewResultFactory#createFor(Object)} derives the template file name from the
 * simple name of the controller class. Renaming a controller without renaming its template therefore
 * compiles cleanly and only fails at runtime, when the page is opened, with
 * "Konnte Template nicht laden". This test turns that into a build failure.
 *
 * <p>Only controllers that actually use the derived path are checked. Whether a controller calls
 * {@code createFor(this)} is not visible via reflection, so the source files are scanned for it —
 * controllers naming their template explicitly via {@code createForTemplate} are free to be renamed.
 *
 * <p>The check is against the template <em>source</em> file rather than the generated class on the
 * classpath, deliberately. {@code TemplateCompiler} does not clean its output directory, so a renamed
 * or deleted template leaves its previously generated class behind in {@code target}; resolving via
 * {@link TemplateLoader} would keep succeeding against that stale artifact and the test would guard
 * nothing on an incremental build.
 */
public class ControllerTemplateResolutionTest
{
	private static final String DERIVED_TEMPLATE_CALL = "createFor(this)";

	@Test
	public void everyControllerDerivingItsTemplateNameResolvesThatTemplate() throws IOException
	{
		Path sourceRoot = Paths.get("src", "main", "java");
		assertTrue("Source root not found, run this test with the game module as working directory: "
				+ sourceRoot.toAbsolutePath(), Files.isDirectory(sourceRoot));

		List<Path> controllerSources;
		try (Stream<Path> paths = Files.walk(sourceRoot))
		{
			controllerSources = paths
					.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
					.sorted()
					.collect(Collectors.toList());
		}

		Path templateRoot = Paths.get("src", "main", "templates");
		assertTrue("Template root not found: " + templateRoot.toAbsolutePath(), Files.isDirectory(templateRoot));

		List<String> checked = new ArrayList<>();
		List<String> unresolved = new ArrayList<>();

		for (Path source : controllerSources)
		{
			if (!new String(Files.readAllBytes(source), StandardCharsets.UTF_8).contains(DERIVED_TEMPLATE_CALL))
			{
				continue;
			}

			String fileName = source.getFileName().toString();
			String className = fileName.substring(0, fileName.length() - ".java".length());
			String templateName = TemplateViewResultFactory.templateNameFor(className);

			checked.add(className);
			if (!Files.isRegularFile(templateRoot.resolve(templateName)))
			{
				unresolved.add(className + " -> " + templateName);
			}
		}

		assertTrue("Found no controller using " + DERIVED_TEMPLATE_CALL
				+ ", so this test is no longer guarding anything", checked.size() > 0);
		assertEquals("Controllers whose derived template does not resolve. Rename the template file in "
				+ "src/main/templates to match, or switch the controller to createForTemplate: " + unresolved,
				new ArrayList<String>(), unresolved);
	}
}
