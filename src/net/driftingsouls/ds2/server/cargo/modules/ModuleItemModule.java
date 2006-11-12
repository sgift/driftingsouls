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
package net.driftingsouls.ds2.server.cargo.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.IEModule;
import net.driftingsouls.ds2.server.config.IEModuleSetMeta;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert ein Modul auf Item-Basis
 * @author Christopher Jung
 *
 */
public class ModuleItemModule extends Module implements Loggable {
	private int slot;
	private int itemid;
	private String[] weaponrepl;
	private boolean calculateSetEffect;
	
	protected ModuleItemModule( int slot, String data ) {
		this.slot = slot;

		this.itemid = Integer.parseInt(data);
		this.weaponrepl = new String[0];
		this.calculateSetEffect = true;
	}
	
	private static final String[] stringVals = {"nickname", "picture", "werft", "ow_werft"};
	
	private SQLResultRow parseModuleModifiers( SQLResultRow stats, SQLResultRow mods, SQLResultRow typestats ) {
		for( String key : mods.keySet()) {
			
			if( Common.inArray(key, stringVals) ) {
				stats.put( key, mods.get(key));
			}	
			else if( key.equals("weapons") ) {
				Common.stub();
				/*$wpnrpllist = $this->weaponrepl;
				reset($wpnrpllist);
				$wpnrpl = current($wpnrpllist);
				
				$weaponlist = parseWeaponList($stats['weapons']);
				$heatlist = parseWeaponList($stats['maxheat']);
				
				foreach( $mod as $aweapon => $awpnmods ) { 
					if( is_array($awpnmods) ) {
						$acount = $awpnmods[0];
						$aheat = $awpnmods[1];	
					}
					else {
						$aheat = $awpnmods;	
						$acount = 1;
					}

					if( $wpnrpl ) {		
						if( $weaponlist[$wpnrpl] ) {
							if( $weaponlist[$wpnrpl] > $acount ) {
								$heatlist[$wpnrpl] -= $acount*($heatlist[$wpnrpl]/$weaponlist[$wpnrpl]);
								
								$weaponlist[$wpnrpl] -= $acount;
								$weaponlist[$aweapon] += $acount;
								$heatlist[$aweapon] += $aheat;
							}
							else {
								unset($heatlist[$wpnrpl]);
								unset($weaponlist[$wpnrpl]);	
								
								$weaponlist[$aweapon] += $acount;
								$heatlist[$aweapon] += $aheat;
							}
						}
					}
					else {
						$weaponlist[$aweapon] += $acount;
						$heatlist[$aweapon] += $aheat;

						if( $weaponlist[$aweapon] == 0 ) {
							unset($heatlist[$aweapon]);
							unset($weaponlist[$aweapon]);	
						}
					}
					
					next($wpnrpllist);
					$wpnrpl = current($wpnrpllist);
				}	
				$stats['weapons'] = packWeaponList($weaponlist);
				$stats['maxheat'] = packWeaponList($heatlist);*/
			}
			else if( key.equals("maxheat") ) {
				Map<String,String> heatlist = Weapons.parseWeaponList(stats.getString("maxheat"));	
				
				Map<String,Integer> modHeats = (Map<String,Integer>)mods.get(key);
				for( String wpn : modHeats.keySet() ) {
					if( !heatlist.containsKey(wpn) ) {
						heatlist.put(wpn, Integer.toString(modHeats.get(key)));
					}
					else {
						heatlist.put(wpn, Integer.toString(Integer.parseInt(heatlist.get(wpn))+modHeats.get(wpn)));
						
					}
				}	
				
				stats.put("maxheat", Weapons.packWeaponList(heatlist));
			}
			else if( key.equals("flags") ) {
				String[] flags = StringUtils.split(mods.getString("flags"), ' ');
				for( int i=0; i < flags.length; i++ ) {
					String aflag = flags[i];
					if( (stats.getString("flags").length() != 0) && (stats.getString("flags").indexOf(aflag) > -1) ) {
						stats.put("flags", stats.getString("flags")+' '+aflag);	
					}
					else if( stats.getString("flags").length() == 0 ) {
						stats.put("flags", aflag);	
					}
				}
			}
			else if( key.equals("size") ) {
				if( typestats.getInt(key) > 3 ) {
					stats.put( key, stats.getInt(key) + mods.getInt(key));	
					if( stats.getInt(key) < 4 ) {
						stats.put(key, 4);	
					}
				}
				else {
					stats.put( key, stats.getInt(key) + mods.getInt(key));	
					if( stats.getInt(key) > 3 ) {
						stats.put(key, 3);	
					}
				}
			}
			else if( key.equals("heat") || key.equals("cost") || key.equals("crew") || key.equals("hull") ) {
				if( typestats.getInt(key) > 0 ) {
					stats.put( key, stats.getInt(key) + mods.getInt(key));	
					if( stats.getInt(key) < 1 ) {
						stats.put(key, 1);	
					}
				}
			}
			else if( key.equals("picture_mod") ) {
				String[] picturemods = new String[0];
				
				String dir = stats.getString("picture").substring(0,stats.getString("picture").lastIndexOf('/')+1);
				String picture = stats.getString("picture").substring(stats.getString("picture").lastIndexOf('/')+1,stats.getString("picture").length()-4);
				if( picture.indexOf('$') > -1 ) {
					String[] tmp = StringUtils.split(picture, '$');
					picture = tmp[0];
					picturemods = StringUtils.split(tmp[1], '_');
				}
				
				if( !Common.inArray(mods.getString(key), picturemods) ) {
					List<String> tmplist = new ArrayList<String>();
					tmplist.addAll(Arrays.asList(picturemods));
					tmplist.add(mods.getString(key));
					Collections.sort(tmplist);
					picturemods = tmplist.toArray(new String[tmplist.size()]);
				}
				
				stats.put("picture", dir+'/'+picture+'$'+Common.implode("_",picturemods)+".png");
			}
			else {
				stats.put( key, stats.getInt(key) + mods.getInt(key));	
				if( stats.getInt(key) < 0 ) {
					stats.put(key, 0);	
				}
			}
		}
		
		return stats;
	}
	
