<?php
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
define("DS_PATH", "/home/bktheg/Projects/ds2");

define('LIBPATH', DS_PATH.'/php/libs/');
define('CFGPATH', DS_PATH.'/php/cfg/' );
require_once(DS_PATH."/php/cfg/config.inc.php");
require_once(DS_PATH."/php/libs/libpackages.inc.php");
importlib("libitems");

function echoText($text) {
	$text = str_replace('&szlig;','ß',$text);
	$text = str_replace('&uuml;','ü',$text);
	$text = str_replace('&ouml;','ö',$text);
	$text = str_replace('&auml;','ä',$text);
	$text = str_replace('&Uuml;','Ü',$text);
	$text = str_replace('&Ouml;','Ö',$text);
	$text = str_replace('&Auml;','Ä',$text);
	return $text;
}

function echoCargo($cargo) {
	$tagmap = array(	0 => "Nahrung",
						1 => "Deuterium",
						2 => "Kunststoffe",
						3 => "Titan",
						4 => "Uran",
						5 => "Antimaterie",
						6 => "Adamatium",
						7 => "Platin",
						8 => "Silizium",
						9 => "Xentronium",
						10 => "Erz",
						11 => "Isochips",
						12 => "Batterien",
						13 => "LBatterien",
						14 => "Antarit",
						15 => "Shivarte",
						16 => "Ancientarte",
						17 => "BAdmins");
						
	// Itembehandlung nicht notwendig - wir haben keine Itemkosten bei den Items im Moment
	
	foreach( $cargo as $res => $count ) {
		echo "\t\t\t\t<res:".$tagmap[$res]." count=\"".$count."\" />\n";
	}
}

function echoShipData($data) {
	foreach( $data as $key => $value ) {
		if( $key == "weapons" ) {
			echo "\t\t\t\t<shd:".$key.">\n";
			foreach( $value as $wpn => $val ) {
				$maxheat = 1;
				$count = 1;
				if( is_array($val) ) {
					$count = $val[0];
					$maxheat = $val[1];
				}
				else {
					$count = $val;
				}
				echo "\t\t\t\t\t<shd:weapon name=\"".$wpn."\" count=\"".$count."\" maxheat=\"".$maxheat."\" />\n";
			}
			echo "\t\t\t\t</shd:".$key.">\n";
		}
		else if( $key == "maxheat" ) {
			echo "\t\t\t\t<shd:".$key.">\n";
			foreach( $value as $wpn => $val ) {
				echo "\t\t\t\t\t<shd:weapon name=\"".$wpn."\" maxheat=\"".$val."\" />\n";
			}
			echo "\t\t\t\t</shd:".$key.">\n";
		}
		else if( $key == "flags" ) {
			if( !is_array($value) ) {
				$value = array($value);
			}
			echo "\t\t\t\t<shd:".$key.">\n";
			foreach( $value as $flag ) {
				echo "\t\t\t\t\t<shd:set name=\"".$flag."\" />\n";
			}
			echo "\t\t\t\t</shd:".$key.">\n";
		}
		else {
			echo "\t\t\t\t<shd:".$key." value=\"".$value."\" />\n";
		}
	}
}

function effectDraftShip($effect) {
	echo "draft-ship\" shiptype=\"".$effect->shipclass."\" race=\"".$effect->race."\" system-req=\"".($effect->systemreq ? "true" : "false")."\" flagschiff=\"".($effect->flagschiff ? "true" : "false")."\">\n";
	echo "\t\t\t<buildcosts>\n";
	echoCargo($effect->cost);
	echo "\t\t\t\t<crew count=\"".$effect->crew."\" />\n";
	echo "\t\t\t\t<e count=\"".$effect->ecost."\" />\n";
	echo "\t\t\t</buildcosts>\n";
	echo "\t\t\t<dauer count=\"".$effect->dauer."\" />\n";
	foreach( $effect->tr as $tr ) {
		if( $tr == 0 ) continue;
		echo "\t\t\t<tech-req id=\"".$tr."\" />\n";
	}
	foreach( explode(" ",$effect->werftreq) as $werftreq ) {
		echo "\t\t\t<werft-req type=\"".$werftreq."\" />\n";
	}
	
	echo "\t\t</effect>\n";
}

