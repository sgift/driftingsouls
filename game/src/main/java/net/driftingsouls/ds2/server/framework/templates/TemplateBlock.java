/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.templates;

/**
 * Interface fuer Template-Bloecke.
 * @author Christopher Jung
 *
 */
public interface TemplateBlock {
	/**
	 * Gibt die Liste aller Variablen im Block zurueck.
	 * @param all Falls <code>true</code> werden auch Variablen von Unterbloecken zurueckgegeben
	 * @return Die Liste aller Variablen
	 */
    String[] getBlockVars(boolean all);
	
	/**
	 * Generiert die Ausgabe des Blocks.
	 * @param te Das TemplateEngine
	 * @return Die Ausgabe
	 */
    String output(TemplateEngine te);
}
