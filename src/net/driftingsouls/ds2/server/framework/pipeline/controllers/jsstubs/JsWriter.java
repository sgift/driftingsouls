package net.driftingsouls.ds2.server.framework.pipeline.controllers.jsstubs;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

class JsWriter implements Closeable
{
	private Writer writer;
	private int indent;

	JsWriter(Writer writer)
	{
		this.writer = writer;
	}

	public JsWriter writeLine(String text) throws IOException
	{
		doIndent();
		writer.write(text+"\n");
		return this;
	}

	public JsWriter doIndent() throws IOException
	{
		for (int i = 0; i < indent; i++)
		{
			writer.write('\t');
		}
		return this;
	}

	public JsWriter write(String text) throws IOException
	{
		writer.write(text);
		return this;
	}

	public JsWriter indent(int indentMod)
	{
		indent += indentMod;
		return this;
	}

	@Override
	public void close() throws IOException
	{
		writer.close();
	}
}
