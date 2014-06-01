package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.SortedSet;

import static org.junit.Assert.fail;

public class ControllerKonformitaetTest
{
	@Test
	public void alleAnnotiertenControllerBesitzenEinenSichtbarenParameterlosenKonstruktor()
	{
		SortedSet<Class<?>> classes = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class);
		for (Class<?> aClass : classes)
		{
			try
			{
				Constructor<?> constructor = aClass.getConstructor();
				if( (constructor.getModifiers() & Modifier.PUBLIC) == 0 )
				{
					fail("Der parameterlose Konstruktor von "+aClass.getCanonicalName()+" ist nicht public");
				}
			}
			catch (NoSuchMethodException e)
			{
				fail(aClass.getCanonicalName()+" besitzt keinen parameterlosen Konstruktor");
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
