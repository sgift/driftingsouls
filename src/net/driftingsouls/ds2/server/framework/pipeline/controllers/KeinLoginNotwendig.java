package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Erlaubt die Nutzung eines Controllers oder eine Actionmethode ohne gueltige Anmeldedaten.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface KeinLoginNotwendig
{
}
