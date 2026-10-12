package net.driftingsouls.ds2.server.framework.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringToTypeConverterTest
{
	@Test
	public void givenANumberAsStringAndIntegerAsTargetType_convert_shouldConvertTheStringToAnInteger() {
		// setup
		String valueStr = "42";

		// run
		Integer value = StringToTypeConverter.convert(Integer.class, valueStr);

		// assert
		assertEquals((Integer)42, value);
	}

	@Test
	public void givenAStringAndStringAsTargetType_convert_shouldReturnTheStringUnchanged() {
		// setup
		String valueStr = "ABCtest42";

		// run
		String value = StringToTypeConverter.convert(String.class, valueStr);

		// assert
		assertEquals(valueStr, value);
	}

	@Test
	public void givenABooleanAsStringAndBooleanAsTargetType_convert_shouldConvertTheStringToABoolean() {
		// setup
		String valueStr = Boolean.TRUE.toString();

		// run
		Boolean value = StringToTypeConverter.convert(Boolean.class, valueStr);

		// assert
		assertEquals(Boolean.TRUE, value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void givenADoubleAsStringAndIntegerAsTargetType_convert_shouldThrowAnIllegalArgumentException() {
		// setup
		String valueStr = "1.42";

		// run
		StringToTypeConverter.convert(Integer.class, valueStr);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void givenAnyStringAndThisClassAsTargetType_convert_shouldThrowAnUnsupportedOperationException() {
		// setup
		String valueStr = "1.42";

		// run
		StringToTypeConverter.convert(getClass(), valueStr);
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void givenNullAsValueAndAnyClassAsTargetType_convert_shouldReturnNull() {
		// setup
		String valueStr = null;

		// run
		Integer convert = StringToTypeConverter.convert(Integer.class, valueStr);

		// assert
		assertNull(convert);
	}

	@Test
	public void givenADoubleAsValueAndBigDecimalAsTargetType_convert_shouldConvertTheStringToABigDecimal() {
		// setup
		String valueStr = "42.01";

		// run
		BigDecimal convert = StringToTypeConverter.convert(BigDecimal.class, valueStr);

		// assert
		assertEquals(new BigDecimal("42.01"), convert);
	}

	@Test
	public void givenAnIntegerAsValueAndBigIntegerAsTargetType_convert_shouldConvertTheStringToABigInteger() {
		// setup
		String valueStr = "4242";

		// run
		BigInteger convert = StringToTypeConverter.convert(BigInteger.class, valueStr);

		// assert
		assertEquals(BigInteger.valueOf(4242), convert);
	}

	@Test
	public void givenAPrimitiveIntAsValue_convert_shouldReturnAnInteger() {
		// setup
		String valueStr = "42";

		// run
		Integer value = StringToTypeConverter.convert(Integer.TYPE, valueStr);

		// assert
		assertEquals((Integer)42, value);
	}

	@Test
	public void givenAPrimitiveBooleanAsValue_convert_shouldReturnABoolean() {
		// setup
		String valueStr = Boolean.TRUE.toString();

		// run
		Boolean value = StringToTypeConverter.convert(Boolean.TYPE, valueStr);

		// assert
		assertTrue(value);
	}
}
