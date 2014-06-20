/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.pipeline;

import net.driftingsouls.ds2.server.framework.pipeline.controllers.OutputHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker fuer Pipeline-Module. Eine hiermit annotierte Klasse muss
 * von {@link net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller} abgeleitet sein.
 * @author Christopher Jung
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Scope("prototype")
@Documented
public @interface Module
{
	/**
	 * Der Name des Moduls unter dem es in URLs/in der Pipeline angesprochen werden kann.
	 */
	String name();
	/**
	 * <code>true</code>, falls es das Default-Module ist, welches beim Fehlen einer Modulangabe
	 * verwendet werden soll. Es darf zu jedem Zeitpunkt nur ein Default-Module geben.
	 */
	boolean defaultModule() default false;

	/**
	 * Die fuer die Ausgabe von Header, Footer und Fehlern zustaendige Implementierung.
	 * Standardmaessig waehlt das Framework selbst die passende Implementierung in
	 * Abhaengigkeit von gewaehlten {@link net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType}.
	 * Der hier angegebene Handler kann noch durch eine explizite Angabe von {@link net.driftingsouls.ds2.server.framework.pipeline.controllers.Action#outputHandler}
	 * ueberschrieben werden.
	 */
	Class<? extends OutputHandler> outputHandler() default OutputHandler.class;
}
