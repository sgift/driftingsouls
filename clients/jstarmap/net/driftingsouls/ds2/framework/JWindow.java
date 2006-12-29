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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * JWindow ist die Basisklasse für alle Fenster. Jedes Fenster muss auf JWindow basieren.
 * JWindow selbst implementiert praktisch keine funktionalität. Es wird nichts gezeichnet,
 * nichts positioniert, nichts vergrößert usw.
 * Auch die allermeisten Methoden sind nur Proxy-Funktionen auf entsprechende Methoden des Fenstermanagers.
 * Dennoch ist es deutlich besser die Methoden von JWindow zu verwenden als direkt den Fenstermanager
 * aufzurufen. Zum einen sieht der Code so besser aus. Zum anderen ändert sich das Format der Methoden
 * des Fenstermanagers eher als bei JWindow.
 * 
 * @author bKtHeG (Christopher Jung)
 *
 */
public class JWindow {
	private int handle;
	
	private IWindowManager windowmanager;
	private JWindow parent;
	
	private boolean disableVScrolling;
	
	/**
	 * Erzeugt ein neues Fenster
	 * @param parent		Das Elternfenster
	 * @param windowmanager	Der Windowmanager
	 */
	public JWindow( JWindow parent, IWindowManager windowmanager ) {
		handle = -1;
		
		this.parent = parent;
		
		this.windowmanager = windowmanager;
		
		handle = windowmanager.registerWindow(this, parent);
	}
	
	/**
	 * Gibt den ImageCache zurück
	 * 
	 * @return Das ImageCache-Objekt
	 */
	protected JImageCache getImageCache() {
		return getWindowManager().getImageCache();
	}
	
	/**
	 * Gibt den WindowManager zurück
	 * 
	 * @return Das WindowManager-Objekt
	 */
	protected IWindowManager getWindowManager() {
		return windowmanager;
	}
	
	/**
	 * Gibt das Handle des Fensters zurück
	 * 
	 * @return Das Handle
	 */
	public int getHandle() {
		return handle;
	}
	
	/**
	 * Loggt einen String in der Java-Konsole.
	 * Dem Text wird zur einfacheren Identifikation der Klassenname und das Handle vorran gestellt
	 * 
	 * @param str Der zu loggende String
	 */
	protected void log( String str ) {
		System.out.println("["+System.currentTimeMillis() % 100000+":"+this.getClass().getName()+":"+getHandle()+"] "+str);
	}
	
	/**
	 * Liefert den Fensterpositionsmodus zurück
	 * 
	 * @return Der Fensterpositionsmodus
	 * 
	 * @see #setPosition(int)
	 */
	public int getPositionMode() {
		return getWindowManager().getWindowPositionMode(this);
	}
	
	/**
	 * Setzt die Fensterposition auf einen bestimmten Modus.
	 * Mögliche Modi sind {@link IWindowManager#POSITION_CUSTOM}, {@link IWindowManager#POSITION_SCREEN_CENTER} und
	 * {@link IWindowManager#POSITION_ALWAYS_WITHIN_SCREEN}
	 * 
	 * @param mode		Der neue Fensterplatzierungsmodus
	 */
	public void setPosition( int mode ) {
		getWindowManager().setWindowPosition( this, mode );
	}
	
	/**
	 * Setzt die Fensterposition auf eine neue x/y-Position
	 * 
	 * @param x	Die x-Koordinate
	 * @param y	Die y-Koordinate
	 */
	public void setPosition( int x, int y ) {
		getWindowManager().setWindowPosition(this, x, y);
	}
	
	/**
	 * Setzt den Fensterrand eines bestimmten Fensters.
	 * Diese Aufgabe übernimmt im Normalfall JWindow.
	 * 
	 * @param left		Der linke Rand
	 * @param top		Der obere Rand
	 * @param right		Der rechte Rand
	 * @param bottom	Der untere Rand
	 */
	public void setBorder( int left, int top, int right, int bottom ) {	
		getWindowManager().setWindowBorder(this, left, top, right, bottom);
	}
	
