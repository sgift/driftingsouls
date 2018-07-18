package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.io.IOException;

/**
 * Identisch zum {@link HtmlOutputHandler},
 * nur das kein Header generiert wird.
 */
public class EmptyHeaderOutputHandler extends HtmlOutputHandler
{
	@Override
	public void printHeader() throws IOException
	{
	}
}
