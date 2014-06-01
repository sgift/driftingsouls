package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.io.IOException;

/**
 * Identisch zum {@link HtmlOutputHandler},
 * nur das kein Header und Footer generiert wird.
 */
public class EmptyHeaderFooterOutputHandler extends HtmlOutputHandler
{
	@Override
	public void printHeader() throws IOException
	{
	}

	@Override
	public void printFooter() throws IOException
	{
	}
}
