package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert eine Klasse als Konverter fuer einen bestimmten Datentyp aus einem URL-Parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UrlParamKonverterFuer
{
	/**
	 * Der Zieltyp zu dem hin konvertiert werden soll.
	 * @return Der Zieltyp
	 */
	Class<?> value();
}
