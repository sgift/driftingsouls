package net.driftingsouls.ds2.server.framework.pipeline;

import java.io.IOException;

/**
 * Ein Ergebnis einer Controller-Action mit einer gesonderten (speziellen) Serialisierungsmethode.
 */
public interface ViewResult
{
	/**
	 * Serialisiert das Ergebnis und schreibt dieses in die Antwort.
	 * @param response Die Antwort
	 */
    void writeToResponse(Response response) throws IOException;
}
