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

package net.driftingsouls.ds2.framework.services;


import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;


/**
 * SoapConnector ermöglicht ein stressfreies Aufrufen der Soap-Funktionen
 * von DS2 (es sind praktisch die selben wie die in DS1 für das Flottentool).
 * Jeder Aufruf verwendet die in {@see ServerConnector} eingestellte Session-ID.
 *
 * @author Christopher Jung
 */

public class SoapConnector implements ServerConnectable {
	public static final String SERVICE = SoapConnector.class.getName();
	
	/**
	 * Gibt den Namen eines Schiffes zurück. Dies funktioniert natürlich
	 * nur mit eigenen Schiffen (sessionID)
	 * 
	 * @param id	Die ID des Schiffes
	 * @return Der Name des Schiffes
	 */
	public String identifyShip( int id ) {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "identifyShip");

		rpc.addProperty("shipid", String.valueOf(id) );
			
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (String)envelope.getResponse();			
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return null;
		}
	}
	
	/**
	 * Gibt den Namen einer Basis zurück. Dies funktioniert natürlich
	 * nur mit eigenen Basen (sessionID)
	 * 
	 * @param id	Die ID der Basis
	 * @return Der Name der Basis
	 */
	public String identifyBase( int id ) {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "identifyBase");

		rpc.addProperty("baseid", String.valueOf(id) );
			
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (String)envelope.getResponse();			
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return null;
		}
	}
	
	/**
	 * Überprüft eine SessionID auf Gültigkeit
	 * 
	 * @return <code>true</code> wenn die SessionID gültig ist. Sonst <code>false</code>
	 */
	public boolean validateSessID() {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "validateSessID");
			
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (Boolean)envelope.getResponse();			
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return false;
		}
	}
	
	/**
	 * Überprüft, ob der Account neue PMs erhalten hat und gibt deren Anzahl zurück
	 * 
	 * @return Die Anzahl der noch ungelesenen PMs 
	 */
	public int hasNewPM() {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "hasNewPM");
			
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (Integer)envelope.getResponse();
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return -1;
		}
	}
	
	/**
	 * Führt ein Admin-Kommando aus. Dies geht natürlich nur mit Administrationsrechten :)
	 * 
	 * @param command Das auszuführende Kommando
	 * @return Ein Rückgabewert (abhängig von Kommando)
	 */
	public String admin_execcmd( String command ) {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "admin_execcmd");

		rpc.addProperty("command", command);
			
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (String)envelope.getResponse();
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return null;
		}
	}
	
	/**
	 * Überprüft, ob ein Account Admin-Rechte hat. Der Account wird anhand der SessionID
	 * ermittelt.
	 * 
	 * @return <code>true</code> falls der Account über Admin-Rechte verfügt. Sonst <code>false</code>
	 */
	public boolean admin_isAdmin() {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "admin_isAdmin");
		
		SoapSerializationEnvelope envelope = soapCall(rpc);

		try {
			return (Boolean)envelope.getResponse();
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return false;
		}
	}
	
	public static final String USERVALUE_STARMAP_BUFFEREDOUTPUT = "TBLORDER/clients/jstarmap/bufferedoutput";	// 1 oder 0
	
	/**
	 * Liefert den Wert eines UserValues zurück. Das ganze funktioniert allerdings nur
	 * mit UserValues, welche für Clients freigegeben sind (Liste s.o.)
	 * 
	 * @param sess	Eine gültige SessionID
	 * @param uservalue Das abzufragende UserValue
	 * @return Der Inhalt des UserValues
	 */
	public String getUserValue( String uservalue ) {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "getUserValue");

		rpc.addProperty("uservalue", uservalue);
			
		SoapSerializationEnvelope envelope = soapCall(rpc);
		try {
			return ((SoapPrimitive)envelope.getResponse()).toString();
		}
		catch( SoapFault f ) {
			System.err.println(f);
			return null;
		}
	}
	
	/**
	 * Setzt ein UserValue auf einen bestimmten Wert. Das ganze funktioniert allerdings nur
	 * mit UserValues, welche für Clients freigegeben sind (Liste s.o.)
	 * 
	 * @param uservalue	Das zu setzende UserValue
	 * @param newvalue	Der neue Wert des UserValues
	 */
	public void setUserValue( String uservalue, String newvalue ) {
		SoapObject rpc =
			new SoapObject("http://ds.drifting-souls.net/", "setUserValue");

		rpc.addProperty("uservalue", uservalue);
		rpc.addProperty("newvalue", newvalue);
			
		SoapSerializationEnvelope envelope = soapCall(rpc);
	}
	
	/**
	 * Fuehrt den eigendlichen Soap-Aufruf durch und liefert das Ergebnis zurueck
	 * @param rpc Soap-Aufruf
	 * 
	 * @return Ergebnis des Soap-Aufrufs
	 */
	private SoapSerializationEnvelope soapCall( SoapObject rpc ) {
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		
		try {
			envelope.bodyOut = rpc;

			String soapPath = "soap";
			String sess = ServerConnector.getInstance().getSession();
			if( sess != null && sess.length() > 0 ) {
				soapPath += "/"+sess;
			}
			
			HttpTransportSE ht = new HttpTransportSE(ServerConnector.getInstance().getServerURL()+soapPath+"/interface");
			
			ht.call("http://ds.drifting-souls.net/#"+rpc.getName(), envelope);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error: "+e.toString());
		}
		
		return envelope;
	}
}
