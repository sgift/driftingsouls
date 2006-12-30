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
package net.driftingsouls.ds2.server.webservices;

/**
 * Repraesentiert einen Fehler in einem WebService
 * @author Christopher Jung
 *
 */
// TODO: SOAP-Mapping
public class WebServiceException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor
	 *
	 */
	public WebServiceException() {
		super();
	}

	/**
	 * Konstruktor
	 * @param message Die Fehlerbeschreibung
	 * @param cause Der Fehlergrund (Nested Exception)
	 */
	public WebServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Konstruktor
	 * @param message Die Fehlerbeschreibung
	 */
	public WebServiceException(String message) {
		super(message);
	}

	/**
	 * Konstruktor
	 * @param cause Der Fehlergrund (Nested Exception)
	 */
	public WebServiceException(Throwable cause) {
		super(cause);
	}
}
