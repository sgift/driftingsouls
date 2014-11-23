package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.framework.TestRequest;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParameterReaderTest extends DBSingleTransactionTest
{
	@Test
	public void gegebenEinStringAlsZieltypUndEineLeereRequest_readParameterAsType_sollteEinenLeerstringZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = String.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals("", result);
	}

	@Test
	public void gegebenEinStringAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteEinenLeerstringZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "123").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = String.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals("123", result);
	}

	@Test
	public void gegebenEineMapAlsZieltypUndEineLeereRequest_readParameterAsType_sollteEineLeereMapZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		assertEquals(0, ((Map)result).size());
	}

	@Test
	public void gegebenEinIntAlsZieltypUndEineLeereRequest_readParameterAsType_sollte0Zurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Integer.TYPE;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(0, result);
	}

	@Test
	public void gegebenEinIntAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "123").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Integer.TYPE;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(123, result);
	}

	@Test
	public void gegebenEinIntegerAlsZieltypUndEineLeereRequest_readParameterAsType_sollteNullZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Integer.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertNull(result);
	}

	@Test
	public void gegebenEinIntegerAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "123").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Integer.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(123, result);
	}

	@Test
	public void gegebenEinPrimitivesBooleanAlsZieltypUndEineRequestMitPassendemTrueWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "1").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.TYPE;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(true, result);
	}

	@Test
	public void gegebenEinPrimitivesBooleanAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "0").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.TYPE;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(false, result);
	}

	@Test
	public void gegebenEinPrimitivesBooleanAlsZieltypUndEineLeereRequest_readParameterAsType_sollteFalseZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.TYPE;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(false, result);
	}

	@Test
	public void gegebenEinBooleanAlsZieltypUndEineLeereRequest_readParameterAsType_sollteNullZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertNull(result);
	}

	@Test
	public void gegebenEinBooleanAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "0").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(false, result);
	}

	@Test
	public void gegebenEinBooleanAlsZieltypUndEineRequestMitPassendemTrueWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "1").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Boolean.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(true, result);
	}

	@Test
	public void gegebenEineEntityAlsZieltypUndEineRequestMitPassendemWert_readParameterAsType_sollteDenTransformiertenWertZurueckgeben()
	{
		// setup
		Rasse rasse = persist(new Rasse("test", false));
		Request req = new TestRequest("test", Integer.toString(rasse.getId())).addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Rasse.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertEquals(rasse, result);
	}

	@Test
	public void gegebenEineEntityAlsZieltypUndEineRequestMitNichtInDerDBVorhandenemWert_readParameterAsType_sollteNullZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test", "1").addParameter("nichtRelevant", "foobar");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Rasse.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertNull(result);
	}

	@Test
	public void gegebenEineEntityAlsZieltypUndEineLeereRequest_readParameterAsType_sollteNullZurueckgeben()
	{
		// setup
		Request req = new TestRequest();
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = Rasse.class;

		// run
		Object result = reader.readParameterAsType("test", type);

		// assert
		assertNull(result);
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameter_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testABC", "1").addParameter("testDEF", "2").addParameter("testGHI", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameterSowieEinNachDemPrefixBenannterParameter_readParameterAsType_sollteEineMapNurMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testABC", "1").addParameter("testDEF", "2").addParameter("testGHI", "3").addParameter("test", "FEHLER");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameterSowieEinigeUnpassende_readParameterAsType_sollteEineMapNurMitDenPassendenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testABC", "1").addParameter("testDEF", "2").addParameter("testGHI", "3").addParameter("abc", "PASST NICHT").addParameter("def", "PASST NICHT");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameterSowieEinPrefixUndSuffix_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testABC42", "1").addParameter("testDEF42", "2").addParameter("testGHI42", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("test#42", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameterSowieNurEinSuffix_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("ABC42", "1").addParameter("DEF42", "2").addParameter("GHI42", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("#42", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineMapAlsZieltypUndPassendeRequestParameterOhnePrefixUndSuffix_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("ABC", "1").addParameter("DEF", "2").addParameter("GHI", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, String.class);

		// run
		Object result = reader.readParameterAsType("#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals("1", resultMap.get("ABC"));
		assertEquals("2", resultMap.get("DEF"));
		assertEquals("3", resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineStringIntegerMapAlsZieltypUndPassendeRequestParameter_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testABC", "1").addParameter("testDEF", "2").addParameter("testGHI", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, String.class, Integer.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals(1, resultMap.get("ABC"));
		assertEquals(2, resultMap.get("DEF"));
		assertEquals(3, resultMap.get("GHI"));
	}

	@Test
	public void gegebenEineIntegerIntegerMapAlsZieltypUndPassendeRequestParameter_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("test2", "1").addParameter("test3", "2").addParameter("test42", "3");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, Integer.class, Integer.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals(1, resultMap.get(2));
		assertEquals(2, resultMap.get(3));
		assertEquals(3, resultMap.get(42));
	}

	@Test
	public void gegebenEineEnumEnumMapAlsZieltypUndPassendeRequestParameter_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Request req = new TestRequest("testTEST1", "TEST2").addParameter("testTEST2", "TEST3").addParameter("testTEST3", "TEST1");
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, TestEnum.class, TestEnum.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals(TestEnum.TEST2, resultMap.get(TestEnum.TEST1));
		assertEquals(TestEnum.TEST3, resultMap.get(TestEnum.TEST2));
		assertEquals(TestEnum.TEST1, resultMap.get(TestEnum.TEST3));
	}

	@Test
	public void gegebenEineEntityEntityMapAlsZieltypUndPassendeRequestParameter_readParameterAsType_sollteEineMapMitDenTransformiertenParameternZurueckgeben()
	{
		// setup
		Rasse rasse1 = persist(new Rasse("Test1", false));
		Rasse rasse2 = persist(new Rasse("Test2", false));
		Rasse rasse3 = persist(new Rasse("Test3", false));
		Medal medal1 = persist(new Medal("M1", "", ""));
		Medal medal2 = persist(new Medal("M2", "", ""));
		Medal medal3 = persist(new Medal("M3", "", ""));

		Request req = new TestRequest("test"+rasse1.getId(), Integer.toString(medal3.getId()))
				.addParameter("test"+rasse2.getId(), Integer.toString(medal2.getId()))
				.addParameter("test"+rasse3.getId(), Integer.toString(medal1.getId()));
		ParameterReader reader = new ParameterReader(req, getDB());
		Type type = new DummyParameterizedType(Map.class, Rasse.class, Medal.class);

		// run
		Object result = reader.readParameterAsType("test#", type);

		// assert
		assertNotNull(result);
		assertTrue(result instanceof Map);
		Map<?,?> resultMap = (Map<?, ?>) result;
		assertEquals(3, resultMap.size());
		assertEquals(medal3, resultMap.get(rasse1));
		assertEquals(medal2, resultMap.get(rasse2));
		assertEquals(medal1, resultMap.get(rasse3));
	}

	private static enum TestEnum {
		TEST1,
		TEST2,
		TEST3
	}

	private static class DummyParameterizedType implements ParameterizedType{
		private Type[] actualTypeArguments;
		private Type rawType;

		public DummyParameterizedType(Type rawType, Type ... actualTypeArguments)
		{
			this.actualTypeArguments = actualTypeArguments;
			this.rawType = rawType;
		}

		@Override
		public Type[] getActualTypeArguments()
		{
			return actualTypeArguments;
		}

		@Override
		public Type getRawType()
		{
			return rawType;
		}

		@Override
		public Type getOwnerType()
		{
			return null;
		}
	}
}