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

import java.awt.image.*;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.*;
import java.io.*;
import java.util.Vector;

import javax.imageio.*;

/**
 * JImageCache ist zuständig für das Erstellen/Laden/Cachen/Verändern von DS2-Grafiken.
 * Die Klasse arbeitet dabei unabhängig vom Hauptprogramm, d.h. Bilder können z.B. geladen werden
 * ohne das die Ausführung im Hauptprogramm angehalten werden muss. Der gesamte Prozess arbeitet dabei
 * mir einer Reihe von Arbeiterthreads sowie einer Warteschlange.
 * Während das zu ladende Bild in der Warteschlange ist, wird eine Platzhaltergrafik zurückgegeben um den Prozess
 * möglichst unsichtbar für das Hauptprogramm zu machen.
 * Ähnlich wie das Laden funktioniert auch das Vergrößern/Verkleinern von Grafiken.
 * Alle geladenden/vergrößerten/verkleinerten Bilder werden zudem im Cache zwischengespeichert.
 * Somit müssen andere Klassen sich nur noch den Namen merken, unter dem die Grafik abgelegt ist (was sie auch auf
 * jeden Fall nur tun sollten).
 * 
 * @author bKtHeG (Christopher Jung)
 */
public class JImageCache implements ImageObserver {
	private HashMap images;
	private HashMap imageNotifierList;
	private HashMap imageNotifierData;
	private Vector resizeTodo;
	private String datapath;
	private final GraphicsConfiguration gc;
	private BufferedImage unknownImage;
	private int loadercount;
	private int resizerCount;
	private Vector loadTodo;
	private IWindowManager wm;
	
	/**
	 * Erzeugt ein neues JImageCache-Objekt
	 * 
	 * @param wm		Der Fenstermanager
	 * @param datapath	Der DS2-Datenpfad
	 */
	public JImageCache( IWindowManager wm, String datapath ) {
		images = new HashMap();
		imageNotifierList = new HashMap();
		imageNotifierData = new HashMap();
		loadTodo = new Vector();
		resizeTodo = new Vector();
		resizerCount = 0;
		this.wm = wm;
		loadercount = 0;
		
		this.datapath = datapath;
		
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice ().getDefaultConfiguration();
		this.gc = gc;
		
		unknownImage = loadImage("interface/jstarmap/starmap_unknown.png");
		images.put("interface/jstarmap/starmap_unknown.png",unknownImage);
	}
	
