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
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.pipeline.serializer.Serializer;
import net.driftingsouls.ds2.server.framework.pipeline.transformer.Transformer;

/**
 * Eine Generator-basierte Pipeline bestehend aus Generator, Transformer und Serializer,
 * welche in dieser Reihenfolge abgearbeitet werden.
 * @author Christopher Jung
 *
 */
public class GeneratorPipeline implements Pipeline {
	private Class generator;
	private Class transformer;
	private Class serializer;
	private DSGenerator.ActionType execType = DSGenerator.ActionType.DEFAULT;
	
	public GeneratorPipeline( String execType, Class generator, Class transformer, Class serializer ) {
		this.generator = generator;
		this.transformer = transformer;
		this.serializer = serializer;
		if( "ajax".equals(execType) ) {
			this.execType = DSGenerator.ActionType.AJAX;
		}
	}
	
	private void generateContent(Context context, Class generator) throws Exception {
		Constructor constr = generator.getConstructor(Context.class);
		constr.setAccessible(true);

		Object cntl = constr.newInstance(context);

		// TODO: seeeehr unschoen
		((DSGenerator)cntl).handleAction(context.getRequest().getParameter("action"), execType);
	}

	private void applyTransformer(Context context, Class transformer) throws Exception {
		Transformer tf = (Transformer)transformer.newInstance();

		tf.transform(context);
	}

	private void applySerializer(Context context, Class serializer) throws Exception {
		Serializer ser = (Serializer)serializer.newInstance();

		ser.serialize(context);
	}
	
	public void execute(Context context) throws Exception {
		generateContent(context, generator);
		applyTransformer(context, transformer);
		applySerializer(context, serializer);
	}

}
