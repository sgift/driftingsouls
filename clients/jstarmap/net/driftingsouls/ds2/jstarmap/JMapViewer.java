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

package net.driftingsouls.ds2.jstarmap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.Transparency;
import java.util.Vector;

import net.driftingsouls.ds2.framework.JWindow;
import net.driftingsouls.ds2.framework.IWindowManager;
import net.driftingsouls.ds2.framework.services.MapConnector;

/**
 * JMapViewer übernimmt die grafische Darstellung der Sternenkarte.
 * Zugleich fungiert es als Rootwindow für die Sternenkarte und ist somit
 * Ausgangspunkt für fast alle Dinge ({@link JAboutDialog} ausgenommen. Der wird
 * von {@link JStarmap} aufgerufen)
 * 
 * @author Christopher Jung
 *
 */
public class JMapViewer extends JWindow {
	private static int tilesizeX = 25;
	private static int tilesizeY = 25;
	
	private JInfoDialog dlgToolTip;	
	private JMapContextDialog dlgMapContext;
	protected Vector<String> mapImageList;
	
	private int xOffset;
	private int yOffset;
	//private int oldXOffset;
	//private int oldYOffset;
	
	//private int mapXOffset;
	//private int mapYOffset;
	
	private int lastX;
	private int lastY;
	
	protected MapConnector map;
	
	private int currentZoom;
	
	/**
	 * Konstruktor
	 * @param parent Das Elternfenster
	 * @param windowmanager Der Fenstermanager
	 * @param map Der Map-Connector
	 */
	public JMapViewer( JWindow parent, IWindowManager windowmanager, MapConnector map ) {
		super( parent, windowmanager );
		
		this.map = map;
		
		currentZoom = 100;
		
		xOffset = 0;
		yOffset = 0;
		/*oldXOffset = 0;
		oldYOffset = 0;
		mapXOffset = 0;
		mapYOffset = 0;*/
		lastX = 0;
		lastY = 0;
		
		dlgToolTip = new JInfoDialog( this, windowmanager, getWindowManager().getDefaultFont(), map );
		getWindowManager().setVisibility( dlgToolTip, false );
		
		dlgMapContext = new JMapContextDialog( this, windowmanager, map );
		getWindowManager().setVisibility( dlgMapContext, false );
		
		map.notifyOnChange(this);
	}
	
	private void cleanUpGFX() {
		final int oldTileSize = tilesizeX;
		
		Thread cleanerthread = new Thread( 
			new Runnable() {
				public void run() {
					// Bild laden
					try {
						if( mapImageList == null ) {
							mapImageList = new Vector<String>();
									
							for( int tmpy=0; tmpy < map.getHeight(); tmpy++ ) {
								for( int tmpx=0; tmpx < map.getWidth(); tmpx++ ) {
									String file = map.getSector(tmpx, tmpy);
										
									if( !mapImageList.contains(file+".png") ) {
										mapImageList.add(file+".png");
									}
								}
							}
						}
								
						for( int i=0; i < mapImageList.size(); i++ ) {
							String file = mapImageList.get(i);
									
							getImageCache().dropImages(file+"#"+oldTileSize+"_"+oldTileSize);
						}
					}
					catch( Exception e) {}
				}
			}
		);
		cleanerthread.setPriority(Thread.MIN_PRIORITY);
		cleanerthread.start();
	}
	
	@Override
	public boolean handleEvent( int handle, String event ) {
		boolean result = super.handleEvent( handle, event );
		
		if( (handle == getHandle()) && (event == "map_changed") ) {
			cleanUpGFX();
			mapImageList = null;			
			
			return true;
		}
		
		return result;
	}
	
	/**
	 * Gibt den aktuellen Zoom der Karte zurueck
	 * @return Der aktuelle Zoom
	 */
	public int getCurrentZoom() {
		return currentZoom;
	}
	
	/**
	 * Zoomt die Karte um einen bestimmten Faktor um eine Koordinate des Fensters herum
	 * @param factor der Faktor um den gezoomt werden soll (100 = Normalgroesse, 100%)
	 * @param x Die X-Koordinate im Fenster auf die gezoomt werden soll
	 * @param y Die Y-Koordinate im Fenster auf die gezoomt werden soll
	 * 
	 */
	public void zoomMap( int factor, int x, int y ) {
		if( factor < 1 ) {
			return;
		}
		
		currentZoom = factor;
		
		int newTileSize = (int)(25*((float)factor/100));
		
		if( tilesizeX != 25 ) {
			cleanUpGFX();
		}
		
		xOffset -= x;
		yOffset -= y;
		
		if( tilesizeX != newTileSize ) {
			xOffset = xOffset/tilesizeX * newTileSize;
			yOffset = yOffset/tilesizeY * newTileSize;
			
			tilesizeX = newTileSize;
			tilesizeY = newTileSize;
			
			//oldXOffset = 0;
			//oldYOffset = 0;
		}
		
		xOffset += getClientWidth()/2;
		yOffset += getClientHeight()/2;
		
		if( (-xOffset + getClientWidth()) / tilesizeX > map.getWidth() ) {
			xOffset = getWidth() - map.getWidth()*tilesizeX;
		}
		
		if( (-yOffset + getClientHeight()) / tilesizeY > map.getHeight() ) {
			yOffset = getClientHeight() - map.getHeight()*tilesizeY;
		}
		
		if( xOffset > 0 ) {
			xOffset = 0;
		}
		
		if( yOffset > 0 ) {
			yOffset = 0;
		}
		
		getWindowManager().setVisibility( dlgToolTip, false );
		
		getWindowManager().requestRedraw(this);
	}
	
