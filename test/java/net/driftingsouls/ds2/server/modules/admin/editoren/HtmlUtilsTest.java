package net.driftingsouls.ds2.server.modules.admin.editoren;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HtmlUtilsTest
{
	@Test
	public void gegebenMinimaleParameter_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();

		// run
		HtmlUtils.select(out, name, false, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"></select>", out.toString());
	}

	@Test
	public void gegebenGenauEineOption_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");

		// run
		HtmlUtils.select(out, name, false, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"1\">Foo</option></select>", out.toString());
	}

	@Test
	public void gegebenMehrereOptions_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		HtmlUtils.select(out, name, false, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"1\">Foo</option><option value=\"2\">Bar</option><option value=\"3\">FooFoo</option></select>", out.toString());
	}

	@Test
	public void gegebenReadOnlyUndMinimaleParameter_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();

		// run
		HtmlUtils.select(out, name, true, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" disabled=\"disabled\" name=\"test\"></select>", out.toString());
	}

	@Test
	public void gegebenMinimaleParameterUndEinNichtInDenOptionsVorkommenderAktiverWert_select_sollteEinPassendesHtmlSelectErzeugenUndDenWertIgnorieren()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();

		// run
		HtmlUtils.select(out, name, false, options, 42);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"></select>", out.toString());
	}

	@Test
	public void gegebenMehrereOptionsUndEinAktiverWert_select_sollteEinPassendesHtmlSelectErzeugenUndDiePassendeOptionAktivieren()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		HtmlUtils.select(out, name, false, options, 3);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"1\">Foo</option><option value=\"2\">Bar</option><option value=\"3\" selected=\"selected\">FooFoo</option></select>", out.toString());
	}

	@Test
	public void gegebenMehrereOptionsMitEinemAktivenNullEintrag_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(null, ":P");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		HtmlUtils.select(out, name, false, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"\" selected=\"selected\">:P</option><option value=\"2\">Bar</option><option value=\"3\">FooFoo</option></select>", out.toString());
	}

	@Test
	public void gegebenMehrereOptionsMitEinemNichtAktivenNullEintrag_select_sollteEinPassendesHtmlSelectErzeugen()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(null, ":P");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		HtmlUtils.select(out, name, false, options, 3);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"\">:P</option><option value=\"2\">Bar</option><option value=\"3\" selected=\"selected\">FooFoo</option></select>", out.toString());
	}
}
