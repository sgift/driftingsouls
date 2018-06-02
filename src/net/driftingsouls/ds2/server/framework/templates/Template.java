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
 * Interface fuer Template-Files.
 * @author Christopher Jung
 *
 */
public interface Template {
	/**
	 * Bereitet das Template fuer die Ausgabe vor.
	 * @param te Das TemplateEngine
	 * @param parent Das Elterntemplate
	 */
    void prepare(TemplateEngine te, String parent);
	/**
	 * Gibt die Liste aller Variablen im Template zurueck.
	 * @param all Falls <code>true</code> werden auch Variablen von Bloecken zurueckgegeben
	 * @return Die Liste aller Variablen
	 */
    String[] getVarList(boolean all);
	/**
	 * Verarbeitet das Template.
	 * @param te Das Templateengine
	 * @return Das verarbeitete Template
	 */
    String main(TemplateEngine te);
	
	/**
	 * Gibt den Templateblock mit dem angegebenen Namen zurueck.
	 * @param name Der Name des Blocks
	 * @return Der TemplateBlock
	 */
    TemplateBlock getBlock(String name);
}
