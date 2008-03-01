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
package net.driftingsouls.ds2.server.scripting.dsscript;

class Value {
	/**
	 * Konvertiert einen beliebigen String in einen Integerwert.
	 * Wenn der String keine Zahl ist wird 0 zurueckgegeben.
	 * @param val Der String
	 * @return Die Zahl oder 0
	 */
	public static int Int(String val) {
		try {
			return Integer.parseInt(val);
		}
		catch( NumberFormatException e ) {
			try {
				return (int)Double.parseDouble(val);
			}
			catch( NumberFormatException e2 ) {
				return 0;
			}
		}
	}
	
	/**
	 * Konvertiert einen beliebigen String in einen Longwert.
	 * Wenn der String keine Zahl ist wird 0 zurueckgegeben.
	 * @param val Der String
	 * @return Die Zahl oder 0
	 */
	public static long Long(String val) {
		try {
			return Long.parseLong(val);
		}
		catch( NumberFormatException e ) {
			try {
				return (long)Double.parseDouble(val);
			}
			catch( NumberFormatException e2 ) {
				return 0;
			}
		}
	}
	
	/**
	 * Konvertiert einen beliebigen String in einen Doublewert.
	 * Wenn der String keine Zahl ist wird 0 zurueckgegeben.
	 * @param val Der String
	 * @return Die Zahl oder 0
	 */
	public static double Double(String val) {
		try {
			return Double.parseDouble(val);
		}
		catch( NumberFormatException e ) {
			return 0d;
		}
	}
}
