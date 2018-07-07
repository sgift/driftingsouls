package net.driftingsouls.ds2.server.framework;

/**
 * Exception to show that the user is not logged in, but must be logged in to access
 * this page.
 * 
 * @author Drifting-Souls Team
 */
public class NotLoggedInException extends RuntimeException 
{
	private static final long serialVersionUID = 7525573737127620345L;
}
