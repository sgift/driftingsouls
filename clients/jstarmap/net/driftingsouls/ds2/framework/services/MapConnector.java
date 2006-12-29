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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.Vector;

import net.driftingsouls.ds2.framework.JImageCache;
import net.driftingsouls.ds2.framework.JWindow;

import org.w3c.dom.Document;

// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
// TODO: rewrite!!!
/**
 * MapConnector ist für die gesamte Serverkommunikation zuständig.
 * Dementsprechend sieht das ganze auch leider aus.....
 * (Nichts anfassen, von dem ihr nicht wisst, was es tut)
 * 
 * @author Christopher Jung
 */
public class MapConnector implements ServerConnectable {
	public static final String SERVICE = MapConnector.class.getName();
	
	private int map[][]; 
	private String mapDescription[][];
	private String mapLargeObjects[][];
	private Stack mapLargeObjectsPrecache;
	private int nSystem;
	private int mapWidth;
	private int mapHeight;
	private int currentUser;
	
	private int mapLoadingStatus;	// 0-100 -> Lade Kartendaten ; 101 -> Lade weitere Daten; 102 -> fertig
	
	private boolean bMapLoaded;
	
	private Vector notifyWindowList;
	
	private static final int PROTOCOL = 8;
	private static final int PROTOCOL_MINOR = 0;
	private static final int ADDDATA_TEXT = 1;
	private static final int ADDDATA_LARGE_OBJECT = 2;
	
	public static final int SECTOR_OWN_SHIPS = 1;
	public static final int SECTOR_ALLY_SHIPS = 2;
	public static final int SECTOR_ENEMY_SHIPS = 3;
	
	public MapConnector() {
		bMapLoaded = false;
		nSystem = 0;
		mapLoadingStatus = 0;
		currentUser = 0;
		
		notifyWindowList = new Vector();
		
		mapLargeObjectsPrecache = new Stack();
	}
	
	/**
	 * Fuegt ein Fenster der Benachrichtungsliste zu. Diese Fenster werden benachrichtigt, wenn die
	 * Karte geladen wurde.
	 * 
	 * @param wnd Das Fenster
	 */
	public void notifyOnChange( JWindow wnd ) {
		notifyWindowList.add(wnd);
	}
	
