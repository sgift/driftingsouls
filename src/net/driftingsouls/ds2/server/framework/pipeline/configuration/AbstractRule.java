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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Pipeline;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;

// TODO: Behandlung Module, Reader und Servlet in eigene Klassen auslagern
abstract class AbstractRule implements Rule
{
	private final Node config;

	private final Executer executer;

	private final ParameterMap parameterMap;

	AbstractRule(PipelineConfig pipelineConfig, Node matchNode) throws Exception
	{
		Node config = XMLUtils.getNodeByXPath(matchNode, "config");
		if (config != null)
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			doc.appendChild(doc.importNode(config, true));
			this.config = doc.getFirstChild();
		}
		else
		{
			this.config = null;
		}

		Node paramMap = XMLUtils.getNodeByXPath(matchNode, "parameter-map");
		if (paramMap != null)
		{
			this.parameterMap = new ParameterMap(paramMap);
		}
		else
		{
			this.parameterMap = null;
		}

		Node node = XMLUtils.getNodeByXPath(matchNode, "execute-module");
		if (node != null)
		{
			this.executer = new ModuleExecuter(pipelineConfig, node);
			return;
		}

		node = XMLUtils.getNodeByXPath(matchNode, "execute-reader");
		if (node != null)
		{
			this.executer = new ReaderExecuter(node);
			return;
		}

		throw new Exception("Unable to determine execution type of rule");
	}

	@Override
	public boolean executeable(Context context) throws Exception
	{
		return true;
	}

	@Override
	public Pipeline execute(Context context) throws Exception
	{
		if (!executeable(context))
		{
			return null;
		}

		if (parameterMap != null)
		{
			parameterMap.apply(context);
		}

		Pipeline pipe = this.executer.execute(context);

		if (pipe != null)
		{
			pipe.setConfiguration(config);
		}

		return pipe;
	}
}
