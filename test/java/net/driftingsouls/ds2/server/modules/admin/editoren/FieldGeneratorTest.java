package net.driftingsouls.ds2.server.modules.admin.editoren;

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
	public void gegebenEinBooleanFeldMitWertFalse_generate_sollteEineCheckboxErzeugen() throws IOException
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
	public void gegebenEinStringFeldOhneWert_generate_sollteEinTextfeldErzeugen() throws IOException
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
	public void gegebenEinIntegerFeld_generate_sollteEinTextfeldErzeugen() throws IOException
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
	public void gegebenEinBooleanFeldMitWertTrue_generate_sollteEineAktivierteCheckboxErzeugen() throws IOException
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
	public void gegebenEinStringFeldMitWert_generate_sollteEinTextfeldMitEntsprechendemInhaltErzeugen() throws IOException
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
	public void gegebenEinStringFeldMitHtmlZeichenImWert_generate_sollteEinTextfeldMitEntsprechendemEscaptenInhaltErzeugen() throws IOException
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
	public void gegebenEinReadonlyBooleanFeldMitWertTrue_generate_sollteEineAktivierteNurLesbareCheckboxErzeugen() throws IOException
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
	public void gegebenEinReadonlyIntegerFeld_generate_sollteEinTextfeldErzeugen() throws IOException
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
	public void gegebenEinReadonlyStringFeldMitWert_generate_sollteEinTextfeldMitEntsprechendemInhaltNurLesbarErzeugen() throws IOException
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
	public void gegebenEinBooleanFeldUndEineRequestMitWertTrue_applyRequestValues_sollteDasFeldEntsprechendAufTrueSetzen() throws IOException
	{
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
	public void gegebenEinStringFeldUndEineRequestMitWert_applyRequestValues_sollteDasFeldEntsprechendAufDenWertSetzen() throws IOException
	{
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
	public void gegebenEinBooleanFeldUndEineRequestOhneWert_applyRequestValues_sollteDasFeldEntsprechendAufFalseSetzen() throws IOException
	{
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
	public void gegebenEinIntegerFeldUndEineRequestMitWert10_applyRequestValues_sollteEinDasFeldAuf10Setzen() throws IOException
	{
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
	public void gegebenEinStringFeldUndEineRequestOhneWert_applyRequestValues_sollteDasFeldNichtAktualisieren() throws IOException
	{
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
	public void gegebenEinIntegerFeldUndEineRequestOhneWert_applyRequestValues_sollteEinDasFeldNichtAktualisieren() throws IOException
	{
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
	public void gegebenEinBooleanMitWertFalse_serializedValueOf_sollteFalseZurueckgeben() throws IOException
	{
		// setup
		FieldGenerator<DummyEntity,Boolean> generator = new FieldGenerator<>("Test", "foobar", Boolean.class, Boolean.class, (e) -> e.bool, (e,b) -> e.bool = b);
		DummyEntity entity = new DummyEntity();

		// run
		String value = generator.serializedValueOf(entity);

		// assert
		assertEquals("false", value);
	}

	@Test
	public void gegebenEinStringFeldMitWert_serializedValueOf_sollteDenWertZurueckgeben() throws IOException
	{
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
	public void gegebenEinIntegerFeldMitWert_serializedValueOf_sollteDenWertZurueckgeben() throws IOException
	{
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
	public void gegebenEinBooleanFeldMitWertTrue_serializedValueOf_sollteTrueZurueckgeben() throws IOException
	{
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
	public void gegebenEinStringMitWertNull_serializedValueOf_sollteEinenLeerstringZurueckgeben() throws IOException
	{
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
