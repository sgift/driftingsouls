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

import java.awt.Shape;
import java.util.Vector;

/**
 * IWindowManager gibt an mit welchen Funktionen des WindowManagers ein Programm
 * rechnen kann. Auf gar keinen Fall sollte das Programm (nichtmal Teile dieser Bibliothek)
 * davon ausgehen, dass es sich bei dem WindowManager immer um CanvasWindowManager handelt!
 * 
 * Zudem sollten die meisten Funktionen nicht direkt aufgerufen werden. Stattdessen sollte man die
 * entsprechenden Proxy-Methoden von JWindow verwenden. 
 * 
 * @author Christopher Jung
 */
public interface IWindowManager {
	/**
	 * Kennzeichnet ein Fenster, welches ausschließlich durch die x und y Koordinaten platziert wird
	 */
	public static final int POSITION_CUSTOM = 1;					// 001
	/**
	 * Kennzeichnet ein Fenster, welches immer in der Bildschirmmitte verharrt. Die x/y-Koordinaten werden ignoriert
	 */
	public static final int POSITION_SCREEN_CENTER = 2;				// 010
	/**
	 * Kennzeichnet ein Fenster, welches zwar primär durch die x und y Koordinaten positioniert wird, jedoch nur solange wie das Fenster selbst immer auf den Bildschirm passt.
	 * Wenn dies nicht mehr der Fall ist wird das Fenster automatisch verschoben.
	 */
	public static final int POSITION_ALWAYS_WITHIN_SCREEN = 4;		// 100
	
	/**
	 * [Boolean] Aktiviert eine zusätzliche Pufferung der Bildschirmausgabe 
	 */
	public static final int PROPERTY_BUFFERED_OUTPUT = 1;
	
	/**
	 * Registriert ein neues Fenster im WindowManager.
	 * Diese Aufgabe übernimmt im Normalfall JWindow.
	 * 
	 * @param aWindow	Das neue Fensterobjekt
	 * @param aParent	Das Elternfenster
	 * 
	 * @return Das Handle des neuen Fensters
	 */
	public int registerWindow( JWindow aWindow, JWindow aParent );
	
	/**
	 *  Entfernt das angegebene Fenster. 
	 *  Es ist jedoch grundsätzlich besser ein Fenster über JWindow.dispose() zu entfernen.  
	 * 
	 * @param aWindow Das zu entfernende Fenster
	 */
	public void removeWindow( JWindow aWindow );
	
	/**
	 * Setzt die Sichtbarkeit eines Fensters auf sichtbar (<code>true</code>) oder unsichtbar (<code>false</code>)
	 * Wenn die Sichtbarkeit eines Fensters manuell gesetzt wird, erbt dieses nicht mehr die Sichtbarkeit des Elternfensters
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * @param vis		Die neue Sichtbarkeit
	 */
	public void setVisibility( JWindow aWindow, boolean vis );
	
	/**
	 * Liefert zurück, ob ein bestimmtes Fenster sichtbar ist oder nicht.
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * 
	 * @return <code>true</code> (sichtbar) oder <code>false</code> (nicht sichtbar) 
	 */
	public boolean getVisibility( JWindow aWindow );
	
	/**
	 * Setzt die Fensterform auf ein {@link Shape}-Objekt.
	 * Mit jedem Fenster muss ein Shape-Objekt verknüpft sein. 
	 * Im Normalfall übernimmt JWindow allerdings die Generierung.
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * @param ashape	Die neue Fensterform
	 */
	public void setWindowShape( JWindow aWindow, Shape ashape);
	
	/**
	 * Setzt den Fensterrand eines bestimmten Fensters.
	 * Diese Aufgabe übernimmt im Normalfall JWindow.
	 * 
	 * @param aWindow	Das Fenster
	 * @param left		Der linke Rand
	 * @param top		Der obere Rand
	 * @param right		Der rechte Rand
	 * @param bottom	Der untere Rand
	 */
	public void setWindowBorder( JWindow aWindow, int left, int top, int right, int bottom );
	
