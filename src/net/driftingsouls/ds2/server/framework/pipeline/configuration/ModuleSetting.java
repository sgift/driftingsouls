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
package net.driftingsouls.ds2.server.framework.pipeline.configuration;

import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;

class ModuleSetting implements Cloneable {
	Class<? extends Controller> generator = null;

	ModuleSetting(Class<?> generator) {
		this.generator = generator.asSubclass(Controller.class);
	}
	
	/**
	 * Ergaenzt diese Moduleinstellungen um die angegebenen Moduleinstellungen.
	 * Noch nicht gesetzte Werte werden dabei uebernommen.
	 * @param module Die Moduleinstellungen, um die diese ergaenzt werden sollen
	 */
	public void use(ModuleSetting module) {
		if( module.generator != null ) {
			generator = module.generator;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		try {
			ModuleSetting setting = (ModuleSetting)super.clone();
			setting.generator = this.generator;

			return setting;
		}
		catch( Exception e ) {
			// Should not happen
			e.printStackTrace();
			
			throw new CloneNotSupportedException();
		}
	}
	
	@Override
	public String toString() {
		return "Generator: "+generator.getName();
	}
}