	/**
	 * Ueberprueft ob sich zwei Objekte schneiden
	 * @param x1 x-Koordinate Objekt 1
	 * @param y1 y-Koordinate Objekt 1
	 * @param size1 Groesse Objekt 1 (Radius)
	 * @param x2 x-Koordinate Objekt 2
	 * @param y2 y-Koordinate Objekt 2
	 * @param size2 Groesse Objekt 2 (Radius)
	 * 
	 * @return true, falls sich die Objekte schneiden 
	 */
	private boolean sameSector( int x1, int y1, int size1, int x2, int y2, int size2 ) {
		boolean samesector = true;

		if( Math.floor(Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2))) > size1+size2 ) {
			samesector = false;	
		}
		
		return samesector;
	}
	
	/**
	 * Gibt an, ob die Karte komplett geladen wurde
	 * 
	 * @return true, falls die Karte geladen wurde
	 */
	public boolean isMapLoaded() {
		return bMapLoaded;
	}
	
	/**
	 * Liefert den Status des Ladevorgangs zurueck
	 * @return Status
	 */
	public int getMapLoadingStatus() {
		return mapLoadingStatus;
	}
	
	/**
	 * Laedt einen kodierten Wert aus dem Singabestrom
	 * @param currentInput
	 * @param in Der bisher geladene Teil od. -2 falls noch nichts geladen wurde 
	 * @return der dekodierte Wert
	 * 
	 * @throws Exception
	 */
	private int readValue( int currentInput, BufferedReader in ) throws Exception {
		int tmpInput = 0;
		
		if( currentInput == -2 ) {
			currentInput = in.read();
		}
		
		if( currentInput == 255 ) {			
			while( currentInput == 255 ) {
				tmpInput += currentInput;
				currentInput = in.read();
				if( currentInput == -1 ) {
					throw new Exception("ERROR: Input-Stream vorzeitig beendet beim Versuch einen 255er-Wert zu laden");
				}
			}
		}
		
		tmpInput += currentInput;
		
		return tmpInput;
	}
	
	/**
	 * Laedt das angegebene System. Siehe {@link #loadMap(int, int)}
	 * 
	 * @param system System
	 * 
	 * @throws Exception
	 */
	public void loadMap( int system ) throws Exception {
		loadMap( system, 0 );
	}
	
	/**
	 * Laedt das angegebene System unter Verwendung des angegebenen Benutzers
	 * 
	 * @param system System
	 * @param user Benutzer (0, falls der zur Session gehoerende Benutzer verwendet werden soll)
	 * 
	 * @throws Exception
	 */
	public void loadMap( int system, int user ) throws Exception {
		System.out.print("Lade Karte..");
		
		bMapLoaded = false;
		mapLoadingStatus = 0;
		
		nSystem = system;
		
		String tmpurl = ServerConnector.getInstance().getServerURL() + "php/main.php?module=mapdata&sess="+ServerConnector.getInstance().getSession()+"&sys=" + system;
		if( user != 0 ) {
			tmpurl += "&forceuser="+user;
		}
		
		// Verbindung erstellen....
		URL url = new URL(tmpurl);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection) connection;

		// Verbindung oeffnen und HTTP-Anfrage senden....
		httpConn.setRequestProperty( "Content-Length", "0" );
		httpConn.setRequestProperty( "User-Agent", "DS2.JStarmap" );
		httpConn.setRequestProperty("Content-Type","text/html; charset=ISO-8859-1");
		httpConn.setDoInput(true);

		// ....und nun lesen wir mal ein wenig :)

		InputStreamReader isr = new InputStreamReader(httpConn.getInputStream(),"ISO-8859-1");
		BufferedReader in = new BufferedReader(isr);

		int x,y;
		x = 0;
		y = 0;
		
		int count = 0;
		int coord = 1;
		int zerocount = 0;
		int input = 0;
		
		// Die Protokollversion lesen
		int protocolVersion = in.read();
		if( protocolVersion == -1 ) {
			throw new Exception("FATAL ERROR: Kann Protokoll-Version nicht lesen");
		}
		
		if( protocolVersion != PROTOCOL ) {
			throw new Exception("ERROR: Dieses Programm unterstuetzt nur die Protokoll-Version "+PROTOCOL+". Gefunden wurde jedoch Version "+protocolVersion+"\nURL: "+tmpurl);
		}
		
		int protocolMinorVersion = in.read();
		if( protocolMinorVersion == -1 ) {
			throw new Exception("FATAL ERROR: Kann Protokoll-Minor-Version nicht lesen");
		}
		
		if( protocolMinorVersion < PROTOCOL_MINOR ) {
			throw new Exception("ERROR: Dieses Programm unterstuetzt nur die Protokoll-Version "+PROTOCOL+". Gefunden wurde jedoch Version "+protocolVersion);
		}
		
		int neguservalue = in.read();
		if( neguservalue == -1 ) {
			throw new Exception("FATAL ERROR: Kann Teile der User-ID nicht lesen");
		}
		
		currentUser = this.readValue(-2, in);
		
		if( neguservalue != 0 ) {
			currentUser = -currentUser;
		}
		
		System.out.println("Got User: "+currentUser);
		
		// Laenge und breite Lesen
		mapWidth = this.readValue(-2, in);
		
		mapHeight = this.readValue(-2, in);
		
		if( (mapWidth < 0) || (mapHeight < 0) ) {
			throw new Exception("FATAL ERROR: Kann Kartengroesse nicht lesen");
		}
		System.out.println("Lade Karte mit Groesse "+mapWidth+"x"+mapHeight);
		
		// Das System lesen
		nSystem = this.readValue(-2, in);
		if( nSystem < 0 ) {
			throw new Exception("ERROR: Kann das Sternensystem nicht identifizieren");
		}
		
		// Nun die eigendlichen Kartendaten lesen
		map = new int[mapWidth][mapHeight];
		mapDescription = new String[mapWidth][mapHeight];
		mapLargeObjects = new String[mapWidth][mapHeight];
		
		// Karteninformationen laden
		input = in.read();
		while( count < mapWidth*mapHeight ) {
			coord = input;
					
			// Haben wir es mit komprimierten leeren Feldern zu tun?
			if( coord == 0 ) {					
				zerocount = this.readValue(-2, in);
				
				for( int j = 0; j < zerocount; j++ ) {
					map[x][y] = 0;
					x++;
					count++;
					if( (count % (mapWidth*mapHeight/5)) == 0 ) {
						System.out.print((count/(mapWidth*mapHeight/5)*20)+"%..");
						mapLoadingStatus = count/(mapWidth*mapHeight/5)*20;
					}
					if( x > mapWidth-1 ) {
						y++;
						x=0;
					}
					if( y > mapHeight-1 ) {
						break;
					}
				}
			}
			else {	
				coord = this.readValue( coord, in );
							
				map[x][y] = coord;
      			
				if( (count % (mapWidth*mapHeight/5)) == 0 ) {
					System.out.print((count/(mapWidth*mapHeight/5)*20)+"%..");
					mapLoadingStatus = count/(mapWidth*mapHeight/5)*20;
				}
      			
				x++;
				count++;					
			}
			
			if( x > mapWidth-1 ) {
				y++;
				x=0;
			}
			
			if( y > mapHeight-1 ) {
				break;
			}
			
			input = in.read();
		}
		System.out.println("Karte geladen");
		
		String text = "";
		int textLength = 0;
		int tmpChar = 0;
		int nSize = 0;
		System.out.print("Lade weitere Daten...");
		
		mapLoadingStatus = 101;
		
		// Weitere Daten wie Text oder große Objekte laden
		input = in.read();
		while( input != -1 ) {
			if( input == ADDDATA_TEXT ) {			
				x = this.readValue( -2, in );
				y = this.readValue( -2, in );
			
				text = "";
			
				textLength = 0;
				textLength = this.readValue( -2, in );
						
				for( int i=0; i < textLength; i++ ) {
					tmpChar = in.read();
					if( tmpChar == -1 ) {
						throw new Exception("ERROR: Input-Stream vorzeitig beendet beim Versuch Texte zu laden");
					}
				
					text += (char)tmpChar;
				}
				
				mapDescription[x][y] = text;
			}
			else if( input == ADDDATA_LARGE_OBJECT ) {
				x = this.readValue( -2, in );
				y = this.readValue( -2, in );
				nSize = this.readValue( -2, in );
				
				textLength = 0;
				textLength = this.readValue( -2, in );
				
				text = "";
				
				for( int i=0; i < textLength; i++ ) {
					tmpChar = in.read();
					if( tmpChar == -1 ) {
						throw new Exception("ERROR: Input-Stream vorzeitig beendet beim Versuch Texte zu laden");
					}
				
					text += (char)tmpChar;
				}
				
				mapLargeObjectsPrecache.push(text);
				
				int index = 0;
				
				for( int objheight = y-nSize; objheight <= y+nSize; objheight++ ) {
					for( int objwidth = x-nSize; objwidth <= x+nSize; objwidth++ ) {
						if( Math.floor( Math.sqrt((x-objwidth)*(x-objwidth)+(y-objheight)*(y-objheight))) > nSize ) {
							continue;
						}
						if( (objwidth >= 0) && (objwidth < mapWidth) && (objheight >= 0) && (objheight < mapHeight) ) {
							mapLargeObjects[objwidth][objheight] = text+"/"+text+index;
						}
						index++;
					}
				}
			}
			
			input = in.read();
		}
		
		mapLoadingStatus = 102;
		
		System.out.println("fertig");
		
		in.close();
		bMapLoaded = true;
		
		// Alle eingetragenen Fenster informieren, dass wir jetzt fertig sind
		for( int i=0; i < notifyWindowList.size(); i++ ) {
			JWindow wnd = (JWindow)notifyWindowList.get(i);
			wnd.handleEvent(wnd.getHandle(), "map_changed" );
		}
	}
	
	/**
	 * Liefert den aktuellen Benutzer
	 * @return Benutzer
	 */
	public int getCurrentUser() {
		return currentUser;
	}
	
	/**
	 * Liefert das Bild zu einem Sektor ohne Dateierweiterung
	 * 
	 * @param x x-Koordinate (von 0 aus)
	 * @param y y-Koordinate (von 0 aus)
	 * @return Bildname
	 */
	public String getSector( int x, int y ) {
		if( !bMapLoaded ) {
			return "";
		}
		
		if( x > mapWidth-1 ) {
			System.out.println("WARNUNG: x > "+(mapWidth-1)+" (ist "+x+")");
			x = mapWidth-1;
		}
		
		if( y > mapHeight-1 ) {
			System.out.println("WARNUNG: y > "+(mapHeight-1)+" (ist "+y+")");
			y = mapHeight-1;
		}
		
		String myimgfile = "";
		int sector = map[x][y];
		
		int base = 0;
		if( sector > 0 ) {
			base = sector / 8;
		}

		switch( base ) {
		case 0: 
			myimgfile = "starmap/space/space";
			break;
		case 1:
			myimgfile = "starmap/kolonie1_lrs/kolonie1_lrs";
			break;
		case 2:
			myimgfile = "starmap/asti_own/asti_own";
			break;
		case 3:
			myimgfile = "starmap/asti_ally/asti_ally";
			break;
		case 4:
			myimgfile = "starmap/asti_enemy/asti_enemy";
			break;
		case 5: 
			myimgfile = "starmap/fog1/fog1";
			break;
		case 6: 
			myimgfile = "starmap/fog0/fog0";
			break;
		case 7: 
			myimgfile = "starmap/fog2/fog2";
			break;
		case 8: 
			myimgfile = "starmap/fog3/fog3";
			break;
		case 9: 
			myimgfile = "starmap/fog4/fog4";
			break;
		case 10: 
			myimgfile = "starmap/fog5/fog5";
			break;
		case 11: 
			myimgfile = "starmap/fog6/fog6";
			break;
		case 12: 
			myimgfile = "starmap/jumpnode/jumpnode";
			break;
		default:
			System.out.println("UNKNOWN SECTOR TYPE "+base);
			myimgfile = "starmap/space/space";
			break;
		}
		
		if( (base != 1) && (mapLargeObjects[x][y] != null )  ) {
			myimgfile = "starmap/"+mapLargeObjects[x][y];
		}
		
		if( base*8 != sector ) {
			if( (sector & 4) > 0 ) {
				myimgfile += "_fo";
			}
			if( (sector & 2) > 0 ) {
				myimgfile += "_fa";
			}
			if( (sector & 1) > 0 ) {
				myimgfile += "_fe";
			}
		}
		
		return myimgfile;
	}
	
	/**
	 * Liefert das aktuelle System
	 * 
	 * @return System
	 */
	public int getSystem() {
		return nSystem;
	}
	
	/**
	 * Liefert die Hoehe des Systems
	 * @return Hoehe
	 */
	public int getWidth() {
		return mapWidth;
	}
	
	/**
	 * Liefert die Breite des Systems
	 * @return Breite
	 */
	public int getHeight() {
		return mapHeight;
	}
	
	/**
	 * Liefert die Beschreibung zu einem Sektor (Schiffe, Basen usw)
	 * @param x x-Koordinate (von 0 aus)
	 * @param y y-Koordinate (von 0 aus)
	 * 
	 * @return Sektorbeschreibung
	 */
	public String getSectorDescription( int x, int y ) {
		String text;
		String tmpText;
		
		text = "Koordinaten: "+x+"/"+y;

		if( mapDescription[x-1][y-1] != null ) {
			tmpText = mapDescription[x-1][y-1];
			
			text += "\n"+tmpText;
		}
		
		return text;
	}
	
	/**
	 * Ueberprueft ob ein Sektor eine bestimmte Eigenschaft besitzt
	 * 
	 * @param x x-Koordinate (von 1 aus)
	 * @param y y-Koordinate (von 1 aus)
	 * @param property Eigenschaft
	 * 
	 * @return true, wenn der Sektor die Eigenschaft besitzt
	 */
	public boolean sectorHasProperty( int x, int y, int property ) {
		if( !bMapLoaded ) {
			return false;
		}
		
		x--;
		y--;
		
		if( x > mapWidth-1 ) {
			System.out.println("WARNUNG: x > "+(mapWidth-1)+" (ist "+x+")");
			x = mapWidth-1;
		}
		
		if( y > mapHeight-1 ) {
			System.out.println("WARNUNG: y > "+(mapHeight-1)+" (ist "+y+")");
			y = mapHeight-1;
		}
		
		if( property == SECTOR_OWN_SHIPS ) {
			if( (map[x][y] & 4) != 0 ) {
				return true;
			}
			else {
				return false;
			}
		}
		else if( property == SECTOR_ALLY_SHIPS ) {
			if( (map[x][y] & 2) != 0 ) {
				return true;
			}
			else {
				return false;
			}
		}
		else if( property == SECTOR_ENEMY_SHIPS ) {
			if( (map[x][y] & 1) != 0 ) {
				return true;
			}
			else {
				return false;
			}
		}
		
		return false;
	}
	
	/**
	 * Laedt alle fuer die Anzeige der Karte benoetigten Bilder. Zusaetzlich wird bei großen Objekten versucht
	 * eine einzige Version des Bilds zu Laden und die Flottenmarkierungen on-the-fly zu erstellen anstatt fuer
	 * jeden Sektor ein einzelnes Bild zu laden.
	 * Diese Methode sollte aufgerufen werden, nachdem die Karte geladen wurde.
	 * 
	 * @param imagecache Der zu verwendende ImageCache
	 */
	public void precacheImages(JImageCache imagecache ) {
		while( !mapLargeObjectsPrecache.empty() ) {
			String text = (String)mapLargeObjectsPrecache.pop();
			
			BufferedImage masterimg = imagecache.loadImage("starmap/"+text+"_precache.png");
			
			int width = masterimg.getWidth() / 25;
			int height = masterimg.getHeight() / 25;
			
			int size = (width - 1)/2;
			int index = 0;
			int cx = size + 1;
			int cy = size + 1;

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					if( !sameSector( cx, cy, size, x+1, y+1, 0 ) ) {
						continue;
					}
					
					//BufferedImage imgpart = gc.createCompatibleImage(25,25,masterimg.getColorModel().getTransparency());
					
					BufferedImage imgpart = new BufferedImage( 25, 25, BufferedImage.TYPE_INT_RGB );
					Graphics2D g = imgpart.createGraphics();
					g.drawImage(masterimg,0,0,25,25,x*25,y*25,x*25+25,y*25+25, null );
					g.dispose();
					
					imagecache.cacheImage("starmap/"+text+"/"+text+index+".png",imgpart);
					index++;
				}
			}
		}
		
		for( int i=0; i<this.mapWidth; i++ ) {
			for( int j=0; j<this.mapHeight; j++ ) {
				imagecache.getImage(this.getSector(i,j)+".png",true);
			}
		}
	}
	
	/**
	 * Liefert die XML-Beschreibung eines Sektors (enthaelt alle im Sektor befindlichen Schiffe)
	 * 
	 * @param x x-Koordinate (von 1 aus)
	 * @param y y-Koordinate (von 1 aus)
	 * @return Das XML-Dokument
	 * 
	 * @throws XMLConnectorException
	 */
	public Document fetchSectorObjectList( int x, int y ) throws XMLConnectorException {
		XMLConnector xml = (XMLConnector)ServerConnector.getInstance().getService(XMLConnector.SERVICE);
		
		return xml.fetchURL("mapdata","showSector","&sys="+nSystem+"&x="+x+"&y="+y+"&forceuser="+currentUser);
	}
}
