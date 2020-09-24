/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.tick.RareTick;
import net.driftingsouls.ds2.server.tick.RegularTick;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.tick.TickPartExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementiert Admin-Kommandos rund um das Ticksystem.
 * @author Christopher Jung
 *
 */
@Component
@Lazy
public class TickAdminCommand
{
	private final RegularTick regularTick;
	private final RareTick rareTick;

	private static final ExecutorService tickExecutor = Executors.newSingleThreadExecutor();

	@Autowired
	public TickAdminCommand(RegularTick regularTick, RareTick rareTick) {
		this.regularTick = regularTick;
		this.rareTick = rareTick;
	}
	
	/**
	 * Fuehrt den normalen DS-Tick aus.
	 */
	public void runRegularTick() {
		tickExecutor.submit(regularTick::execute);
	}
	
	/**
	 * Fuehrt den seltenen DS-Tick aus.
	 */
	public void runRareTick() {
		tickExecutor.submit(rareTick::execute);
	}
	
	/**
	 * Fuehrt einen Teil des normalen DS-Tick aus.
	 * @param tickPart der auszufuehrende Teiltick
	 */
	public void runRegularTick(Class<? extends TickController> tickPart) {
		TickPartExecutor tickPartExecutor = new TickPartExecutor(tickPart, "tick");
		tickExecutor.submit(tickPartExecutor::execute);
	}
	
	/**
	 * Fuehrt einen Teil des seltenen DS-Tick aus.
	 * @param tickPart der auszufuehrende Teiltick
	 */
	public void runRareTick(Class<? extends TickController> tickPart) {
		TickPartExecutor tickPartExecutor = new TickPartExecutor(tickPart, "tick");
		tickExecutor.submit(tickPartExecutor::execute);
	}
}