	@Override
	public boolean mousePressed( int x, int y, int button ) {
		boolean result = super.mousePressed(x, y, button);
		
		if( !map.isMapLoaded() ) {
			return result;
		}
		
		if( button == InputEvent.BUTTON3_MASK ) {	
			lastX = x;
			lastY = y;
			
			getWindowManager().setVisibility( dlgToolTip, false );
			
			getWindowManager().requestRedraw(this);
			
			return true;
		}
		else if( button == InputEvent.BUTTON2_MASK ) {
			getWindowManager().setVisibility( dlgMapContext, true );
			
			dlgMapContext.setPosition( x, y );
			
			getWindowManager().requestRedraw(this);
			
			return true;
		}
		else if( button == InputEvent.BUTTON1_MASK ) {
			int xcell = 0;
			int ycell = 0;
			
			xcell = (x - xOffset) / tilesizeX + 1;
			ycell = (y - yOffset) / tilesizeY + 1;
			System.out.println("Koordinaten: "+xcell+"/"+ycell+" - Sectorvalue: "+map.getSector(xcell-1,ycell-1));

			getWindowManager().setVisibility( dlgToolTip, true );
			
			dlgToolTip.setPosition( x, y );
			dlgToolTip.setSize(230,dlgToolTip.getHeight());
			dlgToolTip.setAutoHeight( true );
			dlgToolTip.setSector(xcell,ycell);
			
			return true;
		}
		
		return result;
	}
	
	@Override
	public boolean mouseDragged( int x, int y, int button ) {
		boolean result = super.mouseDragged( x, y, button );
		
		if( button != InputEvent.BUTTON3_MASK ) {
			return result;	
		}
		
		if( !map.isMapLoaded() ) {
			return result;
		}
		
		//oldXOffset = xOffset;
		//oldYOffset = yOffset;
		
		xOffset += x - lastX;
		yOffset += y - lastY;
		
		if( xOffset > 0 ) {
			xOffset = 0;
		}
		
		if( yOffset > 0 ) {
			yOffset = 0;
		}
		
		if( xOffset < -map.getWidth()*tilesizeX+getClientWidth() ) {
			xOffset = -map.getWidth()*tilesizeX+getClientWidth();
		}
		
		if( yOffset < -map.getHeight()*tilesizeY+getClientHeight() ) {
			yOffset = -map.getHeight()*tilesizeY+getClientHeight();
		}
		
		lastX = x;
		lastY = y;
		
		getWindowManager().requestRedraw(this);
		
		return true;
	}
	
	@Override
	public boolean keyPressed( int keycode, char key ) {
		if( key == 'z' ) {	
			getWindowManager().setVisibility( dlgMapContext, !getWindowManager().getVisibility(dlgMapContext) );
			
			dlgMapContext.setPosition( lastX, lastY );
			
			getWindowManager().requestRedraw(this);
			
			return true;
		}
		
		return super.keyPressed(keycode,key);
	}
	
