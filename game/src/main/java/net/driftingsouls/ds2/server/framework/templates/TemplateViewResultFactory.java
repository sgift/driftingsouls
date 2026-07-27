package net.driftingsouls.ds2.server.framework.templates;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class TemplateViewResultFactory
{
	private static final String MASTERTEMPLATE = "__MASTERTEMPLATE";
	private final Version version;
	private final TemplateLoader templateLoader;

	@Autowired
	public TemplateViewResultFactory(Version version, TemplateLoader templateLoader)
	{
		this.version = version;
		this.templateLoader = templateLoader;
	}

	/**
	 * Leitet den Namen der Templatedatei aus dem Namen einer Controllerklasse ab.
	 * Diese Kopplung ist zur Uebersetzungszeit nicht pruefbar: Wird eine Controllerklasse
	 * umbenannt, ohne die zugehoerige Templatedatei mit umzubenennen, faellt das erst zur
	 * Laufzeit beim Laden der Seite auf. Sichtbar, damit ein Test die Kopplung pruefen kann.
	 * @param controllerClass Die Controllerklasse
	 * @return Der Name der Templatedatei
	 */
	public static String templateNameFor(Class<?> controllerClass)
	{
		return templateNameFor(controllerClass.getSimpleName());
	}

	/**
	 * Leitet den Namen der Templatedatei aus dem einfachen Namen einer Controllerklasse ab.
	 * @param simpleClassName Der einfache Name der Controllerklasse
	 * @return Der Name der Templatedatei
	 */
	public static String templateNameFor(String simpleClassName)
	{
		String clsName = simpleClassName;
		if( clsName.endsWith("Controller") ) {
			clsName = clsName.substring(0, clsName.length()-"Controller".length());
		}
		return clsName.toLowerCase() + ".html";
	}

	private void setzeTemplateViaControllerNamen(TemplateEngine engine, Object controller)
	{
		setTemplate(engine, templateNameFor(controller.getClass()));
	}

	private TemplateEngine createTemplateEngine(String masterTemplateId)
	{
		TemplateEngine templateEngine = new TemplateEngine(masterTemplateId, templateLoader);

		templateEngine.setVar("global.module", ContextMap.getContext().getRequest().getParameter("module"));
		templateEngine.setVar("global.version", version.getVersion());

		return templateEngine;
	}

	private void setTemplate(TemplateEngine templateEngine, String file)
	{
		if (!templateEngine.setFile(MASTERTEMPLATE, file))
		{
			throw new IllegalArgumentException("Konnte Template nicht laden: "+file);
		}

		if (ContextMap.getContext().getActiveUser() != null)
		{
			ContextMap.getContext().getActiveUser().setTemplateVars(templateEngine);
		}
	}

	/**
	 * Erzeugt eine leere Templateinstanz ohne geladenes Haupttemplate.
	 * @return Die Templateinstanz
	 */
	public TemplateEngine createEmpty()
	{
		return createTemplateEngine(MASTERTEMPLATE);
	}

	/**
	 * Erzeugt eine Templateinstanz fuer die angegebene Controllerklasse.
	 * Der Templatename wird aus dem Namen des Controllers abgeleitet.
	 * @param controller Der Controller
	 * @return Die Templateinstanz
	 */
	public TemplateEngine createFor(@Nonnull Object controller)
	{
		TemplateEngine engine = createTemplateEngine(MASTERTEMPLATE);
		setzeTemplateViaControllerNamen(engine, controller);

		return engine;
	}

	/**
	 * Erzeugt eine Templateinstanz fuer das angegebene Template.
	 * @param templateName Der Controller
	 * @return Die Templateinstanz
	 */
	public TemplateEngine createForTemplate(@Nonnull String templateName)
	{
		TemplateEngine engine = createTemplateEngine(MASTERTEMPLATE);
		setTemplate(engine, templateName);

		return engine;
	}
}
