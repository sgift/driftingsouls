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
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Die Logoutfunktion.
 * @author Christopher Jung
 *
 */
@Module(name="logout")
public class LogoutController extends TemplateGenerator {
	private AuthenticationManager authManager;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public LogoutController(Context context) {
		super(context);
		
		setTemplate("logout.html");
	}
	
	/**
	 * Injiziert den DS-AuthenticationManager zum einloggen von Benutzern.
	 * @param authManager Der AuthenticationManager
	 */
	@Autowired
	@Required
	public void setAuthenticationManager(AuthenticationManager authManager) {
		this.authManager = authManager;
	}

	/**
	 * Loggt den Spieler aus.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		this.authManager.logout();
	}
}