	/**
	 * Laedt eine Grafik vom Server. Es wird jedoch dringend die Verwendung von {@link #getImage(String,boolean)} empfohlen,
	 * da <code>loadImage</code> solange mit der Rückkehr ins Hauptprogramm wartet, bis die Grafik geladen ist.
	 * 
	 * @param myfile	Der Pfad zur Grafik relativ zum DS2-Datenpfad
	 * 
	 * @return Die Grafik in Form eines {@link BufferedImage}
	 * 
	 * @see #getImage(String,boolean)
	 * @see #getImage(String,boolean,IImageStatusNotifier,Object)
	 */
	public BufferedImage loadImage(String myfile) {
		try {			
			URL url = new URL(datapath+myfile);
			if( url == null ) {
				System.out.println("Kann Datei " + myfile +" nicht finden");
				System.exit(0);
				return null;
			}
				
			ImageIO.setUseCache(false);
			BufferedImage srcImage = ImageIO.read(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(datapath+myfile)));
						
			BufferedImage dstImage = gc.createCompatibleImage(srcImage.getWidth(),srcImage.getHeight(),srcImage.getColorModel().getTransparency());
			Graphics2D g = dstImage.createGraphics();
			g.setComposite(AlphaComposite.Src);
			
			g.drawImage(srcImage,0,0,null);
			g.dispose();
			
			return dstImage; 
		} 
		catch( IOException e ) {
			System.out.println("Kann Datei " + myfile +" nicht laden");
			System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
			
			return null;
		}
		catch( Exception e ) {
			System.out.println("Kann Datei " + myfile +" nicht laden");
			System.out.println("Fehlerbeschreibung: "+e.getClass().getName()+" "+e.getMessage());
			
			return null;
		}
	}
	
	/**
	 * Laedt eine Grafik vom Server ohne das Hauptprogramm anzuhalten und cached diese anschließend.
	 * Der Ladevorgang erfolgt dabei in Workerthreads. Solange bis die Grafik geladen ist,
	 * fungiert eine Placeholder-Grafik als Ersatz.
	 * Sollte die Grafik bereits geladen sein, wird diese sofort zurückgegeben.
	 * 
	 * @param myfile		Der Pfad zur Datei relativ zum DS2-Datenpfad
	 * @param forceLoad		Gibt an, ob die Grafik eine hohe Prioritaet genießt beim laden
	 * @return Die geladene Grafik oder ein Platzhalter
	 * 
	 * @see #getImage(String,boolean,IImageStatusNotifier,Object)
	 */
	public BufferedImage getImage(final String myfile, boolean forceLoad ) {
		BufferedImage myimg = (BufferedImage)images.get(myfile);
	
		if( (myimg == null) && (myfile.indexOf('#') == -1) ) {
			int status = loadTodo.indexOf(myfile); 
	
			if( forceLoad && (status == -1) && (loadercount >= 10) ) {
				loadTodo.add(0,myfile);			
			}
		
			if( (status == -1) && (loadercount < 10) ) {
				loadTodo.addElement(myfile);

				Thread loaderthread = new Thread( 
					new Runnable() {
						public void run() {
							// Bild laden
							try {
								loadercount++;
								while( !loadTodo.isEmpty() ) {
									String nextfile;
									synchronized(loadTodo) {
										nextfile = (String)loadTodo.firstElement();
										loadTodo.remove(0);
									}
									BufferedImage aimg = loadImage(nextfile);
									
									images.put(nextfile,aimg);
									
									if( imageNotifierList.get(nextfile) != null ) {
										Object data = imageNotifierData.get(nextfile);
										((IImageStatusNotifier)imageNotifierList.get(nextfile)).onImageLoaded(nextfile,data);
										
										imageNotifierData.remove(nextfile);
										imageNotifierList.remove(nextfile);
									}
									else {
										wm.requestRedraw();
									}
								}
								loadercount--;
							}
							catch( Exception e) {}
						}
					}
				);
				loaderthread.setPriority(Thread.MIN_PRIORITY);
				loaderthread.start();
				myimg = this.unknownImage;
			}
			else {
				myimg = this.unknownImage;
			}
		}
		else if( (myimg == null) && (myfile.indexOf('#') > -1) ) {
			String str = "interface/jstarmap/starmap_unknown.png" + myfile.substring(myfile.indexOf('#'));

			myimg = (BufferedImage)images.get(str);
			if( myimg == null ) {
				myimg = this.unknownImage;
			}
		}
		
		return myimg;
	}
	
	/**
	 * Laedt eine Grafik vom Server ohne das Hauptprogramm anzuhalten und cached diese anschließend.
	 * Dabei arbeitet die Methode analog zu {@link #getImage(String,boolean)} mit dem Unterschied, dass nach dem
	 * Laden der Grafik das übergebene ImageStatusNotifier-Objekt benachrichtigt wird.
	 * 
	 * @param myfile		Der Dateiname der zu ladenden Grafik relativ zum DS2-Datenpfad
	 * @param forceLoad		Gibt an, ob die Grafik beim laden eine hohe Prioritaet genießt
	 * @param isn			Das nach dem Laden zu benachrichtigende ImageStatusNotifier-Objekt
	 * @param customData	Zusätzliche Daten, die das ImageStatusNotifier-Objekt beim benachrichtigen erhält
	 * 
	 * @return	Die geladene Grafik oder eine Placeholder-Grafik
	 */
	public BufferedImage getImage(final String myfile, boolean forceLoad, IImageStatusNotifier isn, Object customData ) {
		imageNotifierList.put(myfile,isn);
		imageNotifierData.put(myfile,customData);
		
		return getImage(myfile, forceLoad);
	}
	
	/**
	 * Speichert eine Grafik unter dem angegebenen Pfad im Cache.
	 * 
	 * @param myfile	Der Pfad der Grafik unterdem diese gespeichert werden soll
	 * @param image		Die Grafik selbst
	 */
	public void cacheImage( String myfile, BufferedImage image ) {
		images.put( myfile, image );
	}
	
	public boolean imageUpdate(Image img, int infoflags,int x, int y, int w, int h) {
		   return (infoflags & (ALLBITS|ABORT)) == 0;
	}
	
	/**
	 * Ueberprueft, ob eine Grafik bereits geladen ist
	 * 
	 * @param myfile	Der Pfad der Grafik
	 * @return <code>true</code> fuer geladen. Andernfalls <code>false</code>
	 */
	public boolean isLoaded( String myfile ) {
		BufferedImage myimg = (BufferedImage)images.get(myfile);
		if( myimg == null ) {
			return false;
		}
		else {
			return true;
		}
	}
	
	/**
	 * Erzeugt eine neue leere Grafik
	 * @param width		Die Breite der zu erzeugenden Grafik
	 * @param height	Die Hoehe der zu erzeugenden Grafik
	 * @param bitmask	Die Bitflags, welche zur Erzeugung benutzt werden sollen ({@link GraphicsConfiguration#createCompatibleImage(int,int,int)})
	 * @return Die neu erzeugte Grafik
	 */
	public BufferedImage createImg( int width, int height, int bitmask ) {
		return gc.createCompatibleImage(width,height, bitmask );
	}
	
	/**
	 * Erzeugt eine neue leere Grafik. Dies kann entweder ein {@link VolatileImage} oder (falls das System 
	 * diese Klasse nicht unterstützt) ein {@link BufferedImage} sein.
	 * @param width		Die Breite der zu erzeugenden Grafik
	 * @param height	Die Hoehe der zu erzeugenden Grafik
	 * @param bitmask	Die Bitflags, welche zur Erzeugung benutzt werden sollen ({@link GraphicsConfiguration#createCompatibleImage(int,int,int)})
	 * @return Die neu erzeugte Grafik
	 */
	public Image createHWImage( int width, int height, int bitmask ) {
		try {
			Class.forName("java.awt.image.VolatileImage");
			return gc.createCompatibleVolatileImage(width, height, bitmask);
		}
		catch( ClassNotFoundException cnfe ) {
			return createImg(width, height, bitmask);
		}
	}
	
	/**
	 * Erzeugt aus einer bereits geladenen Grafik eine neue mit anderen Groessenwerten ohne das Hauptprogramm zu blockieren.
	 * Die neue Grafik hat dabei den Pfadnamen der alten Grafik mit dem zusatz "#$width$height" (ohne " und $).
	 * Der Resize-Vorgang selbst wird in Threads abgearbeitet. Sobald der Vorgang beendet ist, kann das Bild via {@link #getImage(String,boolean)} geladen werden
	 * 
	 * @param image		Das zu vergroessernde/verkleinernde Bild
	 * @param width		Die neue Breite
	 * @param height	Die neue Hoehe
	 */
	public void createResizedImage( final String image, final int width, final int height ) {
		if( images.get(image+"#"+width+"_"+height) != null ) {
			return;
		}

		Vector todoentry = new Vector();
		todoentry.addElement(image);
		todoentry.addElement(new Integer(width));
		todoentry.addElement(new Integer(height));
		
		int status = resizeTodo.indexOf(todoentry);
		
		if( (image != "interface/jstarmap/starmap_unknown.png") && (images.get("interface/jstarmap/starmap_unknown.png"+"#"+width+"_"+height) == null) ) {
			createResizedImage("interface/jstarmap/starmap_unknown.png", width, height );
		}
		
		
		if( (status == -1) && (resizerCount < 1) ) {
			resizerCount++;
			
			resizeTodo.addElement(todoentry);
			
			Thread loaderthread = new Thread( 
					new Runnable() {
						public void run() {
							// Bild laden
							try {
								while( !resizeTodo.isEmpty() ) {
									Vector nextfile;
									synchronized(resizeTodo) {
										nextfile = (Vector)resizeTodo.firstElement();
										resizeTodo.remove(0);
									}
									
									String filename = (String)nextfile.get(0);
									Integer newwidth = (Integer)nextfile.get(1);
									Integer newheight = (Integer)nextfile.get(2);
															
									if( images.get(filename) == null ) {
										getImage(filename, true);
										
										int count = 0;
										
										while( images.get(filename) == null ) {
											try {
												if( count > 20 ) {
													break;
												}
												
												Thread.sleep(50);
												
												count++;
											}
											catch( InterruptedException e ) {
												System.out.println("Ups...ne InterruptedException in JImageCache. Wo kommt die den her?");
											}
										}
										
										if( count > 20 ) {
											continue;
										}
									}
									
									BufferedImage currentImage = (BufferedImage)images.get(filename);
											
									BufferedImage sizedImage = createImg( newwidth.intValue(), newheight.intValue(), currentImage.getColorModel().getTransparency() );
									Graphics2D g = sizedImage.createGraphics();
									g.drawImage( currentImage, 0, 0, width, height, null );
									g.dispose();
									
									images.put( filename+"#"+newwidth+"_"+newheight, sizedImage );
								}
								resizerCount--;
							}
							catch( Exception e) {}
						}
					}
				);
				loaderthread.setPriority(Thread.MIN_PRIORITY);
				loaderthread.start();
		}
		else if( status == -1 ) {
			resizeTodo.addElement(todoentry);
		}
	}
	
	/**
	 *  Entfernt alle Bilder mit dem angegebenen Namen aus dem Cache.
	 *  Dabei werden jedoch auch alle selbstgenerierten Varianten entfernt.
	 *  Vergroesserte/Verkleinerte Versionen werden ignoriert.
	 * 
	 * @param filename	Der Pfad des zu entfernenden Bilds
	 * 
	 */
	
	public void dropImages( String filename ) {
		HashMap cloneMap = (HashMap)images.clone();
		
		Iterator itr = cloneMap.keySet().iterator();
		
		while( itr.hasNext() ) {
			String key = (String)itr.next();
			
			if( key.equals(filename) ) {
				images.remove(key);
			}
			else if( key.indexOf( filename+"$" ) > -1 ) {
				images.remove(key);
			}
		}
	}
}

