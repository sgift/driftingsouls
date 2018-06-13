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
package net.driftingsouls.ds2.server.framework.bbcode;

/**
 * Repraesentiert einen BBCode und seine Formatierungsfunktion. Die Formatierungsfunktion
 * wird immer aufgerufen, wenn in einem Text ein BBCode gefunden wurde.
 * Die Funktion muss Threadsafe implementiert werden.
 * @author Christopher Jung
 *
 */
public interface BBCodeFunction {
	/**
	 * Behandelt einen gefundenen und zu der Funktion passenden BBCode. Der formatierte Abschnitt
	 * wird anschliessend zurueckgegeben.
	 * 
	 * @param content Der Inhalt zwischen den Tags ([name]HierStehtDerInhalt[/name]) oder null, wenn
	 *            der BBCode keinen Inhalt hat.
	 * @param values Die Liste der Parameter des BBCodes
	 * @return Der formatierte Abschnitt
	 */
    String handleMatch(String content, String... values);
}
