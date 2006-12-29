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

package net.driftingsouls.ds2.framework;

/**
 * Das Interface IImageStatusNotifier dient dazu Klassen nach dem Laden einer
 * Grafik zu benachrichtigen, sofern das laden durch {@link JImageCache#getImage(String, boolean, IImageStatusNotifier, Object)}
 * erfolgt.
 * 
 * @author Christopher Jung
 */
public interface IImageStatusNotifier {
	/**
	 * Wird von {@link JImageCache} aufgerufen, wenn eine Grafik geladen wurde.
	 * Übergeben wird der Pfad der Grafik sowie das in {@link JImageCache#getImage(String, boolean, IImageStatusNotifier, Object)}
	 * angebenene Objekt.
	 * 
	 * @param image			Der Pfad der geladenen Grafik
	 * @param customData	Die angegebenen Objektdaten
	 */
	public void onImageLoaded(String image,Object customData);
}