	/**
	 * Verändert die Größe eines Fensters.
	 * Alle Werte sind inklusive der Ränder. 
	 * Ein Fenster kann nicht kleiner als die betreffenden Ränder + 2 werden
	 * 
	 * @param aWindow	Das Fenster
	 * @param width		Die neue Breite
	 * @param height	Die neue Höhe
	 */
	public void setWindowSize( JWindow aWindow, int width, int height );
	
	/**
	 * Setzt die Clientgröße eines Fensters auf neue Werte
	 * 
	 * @param aWindow	Das Fenster
	 * @param cwidth	Die neue Client-Breite
	 * @param cheight	Die neue Client-Höhe
	 */
	public void setWindowClientSize( JWindow aWindow, int cwidth, int cheight );
	
	/**
	 * Liefert die Fensterbreite zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fensterbreite
	 */
	public int getWindowWidth( JWindow aWindow );
	
	/**
	 * Liefert die Fensterhöhe zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fensterhöhe
	 */
	public int getWindowHeight( JWindow aWindow );
	
	/**
	 * Liefert die Fenster-Clientbreite zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fenster-Clientbreite
	 */
	public int getWindowClientWidth( JWindow aWindow );
	
	/**
	 * Liefert die Fenster-Clienthöhe zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fenster-Clienthöhe
	 */
	public int getWindowClientHeight( JWindow aWindow );
	
	/**
	 * Setzt die Fensterposition auf einen bestimmten Modus.
	 * Mögliche Modi sind {@link IWindowManager#POSITION_CUSTOM}, {@link IWindowManager#POSITION_SCREEN_CENTER} und
	 * {@link IWindowManager#POSITION_ALWAYS_WITHIN_SCREEN}
	 * 
	 * @param aWindow	Das Fenster
	 * @param mode		Der neue Fensterplatzierungsmodus
	 */
	public void setWindowPosition( JWindow aWindow, int mode );
	
	/**
	 * Setzt die Fensterposition auf eine neue x/y-Position
	 * 
	 * @param aWindow Das Fenster
	 * @param x	Die x-Koordinate
	 * @param y	Die y-Koordinate
	 */
	public void setWindowPosition( JWindow aWindow, int x, int y );
	
	/**
	 * Liefert den Fensterpositionsmodus zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der Fensterpositionsmodus
	 */
	public int getWindowPositionMode( JWindow aWindow );
	
	/**
	 * Liefert die x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowX( JWindow aWindow );
	
	/**
	 * Liefert die y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Y-Koordinate
	 */
	public int getWindowY( JWindow aWindow );
	
	/**
	 * Liefert die absolute x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowAbsoluteX( JWindow aWindow );
	
	/**
	 * Liefert die absolute y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die y-Koordinate
	 */
	public int getWindowAbsoluteY( JWindow aWindow );
	
	/**
	 * Liefert die x-Koordinate des Clientbreichs des Fensters zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowClientX( JWindow aWindow );
	
	/**
	 * Liefert die y-Koordinate des Clientbreichs des Fensters zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die y-Koordinate
	 */
	public int getWindowClientY( JWindow aWindow );
	
	/**
	 * Liefert zurück um wieviel das Clientrect größer ist als die anzeigbare Größe in Y-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der überstehende Clientrect-Wert in Pixel
	 */
	public int getWindowVClientOverflow( JWindow aWindow );
	
	/**
	 * Scrollt das Fenster in y-Richtung um den angegebenen Betrag
	 * 
	 * @param aWindow	Das Fenster
	 * @param value		Der Wert um das das Fenster gescrollt werden soll
	 */
	public void vScrollClientWindow( JWindow aWindow, int value );
	
	/**
	 * Gibt zurück, um wieviel das Fenster im Moment in y-Richtung gescrollt ist
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der Betrag um den das Fenster gescrollt ist
	 */
	public int getWindowVClientOffset( JWindow aWindow );
	
	/**
	 * Setzt die minimale Clientbereichsgröße in Y-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * @param size		Die minimale Größe des Clientbereichs
	 */
	public void setWindowVClientMinSize( JWindow aWindow, int size );
	
	/**
	 * Setzt die minimale Clientbereichsgröße in X-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * @param size		Die minimale Größe des Clientbereichs
	 */
	public void setWindowHClientMinSize( JWindow aWindow, int size );
	
