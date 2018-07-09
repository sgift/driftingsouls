package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.ViewMessage;

import static org.junit.Assert.*;

/**
 * Assert-Hilfsfunktionen fuer {@link net.driftingsouls.ds2.server.framework.ViewMessage}s.
 */
public final class ViewMessageAssert
{
	private ViewMessageAssert()
	{
		// EMPTY
	}

	/**
	 * Ueberprueft, ob es sich um eine Erfolgsmeldung handelt.
	 * @param message Die zu pruefende ViewMessage
	 */
	public static void assertSuccess(ViewMessage message)
	{
		assertNotNull("Es wurde eine ViewMessage erwartet", message);
		assertNotNull("Es wurde eine vollstaendige ViewMessage erwartet", message.message);
		if( !"success".equals(message.message.type) )
		{
			fail("Es wurde eine erfolgreiche ViewMessage erwartet, jedoch wurde folgender Fehler gemeldet: "+message.message.description);
		}
	}

	/**
	 * Ueberprueft, ob es sich um eine Fehlermeldung handelt.
	 * @param message Die zu pruefende ViewMessage
	 */
	public static void assertFailure(ViewMessage message)
	{
		assertNotNull("Es wurde eine ViewMessage erwartet", message);
		assertNotNull("Es wurde eine vollstaendige ViewMessage erwartet", message.message);
		if( !"failure".equals(message.message.type) )
		{
			fail("Es wurde ViewMessage-Fehlerobjekt erwartet");
		}
	}
}
