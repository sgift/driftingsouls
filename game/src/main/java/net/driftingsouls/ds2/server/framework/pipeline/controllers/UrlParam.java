package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.lang.annotation.*;

/**
 * Ein URL-Parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface UrlParam {
	/**
	 * Der Name des Parameters.
	 */
	String name() default "";
}
