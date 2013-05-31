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

import java.util.regex.Pattern;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

class MatchRule extends AbstractRule {
	private Pattern match = null;
	
	MatchRule( PipelineConfig pipelineConfig, Node matchNode ) throws Exception {
		super(pipelineConfig,matchNode);
		match = Pattern.compile( XMLUtils.getStringByXPath(matchNode, "@pattern") );
	}
	
	@Override
	public boolean executeable(Context context) throws Exception {
		if( !match.matcher(context.getRequest().getPath()).matches() ) {
			return false;
		}
		return super.executeable(context);
	}
}
