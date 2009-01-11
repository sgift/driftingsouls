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
package net.driftingsouls.ds2.server.framework.pipeline.actions;

import net.driftingsouls.ds2.server.framework.Context;

/**
 * Repraesentiert eine Aktion innerhalb der Pipeline.
 * Eine Aktion "filtert" die Pipelines.
 * 
 * @author Christopher Jung
 *
 */
public interface Action {
	/**
	 * Bereitet die Action auf einen neuen "Einsatz" vor.
	 * Wird immer dann aufgerufen, wenn die Action eine neue Request bearbeiten soll
	 * 
	 */
	public void reset();
	
	/**
	 * Setzt alle fuer diesen "Einsatz" vorhandenen Parameter. Das setzen der Parameter
	 * erfolgt nach dem Aufruf von reset aber noch vor dem Aufruf von action.
	 * 
	 * @param name Der Name des Parameters
	 * @param value Der Wert
	 */
	public void setParameter(String name, String value);
	
	/**
	 * Fuehrt die eigendliche Aktion aus. Wenn diese true zurueck gibt, 
	 * wird mit der Ausfuehrung der Pipeline fortgefahen. Andernfalls 
	 * gilt die Pipeline als nicht passend und es wird nach einer anderen Pipeline
	 * gesucht.
	 * 
	 * @param context der mit dem Aufruf verbundene Kontext
	 * @return true falls alles Ok ist oder false, falls die Pipeline nicht passt
	 */
	public boolean action(Context context);
}