	/**
	 * Zeichnet die Karte
	 * @param g Der Zeichenkontext in dem gezeichnet werden soll
	 */
	protected void drawMap(Graphics2D g) {
		int xbase = 1;
		int ybase = 1;
		
		if( xOffset < 0 ) {
			xbase = -(xOffset / tilesizeX);
			if( xbase < 1 ) {
				xbase = 1;
			}
		}
		
		if( yOffset < 0 ) {
			ybase = -(yOffset / tilesizeY);
			if( ybase < 1 ) {
				ybase = 1;
			}
		}
		
		int maxx = map.getWidth();
		maxx = xbase + getClientWidth() / tilesizeX + 1;
		if( maxx > map.getWidth() ) {
			maxx = map.getWidth();
		}
		
		if( xbase > maxx ) {
			xbase = maxx - getClientWidth() / tilesizeX + 1;
		}
		
		int maxy = map.getHeight();
		maxy = ybase + getClientHeight() / tilesizeY + 1;
		
		if( maxy > map.getHeight() ) {
			maxy = map.getHeight();
		}
		
		if( ybase > maxy ) {
			ybase = maxy - getClientHeight() / tilesizeY + 1;
		}
		
		/*int rect_xstart = 0;
		int rect_xend = getClientWidth();
		int rect_ystart = 0;
		int rect_yend = getClientHeight();
		int newx = 0;
		int newy = 0;
		
		// Verschiebung nach links = +; rechts = -
		if( oldXOffset - xOffset < 0 ) {
			rect_xend = rect_xend - oldXOffset + xOffset;
			newx = xOffset - oldXOffset;
		}
		else if( oldXOffset - xOffset > 0 ) {
			rect_xstart = xOffset - oldXOffset;
		}
		else {
			rect_xend = 0;
		}
		
		// Verschiebung nach oben = +; unten = -
		if( oldYOffset - yOffset < 0 ) {
			rect_yend = rect_yend - oldYOffset + yOffset;
			newy = yOffset - oldYOffset;
		}
		else if( oldXOffset - xOffset > 0 ) {
			rect_ystart = yOffset - oldYOffset;
		}
		else {
			rect_yend = 0;
		}
		
		if( (rect_xend > 0) && (rect_yend > 0) ) { 
			g.copyArea(rect_xstart,rect_ystart,rect_xend-rect_xstart,rect_yend-rect_ystart,newx-rect_xstart,newy-rect_ystart);
			
			oldXOffset = xOffset;
			oldYOffset = yOffset;
		}*/
		
		String currentImage = "";
		int count = 1;
		
		for(int x=xbase; x <= maxx; x++ ) {
			int y = 0;
			String myimgfile = "";
			
			for(y=ybase; y <= maxy; y++ ) {
				/*if( (xOffset-(x-1)*tilesizeX >= newx) && (xOffset+x*tilesizeX <= newx+rect_xend-rect_xstart) &&
					(yOffset-(y-1)*tilesizeY >= newy) && (yOffset+y*tilesizeY <= newy+rect_yend-rect_ystart) ) {
					continue;
				}*/
				
				myimgfile = map.getSector(x-1, y-1)+".png";
				
				if( tilesizeX != 25 ) {					
					if( !getImageCache().isLoaded(myimgfile+"#"+tilesizeX+"_"+tilesizeY) ) {
						getImageCache().createResizedImage(myimgfile, tilesizeX, tilesizeY);
					}
					myimgfile += "#"+tilesizeX+"_"+tilesizeY;
				}
				
				if( currentImage.equals("") ) {
					currentImage = myimgfile;
				}
				else if( !currentImage.equals(myimgfile) ) {
					if( (count > 1) && getImageCache().isLoaded(currentImage) && !getImageCache().isLoaded(currentImage+"$"+count) ) {
						BufferedImage img = getImageCache().createImg(tilesizeX,tilesizeY*count,Transparency.OPAQUE);
						Graphics2D tmpg = img.createGraphics();
						for( int i=0; i < count; i++ ) {
							tmpg.drawImage( getImageCache().getImage( currentImage, false ),
									0, tilesizeY*i, null);
						}
						
						tmpg.dispose();
						
						getImageCache().cacheImage(currentImage+"$"+count,img);
					}
					
					if( (count > 1) && getImageCache().isLoaded(currentImage) ) {
						g.drawImage( getImageCache().getImage( currentImage+"$"+count, false ),
								xOffset+(x-1)*tilesizeX, yOffset+(y-1-count)*tilesizeY, 
								null);
					}
					else {
						g.drawImage( getImageCache().getImage( currentImage, false ),
								xOffset+(x-1)*tilesizeX, yOffset+(y-2)*tilesizeY, 
								null);
					}
					
					currentImage = myimgfile;
					count = 1;
				}
				else {
					count++;
				}
			}
			
			if( (count > 1) && getImageCache().isLoaded(currentImage) && !getImageCache().isLoaded(currentImage+"$"+count) ) {
				BufferedImage img = getImageCache().createImg(tilesizeX,tilesizeY*count,Transparency.OPAQUE);
				Graphics2D tmpg = img.createGraphics();
				for( int i=0; i < count; i++ ) {
					tmpg.drawImage( getImageCache().getImage( currentImage, false ),
							0, tilesizeY*i, null);
				}
				
				tmpg.dispose();
				
				getImageCache().cacheImage(currentImage+"$"+count,img);
			}
			
			if( (count > 1) && getImageCache().isLoaded(currentImage) ) {
				g.drawImage( getImageCache().getImage( currentImage+"$"+count, false ),
						xOffset+(x-1)*tilesizeX, yOffset+(y-1-count)*tilesizeY, 
						null);
			}
			else {
				g.drawImage( getImageCache().getImage( currentImage, false ),
						xOffset+(x-1)*tilesizeX, yOffset+(y-2)*tilesizeY, 
						null);
			}
			
			currentImage = "";
			count = 1;
		}
	}
	
	@Override
	public void paint(Graphics2D g) {
		super.paint(g);
		
		if( map.isMapLoaded() ) {
			drawMap(g);
		}
		else {
			g.setColor(Color.black);
			g.fillRect(0,0,getWidth(),getHeight());
			g.setColor(new Color(0xc7c7c7) );
			
			String loadingString = "Lade...";
			if( map.getMapLoadingStatus() == 0 ) {
				loadingString = "[Warte auf Server]";
			}
			else if( map.getMapLoadingStatus() <= 100 ) {
				loadingString += map.getMapLoadingStatus()+"%";
			}
			else if( map.getMapLoadingStatus() == 101 ) {
				loadingString += "weitere Daten";
			}
			
			g.drawString(loadingString,50,50);
		}
	}
}
