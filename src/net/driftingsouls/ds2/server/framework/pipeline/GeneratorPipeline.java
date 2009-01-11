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

import java.lang.reflect.Constructor;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Generator;

import org.w3c.dom.Node;

/**
 * Eine Generator-basierte Pipeline bestehend aus Generator, Transformer und Serializer,
 * welche in dieser Reihenfolge abgearbeitet werden.
 * @author Christopher Jung
 *
 */
public class GeneratorPipeline implements Pipeline {
	private Class<? extends Generator> generator;

	/**
	 * Konstruktor.
	 * @param generator Der zu verwendende Generator
	 */
	public GeneratorPipeline( Class<? extends Generator> generator ) {
		this.generator = generator;
	}
	
	private void generateContent(Context context, Class<? extends Generator> generator) throws Exception {
		Constructor<? extends Generator> constr = generator.getConstructor(Context.class);
		constr.setAccessible(true);

		Generator cntl = constr.newInstance(context);

		cntl.handleAction(context.getRequest().getParameter("action"));
	}

	@Override
	public void execute(Context context) throws Exception {
		generateContent(context, generator);
	}

	@Override
	public void setConfiguration(Node node)  {
		//Won't used - only to fulfill contract - wrong interface?	
	}
}