	/**
	 * Verändert die Größe eines Fensters.
	 * Alle Werte sind inklusive der Ränder. 
	 * Ein Fenster kann nicht kleiner als die betreffenden Ränder + 2 werden
	 *
	 * @param width		Die neue Breite
	 * @param height	Die neue Höhe
	 */
	public void setSize( int width, int height ) {		
		getWindowManager().setWindowSize(this, width, height);
	}
	
	/**
	 * Setzt die Clientgröße eines Fensters auf neue Werte
	 * 
	 * @param cwidth	Die neue Client-Breite
	 * @param cheight	Die neue Client-Höhe
	 */
	public void setClientSize( int cwidth, int cheight ) {
		getWindowManager().setWindowClientSize(this, cwidth, cheight );
	}
	
	/**
	 * Liefert die x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @return Die X-Koordinate
	 */
	public int getRelativeX() {
		return getWindowManager().getWindowX(this);
	}
	
	/**
	 * Liefert die y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @return Die Y-Koordinate
	 */
	public int getRelativeY() {
		return getWindowManager().getWindowY(this);
	}
	
	/**
	 * Liefert die x-Koordinate des Clientbreichs des Fensters zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @return Die X-Koordinate
	 */
	public int getClientX() {
		return getWindowManager().getWindowClientX(this);
	}
	
	/**
	 * Liefert die y-Koordinate des Clientbreichs des Fensters zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @return Die y-Koordinate
	 */
	public int getClientY() {
		return getWindowManager().getWindowClientY(this);
	}
	
	/**
	 * Liefert die Fenster-Clientbreite zurück
	 *
	 * @return Die Fenster-Clientbreite
	 */
	public int getClientWidth() {
		return getWindowManager().getWindowClientWidth(this);
	}

	/**
	 * Liefert die Fenster-Clienthöhe zurück
	 * 
	 * @return Die Fenster-Clienthöhe
	 */
	public int getClientHeight() {
		return getWindowManager().getWindowClientHeight(this);
	}
	
	/**
	 * Liefert die absolute x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return getWindowManager().getWindowAbsoluteX(this);
	}
	
	/**
	 * Liefert die absolute y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @return Die y-Koordinate
	 */
	public int getY() {
		return getWindowManager().getWindowAbsoluteY(this);
	}
	
	/**
	 * Liefert die Fensterbreite zurück
	 * 
	 * @return Die Fensterbreite
	 */
	public int getWidth() {
		return getWindowManager().getWindowWidth(this);
	}
	
	/**
	 * Liefert die Fensterhöhe zurück
	 * 
	 * @return Die Fensterhöhe
	 */
	public int getHeight() {
		return getWindowManager().getWindowHeight(this);
	}
	
	/**
	 * Wird vom WindowManager aufgerufen, wenn das Fenster neugezeichnet werden soll
	 * 
	 * @param g	Der Zeichenkontext, in dem das Fenster neugezeichnet werden muss
	 */
	public void paint(Graphics2D g) {
		// Nichts zu tun hier
	}
	