function effectModule($effect) {
	echo "module\"".($effect->getModuleSetID() != 0 ? " set=\"".$effect->getModuleSetID()."\"" : "").">\n";
	foreach( $effect->getCategory() as $slot ) {
		echo "\t\t\t<slot id=\"".$slot."\" />\n";
		echo "\t\t\t<shipdata>\n";
		echoShipData($effect->getMods());
		echo "\t\t\t</shipdata>\n";
	}
	echo "\t\t</effect>\n";
}

echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
echo "<items xmlns:res=\"http://www.drifting-souls.net/ds2/resources/2006\" xmlns:shd=\"http://www.drifting-souls.net/ds2/shipdata/2006\">\n";
foreach($__items as $id => $item) {
	echo "\t<!-- $id - ".echoText($item->name)." -->\n";
	echo "\t<item id=\"".$id."\" cargo=\"".$item->cargo."\"";
	if( $item->handel ) {
		echo " handel=\"true\"";
	}
	if( $item->accesslevel != 0 ) {
		echo " accesslevel=\"".$item->accesslevel."\"";
	}
	if( $item->unknownItem ) {
		echo " unknownItem=\"true\"";
	}
	echo ">\n";
	echo "\t\t<name>".echoText($item->name)."</name>\n";
	if( $item->picture != "open.gif" ) {
		echo "\t\t<picture>".$item->picture."</picture>\n";
	}
	if( $item->largepicture != "none" ) {
		echo "\t\t<large-picture>".$item->largepicture."</large-picture>\n";
	}
	if( $item->quality != 0 ) {
		echo "\t\t<quality>";
		switch($item->quality) {
			case 1: echo "rare";
					break;
			case 2: echo "ultra-rare";
					break;
			case 3: echo "epic";
					break;
			case 4: echo "artifact";
					break;
			default:
					echo "<!-- TODO: unknown quality ".$item->quality." -->";
		}
		echo "</quality>\n";
	}
	
	if( is_object($item->effect) && $item->effect->type != EFFECT_NONE ) {
		$effect = $item->effect;
		
		echo "\t\t<effect ";
		if( $item->allyEffect ) {
			echo "ally-effect=\"true\" ";
		}
		echo "type=\"";
		switch( $item->effect->type ) {
		case EFFECT_DRAFT_SHIP:
			effectDraftShip($effect);
			break;
		case EFFECT_DRAFT_AMMO:
			echo "draft-ammo\" ammo=\"".$effect->getAmmoID()."\" />\n";
			break;
		case EFFECT_MODULE:
			effectModule($effect);
			break;
		case EFFECT_AMMO:
			echo "ammo\" ammo=\"".$effect->getAmmoID()."\" />\n";
			break;
		case EFFECT_DISABLE_SHIP:
			echo "disable-ship\" shiptype=\"".$effect->shipclass."\" />\n";
			break;
		case EFFECT_DISABLE_IFF:
			echo "disable-iff\" />\n";
			break;
		case EFFECT_MODULE_SET_META:
			echo "module-set-meta\">\n";
			echo "\t\t\t<name>".echoText($effect->name)."</name>\n";
			foreach( $effect->combomods as $count => $mods ) {
				echo "\t\t\t<combo item-count=\"".$count."\">\n";
				echoShipData($mods);
				echo "\t\t\t</combo>\n";
			}
			echo "\t\t</effect>\n";
			break;
		default:
			echo "\t\t<!-- TODO: unknown effect ".$effect->type." -->\n";
		}
	}
	
	if( $item->description != "" ) {
		echo "\t\t<description>".echoText($item->description)."</description>\n";
	}
	
	echo "\t</item>\n\n";
}
echo "</items>\n";
?>