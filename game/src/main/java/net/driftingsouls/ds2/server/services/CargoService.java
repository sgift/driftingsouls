package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class CargoService {
    private static final Logger log = LoggerFactory.getLogger(CargoService.class);

    @PersistenceContext
    private EntityManager em;

    /**
     * Gibt die Liste der im Cargo vorhandenen Resourcen zurueck. Die Liste
     * wird dabei bereit vorformatiert.
     * @return Eine Resourcenliste
     */
    public ResourceList getResourceList(Cargo cargo) {
        ResourceList reslist = new ResourceList();

        if( !cargo.getItems().isEmpty() ) {
            for( Long[] item : cargo.getItems()  ) {
                Item itemType = Item.getItem( item[0].intValue());
                if( itemType == null )
                {
                    log.warn("Unbekanntes Item "+item[0]+" geortet");
                    continue;
                }

                String name = itemType.getName();
                String plainname = name;
                String image;
                boolean large = false;
                if( !(Boolean)cargo.getOption(Cargo.Option.LARGEIMAGES) ) {
                    image = itemType.getPicture();
                }
                else {
                    large = true;
                    image = itemType.getLargePicture();
                    if( image == null ) {
                        large = false;
                        image = itemType.getPicture();
                    }
                }
                String fcount = Common.ln(item[1]);

                if( (Boolean)cargo.getOption(Cargo.Option.SHOWMASS) && (itemType.getCargo() > 1) ) {
                    fcount += " ("+Common.ln(itemType.getCargo()*item[1])+")";
                }

                ResourceID itemId = cargo.buildItemID(item);
                if( !(Boolean)cargo.getOption(Cargo.Option.NOHTML) ) {
                    String style = "";
                    if( item[3] != 0 ) {
                        style += "text-decoration:underline;";
                    }
                    if( item[2] != 0 ) {
                        style += "font-style:italic;";
                    }

                    if( !itemType.getQuality().color().equals("") ) {
                        style += "color:"+itemType.getQuality().color()+";";
                    }

                    if( !style.equals("") ) {
                        style = "style=\""+style+"\"";
                    }

                    String tooltiptext = "<img align=\"left\" src=\""+itemType.getPicture()+"\" alt=\"\" />"+itemType.getName();
                    if( item[3] != 0 ) {
                        tooltiptext += "<br /><span class=\"verysmallfont\">Questgegenstand</span>";
                    }
                    if( item[2] != 0 ) {
                        name = "<span style=\"font-style:italic\">"+name+"</span>";
                        tooltiptext += "<br /><span class=\"verysmallfont\">Benutzungen: "+item[2]+"</span>";
                    }

                    name = "<a "+style+" class=\"tooltip "+cargo.getOption(Cargo.Option.LINKCLASS)+"\" href=\"./ds?module=iteminfo&amp;itemlist="+itemId+"\">"+
                        name+
                        " <span class=\"ttcontent ttitem\" ds-item-id=\""+itemId+"\">"+tooltiptext+"</span></a>";
                    fcount = "<a "+style+" class=\"tooltip "+cargo.getOption(Cargo.Option.LINKCLASS)+"\" href=\"./ds?module=iteminfo&amp;itemlist="+itemId+"\">"+
                        fcount+
                        " <span class=\"ttcontent ttitem\" ds-item-id=\""+itemId+"\">"+tooltiptext+"</span></a>";
                }
                else {
                    if( item[3] != 0 ) {
                        name += " [quest: "+item[3]+"]";
                    }
                    if( item[2] != 0 ) {
                        name += " [limit: "+item[2]+"]";
                    }
                }

                ResourceEntry res = new ResourceEntry(itemId, name, plainname,
                    image, fcount, item[1] );
                res.setLargeImages(large);

                reslist.addEntry(res);
            }
        }

        return reslist;
    }

    /**
     * Gibt die Gesamtmasse aller Waren und Items im <code>Cargo</code>-Objekt zurueck.
     * @return Die Gesamtmasse
     */
    public long getMass(Cargo cargo) {
        long tmp = 0;

        for (Long[] item1 : cargo.getItems())
        {
            Item item = Item.getItem( item1[0].intValue());
            if (item == null)
            {
                log.warn("Unbekanntes Item " + item1[0] + " geortet");
                continue;
            }
            tmp += item1[1] * item.getCargo();
        }

        return tmp;
    }

    /**
     * Spaltet vom Cargo ein Cargo-Objekt ab. Das neue Cargo-Objekt enthaelt
     * Resourcen in der angegebenen Masse (oder weniger, falls nicht genug im
     * Ausgangsobjekt waren). Das alte Cargoobjekt wird um diese Resourcenmenge
     * verringert.
     * @param mass Die gewuenschte Masse
     * @return Das abgespaltene Cargoobjekt.
     */
    public Cargo cutCargo(Cargo cargo, long mass ) {
        Cargo retcargo;

        if( mass >= getMass(cargo) ) {
            retcargo = cargo.clone();
            cargo.getItems().clear();

            return retcargo;
        }
        retcargo = new Cargo();

        long currentmass = 0;

        if( currentmass != mass ) {
            for( int i=0; i < cargo.getItems().size(); i++ ) {
                Long[] aitem = cargo.getItems().get(i);

                Item item = Item.getItem( aitem[0].intValue());
                if( item.getCargo()*aitem[1] + currentmass < mass ) {
                    currentmass += item.getCargo()*aitem[1];
                    retcargo.getItemArray().add(aitem);
                    cargo.getItems().remove(aitem);
                    i--;
                }
                else if( item.getCargo() > 0 ) {
                    Long[] newitem = aitem.clone();
                    newitem[1] = (mass-currentmass)/item.getCargo();
                    aitem[1] -= newitem[1];
                    currentmass += item.getCargo()*newitem[1];
                    retcargo.getItemArray().add(newitem);
                }
            }
        }

        return retcargo;
    }
}
