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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 * CanvasWindowManager stellt eine Implementierung von IWindowManager für Canvas da.
 * 
 * Die allermeisten Funktionen des Fenstermanagers sollten nicht direkt aufgerufen werden.
 * Stattdessen gibt es für die meisten Funktionen Proxy-Methoden in JWindow die bevorzugt werden sollten.
 * 
 * Ausserdem sollte man nie damit rechnen, dass {@link IWindowManager} irgendwo auch ein CanvasWindowManager ist.
 * Auch wenn dies im Moment noch die einzigste Implementierung von {@link IWindowManager} ist, so wird es jedoch sicherlich
 * nicht für immer dabei bleiben.
 * 
 * @author bKtHeG (Christopher Jung)
 *
 */
public class CanvasWindowManager extends Canvas implements ActionListener, MouseListener, 
												 MouseMotionListener, KeyListener, 
												 Runnable, IWindowManager {
	private static final long serialVersionUID = 1787959099291311735L;

	private JImageCache imgCache;
	private BufferStrategy strategy;
	
	private HashMap<Integer,aWindowEntry> windows;
	private int nextHandle;
	private Vector<Integer> windowZOrder;
	private Vector<Vector<Object>> eventList;
	
	private String fontName;
	
	private boolean redraw;
	
	private int mouseLastWindowHandle;
	
	private int inputFocusWindow;
	private String datapath;

	private Thread mainThread = null;
	private boolean running = true;
	
	private Point mousePosition;
	
	private boolean bufferedOutput;
	
	private static final int GAMETICKS_PER_SECOND = 30;
	private static final int MILLISECONDS_PER_GAMETICK = 1000 / GAMETICKS_PER_SECOND;
	
	private static final int EVENT_ON_CHANGE_VISIBILITY = 1;
	private static final int EVENT_RECREATE_BACKBUFFER = 2;

	public CanvasWindowManager(String datapath) {	
		super();
		nextHandle = 1;		
		redraw = true;
		
		windows = new HashMap<Integer,aWindowEntry>();
		windowZOrder = new Vector<Integer>();
		eventList = new Vector<Vector<Object>>();
		
		mouseLastWindowHandle = -1;
		inputFocusWindow = -1;
		
		bufferedOutput = false;
		
		System.out.println("Datapath: "+datapath);
		this.datapath = datapath;
	}
	
	public void init() {
		
		imgCache = new JImageCache(this,datapath+"data/");

		fontName = "";
		mousePosition = new Point(0,0);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		setVisible(true);
		
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Font fontList[] = ge.getAllFonts();
		for( int i=0; i < fontList.length; i++ ) {
			if( (fontList[i].getName().toLowerCase().equals(new String("bankgothic md bt"))) ||
				(fontList[i].getName().toLowerCase().equals(new String("bank gothic medium bt"))) ) {
				fontName = fontList[i].getName();
				break;
			}
		}
		
		requestFocus();
		setIgnoreRepaint(true);
		
		start();
	}
	
	protected aWindowEntry getWindowEntry( int handle ) {
		return windows.get(handle);
	}
	
	public void update(Graphics2D g) {
		g.setColor(Color.BLACK);
        g.fillRect(0,0,getWidth(),getHeight());
	}
	
	public void start() {
    	if( mainThread == null ) {
    		mainThread = new Thread( this );
			running = true;
        	mainThread.start();
    	}
	}
	
	public void run() {
		long usedTime = 1000;
		long redrawCount = 0;
		
		Font myfont = null;
		Image backbuffer = null;
		
		if( fontName != "" ) {
			myfont = new Font(fontName,Font.TRUETYPE_FONT,12);
		}
		
		ArrayList offScreenBufferList = new ArrayList();
		final int MAX_OFFSCREEN_BUFFERS = 100;
		
		while( running ) {
			long startTime = System.currentTimeMillis();

			redrawCount++;
			
			// Event-List verarbeiten
			synchronized(this) {
				for( int i=0; i < eventList.size(); i++ ) {
					Vector<Object> aEvent = eventList.get(i);
					JWindow aWindow = (JWindow)aEvent.get(1);
					if( aWindow.getHandle() > 0 ) {
						switch( ((Integer)aEvent.get(0)).intValue() ) {
						case EVENT_ON_CHANGE_VISIBILITY:
							aWindow.onChangeVisibility(((Boolean)aEvent.get(2)).booleanValue());
							break;
						case EVENT_RECREATE_BACKBUFFER:
							aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
							if( awnd != null ) {
								// Fuer das Fenster Einen neuen Buffer erstellen und dafuer - sollte kein Platz mehr sein - 
								// bei einem anderen Fenster den Offscreen-Buffer entfernen
								/*if( offScreenBufferList.size() > MAX_OFFSCREEN_BUFFERS ) {
									int handle = ((Integer)offScreenBufferList.remove(0)).intValue();
									if( getWindowEntry(handle) != null ) {
										getWindowEntry(handle).setOffscreenBuffer(null);
									}
								}
								offScreenBufferList.add(new Integer(aWindow.getHandle()));*/
								BufferedImage img = getImageCache().createImg(	awnd.getWindowRect().width,
										awnd.getWindowRect().height,
										Transparency.TRANSLUCENT);
								
								awnd.setOffscreenBuffer(img);
								requestRedraw(awnd.getWindow());
							}
							break;
						}
						eventList.remove(i);
					}
				}
			}
			
  			if( redraw || (redrawCount % 40 == 0) ) {
  				Graphics2D g = null;
  				try {
  					if( !bufferedOutput ) {
  						g = (Graphics2D)strategy.getDrawGraphics();
  						if( backbuffer != null ) {
  							backbuffer = null;
  						}
  					}
  					else {
  						if( backbuffer == null ) {
  							backbuffer = getImageCache().createHWImage(getScreenWidth(),getScreenHeight(),Transparency.OPAQUE);
  						}
  						g = (Graphics2D)backbuffer.getGraphics();
  						update(g);
  					}
  					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  					if( myfont != null ) {
  						g.setFont(myfont);
  					}
  			
  					Vector<Integer> localZOrder = new Vector<Integer>();
  					localZOrder.addAll(windowZOrder);
	  				for( int i=localZOrder.size()-1; i >= 0; i-- ) {
						Integer handle = localZOrder.get(i);
						aWindowEntry entry = getWindowEntry(handle.intValue());
						
						if( entry == null ) {
							continue;
						}
						if( entry.getShape() == null ) {
							continue;
						}
						
						Rectangle rect = entry.getShape().getBounds();
						
						int parentoffset = 0;
						
						if( entry.getParent() != null ) {
							aWindowEntry parent = entry;
													
							do {
								/*
								 * 	TODO: check
								 */
								if( parent.getWindow().getHandle() != getWindowEntry(parent.getParent().getHandle()).getScrollBarHandle() ) {
									parent = getWindowEntry(parent.getParent().getHandle());
									
									parentoffset += parent.getVScrollOffset();
									rect.y += parent.getVScrollOffset();
													
									rect = rect.intersection(parent.getVisibleClientRect());
								}
								else {
									parent = getWindowEntry(parent.getParent().getHandle());
												
									rect = rect.intersection(new Rectangle(	parent.getWindowPosition().x,
																			parent.getWindowPosition().y,
																			parent.getWindowRect().width,
																			parent.getWindowRect().height ));	
								}
							} while( parent.getParent() != null );
						}

						// Wenn das Fenster nicht zu sehen ist brauchen wir es auch nicht zu zeichnen
						if( (entry.getWindowPosition().x > getWidth()) || (entry.getWindowPosition().y+parentoffset > getHeight()) ){
							continue;
						}
						
						if( (entry.getWindowPosition().x+entry.getWindowRect().width < 0) || (entry.getWindowPosition().y+parentoffset+entry.getWindowRect().height < 0) ) {
							continue;
						}
						
						// Sollte das Fenster keinen Offscreen-Buffer besitzen:
						// Einen neuen Buffer erstellen und dafuer - sollte kein Platz mehr sein - 
						// bei einem anderen Fenster den Offscreen-Buffer entfernen
						if( entry.getOffscreenBuffer() == null ) {
							/*if( offScreenBufferList.size() > MAX_OFFSCREEN_BUFFERS ) {
								int hdl = ((Integer)offScreenBufferList.remove(0)).intValue();
								if( getWindowEntry(hdl) != null ) {
									getWindowEntry(hdl).setOffscreenBuffer(null);
								}
							}
							offScreenBufferList.add(handle);*/
							BufferedImage img = getImageCache().createImg(	entry.getWindowRect().width,
									entry.getWindowRect().height,
									Transparency.TRANSLUCENT);
							
							entry.setOffscreenBuffer(img);
						}
						// Offscreen-Buffer wird benoetigt - daher das Fenster in der Entfernen-Liste hinten anstellen
						/*else {
							offScreenBufferList.remove(handle);
							offScreenBufferList.add(handle);
						}*/
						
						BufferedImage offscreenbuffer = entry.getOffscreenBuffer();
						if( entry.getRedrawStatus() || (!redraw && (redrawCount % (entry.getWindow().getHandle()*100) == 0)) ) {
							synchronized(this) {							
								Graphics2D osbuffer = offscreenbuffer.createGraphics();
								osbuffer.setBackground( new Color(255,255,255,0) );
								osbuffer.clearRect(0,0,offscreenbuffer.getWidth(),offscreenbuffer.getHeight());
							
								osbuffer.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
								if( myfont != null ) {
									osbuffer.setFont(myfont);
								}
							
								entry.getWindow().paint(osbuffer);
								entry.setRedrawStatus(false);
								osbuffer.dispose();
								osbuffer = null;
							}
						}
												
						g.setClip(rect);
						g.drawImage(offscreenbuffer,entry.getWindowPosition().x,entry.getWindowPosition().y+parentoffset,null);
					}
	  				
	  				if( bufferedOutput ) {
	  					((Graphics2D)strategy.getDrawGraphics()).drawImage(backbuffer,0,0,getScreenWidth(),getScreenHeight(),null);
	  				}
  				}
  				finally {
  					if( g != null ) {
  						g.dispose();
  					}
  				}
  				
  				if( !strategy.contentsLost() ) {
  					strategy.show();
  				}
  				redraw = false;
  			}
  			else if( redrawCount % 60 == 0 ) {
  				System.gc();
  			}
  			
  			usedTime = System.currentTimeMillis() - startTime;  			
	  		if( usedTime < MILLISECONDS_PER_GAMETICK ) {
	  			try {
	  				Thread.sleep(MILLISECONDS_PER_GAMETICK - usedTime/2);
	  			}
	  			catch( InterruptedException e ) {
	  				System.out.println("Ups...ne InterruptedException. Wo kommt die den her?");
	  			}
	  		}
		}
	}

	public void stop () {
    	if( mainThread != null ) {
        	running = false;
    	}
	}
	
	public void update( Graphics g ) {
		redraw = true;
	}
	
	public void paint( Graphics g ) {
		redraw = true;
	}

	public void destroy() {
		stop();
		try {
			mainThread.join();
		}
		catch( InterruptedException e) {}
	}
	
	public void actionPerformed(ActionEvent evt) {}	
	
	public void mouseReleased(MouseEvent evt) {
		int handle = getWindowUnderCoord(evt.getX(), evt.getY());		
		if( handle > -1 ) {
			/*
			 * 	TODO: check
			 */
			aWindowEntry awnd = getWindowEntry(handle);
						
			if( awnd.getWindow().getHandle() != mouseLastWindowHandle ) {
				if( mouseLastWindowHandle > -1 ) {
					aWindowEntry oldwnd = getWindowEntry(mouseLastWindowHandle);
					if( oldwnd != null ) {
						oldwnd.getWindow().mouseExited( oldwnd.mapX(evt.getX()), 
														oldwnd.mapY(evt.getY()), 
														evt.getModifiers() );
					}
				}
				mouseLastWindowHandle = awnd.getWindow().getHandle();
			}
			
			boolean result = awnd.getWindow().mouseReleased(	awnd.mapX(evt.getX()), 
																awnd.mapY(evt.getY()), 
																evt.getModifiers() );
				
			if( !result && (awnd.getParent() != null) ) {
				aWindowEntry parent = awnd;
					
				do {
					parent = getWindowEntry(parent.getParent().getHandle());
						
					result = parent.getWindow().mouseReleased(	parent.mapX(evt.getX()), 
																parent.mapY(evt.getY()), 
																evt.getModifiers() );
					if( result ) {
						break;
					}
						
				} while( parent.getParent() != null );
			}
				
			return;
		}
		
		if( mouseLastWindowHandle > -1 ) {
			aWindowEntry oldwnd = getWindowEntry(mouseLastWindowHandle);
			if( oldwnd != null ) {
				oldwnd.getWindow().mouseExited(	oldwnd.mapX(evt.getX()), 
												oldwnd.mapY(evt.getY()), 
												evt.getModifiers() );
			}
			
			mouseLastWindowHandle = -1;
		}
	}
	
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {
		int handle = getWindowUnderCoord(evt.getX(), evt.getY());
		if( handle > -1 ) {
			aWindowEntry awnd = getWindowEntry(handle);
			
			/*
			 * TODO: check
			 */
			
			boolean result = awnd.getWindow().mouseClicked(	awnd.mapX(evt.getX()), 
															awnd.mapY(evt.getY()),
															evt.getModifiers() );
				
			if( !result && (awnd.getParent() != null) ) {
				aWindowEntry parent = awnd;
					
				do {
					parent = getWindowEntry(parent.getParent().getHandle());
						
					result = parent.getWindow().mouseClicked(	parent.mapX(evt.getX()), 
																parent.mapY(evt.getY()), 
																evt.getModifiers() );
					if( result ) {
						break;
					}
						
				} while( parent.getParent() != null );
			}
				
			return;
		}	
	}
	
	public void mousePressed(MouseEvent evt) {
		int handle = getWindowUnderCoord(evt.getX(), evt.getY());
		if( handle > -1 ) {
			/*
			 * TODO: check
			 */
			aWindowEntry awnd = getWindowEntry(handle);
			
			if( inputFocusWindow != awnd.getWindow().getHandle() ) {
				requestFocus( awnd.getWindow() );
			}
			
			boolean result = awnd.getWindow().mousePressed(	awnd.mapX(evt.getX()),
															awnd.mapY(evt.getY()), 
															evt.getModifiers() );
				
			if( !result && (awnd.getParent() != null) ) {
				aWindowEntry parent = awnd;
					
				do {
					parent = getWindowEntry(parent.getParent().getHandle());
						
					result = parent.getWindow().mousePressed(	parent.mapX(evt.getX()), 
																parent.mapY(evt.getY()), 
																evt.getModifiers() );
					if( result ) {
						break;
					}
						
				} while( parent.getParent() != null );
			}
				
			return;
		}
	}
	
	public void mouseDragged(MouseEvent evt) {	
		mousePosition.x = evt.getX();
		mousePosition.y = evt.getY();
		
		// Drag-Events grundsaetzlich an das letzte betroffene Fenster senden
		// und kein mouseExited-Ereigniss verschicken!
		if( mouseLastWindowHandle  > -1 ) {
			aWindowEntry awnd = getWindowEntry(mouseLastWindowHandle);
			
			/*
			 * TODO: check
			 */
			
			if( awnd != null ) {
				boolean result = awnd.getWindow().mouseDragged( awnd.mapX(evt.getX()),
																awnd.mapY(evt.getY()), 
																evt.getModifiers() );
				
				if( !result && (awnd.getParent() != null) ) {
					aWindowEntry parent = awnd;
					
					do {
						parent = getWindowEntry(parent.getParent().getHandle());
						
						result = parent.getWindow().mouseDragged(	parent.mapX(evt.getX()), 
																	parent.mapY(evt.getY()), 
																	evt.getModifiers() );
						if( result ) {
							break;
						}
						
					} while( parent.getParent() != null );
				}
				
				return;
			}
			mouseLastWindowHandle = -1;
		}
	}
	
	public void mouseMoved(MouseEvent evt) {
		mousePosition.x = evt.getX();
		mousePosition.y = evt.getY();
		
		int handle = getWindowUnderCoord(evt.getX(), evt.getY());
		if( handle > -1 ) {
			aWindowEntry awnd = getWindowEntry(handle);
			
			/*
			 * TODO: check
			 */
			
			if( awnd.getWindow().getHandle() != mouseLastWindowHandle ) {
				if( mouseLastWindowHandle > -1 ) {
					aWindowEntry oldwnd = getWindowEntry(mouseLastWindowHandle);
					if( oldwnd != null ) {
						oldwnd.getWindow().mouseExited(	oldwnd.mapX(evt.getX()), 
														oldwnd.mapY(evt.getY()), 
														evt.getModifiers() );
					}
				}
				mouseLastWindowHandle = awnd.getWindow().getHandle();
			}
			
			boolean result = awnd.getWindow().mouseMoved(	awnd.mapX(evt.getX()),
															awnd.mapY(evt.getY()), 
															evt.getModifiers() );
				
			if( !result && (awnd.getParent() != null) ) {
				aWindowEntry parent = awnd;
					
				do {
					parent = getWindowEntry(parent.getParent().getHandle());
						
					result = parent.getWindow().mouseMoved(	parent.mapX(evt.getX()), 
															parent.mapY(evt.getY()), 
															evt.getModifiers() );
					if( result ) {
						break;
					}
						
				} while( parent.getParent() != null );
			}
				
			return;
		}
		
		if( mouseLastWindowHandle > -1 ) {
			aWindowEntry oldwnd = getWindowEntry(mouseLastWindowHandle);
			if( oldwnd != null ) {
				oldwnd.getWindow().mouseExited(	oldwnd.mapX(evt.getX()), 
												oldwnd.mapY(evt.getY()), 
												evt.getModifiers() );
			}
			
			mouseLastWindowHandle = -1;
		}
	}
	
	public void keyPressed(KeyEvent evt) {
		int key = evt.getKeyCode();

		if( key == KeyEvent.VK_F11 ) {
			aWindowEntry awnd = getWindowEntry(getWindowUnderCoord(mousePosition.x, mousePosition.y));
			if( awnd != null ) {
				System.out.println("###### [DEBUG] ######");
				awnd.getWindow().log("size "+awnd.getWindowRect().width+"*"+awnd.getWindowRect().height+" csize "+awnd.getClientRect().width+"*"+awnd.getClientRect().height);
				awnd.getWindow().log("apos "+awnd.getWindowPosition().x+"*"+awnd.getWindowPosition().y+" rpos "+awnd.getRelativeWindowPosition().x+"*"+awnd.getRelativeWindowPosition().y);
				awnd.getWindow().log("scroll "+awnd.getScrollBarHandle()+" vcsize "+awnd.getVirtualClientRect().width+"*"+awnd.getVirtualClientRect().height);
				awnd.getWindow().log("visibility: "+awnd.getVisibility());
				System.out.println("#####################");
			}
		}
		
		if( inputFocusWindow != -1 ) {
			aWindowEntry awnd = getWindowEntry(inputFocusWindow);
			
			boolean result = awnd.getWindow().keyPressed( evt.getKeyCode(), evt.getKeyChar() );
				
			if( !result && (awnd.getParent() != null) ) {
				aWindowEntry parent = awnd;
					
				do {
					parent = getWindowEntry(parent.getParent().getHandle());
						
					result = parent.getWindow().keyPressed( evt.getKeyCode(), evt.getKeyChar() );
					if( result ) {
						break;
					}
						
				} while( parent.getParent() != null );
			}
				
			return;
		}
	}
	
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	/**
	 * Registriert ein neues Fenster im WindowManager.
	 * Diese Aufgabe übernimmt im Normalfall JWindow.
	 * 
	 * @param aWindow	Das neue Fensterobjekt
	 * @param aParent	Das Elternfenster
	 * 
	 * @return Das Handle des neuen Fensters
	 */
	public int registerWindow( JWindow aWindow, JWindow aParent ) {
		int handle = 0;
		
		synchronized(this) {
			handle = nextHandle++;
			aWindowEntry entry = new aWindowEntry( this, aWindow, aParent );
			windows.put(new Integer(handle),entry);
		}
		
		System.out.println("Added new Window "+handle+" as "+aWindow.getClass().getName());
		
		if( aParent != null ) {
			if( getVisibility(aParent) ) {
				setVisibility(handle, aWindowEntry.VISIBILITY_ON);
			}
		}
		
		return handle;
	}
	
	/**
	 * Setzt die Sichtbarkeit eines Fensters auf sichtbar (<code>true</code>) oder unsichtbar (<code>false</code>)
	 * Wenn die Sichtbarkeit eines Fensters manuell gesetzt wird, erbt dieses nicht mehr die Sichtbarkeit des Elternfensters
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * @param vis		Die neue Sichtbarkeit
	 * 
	 * @see #setVisibility(JWindow,int)
	 */
	public void setVisibility( JWindow aWindow, boolean vis ) {
		setVisibility(aWindow.getHandle(), vis);
	}
	
	/**
	 * Setzt die Sichtbarkeit eines Fensters auf einen bestimmten Zustand.
	 * Gültige Zustände sind {@link aWindowEntry#VISIBILITY_FORCE_ON},
	 * {@link aWindowEntry#VISIBILITY_FORCE_OFF},{@link aWindowEntry#VISIBILITY_ON} sowie
	 * {@link aWindowEntry#VISIBILITY_OFF}.
	 * 
	 * @param aWindow	Das betreffenden Fensters
	 * @param vis		Der neue Sichtbarkeitswert
	 * 
	 * @see #setVisibility(JWindow,boolean)
	 */
	public void setVisibility( JWindow aWindow, int vis ) {
		setVisibility(aWindow.getHandle(), vis);
	}
	
	
	/**
	 * Setzt die Sichtbarkeit eines Fensters auf sichtbar (<code>true</code>) oder unsichtbar (<code>false</code>)
	 * Wenn die Sichtbarkeit eines Fensters manuell gesetzt wird, erbt dieses nicht mehr die Sichtbarkeit des Elternfensters
	 * 
	 * @param handle	Das Handle des betreffenden Fenster
	 * @param vis		Die neue Sichtbarkeit
	 * 
	 * @see #setVisibility(int,boolean)
	 * @see #setVisibility(JWindow,int)
	 * @see #setVisibility(JWindow,boolean)
	 */
	protected void setVisibility( int handle, boolean vis ) {
		if( handle == -1 ) {
			return;
		}
		
		int newvis = 0;
		
		if( vis ) {
			newvis = aWindowEntry.VISIBILITY_FORCE_ON;
		}
		else {
			newvis = aWindowEntry.VISIBILITY_FORCE_OFF;
		}
		
		setVisibility( handle, newvis );
	}
	
	/**
	 * Setzt die Sichtbarkeit eines Fensters auf einen bestimmten Zustand.
	 * Gültige Zustände sind {@link aWindowEntry#VISIBILITY_FORCE_ON},
	 * {@link aWindowEntry#VISIBILITY_FORCE_OFF},{@link aWindowEntry#VISIBILITY_ON} sowie
	 * {@link aWindowEntry#VISIBILITY_OFF}.
	 * 
	 * @param handle	Das Handle des betreffenden Fensters
	 * @param vis		Der neue Sichtbarkeitswert
	 * 
	 * @see #setVisibility(int,boolean)
	 * @see #setVisibility(JWindow,int)
	 * @see #setVisibility(JWindow,boolean)
	 */
	protected synchronized void setVisibility( int handle, int vis ) {
		if( handle == -1 ) {
			return;
		}
		
		aWindowEntry entry = getWindowEntry(handle);
		if( entry != null ) {
			if( (entry.getVisibility() == vis) || 
				( ((vis & aWindowEntry.VISIBILITY_FORCE) == 0 ) && 
				  ((entry.getVisibility() & aWindowEntry.VISIBILITY_FORCE) > 0) 
				) ) {

				return;
			}
			
			if( (vis == aWindowEntry.VISIBILITY_ON) || (vis == aWindowEntry.VISIBILITY_FORCE_ON) ) {				
				int index = 0;
				
				if( entry.getParent() != null ) {
					Vector children = getChildren(entry.getParent());
					for( int i=0; i < children.size(); i++ ) {
						Integer chandle = new Integer(((JWindow)children.get(i)).getHandle());
						int curindex = windowZOrder.indexOf(chandle);
						
						if( curindex > -1 ) {
							index = Math.min(index,curindex);
						}
					}
					if( index > windowZOrder.indexOf(new Integer(entry.getParent().getHandle())) ) {
						index = windowZOrder.indexOf(new Integer(entry.getParent().getHandle()));
					}
				}
				windowZOrder.add(index,new Integer(handle));
			}
			else {
				int index = windowZOrder.indexOf(new Integer(handle));
				
				if( index != -1 ) {
					windowZOrder.remove(index);
				}
			}
			
			entry.setVisibility(vis);
			
			// Wenn wir es mit einem neuen Fenster zu tun haben, müssen wir den
			// gesamten Bildschirm löschen, da die Fensterklasse noch nicht das Handle
			// bekommen hat
			if( getWindowEntry(handle).getWindow().getHandle() > 0 ) {
				requestRedraw(getWindowEntry(handle).getWindow());
			}
			else {
				requestRedraw();
			}
			
			if( (vis | aWindowEntry.VISIBILITY_FORCE) == aWindowEntry.VISIBILITY_FORCE_ON ) {
				
				if( inputFocusWindow != entry.getWindow().getHandle() ) {
					requestFocus(entry.getWindow());
				}
				
				Vector<Object> event = new Vector<Object>();
				event.add(0, new Integer(EVENT_ON_CHANGE_VISIBILITY));
				event.add(1, entry.getWindow());
				event.add(2, Boolean.TRUE);
				
				eventList.add(event);
			}
			else if( (vis | aWindowEntry.VISIBILITY_FORCE) == aWindowEntry.VISIBILITY_FORCE_OFF ) {
				if( inputFocusWindow == entry.getWindow().getHandle() ) {
					/*
					 * TODO: in ein event ueberfuehren
					 */
					entry.getWindow().onFocusChanged(false);
					inputFocusWindow = -1;
				}
				
				Vector<Object> event = new Vector<Object>();
				event.add(0, new Integer(EVENT_ON_CHANGE_VISIBILITY));
				event.add(1, entry.getWindow());
				event.add(2, Boolean.FALSE);
				
				eventList.add(event);
			}
		}
	}
	
	/**
	 * Liefert zurück, ob ein bestimmtes Fenster sichtbar ist oder nicht.
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * 
	 * @return <code>true</code> (sichtbar) oder <code>false</code> (nicht sichtbar) 
	 */
	public boolean getVisibility( JWindow aWindow ) {
		aWindowEntry entry = getWindowEntry(aWindow.getHandle());
		if( entry != null ) {
			if( (entry.getVisibility() | aWindowEntry.VISIBILITY_FORCE) == aWindowEntry.VISIBILITY_FORCE_ON ) {
				return true;
			}
			return false;
		}
		return false;
	}
	
	/**
	 * Setzt die Fensterform auf ein {@link Shape}-Objekt.
	 * Mit jedem Fenster muss ein Shape-Objekt verknüpft sein. 
	 * Im Normalfall übernimmt JWindow allerdings die Generierung.
	 * 
	 * @param aWindow	Das betreffende Fenster
	 * @param ashape	Die neue Fensterform
	 */
	public void setWindowShape( JWindow aWindow, Shape ashape) {
		if( aWindow == null ) {
			return;
		}

		aWindowEntry entry = getWindowEntry(aWindow.getHandle());

		entry.setShape( ashape );
	}
	
	/**
	 * Liefert eine Instanz von {@link JImageCache}
	 * 
	 * @return Eine {@link JImageCache}-Instanz
	 */
	public JImageCache getImageCache() {
		return imgCache;
	}
	
	/**
	 * Liefert alle Kind-Fenster des angegebenen Fensters zurück
	 * 
	 * @param aWindow Das Fenster zu dem die Kind-Fenster ermittelt werden sollen
	 * 
	 * @return Ein {@link Vector} mit allen Kind-Fenstern
	 */
	public Vector getChildren( JWindow aWindow ) {
		if( aWindow == null ) {
			return new Vector();
		}
		
		Vector<JWindow> parentList = new Vector<JWindow>();
		
		Iterator iter = windows.values().iterator();
		while( iter.hasNext() ) {
			aWindowEntry awnd = (aWindowEntry)iter.next();
			JWindow wnd = awnd.getParent();
			
			if( wnd != null ) {
				if( wnd.getHandle() == aWindow.getHandle() ) {
					parentList.add(awnd.getWindow());
				}
			}
		}
		
		return parentList;
	}
	
	/**
	 * Fordert ein Neuzeichnen des gesamten Bildschirms an
	 * 
	 * @see #requestRedraw(JWindow)
	 */
	public void requestRedraw() {
		requestRedraw(null);
	}
	
	/**
	 * Fordert ein Neuzeichnen für den gesamten Bildschirm bzw ein Fenster an.
	 * 
	 * @param wnd Das neuzuzeichnende Fenster. <code>NULL</code> für den gesamten Bildschirm
	 */
	public void requestRedraw( JWindow wnd ) {
		redraw = true;
		
		if( wnd == null ) {
			Iterator iter = windows.values().iterator();
			while( iter.hasNext() ) {
				aWindowEntry awnd = (aWindowEntry)iter.next();
				awnd.setRedrawStatus(true);
			}
		}
		else {
			getWindowEntry(wnd.getHandle()).setRedrawStatus(true);
		}
	}
	
	/**
	 *  Entfernt das angegebene Fenster. 
	 *  Es ist jedoch grundsätzlich besser ein Fenster über JWindow.dispose() zu entfernen.  
	 * 
	 * @param aWindow Das zu entfernende Fenster
	 */
	public void removeWindow( JWindow aWindow ) {
		if( aWindow == null ) {
			return;
		}
		
		if( aWindow.getHandle() <= 0 ) {
			aWindow.log("Warnung: Ein Fenster kann nicht entfernt werden bevor es nicht initalisiert wurde");
			return;
		}
		
		aWindowEntry parent = null;
		if( aWindow.getParent() != null ) {
			parent = getWindowEntry(aWindow.getParent().getHandle());
		}
		
		Vector children = getChildren(aWindow);
		
		System.out.println("Removing Window with handle "+aWindow.getHandle());
		
		for( int i=0; i < children.size(); i++ ) {
			((JWindow)children.get(i)).dispose();
		}
		
		setVisibility( aWindow, false );
		
		synchronized(this) {
			// Events durchgehen
			for( int i=0; i < eventList.size(); i++ ) {
				Vector<Object> event = eventList.get(i);
				if( ((JWindow)event.get(1)).getHandle() == aWindow.getHandle() ) {
					eventList.remove(i);
				}
			}
			
			windows.remove(new Integer(aWindow.getHandle()));
		}
		if( parent != null ) {
			recalcVClientRect(parent);
		}
	}
	
	/**
	 * Liefert das Fenster unter den angegebenen Koordinaten
	 *  
	 * @param x Der X-Teil der Koordinate
	 * @param y Der Y-Teil der Koordinate
	 * @return Gibt das Handle des Fensters zurück. Wenn kein Fenster gefunden wurde -1
	 */
	
	public int getWindowUnderCoord( int x, int y ) {		
		for( int i = 0; i < windowZOrder.size(); i++ ) {
			int handle = windowZOrder.get(i);
		
			aWindowEntry awnd = getWindowEntry(handle);
			
			if( awnd.getShape() == null ) {
				awnd.getWindow().log("WARNUNG: Das Fenster verfuegt ueber keine Shape");
				continue;
			}
			
			if( !awnd.getShape().contains(x,y) ) {
				continue;
			}
			
			if( awnd.getParent() != null ) {
				aWindowEntry parent = awnd;
				boolean none = true;
				do {
					/*
					 * TODO: check
					 */
					if( JScrollBar.class.isInstance(parent.getWindow()) ) {
						parent = getWindowEntry(parent.getParent().getHandle());
						
						Rectangle rect = new Rectangle(	parent.getWindowPosition().x,
														parent.getWindowPosition().y,
														parent.getWindowRect().width,
														parent.getWindowRect().height); 
						
						if( rect.contains(x,y) ) {
							none = false;
							break;
						}
					}
					else {
						parent = getWindowEntry(parent.getParent().getHandle());
						
						Rectangle rect = parent.getVisibleClientRect();
						
						if( rect.contains(x,y) ) {
							none = false;
							break;
						}
					}
				} while( parent.getParent() != null );
				
				if( none ) {
					continue;
				}
			}
				
			return awnd.getWindow().getHandle();
		}
		
		return -1;
	}
	
	/**
	 * Liefert die Bildschirmbreite zurück.
	 * Dabei handelt es sich nicht um die Breite des gesamten (physischen) Bildschirms, 
	 * sondern um die Breite, die diese Anwendung/dieses Applet hat in Pixeln
	 * 
	 * @return Die Bildschirmbreite
	 * 
	 * @see #getScreenHeight() 
	 */
	public int getScreenWidth() {
		return getWidth();
	}
	
	/**
	 * Liefert die Bildschirmhöhe zurück.
	 * Dabei handelt es sich nicht um die Höhe des gesamten (physischen) Bildschirms, 
	 * sondern um die Höhe, die diese Anwendung/dieses Applet hat in Pixeln
	 * 
	 * @return Die Bildschirmhöhe
	 * 
	 * @see #getScreenWidth() 
	 */
	public int getScreenHeight() {
		return getHeight();
	}
	
	/**
	 * Liefert den Namen der zu verwendenden Standardfont zurück
	 * 
	 * @return Der Name der Standardfont
	 */
	public String getDefaultFont() {
		return fontName;
	}
	
	/**
	 * Liefert den zu verwendenden Datenpfad zurück.
	 * Dieser wird beim Starten der Anwendung übergeben und zeigt auf das DS2-Datenverzeichnis
	 * 
	 * @return Der Pfad zum DS2-Datenverzeichnis
	 */
	public String getDataPath() {
		return datapath;
	}
	
	/**
	 * Fragt an, ob ein bestimmtes Fenster den Fokus bekommen kann
	 * 
	 * @param wnd	Das Fenster, das den Fokus haben möchte
	 */
	public void requestFocus( JWindow wnd ) {
		if( inputFocusWindow != wnd.getHandle() ) {
			if( inputFocusWindow != -1 ) {
				/*
				 * TODO: in ein event ueberfuehren
				 */
				getWindowEntry(inputFocusWindow).getWindow().onFocusChanged(false);
			}
			
			inputFocusWindow = wnd.getHandle();
			/*
			 * TODO: in ein event ueberfuehren
			 */
			wnd.onFocusChanged(true);
			
			requestRedraw();
		}
	}
	
	/**
	 * Liefert zurück ob ein bestimmtes Fenster den Fokus hat
	 * 
	 * @param wnd	Das zu überprüfende Fenster
	 * 
	 * @return <code>true</code> falls das Fenster den Fokus hat. Andernfalls <code>false</code>
	 */
	public boolean hasFocus( JWindow wnd ) {
		if( inputFocusWindow == wnd.getHandle() ) {
			return true;
		}
		
		return false;
	}
	
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
	public void setWindowBorder( JWindow aWindow, int left, int top, int right, int bottom ) {
		if( aWindow == null ) {
			return;
		}
		
		getWindowEntry(aWindow.getHandle()).setBorderSize( new Rectangle(left,top,right,bottom) );
		Rectangle rect = getWindowEntry(aWindow.getHandle()).getWindowRect();
		
		if( rect.getWidth() <= left+right+1 ) { 
			rect.width = left+right+2;
		}
		
		if( rect.getHeight() <= top+bottom+1 ) { 
			rect.height = top+bottom+2;
		}
		getWindowEntry(aWindow.getHandle()).setWindowRect(rect);
		setWindowShape(aWindow, aWindow.getShape());
	}
	
	/**
	 * Verändert die Größe eines Fensters.
	 * Alle Werte sind inklusive der Ränder. 
	 * Ein Fenster kann nicht kleiner als die betreffenden Ränder + 2 werden
	 * 
	 * @param aWindow	Das Fenster
	 * @param width		Die neue Breite
	 * @param height	Die neue Höhe
	 */
	public void setWindowSize( JWindow aWindow, int width, int height ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		
		Rectangle border = awnd.getBorderSize();
		if( width <= border.x+border.width+1 ) { 
			width = border.x+border.width+2;
		}
		
		if( height <= border.y+border.height+1 ) { 
			height = border.y+border.height+2;
		}
		
		Rectangle rect = awnd.getWindowRect();
	
		boolean changed = false;
		
		if( (rect.getWidth() != width) || (rect.getHeight() != height) ) {
			changed = true;
		}
		awnd.setWindowRect( new Rectangle(width,height) );
		
		recalcWindow(awnd);
		
		if( changed ) {
			/*
			 * TODO: Das sollte ins Event-System
			 */
			awnd.getWindow().onResize();
		}
	}
	
	/**
	 * Setzt die Clientgröße eines Fensters auf neue Werte
	 * 
	 * @param aWindow	Das Fenster
	 * @param cwidth	Die neue Client-Breite
	 * @param cheight	Die neue Client-Höhe
	 */
	public void setWindowClientSize( JWindow aWindow, int cwidth, int cheight ) {
		if( aWindow == null ) {
			return;
		}
		if( cwidth < 2 ) {
			cwidth = 2;
		}
		if( cheight < 2 ) {
			cheight = 2;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		
		boolean changed = false;
		
		if( (awnd.getClientRect().getWidth() != cwidth) || (awnd.getClientRect().getHeight() != cheight) ) {
			changed = true;
		}
		
		awnd.setClientRect( new Rectangle(cwidth, cheight) );
		/*if( getWindowEntry(aWindow.getHandle()).getScrollBarHandle() > 0 ) {
			cwidth += 30;
		}*/
		Rectangle border = getWindowEntry(aWindow.getHandle()).getBorderSize();
		awnd.setWindowRect( new Rectangle(	cwidth + (int)border.getX() + (int)border.getWidth(), 
																			cheight + (int)border.getY() + (int)border.getHeight()) );
		
		recalcWindow(awnd);
		
		if( changed ) {
			/*
			 * TODO: Das sollte ins Event-System
			 */
			awnd.getWindow().onResize();
		}
	}
	
	/**
	 * Liefert die Fensterbreite zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fensterbreite
	 */
	public int getWindowWidth( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		
		return (int)getWindowEntry(aWindow.getHandle()).getWindowRect().getWidth();
	}
	
	/**
	 * Liefert die Fensterhöhe zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fensterhöhe
	 */
	public int getWindowHeight( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		
		return (int)getWindowEntry(aWindow.getHandle()).getWindowRect().getHeight();
	}
	
	/**
	 * Liefert die Fenster-Clientbreite zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fenster-Clientbreite
	 */
	public int getWindowClientWidth( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		
		return (int)getWindowEntry(aWindow.getHandle()).getClientRect().getWidth();
	}
	
	/**
	 * Liefert die Fenster-Clienthöhe zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Fenster-Clienthöhe
	 */
	public int getWindowClientHeight( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		
		return (int)getWindowEntry(aWindow.getHandle()).getClientRect().getHeight();
	}
	
	/**
	 * Setzt die Fensterposition auf einen bestimmten Modus.
	 * Mögliche Modi sind {@link IWindowManager#POSITION_CUSTOM}, {@link IWindowManager#POSITION_SCREEN_CENTER} und
	 * {@link IWindowManager#POSITION_ALWAYS_WITHIN_SCREEN}
	 * 
	 * @param aWindow	Das Fenster
	 * @param mode		Der neue Fensterplatzierungsmodus
	 */
	public void setWindowPosition( JWindow aWindow, int mode ) {
		if( aWindow == null ) {
			return;
		}
		
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		
		if( mode == IWindowManager.POSITION_SCREEN_CENTER ) {
			int x = (getScreenWidth() - awnd.getWindowRect().width)/2;
			int y = (getScreenHeight() - awnd.getWindowRect().height)/2;
			
			awnd.setWindowPositionMode(mode);
			awnd.setWindowPosition( new Point(x,y) );
			
			if( awnd.getParent() != null ) {
				x -= getWindowClientX(awnd.getParent());
				y -= getWindowClientY(awnd.getParent());
			}
			awnd.setWindowRelativePosition( new Point(x,y) );
		}
		else if( mode == IWindowManager.POSITION_ALWAYS_WITHIN_SCREEN ) {
			int curmode = awnd.getWindowPositionMode();
			if( (curmode & POSITION_ALWAYS_WITHIN_SCREEN) > 0 ) {
				curmode ^= POSITION_ALWAYS_WITHIN_SCREEN;
			}
			else {
				curmode |= POSITION_ALWAYS_WITHIN_SCREEN;
			}
			awnd.setWindowPositionMode(curmode);
		}
		
		recalcWindow(awnd);
	}
	
	/**
	 * Setzt die Fensterposition auf eine neue x/y-Position
	 * 
	 * @param aWindow	Das Fenster
	 * @param x			Die x-Koordinate
	 * @param y			Die y-Koordinate
	 */
	public void setWindowPosition( JWindow aWindow, int x, int y ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		awnd.setWindowRelativePosition( new Point(x,y) );
		
		if( awnd.getParent() != null ) {
			aWindowEntry parent = getWindowEntry(aWindow.getParent().getHandle());
			x += parent.getWindowPosition().x+parent.getBorderSize().x;
			y += parent.getWindowPosition().y+parent.getBorderSize().y;
		}
		
		awnd.setWindowPosition( new Point(x,y) );
		awnd.setWindowPositionMode( awnd.getWindowPositionMode() | IWindowManager.POSITION_CUSTOM );
		
		recalcWindow(awnd);
	}
	
	/**
	 * Liefert den Fensterpositionsmodus zurück
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der Fensterpositionsmodus
	 * 
	 * @see #setWindowPosition(JWindow, int)
	 */
	public int getWindowPositionMode( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getWindowPositionMode();
	}
	
	/**
	 * Liefert die x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowX( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getRelativeWindowPosition().x;
	}
	
	/**
	 * Liefert die y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die Y-Koordinate
	 */
	public int getWindowY( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getRelativeWindowPosition().y;
	}
	
	/**
	 * Liefert die absolute x-Koordinate der Fensterposition zurück.
	 * Die x-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowAbsoluteX( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getWindowPosition().x;
	}
	
	/**
	 * Liefert die absolute y-Koordinate der Fensterposition zurück.
	 * Die y-Koordinate ist hierbei relativ zur oberen linken Ecke des Anwendungsfensters/Applets!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die y-Koordinate
	 */
	public int getWindowAbsoluteY( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getWindowPosition().y;
	}
	
	/**
	 * Liefert die x-Koordinate des Clientbreichs des Fensters zurück.
	 * Die x-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die X-Koordinate
	 */
	public int getWindowClientX( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getWindowPosition().x+getWindowEntry(aWindow.getHandle()).getBorderSize().x;
	}
	
	/**
	 * Liefert die y-Koordinate des Clientbreichs des Fensters zurück.
	 * Die y-Koordinate ist relativ zum Elternfenster!
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Die y-Koordinate
	 */
	public int getWindowClientY( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getWindowPosition().y+getWindowEntry(aWindow.getHandle()).getBorderSize().y;
	}
	
	/**
	 * Liefert zurück um wieviel das Clientrect größer ist als die anzeigbare Größe in Y-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der überstehende Clientrect-Wert in Pixel
	 */
	public int getWindowVClientOverflow( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		if( awnd.getVirtualClientRect().height > awnd.getClientRect().height ) {
			return awnd.getVirtualClientRect().height - awnd.getClientRect().height;
		}
		
		return 0;
	}
	
	/**
	 * Scrollt das Fenster in y-Richtung um den angegebenen Betrag
	 * 
	 * @param aWindow	Das Fenster
	 * @param value		Der Wert um das das Fenster gescrollt werden soll
	 */
	public void vScrollClientWindow( JWindow aWindow, int value ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		
		if( awnd.getVScrollOffset() + value > 0 ) {
			awnd.setVScrollOffset(0);
			return;
		}
		if( awnd.getVScrollOffset() + value < -getWindowVClientOverflow(aWindow) ) {
			awnd.setVScrollOffset(-getWindowVClientOverflow(aWindow));
		}
		awnd.setVScrollOffset(awnd.getVScrollOffset() + value);
	}
	
	/**
	 * Gibt zurück, um wieviel das Fenster im Moment in y-Richtung gescrollt ist
	 * 
	 * @param aWindow	Das Fenster
	 * 
	 * @return Der Betrag um den das Fenster gescrollt ist
	 */
	public int getWindowVClientOffset( JWindow aWindow ) {
		if( aWindow == null ) {
			return 0;
		}
		return getWindowEntry(aWindow.getHandle()).getVScrollOffset();
	}
	
	/**
	 * Setzt die minimale Clientbereichsgröße in Y-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * @param size		Die minimale Größe des Clientbereichs
	 */
	public void setWindowVClientMinSize( JWindow aWindow, int size ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		Rectangle min = awnd.getMinimalVirtualClientRect();
		min.height = size;
		awnd.setMinimalVirtualClientRect(min);
		
		recalcVClientRect(awnd);
	}
	
	/**
	 * Setzt die minimale Clientbereichsgröße in X-Richtung
	 * 
	 * @param aWindow	Das Fenster
	 * @param size		Die minimale Größe des Clientbereichs
	 */
	public void setWindowHClientMinSize( JWindow aWindow, int size ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		Rectangle min = awnd.getMinimalVirtualClientRect();
		min.width = size;
		awnd.setMinimalVirtualClientRect(min);
		
		recalcVClientRect(awnd);
	}
	
	/**
	 * Aktiviert/Deaktiviert die Scrollbarkeit eines Fensters.
	 * Wenn die Scrollbarkeit deaktiviert ist und der Clientbereich des Fensters
	 * zu groß wird, wird keine Scrollbar angezeigt und der überstehende Teil einfach
	 * abgeschnitten
	 * 
	 * @param aWindow	Das Fenster
	 * @param value		<code>true</code> für scrollbar und <code>false</code> für nicht scrollbar 
	 */
	public void setWindowScrollability( JWindow aWindow, boolean value ) {
		if( aWindow == null ) {
			return;
		}
		aWindowEntry awnd = getWindowEntry(aWindow.getHandle());
		awnd.setScrollable(value);
		
		recalcVClientRect(awnd);
	}
	
	/**
	 * Berechnet diverse Werte eines Fensters neu (Absolute Position, evt notwendige Scrollbars usw)
	 * 
	 * @param awnd	Das zu berechnende Fenster
	 */
	private synchronized void recalcWindow( aWindowEntry awnd ) {
		if( (awnd.getWindowPositionMode() & IWindowManager.POSITION_SCREEN_CENTER) > 0 ){
			int x = (getScreenWidth() - awnd.getWindowRect().width)/2;
			int y = (getScreenHeight() - awnd.getWindowRect().height)/2;
						
			if( (x != awnd.getWindowPosition().x) || (y != awnd.getWindowPosition().y) ) {
				setWindowPosition( awnd.getWindow(), IWindowManager.POSITION_SCREEN_CENTER );
			}
			
			if( awnd.getParent() != null ) {
				x -= getWindowClientX(awnd.getParent());
				y -= getWindowClientY(awnd.getParent());
			}
			awnd.setWindowRelativePosition( new Point(x,y) );
		}
		else if( (awnd.getWindowPositionMode() & IWindowManager.POSITION_ALWAYS_WITHIN_SCREEN) > 0 ) {
			Point pos = awnd.getWindowPosition();
			if( (awnd.getWindowRect().width != 0) && (awnd.getWindowRect().height != 0) ) {
				if( pos.x + awnd.getWindowRect().width > getScreenWidth() ) {
					pos.x -= awnd.getWindowRect().width;
				}
				
				if( pos.y + awnd.getWindowRect().height > getScreenHeight() ) {
					pos.y -= awnd.getWindowRect().height;
				}
			}
			if( pos.x < 0 ) {
				pos.x = awnd.getWindowPosition().x;
			}
			
			if( pos.y < 0 ) {
				pos.y = awnd.getWindowPosition().y;
			}
			awnd.setWindowPosition(pos);
			
			pos = awnd.getRelativeWindowPosition();
			if( awnd.getParent() != null ) {
				pos.x = awnd.getWindowPosition().x - awnd.getParent().getClientX();
				pos.y = awnd.getWindowPosition().y - awnd.getParent().getClientY();
			}
			else {
				pos = awnd.getWindowPosition();
			}
			awnd.setWindowRelativePosition(pos);
		}
		
		Rectangle border = awnd.getBorderSize();
		
		Rectangle clientrect = new Rectangle(	awnd.getWindowRect().width-border.x-border.width,
												awnd.getWindowRect().height-border.y-border.height);
		
		if( awnd.getScrollBarHandle() > 0 ) {
			clientrect.width -= 30;
		}
		
		if( (clientrect.width != awnd.getClientRect().width) ||
			(clientrect.height != awnd.getClientRect().height) ) {
			awnd.setClientRect( clientrect );
		}
		
		// Kinder repositionieren
		int xoffset = awnd.getWindowPosition().x+awnd.getBorderSize().x;
		int yoffset = awnd.getWindowPosition().y+awnd.getBorderSize().y;
		
		Vector children = getChildren(awnd.getWindow());
		for( int i=0; i < children.size(); i++ ) {
			JWindow aChild = (JWindow)children.get(i);
			aWindowEntry aChildWnd = getWindowEntry(aChild.getHandle());
			if( aChildWnd != null ) {
				Point relpos = aChildWnd.getRelativeWindowPosition();
				aChildWnd.setWindowPosition( new Point(relpos.x+xoffset,relpos.y+yoffset));
			
				recalcWindow(aChildWnd);
			}
		}
		
		// VirtualClientRect vom Elternfenster aktuallisieren
		if( awnd.getParent() != null ) {
			aWindowEntry parent = getWindowEntry(awnd.getParent().getHandle());
			if( awnd.getWindow().getHandle() != parent.getScrollBarHandle() ) {
				recalcVClientRect(parent);
			}
		}
		
		if( awnd.getScrollBarHandle() > 0 ) {
			aWindowEntry scrollbar = getWindowEntry(awnd.getScrollBarHandle());
			if( scrollbar != null ) {
				scrollbar.getWindow().setPosition(awnd.getClientRect().width,0);
				scrollbar.getWindow().setSize(30,awnd.getClientRect().height);
			}
		}
		
		if( (awnd.getOffscreenBuffer() == null) || 
			(awnd.getOffscreenBuffer().getWidth() != awnd.getWindowRect().width) ||
			(awnd.getOffscreenBuffer().getHeight() != awnd.getWindowRect().height) ) {
			
			Vector event = new Vector();
			event.add(0, new Integer(EVENT_RECREATE_BACKBUFFER));
			event.add(1, awnd.getWindow());
			
			eventList.add(event);
			requestRedraw(awnd.getWindow());
		}
		
		setWindowShape(awnd.getWindow(), awnd.getWindow().getShape());
	}
	
	/**
	 * Berechnet den Clientbereich in Y-Richtung neu
	 * 
	 * @param awnd	Das Fenster
	 */
	private synchronized void recalcVClientRect( aWindowEntry awnd ) {
		if( awnd == null ) {
			return;
		}
		
		Rectangle vClientRect = (Rectangle)awnd.getClientRect().clone();
		if( awnd.isScrollable() ) {
			Rectangle min = awnd.getMinimalVirtualClientRect();
			if( min.width > vClientRect.width ) {
				vClientRect.width = min.width;
			}
			if( min.height > vClientRect.height ) {
				vClientRect.height = min.height;
			}
		
			Vector children = getChildren(awnd.getWindow());
			for( int i=0; i < children.size(); i++ ) {
				JWindow aChild = (JWindow)children.get(i);
				if( aChild.getHandle() ==  awnd.getScrollBarHandle() ) {
					continue;
				}
				aWindowEntry aChildWnd = getWindowEntry(aChild.getHandle());
				Rectangle childWndRect = aChildWnd.getWindowRect();
			
				if( childWndRect.width+aChildWnd.getRelativeWindowPosition().x > vClientRect.width ) {
					vClientRect.width = childWndRect.width+aChildWnd.getRelativeWindowPosition().x;
				}
			
				if( childWndRect.height+aChildWnd.getRelativeWindowPosition().y > vClientRect.height ) {
					vClientRect.height = childWndRect.height+aChildWnd.getRelativeWindowPosition().y;
				}
			}
		}
		awnd.setVirtualClientRect(vClientRect);

		if( awnd.isScrollable() && (vClientRect.height > awnd.getClientRect().height) && (awnd.getScrollBarHandle() <= 0) ) {
			JScrollBar vScrollWindow = new JScrollBar( awnd.getWindow(), this );
			awnd.setScrollBarHandle(vScrollWindow.getHandle());
			
			aWindowEntry scrollbar = getWindowEntry(awnd.getScrollBarHandle());
			scrollbar.getWindow().setPosition(awnd.getClientRect().width-30,0);
			scrollbar.getWindow().setSize(30,awnd.getClientRect().height);
			
			Rectangle border = awnd.getBorderSize();
			awnd.setClientRect( new Rectangle(	awnd.getWindowRect().width-border.x-border.width-30,
												awnd.getWindowRect().height-border.y-border.height) );
		}
		else if( (!awnd.isScrollable() || (vClientRect.height <= awnd.getClientRect().height)) && 
				(awnd.getScrollBarHandle() > 0) ) {			
			aWindowEntry scrollbar = getWindowEntry(awnd.getScrollBarHandle());
			if( scrollbar != null ) {
				awnd.setScrollBarHandle(0);
				scrollbar.getWindow().dispose();
				
				Rectangle clientRect = awnd.getClientRect();
				clientRect.width += 30;
				awnd.setClientRect(clientRect);
			}
			else {
				awnd.getWindow().log("WARNUNG: scrollbar konnte nicht entfernt werden");
			}
		}
	}
	
	public void setProperty( int mode, Object value ) {
		switch( mode ) {
		case IWindowManager.PROPERTY_BUFFERED_OUTPUT:
			bufferedOutput = ((Boolean)value).booleanValue();
			System.out.println("Setting BufferedOutput to: "+value);
			break;
		}
	}
	
	public Object getProperty( int mode ) {
		switch( mode ) {
		case IWindowManager.PROPERTY_BUFFERED_OUTPUT:
			return new Boolean(bufferedOutput);
		}
		return null;
	}
}