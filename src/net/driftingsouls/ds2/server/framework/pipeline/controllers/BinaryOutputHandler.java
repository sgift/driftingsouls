package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import java.io.IOException;

/**
 * Ausgabeklasse fuer Binary-Antworten.
 */
public class BinaryOutputHandler extends OutputHandler
{
	@Override
	public void printHeader() {}
	@Override
	public void printFooter() {}
	@Override
	public void printErrorList() throws IOException
	{
	}
}
