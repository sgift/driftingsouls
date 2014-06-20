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
package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kennzeichnet eine Action.
 * @author Christopher Jung
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
	/**
	 * Der Typ der Action.
	 */
	ActionType value();
	/**
	 * Marktiert die Aktion als nur lesend. Dies erlaubt
	 * dem Datenbanksystem einige Optimierungen (z.B. keine flushes usw).
	 */
	boolean readOnly() default false;

	/**
	 * Die fuer die Ausgabe von Header, Footer und Fehlern zustaendige Implementierung.
	 * Standardmaessig waehlt das Framework selbst die passende Implementierung in
	 * Abhaengigkeit von gewaehlten {@link ActionType}.
	 */
	Class<? extends OutputHandler> outputHandler() default OutputHandler.class;
}