	/**
	 * Wird von WindowManager aufgerufen, um die neue Form des Fensters zu ermitteln
	 * 
	 * @return	Die Form des Fensters
	 */
	public Shape getShape() {
		Rectangle rect = new Rectangle();
		rect.setRect(getX(),getY(),getWidth(),getHeight());
		
		return rect;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn das Fenster den Fokus bekommt oder verloren hat.
	 * 
	 * @param hasFocus Der Status des Fokus
	 */
	public void onFocusChanged( boolean hasFocus ) {
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn ein Tastendruck ausgeführt wurde
	 * 
	 * @param keycode	Der Keycode der Taste
	 * @param key		Der Character, welcher eingegeben wurde
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean keyPressed( int keycode, char key ) {
		log("got 'keyPressed': "+key);
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn sich ein Mausklick ereignet hat.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean mouseClicked(int x, int y, int button) {
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn eine Maustaste gedrückt wird.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean mousePressed(int x, int y, int button) {
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn eine Maustaste wieder losgelassen wurde.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean mouseReleased(int x, int y, int button) {
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn die Maus mit gedrückter Maustaste bewegt wird.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean mouseDragged(int x, int y, int button) {
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn die Maus bewegt wird.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean mouseMoved(int x, int y, int button) {
		return false;
	}
	
	/**
	 * Wird vom Fenstermanager aufgerufen, wenn Maus den Fensterbereich verlässt.
	 * 
	 * @param x			Die x-Koordinate der Maus relativ zum Clientbereich
	 * @param y			Die y-Koordinate der Maus relativ zum Clientbereich
	 * @param button	Die Maustaste ({@link java.awt.event.InputEvent})
	 * 
	 */
	public void mouseExited(int x, int y, int button) {
		return;
	}
	
	/**
	 * Wird von Buttons/Comboboxen usw aufgerufen, wenn sich ein Ereignis ereignet hat
	 * 
	 * @param handle	Das Handle des Fensters, in dem sich das Ereignis ereignet hat
	 * @param event		Der Name des Ereignisses
	 * 
	 * @return <code>true</code> falls man mit dem Ereignis etwas anfangen konnte. Sonst <code>false</code>
	 */
	public boolean handleEvent( int handle, String event ) {
		return false;
	}
	
	/**
	 * Hilfsfunktion zum vergleichen, ob ein Fenster zu einem bestimmten Handle passt
	 * 
	 * @param aWindow	Das Fenster
	 * @param handle	Das Handle
	 * @return <code>true</code>, wenn beide identisch sind. Sonst <code>false</code>
	 */
	public boolean vertifyEventSender( JWindow aWindow, int handle ) {
		if( (aWindow != null) && (handle == aWindow.getHandle()) ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Wird vom WindowManager aufgerufen, wenn sich die Größe des Fensters geändert hat
	 */
	public void onResize() {
		//nothing
	}
	
	/**
	 * Wird vom WindowManager aufgerufen, wenn sich die Sichtbarkeit des Fensters geändert hat
	 * 
	 * @param newVisibility <code>true</code>, falls das Fenster sichtbar ist. Andernfalls <code>false</code>
	 */
	public void onChangeVisibility( boolean newVisibility ) {
	}
	
	/**
	 * Gibt zurück, um wieviel dieses Fenster im Moment in y-Richtung gescrollt ist
	 * 
	 * @return Der Betrag um den das Fenster gescrollt ist
	 */
	public int getVScrollOffset() {
		return getWindowManager().getWindowVClientOffset(this);
	}
	
	/**
	 * Liefert zurück um wieviel das Clientrect größer ist als die anzeigbare Größe in Y-Richtung
	 *
	 * @return Der überstehende Clientrect-Wert in Pixel
	 */
	public int getVScrollOverflow() {
		return getWindowManager().getWindowVClientOverflow(this);
	}
	
	/**
	 * Gibt das Elternfenster zurück, falls vorhanden
	 * 
	 * @return Das Elternfenster
	 */
	public JWindow getParent() {
		return parent;
	}
	
	/**
	 * Scrollt das Fenster in y-Richtung um den angegebenen Betrag
	 *
	 * @param value		Der Wert um das das Fenster gescrollt werden soll
	 */	
	public void vScrollClientWindow( int value ) {
		getWindowManager().vScrollClientWindow(this, value);
	}
	
	/**
	 * Deaktiviert die Scrollbarkeit des Fensters.
	 * Wenn die Scrollbarkeit deaktiviert ist und der Clientbereich des Fensters
	 * zu groß wird, wird keine Scrollbar angezeigt und der überstehende Teil einfach
	 * abgeschnitten
	 * 
	 * @see #enableVScrolling()
	 */
	public void disableVScrolling() {
		getWindowManager().setWindowScrollability(this, false);
	}
	
	/**
	 * Aktiviert die Scrollbarkeit des Fensters
	 *
	 * @see #disableVScrolling()
	 */
	public void enableVScrolling() {
		getWindowManager().setWindowScrollability(this, true);
	}
	
	/**
	 * Entfernt das Fenster
	 */
	public void dispose() {
		getWindowManager().removeWindow(this);
	}
}