package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.pipeline.Module;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.SortedSet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class ControllerKonformitaetTest
{
	@Test
	public void alleAnnotiertenControllerBesitzenEinenSichtbarenParameterlosenOderSindMitAutowiredAnnotiertKonstruktor()
	{
		SortedSet<Class<?>> classes = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
		for (Class<?> aClass : classes)
		{
			assertEquals(1, aClass.getConstructors().length);
			try
			{
				Constructor<?> constructor = aClass.getConstructor();
				if( (constructor.getModifiers() & Modifier.PUBLIC) == 0 )
				{
					fail("Der parameterlose Konstruktor von "+aClass.getCanonicalName()+" ist nicht public");
				}
				if( constructor.isAnnotationPresent(Autowired.class) )
				{
					fail("Der parameterlose Konstruktor von "+aClass.getCanonicalName()+" ist mit Autowired annotiert");
				}
			}
			catch (NoSuchMethodException e)
			{
				Constructor<?> constructor = aClass.getConstructors()[0];
				if( (constructor.getModifiers() & Modifier.PUBLIC) == 0 )
				{
					fail("Der Konstruktor von "+aClass.getCanonicalName()+" ist nicht public");
				}
				assertTrue(constructor.isAnnotationPresent(Autowired.class));
			}
		}

	}

	@Test
	public void alleMitModuleAnnotiertenKlassenSindVonControllerAbgeleitet()
	{
		SortedSet<Class<?>> classes = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
		for (Class<?> aClass : classes)
		{
			if( !Controller.class.isAssignableFrom(aClass) )
			{
				fail(aClass.getCanonicalName()+" ist nicht von Controller abgeleitet obwohl sie mit @Module annotiert ist");
			}
		}

	}
}
