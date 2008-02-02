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
package net.driftingsouls.ds2.server.framework.pipeline;

import net.driftingsouls.ds2.server.framework.Context;

import org.w3c.dom.Node;

/**
 * Standardinterface fuer Pipelines
 * @author Christopher Jung
 *
 */
public interface Pipeline {
	/**
	 * Fuehrt die Pipeline aus
	 * @param context Der Kontext, in dem die Pipeline ausgefuehrt werden soll
	 * @throws Exception
	 */
	public void execute(Context context) throws Exception;
	
	/**
	 * Setzt den XML-Konfigurationsknoten der Pipeline. Wenn dieser <code>null</code> ist, wird
	 * kein Konfigurationsknoten verwendet
	 * @param node Der XML-Konfigurationsknoten der Pipeline oder <code>null</code>
	 */
	public void setConfiguration(Node node);
}