	/**
	 * Aktiviert/Deaktiviert die Scrollbarkeit eines Fensters.
	 * Wenn die Scrollbarkeit deaktiviert ist und der Clientbereich des Fensters
	 * zu groß wird, wird keine Scrollbar angezeigt und der überstehende Teil einfach
	 * abgeschnitten
	 * 
	 * @param aWindow	Das Fenster
	 * @param value		<code>true</code> für scrollbar und <code>false</code> für nicht scrollbar 
	 */
	public void setWindowScrollability( JWindow aWindow, boolean value );
	
	/**
	 * Fordert ein Neuzeichnen des gesamten Bildschirms an
	 * 
	 * @see #requestRedraw(JWindow)
	 */
	public void requestRedraw();
	
	/**
	 * Fordert ein Neuzeichnen für den gesamten Bildschirm bzw ein Fenster an.
	 * 
	 * @param wnd Das neuzuzeichnende Fenster. <code>NULL</code> für den gesamten Bildschirm
	 */
	public void requestRedraw( JWindow wnd );
	
	/**
	 * Liefert eine Instanz von {@link JImageCache}
	 * 
	 * @return Eine {@link JImageCache}-Instanz
	 */
	public JImageCache getImageCache();
	
	/**
	 * Liefert alle Kind-Fenster des angegebenen Fensters zurück
	 * 
	 * @param aWindow Das Fenster zu dem die Kind-Fenster ermittelt werden sollen
	 * 
	 * @return Ein {@link Vector} mit allen Kind-Fenstern
	 */
	public Vector getChildren( JWindow aWindow );
	
	/**
	 * Liefert die Bildschirmbreite zurück.
	 * Dabei handelt es sich nicht um die Breite des gesamten (physischen) Bildschirms, 
	 * sondern um die Breite, die diese Anwendung/dieses Applet hat in Pixeln
	 * 
	 * @return Die Bildschirmbreite
	 * 
	 * @see #getScreenHeight() 
	 */
	public int getScreenWidth();
	
	/**
	 * Liefert die Bildschirmhöhe zurück.
	 * Dabei handelt es sich nicht um die Höhe des gesamten (physischen) Bildschirms, 
	 * sondern um die Höhe, die diese Anwendung/dieses Applet hat in Pixeln
	 * 
	 * @return Die Bildschirmhöhe
	 * 
	 * @see #getScreenWidth() 
	 */
	public int getScreenHeight();
	
	/**
	 * Liefert den Namen der zu verwendenden Standardfont zurück
	 * 
	 * @return Der Name der Standardfont
	 */
	public String getDefaultFont();
	
	/**
	 * Fragt an, ob ein bestimmtes Fenster den Fokus bekommen kann
	 * 
	 * @param wnd	Das Fenster, das den Fokus haben möchte
	 */
	public void requestFocus( JWindow wnd );
	
	/**
	 * Liefert zurück ob ein bestimmtes Fenster den Fokus hat
	 * 
	 * @param wnd	Das zu überprüfende Fenster
	 * 
	 * @return <code>true</code> falls das Fenster den Fokus hat. Andernfalls <code>false</code>
	 */
	public boolean hasFocus( JWindow wnd );
	
	/**
	 * Setzt eine Fenstermanager-Eigenschaft (PROPERTY_*) auf einen bestimmten Wert.
	 * Eine konkrete Fenstermanager-Implementierung muss nicht zwangsläufig alle 
	 * Eigenschaften unterstützen. Also bei Programmen nicht drauf verlassen!
	 * 
	 * @param property	Die zu verändernde Eigenschaft
	 * @param value		Der neue Wert
	 * 
	 * @see #getProperty(int)
	 */
	public void setProperty( int property, Object value );
	
	/**
	 * Liefert den Wert einer Eigenschaft (PROPERTY_*) zurück.
	 * 
	 * @param property	Die Eigenschaft
	 * @return Der Wert der Eigenschaft
	 * 
	 * @see #setProperty(int, Object)
	 */
	public Object getProperty( int property );
}