	@Override
	public SQLResultRow modifyStats(SQLResultRow stats, SQLResultRow typestats,
			List<Module> moduleobjlist) {
		ItemEffect itemEffect = Items.get().item(this.itemid).getEffect();
		if( itemEffect.getType() != ItemEffect.Type.MODULE ) {
			LOG.warn("WARNUNG: Ungueltiger Itemeffect in CModuleItemModule ("+this.itemid+")<br />\n");
			return stats;
		}
		IEModule effect = (IEModule)itemEffect;
		
		stats = parseModuleModifiers( stats, effect.getMods(), typestats );
		
		if( calculateSetEffect && (effect.getSetID() != 0) ) {
			// Set-Item-Count ausrechnen
			List<Integer> gotitems = new ArrayList<Integer>();
			int count = 0;
			
			for( Module moduleobj : moduleobjlist ) {
				if( !(moduleobj instanceof ModuleItemModule) ) {
					continue;
				}
				ModuleItemModule itemModule = (ModuleItemModule)moduleobj;
				
				if( ((IEModule)Items.get().item(itemModule.getItemID().getItemID()).getEffect()).getSetID() != effect.getSetID() ) {
					continue;
				}
				
				itemModule.disableSetEffect();
				
				if( gotitems.contains(itemModule.getItemID().getItemID()) ) {
					continue;
				}
				
				gotitems.add(itemModule.getItemID().getItemID());
				count++;
			}
			
			SQLResultRow[] mods = ((IEModuleSetMeta)Items.get().item(effect.getSetID()).getEffect()).getCombo(count);
			for( int i=0; i < mods.length; i++ ) {
				stats = parseModuleModifiers(stats, mods[i], typestats );
			}
		}
				
		return stats;
	}

	@Override
	public boolean isSame(int slot, int moduleid, String data) {
		if( slot != this.slot ) {
			return false;	
		}	
		else if( moduleid != Modules.MODULE_ITEMMODULE ) {
			return false;	
		}
				
		if( this.itemid != Integer.parseInt(data) ) {
			return false;	
		}
		
		return true;
	}

	@Override
	public void setSlotData(String data) {
		this.weaponrepl = StringUtils.split(data,',');
	}
	
	@Override
	public String getName() {
		return "<a class=\"forschinfo\" href=\"./main.php?module=iteminfo&amp;sess="+ContextMap.getContext().getSession()+"&amp;action=details&amp;item="+this.itemid+"\">"+Items.get().item(this.itemid).getName()+"</a>";
	}
	
	/**
	 * Gibt die ID des zum ItemModul gehoerenden Items zurueck
	 * @return die Item-ID des Moduls
	 */
	public ResourceID getItemID() {
		return new ItemID(itemid, 0, 0);	
	}

	/**
	 * Deaktiviert die Set-Effekte des Moduls
	 *
	 */
	public void disableSetEffect() {
		this.calculateSetEffect = false;
	}
}
