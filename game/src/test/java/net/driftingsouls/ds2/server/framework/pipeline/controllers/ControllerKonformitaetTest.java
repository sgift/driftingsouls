package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import io.github.classgraph.ClassInfoList;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.pipeline.Module;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class ControllerKonformitaetTest
{
	@Test
	public void alleMitModuleAnnotiertenKlassenSindVonControllerAbgeleitet() {
		try(var scanResult = AnnotationUtils.INSTANCE.scanDsClasses()) {
			ClassInfoList classes = scanResult.getClassesWithAnnotation(Module.class.getName());
			for (Class<?> aClass : classes.loadClasses())
			{
				if( !Controller.class.isAssignableFrom(aClass) )
				{
					fail(aClass.getCanonicalName()+" ist nicht von Controller abgeleitet obwohl sie mit @Module annotiert ist");
				}
			}
		}
	}
}
