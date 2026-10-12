package net.driftingsouls.ds2.server.modules.admin.editors;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HtmlUtilsTest
{
	@Test
	public void givenMinimalParameters_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenExactlyOneOption_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenSeveralOptions_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenReadOnlyAndMinimalParameters_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenMinimalParametersAndASelectedValueNotAmongTheOptions_select_shouldCreateAMatchingHtmlSelectAndIgnoreTheValue()
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
	public void givenSeveralOptionsAndASelectedValue_select_shouldCreateAMatchingHtmlSelectAndSelectTheMatchingOption()
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
	public void givenSeveralOptionsWithASelectedNullEntry_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenSeveralOptionsWithAnUnselectedNullEntry_select_shouldCreateAMatchingHtmlSelect()
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
	public void givenSeveralEnumOptions_select_shouldCreateAMatchingHtmlSelectUsingNameInsteadOfToStringForTheValue()
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
	public void givenMinimalParameters_jsSelect_shouldCreateAMatchingViewModel()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();

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
	public void givenExactlyOneOption_jsSelect_shouldCreateAMatchingViewModel()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();
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
	public void givenSeveralOptions_jsSelect_shouldCreateAMatchingViewModel()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();
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
	public void givenReadOnlyAndMinimalParameters_jsSelect_shouldCreateAMatchingViewModel()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();

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
	public void givenSeveralOptionsAndASelectedValue_jsSelect_shouldCreateAMatchingViewModelAndSelectTheMatchingOption()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();
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
	public void givenSeveralOptionsWithANullEntry_jsSelect_shouldCreateAMatchingViewModel()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();
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
	public void givenSeveralEnumOptions_jsSelect_shouldCreateAMatchingViewModelUsingNameInsteadOfToStringForTheValue()
	{
		// setup
		String name = "test";
		Map<Object, Object> options = new HashMap<>();
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
	public void givenMinimalParameters_jsTextInput_shouldCreateAMatchingViewModel()
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
	public void givenNullAsValue_jsTextInput_shouldCreateAMatchingViewModel()
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
	public void givenAnIntegerValue_jsTextInput_shouldCreateAMatchingViewModel()
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
	public void givenAReadonlyField_jsTextInput_shouldCreateAMatchingViewModel()
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
