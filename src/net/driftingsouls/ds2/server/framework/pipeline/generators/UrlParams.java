package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.lang.annotation.*;

/**
 * Eine Menge von {@link UrlParam}-Annotationen zur Deklaration von
 * URL-Parametern.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface UrlParams {
	UrlParam[] value();
}
