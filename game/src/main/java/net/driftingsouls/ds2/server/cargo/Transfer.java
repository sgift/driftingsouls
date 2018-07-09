/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Sebastian Gift
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
package net.driftingsouls.ds2.server.cargo;


/**
 * Klasse zum Transfer von Waren von einem Objekt zu einem anderen.
 * @author Christopher Jung
 *
 */
public final class Transfer {
	/**
	 * Transferiert die angegebene Warenmenge von Quellobjekt zum Zielobjekt.
	 * @param from Das Quellobjekt
	 * @param to Das Zielobjekt
	 * @param resource Die zu transferierende Resource
	 * @param count Die Resourcenmenge
	 * @return Meldungen
	 */
	public String transfer(Transfering from, Transfering to, ResourceID resource, long count) {
		if( to == null ) {
			throw new IllegalArgumentException("To may not be null.");
		}
		
		if( from == null ) {
			throw new IllegalArgumentException("From may not be null.");
		}
		
		if( resource == null ) {
			throw new IllegalArgumentException("Resource may not be null.");
		}
		
		if( count <= 0 ) {
			throw new IllegalArgumentException("Count must be strictly positive.");
		}
		
		if( !to.getLocation().equals(from.getLocation()) ) {
			return "FEHLER: Objekte sind an verschiedenen Positionen.\n";
		}
		
		Cargo fromCargo = from.getCargo();
		Cargo toCargo = to.getCargo();
		long countOnShip = fromCargo.getResourceCount(resource);
		if( count > countOnShip ) {
			count = countOnShip;
		}
		
		long massOfCount = Cargo.getResourceMass(resource, count);
		
		long maxCargo = to.getMaxCargo();
		if( massOfCount > maxCargo ) {
			count = maxCargo/(massOfCount/count);
		}
		
		if( count <= 0 ) {
			return "";
		}
		
		fromCargo.substractResource(resource, count);
		toCargo.addResource(resource, count);
		
		from.setCargo(fromCargo);
		to.setCargo(toCargo);
		
		return "Transferiere "+count+"*"+resource+"\n";
	}
}
