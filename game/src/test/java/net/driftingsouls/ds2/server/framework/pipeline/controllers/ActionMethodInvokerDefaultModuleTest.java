package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.RedirectToPortalController;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@link net.driftingsouls.ds2.server.framework.pipeline.configuration.PipelineConfig} falls back to
 * the default module for every module name it does not know, and passes the request's original
 * action along with it. {@link RedirectToPortalController} is that fallback and only declares a
 * default action, so any stale bookmark, old link or crawler request carrying an unknown action -
 * {@code infosAgb} was the one seen in production - resolved to no method at all.
 *
 * <p>The resulting {@code NoSuchMethodException} is not classified by {@code ErrorHandlerFilter},
 * so it counted as an unexpected error: a mail to the developers per hit, and a generic error page
 * for a visitor who should simply have been sent to the portal.
 */
public class ActionMethodInvokerDefaultModuleTest
{
	@Test
	public void getMethodForAction_onTheDefaultModule_fallsBackToTheDefaultActionForAnUnknownAction()
			throws NoSuchMethodException
	{
		Method method = new ActionMethodInvoker()
				.getMethodForAction(new RedirectToPortalController(), "infosAgb");

		assertEquals("defaultAction", method.getName());
	}

	@Test
	public void getMethodForAction_onTheDefaultModule_stillResolvesAKnownAction()
			throws NoSuchMethodException
	{
		Method method = new ActionMethodInvoker()
				.getMethodForAction(new RedirectToPortalController(), "default");

		assertEquals("defaultAction", method.getName());
	}

	/**
	 * The fallback must stay confined to the default module. An unknown action on a real controller
	 * is a broken link or a renamed action and has to keep failing loudly, rather than silently
	 * running whatever that controller's default action happens to do.
	 */
	@Test
	public void getMethodForAction_onAnOrdinaryController_stillThrowsForAnUnknownAction()
	{
		try
		{
			new ActionMethodInvoker().getMethodForAction(new OrdinaryController(), "infosAgb");
			fail("An unknown action on an ordinary controller must not resolve to its default action");
		}
		catch (NoSuchMethodException e)
		{
			// expected
		}
	}

	/**
	 * Deliberately carries no {@code @Module} annotation: annotated classes under test-classes are
	 * picked up by the module scan, and this one only needs to stand in for "not the default module".
	 */
	private static class OrdinaryController extends Controller
	{
		@Action(ActionType.DEFAULT)
		public TemplateEngine defaultAction()
		{
			return null;
		}
	}
}
