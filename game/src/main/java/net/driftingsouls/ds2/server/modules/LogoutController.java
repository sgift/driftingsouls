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

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationManager;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Controller;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.framework.templates.TemplateViewResultFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Die Logoutfunktion.
 * @author Christopher Jung
 *
 */
@Module(name="logout")
public class LogoutController extends Controller
{
	private AuthenticationManager authManager;
	private TemplateViewResultFactory templateViewResultFactory;

	@Autowired
	public LogoutController(AuthenticationManager authManager, TemplateViewResultFactory templateViewResultFactory)
	{
		this.authManager = authManager;
		this.templateViewResultFactory = templateViewResultFactory;
	}

	/**
	 * Loggt den Spieler aus.
	 */
	@Action(ActionType.DEFAULT)
	public TemplateEngine defaultAction()
	{
		TemplateEngine t = templateViewResultFactory.createFor(this);
		this.authManager.logout();
		return t;
	}
}
