package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deaktiviert die Ticksperre fuer Aufrufe auf einen hiermit annotierten Controller oder eine hiermit annotierte Action.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface KeineTicksperre
{
}
