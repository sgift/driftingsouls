package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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

	private enum TestEnum {
		TEST1,
		TEST2,
		TEST3;

		@Override
		public String toString()
		{
			return "foobar!";
		}
	}

	@Test
	public void gegebenMehrereEnumOptions_select_sollteEinPassendesHtmlSelectErzeugenUndDabeiNameStattToStringFuerDenValueVerwenden()
	{
		// setup
		StringBuilder out = new StringBuilder();
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(TestEnum.TEST1, TestEnum.TEST1);
		options.put(TestEnum.TEST2, TestEnum.TEST2);
		options.put(TestEnum.TEST3, TestEnum.TEST3);

		// run
		HtmlUtils.select(out, name, false, options, null);

		// assert
		assertTrue(out.length() > 0);
		assertEquals("<select size=\"1\" name=\"test\"><option value=\"TEST1\">foobar!</option><option value=\"TEST2\">foobar!</option><option value=\"TEST3\">foobar!</option></select>", out.toString());
	}





	@Test
	public void gegebenMinimaleParameter_jsSelect_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, null);

		// assert
		assertNotNull(model);
		assertEquals("select", model.typ);
		assertEquals(name, model.name);
		assertEquals(0, model.options.size());
		assertNull(model.nullOption);
		assertFalse(model.disabled);
		assertNull(model.selected);
	}

	@Test
	public void gegebenGenauEineOption_jsSelect_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, null);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(1, model.options.size());
		assertEquals("Foo", model.options.get("1"));
		assertNull(model.nullOption);
		assertFalse(model.disabled);
		assertNull(model.selected);
	}

	@Test
	public void gegebenMehrereOptions_jsSelect_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, null);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(3, model.options.size());
		assertEquals("Foo", model.options.get("1"));
		assertEquals("Bar", model.options.get("2"));
		assertEquals("FooFoo", model.options.get("3"));
		assertNull(model.nullOption);
		assertFalse(model.disabled);
		assertNull(model.selected);
	}

	@Test
	public void gegebenReadOnlyUndMinimaleParameter_jsSelect_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, true, options, null);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(0, model.options.size());
		assertNull(model.nullOption);
		assertTrue(model.disabled);
		assertNull(model.selected);
	}

	@Test
	public void gegebenMehrereOptionsUndEinAktiverWert_jsSelect_sollteEinPassendesViewModelErzeugenUndDiePassendeOptionAktivieren()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(1, "Foo");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, 3);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(3, model.options.size());
		assertEquals("Foo", model.options.get("1"));
		assertEquals("Bar", model.options.get("2"));
		assertEquals("FooFoo", model.options.get("3"));
		assertNull(model.nullOption);
		assertFalse(model.disabled);
		assertEquals("3", model.selected);
	}

	@Test
	public void gegebenMehrereOptionsMitEinemNullEintrag_jsSelect_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(null, ":P");
		options.put(2, "Bar");
		options.put(3, "FooFoo");

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, null);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(2, model.options.size());
		assertEquals("Bar", model.options.get("2"));
		assertEquals("FooFoo", model.options.get("3"));
		assertEquals(":P", model.nullOption);
		assertFalse(model.disabled);
		assertNull(model.selected);
	}

	@Test
	public void gegebenMehrereEnumOptions_jsSelect_sollteEinPassendesViewModelErzeugenUndDabeiNameStattToStringFuerDenValueVerwenden()
	{
		// setup
		String name = "test";
		Map<Serializable,Object> options = new HashMap<>();
		options.put(TestEnum.TEST1, TestEnum.TEST1);
		options.put(TestEnum.TEST2, TestEnum.TEST2);
		options.put(TestEnum.TEST3, TestEnum.TEST3);

		// run
		SelectViewModel model = HtmlUtils.jsSelect(name, false, options, null);

		// assert
		assertNotNull(model);
		assertEquals(name, model.name);
		assertEquals(3, model.options.size());
		assertEquals("foobar!", model.options.get("TEST1"));
		assertEquals("foobar!", model.options.get("TEST2"));
		assertEquals("foobar!", model.options.get("TEST3"));
		assertNull(model.nullOption);
		assertFalse(model.disabled);
		assertNull(model.selected);
	}


	@Test
	public void gegebenMinimaleParameter_jsTextInput_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		String value = "1234";

		// run
		TextFieldViewModel model = HtmlUtils.jsTextInput(name, false, String.class, value);

		// assert
		assertNotNull(model);
		assertEquals("textfield", model.typ);
		assertEquals(value, model.value);
		assertEquals(name, model.name);
		assertEquals(name, model.id);
		assertFalse(model.disabled);
		assertNull(model.autoNumeric);
	}

	@Test
	public void gegebenNullAlsWert_jsTextInput_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";

		// run
		TextFieldViewModel model = HtmlUtils.jsTextInput(name, false, String.class, null);

		// assert
		assertNotNull(model);
		assertEquals("textfield", model.typ);
		assertEquals("", model.value);
		assertEquals(name, model.name);
		assertEquals(name, model.id);
		assertFalse(model.disabled);
		assertNull(model.autoNumeric);
	}

	@Test
	public void gegebenEinIntegerWert_jsTextInput_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		int value = 1234;

		// run
		TextFieldViewModel model = HtmlUtils.jsTextInput(name, false, Integer.class, value);

		// assert
		assertNotNull(model);
		assertEquals("textfield", model.typ);
		assertEquals("1234", model.value);
		assertEquals(name, model.name);
		assertEquals(name, model.id);
		assertFalse(model.disabled);
		assertNotNull(model.autoNumeric);
		assertEquals(Integer.MIN_VALUE, model.autoNumeric.vMin);
		assertEquals(Integer.MAX_VALUE, model.autoNumeric.vMax);
		assertEquals(0, model.autoNumeric.mDec);
	}

	@Test
	public void gegebenEinNurLesbaresFeld_jsTextInput_sollteEinPassendesViewModelErzeugen()
	{
		// setup
		String name = "test";
		String value = "1234";

		// run
		TextFieldViewModel model = HtmlUtils.jsTextInput(name, true, String.class, value);

		// assert
		assertNotNull(model);
		assertEquals("textfield", model.typ);
		assertEquals(value, model.value);
		assertEquals(name, model.name);
		assertEquals(name, model.id);
		assertTrue(model.disabled);
		assertNull(model.autoNumeric);
	}
}
