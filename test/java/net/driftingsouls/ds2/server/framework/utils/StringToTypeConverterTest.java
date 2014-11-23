package net.driftingsouls.ds2.server.framework.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringToTypeConverterTest
{
	@Test
	public void gegebenEinZahlAlsStringUndIntegerAlsZieldatentyp_convert_sollteDenStringInEinIntegerWertKonvertieren() throws Exception
	{
		// setup
		String valueStr = "42";

		// run
		Integer value = StringToTypeConverter.convert(Integer.class, valueStr);

		// assert
		assertEquals((Integer)42, value);
	}

	@Test
	public void gegebenEinStringUndStringAlsZieldatentyp_convert_sollteDenStringUnveraendertZurueckgeben() throws Exception
	{
		// setup
		String valueStr = "ABCtest42";

		// run
		String value = StringToTypeConverter.convert(String.class, valueStr);

		// assert
		assertEquals(valueStr, value);
	}

	@Test
	public void gegebenEinBooleanAlsStringUndBooleanAlsZieldatentyp_convert_sollteDenStringInEinBooleanKonvertieren() throws Exception
	{
		// setup
		String valueStr = Boolean.TRUE.toString();

		// run
		Boolean value = StringToTypeConverter.convert(Boolean.class, valueStr);

		// assert
		assertEquals(Boolean.TRUE, value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenEinDoubleAlsStringUndIntegerAlsZieldatentyp_convert_sollteEineIllegalArgumentExceptionWerfen() throws Exception
	{
		// setup
		String valueStr = "1.42";

		// run
		StringToTypeConverter.convert(Integer.class, valueStr);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void gegebenEinBeliebigerStringUndDieseKlasseAlsZieldatentyp_convert_sollteEineUnsupportedOperationExceptionWerfen() throws Exception
	{
		// setup
		String valueStr = "1.42";

		// run
		StringToTypeConverter.convert(getClass(), valueStr);
	}

	@Test
	public void gegebenNullAlsWertUndEineBeliebigeKlasseAlsZieldatentyp_convert_sollteNullZurueckgeben() throws Exception
	{
		// setup
		String valueStr = null;

		// run
		Integer convert = StringToTypeConverter.convert(Integer.class, valueStr);

		// assert
		assertNull(convert);
	}

	@Test
	public void gegebenEinDoubleAlsWertUndBigDecimalAlsZieldatentyp_convert_sollteDenStringInEinBigDecimalKonvertieren() throws Exception
	{
		// setup
		String valueStr = "42.01";

		// run
		BigDecimal convert = StringToTypeConverter.convert(BigDecimal.class, valueStr);

		// assert
		assertEquals(new BigDecimal("42.01"), convert);
	}

	@Test
	public void gegebenEinIntegerAlsWertUndBigIntegerAlsZieldatentyp_convert_sollteDenStringInEinBigIntegerKonvertieren() throws Exception
	{
		// setup
		String valueStr = "4242";

		// run
		BigInteger convert = StringToTypeConverter.convert(BigInteger.class, valueStr);

		// assert
		assertEquals(BigInteger.valueOf(4242), convert);
	}

	@Test
	public void gegebenEinEinfachesIntAlsWert_convert_sollteEinIntegerZurueckgeben() throws Exception
	{
		// setup
		String valueStr = "42";

		// run
		Integer value = StringToTypeConverter.convert(Integer.TYPE, valueStr);

		// assert
		assertEquals((Integer)42, value);
	}

	@Test
	public void gegebenEinEinfachesBooleanAlsWert_convert_sollteEinBooleanZurueckgeben() throws Exception
	{
		// setup
		String valueStr = Boolean.TRUE.toString();

		// run
		Boolean value = StringToTypeConverter.convert(Boolean.TYPE, valueStr);

		// assert
		assertTrue(value);
	}
}
