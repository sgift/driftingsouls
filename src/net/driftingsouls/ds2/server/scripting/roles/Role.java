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
package net.driftingsouls.ds2.server.scripting.roles;

import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * <h1>Interface fuer Rollen.</h1>
 * <p>Eine Rolle implementiert das Verhalten eines scriptbaren Objekts. Die Rolle
 * wird dabei auf Basis einer Rollendefinition initalisiert. Die Daten der
 * Rollendefinition werden dabei in mittels 
 * {@link net.driftingsouls.ds2.server.scripting.roles.interpreter.Attribute} 
 * gekennzeichnete Eigenschaften eingetragen. Nachdem alle Eigenschaften gesetzt wurden
 * wird die Rolle mittels {@link #execute(ScriptContext)} ausgefuehrt.</p>
 * <p>Eine Instanz einer Rolle kann - muss aber nicht - fuer mehrere konkrete
 * Rollendefinitionen verwendet werden. In diesem Fall werden die Eigenschaften
 * jeweils zwischen den Ausfuehrungen neu mit den Daten der jeweiligen Rollendefinition
 * initalisiert.</p>
 * <p>Es wird empfohlen auf einen Konstruktor zu verzichten, da Instanzen der Rolle
 * auch erstellt werden koennen ohne das anschliessend diese auch ausgefuehrt wird.</p>
 * @author Christopher Jung
 *
 */
public interface Role {
	/**
	 * Fuehrt die Rolle aus.
	 * @param context Der Ausfuehrungskontext
	 * @throws ScriptException Falls waehrend der Ausfuehrung ein Fehler auftritt
	 */
	public void execute(ScriptContext context) throws ScriptException;
}
