package net.driftingsouls.ds2.server.modules.admin.editors;

import net.driftingsouls.ds2.server.framework.TestRequest;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.*;

public class FieldGeneratorTest
{
	private static class DummyEntity
	{
		public boolean bool;
		public String string;
		public int zahl;
	}

	@Test
	public void givenABooleanFieldWithValueFalse_generate_shouldCreateACheckbox() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"checkbox\" name=\"foobar\" value=\"true\" />", extractInput(out));
	}

	@Test
	public void givenAStringFieldWithoutValue_generate_shouldCreateATextField() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" name=\"foobar\" value=\"\" />", extractInput(out));
	}

	@Test
	public void givenAnIntegerField_generate_shouldCreateATextField() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Integer> generator = new FieldGenerator<>("Test", "foobar", Integer.class, Integer.class, (e) -> e.zahl, (e,b) -> e.zahl = b);
		DummyEntity entity = new DummyEntity();
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" name=\"foobar\" value=\"0\" />", extractInput(out));
	}

	@Test
	public void givenABooleanFieldWithValueTrue_generate_shouldCreateACheckedCheckbox() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();
		entity.bool = true;
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"checkbox\" name=\"foobar\" value=\"true\" checked=\"checked\" />", extractInput(out));
	}

	@Test
	public void givenAStringFieldWithValue_generate_shouldCreateATextFieldWithThatContent() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		entity.string = "Content";
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" name=\"foobar\" value=\"Content\" />", extractInput(out));
	}

	@Test
	public void givenAStringFieldWithHtmlCharactersInTheValue_generate_shouldCreateATextFieldWithThatContentEscaped() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		entity.string = "<Content\"&";
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" name=\"foobar\" value=\"&lt;Content&quot;&amp;\" />", extractInput(out));
	}

	@Test
	public void givenAReadonlyBooleanFieldWithValueTrue_generate_shouldCreateACheckedReadonlyCheckbox() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		generator.readOnly(true);
		DummyEntity entity = new DummyEntity();
		entity.bool = true;
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"checkbox\" name=\"foobar\" value=\"true\" checked=\"checked\" disabled=\"disabled\" />", extractInput(out));
	}

	@Test
	public void givenAReadonlyIntegerField_generate_shouldCreateATextField() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Integer> generator = new FieldGenerator<>("Test", "foobar", Integer.class, Integer.class, (e) -> e.zahl, (e,b) -> e.zahl = b);
		generator.readOnly(true);
		DummyEntity entity = new DummyEntity();
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" disabled=\"disabled\" name=\"foobar\" value=\"0\" />", extractInput(out));
	}

	@Test
	public void givenAReadonlyStringFieldWithValue_generate_shouldCreateAReadonlyTextFieldWithThatContent() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		generator.readOnly(true);
		DummyEntity entity = new DummyEntity();
		entity.string = "Content";
		StringBuilder out = new StringBuilder();

		// run
		generator.generate(out, entity);

		// assert
		assertEquals("<input type=\"text\" id=\"foobar\" disabled=\"disabled\" name=\"foobar\" value=\"Content\" />", extractInput(out));
	}

	@Test
	public void givenABooleanFieldAndARequestWithValueTrue_applyRequestValues_shouldSetTheFieldToTrue() {
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();
		Request request = new TestRequest("foobar", "true");

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertTrue(entity.bool);
	}

	@Test
	public void givenAStringFieldAndARequestWithValue_applyRequestValues_shouldSetTheFieldToThatValue() {
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		Request request = new TestRequest("foobar", "42");

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertEquals("42", entity.string);
	}

	@Test
	public void givenABooleanFieldAndARequestWithoutValue_applyRequestValues_shouldSetTheFieldToFalse() {
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();
		entity.bool = true;
		Request request = new TestRequest("foobar", null);

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertFalse(entity.bool);
	}

	@Test
	public void givenAnIntegerFieldAndARequestWithValue10_applyRequestValues_shouldSetTheFieldTo10() {
		// setup
		FieldGenerator<DummyEntity,Integer> generator = new FieldGenerator<>("Test", "foobar", Integer.class, Integer.class, (e) -> e.zahl, (e,b) -> e.zahl = b);
		DummyEntity entity = new DummyEntity();
		Request request = new TestRequest("foobar", "10");

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertEquals(10, entity.zahl);
	}

	@Test
	public void givenAStringFieldAndARequestWithoutValue_applyRequestValues_shouldNotUpdateTheField() {
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		entity.string = "test";
		Request request = new TestRequest("foobar", null);

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertEquals("test", entity.string);
	}

	@Test
	public void givenAnIntegerFieldAndARequestWithoutValue_applyRequestValues_shouldNotUpdateTheField() {
		// setup
		FieldGenerator<DummyEntity,Integer> generator = new FieldGenerator<>("Test", "foobar", Integer.class, Integer.class, (e) -> e.zahl, (e,b) -> e.zahl = b);
		DummyEntity entity = new DummyEntity();
		entity.zahl = 10;
		Request request = new TestRequest("foobar", null);

		// run
		generator.applyRequestValues(request, entity);

		// assert
		assertEquals(10, entity.zahl);
	}

	@Test
	public void givenABooleanWithValueFalse_serializedValueOf_shouldReturnFalse() {
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("false", value);
	}

	@Test
	public void givenAStringFieldWithValue_serializedValueOf_shouldReturnTheValue() {
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		entity.string = "Test";

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("Test", value);
	}

	@Test
	public void givenAnIntegerFieldWithValue_serializedValueOf_shouldReturnTheValue() {
		// setup
		FieldGenerator<DummyEntity,Integer> generator = new FieldGenerator<>("Test", "foobar", Integer.class, Integer.class, (e) -> e.zahl, (e,b) -> e.zahl = b);
		DummyEntity entity = new DummyEntity();
		entity.zahl = 10;

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("10", value);
	}

	@Test
	public void givenABooleanFieldWithValueTrue_serializedValueOf_shouldReturnTrue() {
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();
		entity.bool = true;

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("true", value);
	}

	@Test
	public void givenAStringWithValueNull_serializedValueOf_shouldReturnAnEmptyString() {
		// setup
		FieldGenerator<DummyEntity,String> generator = new FieldGenerator<>("Test", "foobar", String.class, String.class, (e) -> e.string, (e,b) -> e.string = b);
		DummyEntity entity = new DummyEntity();
		entity.string = null;

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("", value);
	}

	private static String extractInput(StringBuilder out)
	{
		int start = out.indexOf("<input");
		if( start == -1 )
		{
			return out.toString();
		}
		int end = out.indexOf(">", start);
		if( end == -1 )
		{
			return out.substring(start);
		}
		return out.substring(start, end+1);
	}
}
