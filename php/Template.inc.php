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
require_once('libcommon.inc.php');

function __templatefunction_pre_table_begin( $t, $width=420, $align='center', $imagepath='./' ) {
	if( $width{0} == '$' ) {
		$width = substr($width,1);
		$width = '"); str.append(templateEngine.getVar("'.$width.'")); str.append("';
	}
	if( $align{0} == '$' ) {
		$align = substr($align,1);
		$align = '"); str.append(templateEngine->get_var("'.$align.'")); str.append("';
	}
	if( $imagepath{0} == '$' ) {
		$imagepath = substr($imagepath,1);
		$imagepath = '"); str.append(templateEngine.getVar("'.$imagepath.'")); str.append("';
	}
	// TODO: check & ggf fixme (slash-problem ?)
	return str_replace("\"", "\\\"", table_begin($width,$align,$imagepath));	
}

function __templatefunction_pre_table_end( $t, $imagepath='../' ) {
	return str_replace("\"", "\\\"", table_end($imagepath='./'));	
}

function __templatefunction_pre_image_link_to() {
	$paramlist = array();
	$params = array();
	
	for( $i = 0; $i < func_num_args(); $i++ ) {
		$arg = func_get_arg($i);
		$pos = strpos($arg,':');
		if( $pos !== false ) {
			$pname = trim(substr($arg, 0, $pos));
			$param = trim(substr($arg, $pos+1));
			if( $param{0} == '$' ) {
				$param = substr($param,1);
				$param = '"); str.append(templateEngine.getVar("'.$param.'")); str.append("';
			}
			$paramlist[$pname] = $param;
			
			if( $pname != 'image_css_style' ) {
				$params[] = $arg;	
			}
		}
		else {
			$params[] = $arg;	
		}
	}
	
	$img_css = '';
	if( isset($paramlist['image_css_style']) ) {
		$img_css = ';'.$paramlist['image_css_style'];
		unset($paramlist['image_css_style']);
	}
	
	$params[1] = '<img style=\"border:0px'.$img_css.'\" src=\""); str.append(templateEngine.getVar("URL")); str.append("data/'.$params[1].'\" alt=\"\" />';
	
	$str = call_user_func_array( '__templatefunction_pre_link_to', $params ); 
	return $str;
}

function __templatefunction_pre_link_to() {
	$paramlist = array();
	$t = func_get_arg(0);
	$name = func_get_arg(1);
	$action = func_get_arg(2);
	
	if( $name{0} == '$' ) {
		$name = substr($name,1);
		$name = '"); str.append(templateEngine.getVar("'.$name.'")); str.append("';
	}
	
	if( $action{0} == '$' ) {
		$action = substr($action,1);
		$action = '"); str.append(templateEngine.getVar("'.$action.'")); str.append("';
	}
	
	for( $i = 3; $i < func_num_args(); $i++ ) {
		$arg = func_get_arg($i);
		$pos = strpos($arg,':');
		if( $pos !== false ) {
			$pname = trim(substr($arg, 0, $pos));
			$param = trim(substr($arg, $pos+1));
			if( $param{0} == '$' ) {
				$param = substr($param,1);
				$param = '"); str.append(templateEngine.getVar("'.$param.'")); str.append("';
			}
			$paramlist[$pname] = $param;	
		}
	}	
	
	$text = "<a ";
	
	if( isset($paramlist['link_target']) ) {
		$text .= "target=\\\"".$paramlist['link_target']."\\\" ";
		unset($paramlist['link_target']);	
	}
	
	if( isset($paramlist['css_style']) ) {
		$text .= "style=\\\"".$paramlist['css_style']."\\\" ";
		unset($paramlist['css_style']);	
	}	
	
	if( isset($paramlist['css_class']) ) {
		$text .= "class=\\\"".$paramlist['css_class']."\\\" href=\\\"ds?module=";
		unset($paramlist['css_class']);	
	}	
	else {
		$text .= 'class=\"forschinfo\" href=\"./ds?module=';	
	}
	
	if( isset($paramlist['module']) ) {
		$text .= $paramlist['module']."&amp;sess=";
		unset($paramlist['module']);	
	}
	else {
		$text .= '"); str.append(templateEngine.getVar("global.module")); str.append("&amp;sess=';
	}
	
	$text .= '"); str.append(templateEngine.getVar("global.sess")); str.append("';
	
	$text .= '&amp;action='.$action;
	
	if( count($paramlist) ) {
		foreach( $paramlist as $key=>$param ) {
			$text .= '&amp;'.$key.'='.$param;	
		}
	}
	$text .= '\">'.$name.'</a>';
	
	return $text;
}


function __templatefunction_pre_overlib() {
	$t = func_get_arg(0);
	$text = func_get_arg(1);
	
	if( $text[0] == '$' ) {
		$text = substr($text,1);
		$text = '\"); str.append(templateEngine.getVar(\"'.$text.'\")); str.append(\"';
	}
	else {
		$text = str_replace("\\", "\\\\", addSlashes(str_replace('>', '&gt;', str_replace('<', '&lt;', $text))));	
	}
	
	$event = func_get_arg(2);
	
	$paramlist = array( 'TIMEOUT'	=> '0', 
						'DELAY'	=> '400', 
						'WIDTH'	=> '300' );
	
	for( $i = 3; $i < func_num_args(); $i++ ) {
		$arg = func_get_arg($i);
		$pos = strpos($arg,':');
		if( $pos !== false ) {
			$name = strtoupper(trim(substr($arg, 0, $pos)));
			$param = trim(substr($arg, $pos+1));
			if( $param{0} == '$' ) {
				$param = substr($param,1);
				$param = '\"); str.append(templateEngine.getVar(\"'.$param.'\")); str.append(\"';
			}
			$paramlist[$name] = $param;	
		}
	}
	
	$paramtext = array();
	foreach( $paramlist as $key => $name ) {
		$paramtext[] =  $key.','.$name;
	}
		
	if( $event == 'mo' ) {
		$text = "onmouseover=\\\"return overlib(\\'".$text."\\',".implode(',',$paramtext).");\\\" onmouseout=\\\"return nd();\\\"";		
	}
	elseif( $event = 'md' ) {
		$text = "onclick=\\\"return overlib(\\'".$text."\\',".implode(',',$paramtext).");\\\" onmouseout=\\\"return nd();\\\"";
	}
	
	return $text;
}

function __templatefunction_pre_form_create_hidden() {
	$paramlist = array();
	$t = func_get_arg(0);
	$action = func_get_arg(1);
	
	if( $action{0} == '$' ) {
		$action = substr($action,1);
		$action = '"); str.append(templateEngine.getVar("'.$action.'")); str.append("';
	}
	
	for( $i = 2; $i < func_num_args(); $i++ ) {
		$arg = func_get_arg($i);
		$pos = strpos($arg,':');
		if( $pos !== false ) {
			$name = trim(substr($arg, 0, $pos));
			$param = trim(substr($arg, $pos+1));
			if( $param{0} == '$' ) {
				$param = substr($param,1);
				$param = '"); str.append(templateEngine.getVar("'.$param.'")); str.append("';
			}
			$paramlist[$name] = $param;	
		}
	}
	
	if( $action != '-' ) {
		$text = "<input type=\\\"hidden\\\" name=\\\"action\\\" value=\\\"$action\\\" />\n";
	}
	
	if( isset($paramlist['module']) ) {
		$text .= "<input type=\\\"hidden\\\" name=\\\"module\\\" value=\\\"".$paramlist['module']."\\\" />\n";
		unset($paramlist['module']);	
	}
	else {
		$text .= '<input type=\"hidden\" name=\"module\" value=\""); str.append(templateEngine.getVar("global.module")); str.append("\" />'."\n";
	}
	
	$text .= '<input type=\"hidden\" name=\"sess\" value=\""); str.append(templateEngine.getVar("global.sess")); str.append("\" />'."\n";
	
	foreach( $paramlist as $name => $param ) {
		$text .= "<input type=\\\"hidden\\\" name=\\\"".$name."\\\" value=\\\"".$param."\\\" />\n";
	}
	
	return $text;
}

function __templatefunction_pre_checkbox() {
	$t = func_get_arg(0);
	$text = func_get_arg(1);
	$name = func_get_arg(2);
	$var = func_get_arg(3);
	
	$ret = '<input type=\"checkbox\" name=\"'.$name.'\" id=\"'.$name.'\" {if '.$var.'}checked=\"checked\"{/endif} value=\"1\" /><label for=\"'.$name.'\">'.$text.'</label>';
	
	return str_replace("\"", "\\\"", $ret);
}

class Template {
	/* Debug-Ausgabe aktivieren oder deaktivieren */
	private $m_debug = false;

	/* $file[$handle] = "filename" */
	private $m_file = array();
	
	private $m_controller = null;

	/* relative filenames are relative to this pathname */
	/* overlay is checked first - then base */
	private $overlay = "";
	private $base = "";
	
	/***************************************************************************/
	/* 
	 *	constructor ( $controller, $base )
	 *		$controller -> A valid CController-Object.
	 *		$base -> Template base-dir
	 */
	function __construct( $controller, $base = ".") {
		$this->m_controller = $controller;
		
		$this->setBase($base);
	}
	
	public function setDebugMode( $value ) {
		$this->m_debug = $value;	
	}
	
	public function getController() {
		return $this->m_controller;
	}

	/* 
	 *	setBase($base)
	 *		$base -> new template directory (base).
	 */
	public function setBase($base) {
		if( !is_dir($base) ) {
			$this->error("set_base: $base is not a directory.");
			return false;
		}

		$this->base = $base;
		return true;
	}

	private function parse_if( $bedingung ) {
		$bedingung = trim($bedingung);
		$bed = explode(" ", $bedingung);
		$bedingungen = array();
		foreach( $bed as $val ) {
			if( trim($val) != "" ) {
				$bedingungen[] = $val;
			} 
		}

		if( count($bedingungen) < 2 ) {
			$bedingung = preg_replace("/([a-zA-Z0-9_\.]{3,})/", "templateEngine.isVarTrue(\"\\1\")", $bedingung);
		}
		else if( count($bedingungen) == 2 ) {
			$bedingung = preg_replace("/([a-zA-Z0-9_\.]{3,})/", "!templateEngine.isVarTrue(\"\\1\")", $bedingungen[1]);
		}
		else if( count($bedingungen) == 3 ) {
			$op = $bedingungen[1];
			$val1 = $bedingungen[0];
			$val2 = $bedingungen[2];
			$bedingung = "templateEngine.getNumberVar(\"".$val1."\").doubleValue() ".$op." ".$val2;
		}
		
		return "\"); if( ".$bedingung." ) { str.append(\"";
	}	
	
	private function parse_control_structures( &$block ) {
		$block = preg_replace("/\{if (['\"]?)([^\"']*)\\1}/esiU", "\$this->parse_if('\\2')", $block);
		
		$block = preg_replace("/\{else\}/", "\"); } else { str.append(\"", $block);
		
		$block = preg_replace("/\{\/endif\}/", "\"); } str.append(\"", $block);
	}
	
	private function parse_function( $name, $parameter, $callnow=false ) {
		$parameter = trim($parameter);
		$parameter = explode(',',$parameter);
		if( !$callnow ) {
			foreach( $parameter as $key=>$param ) {
				if( trim($param) != "" ) {
					$parameter[$key] = '"'.trim($param).'"';
				}
				else {
					unset($parameter[$key]);	
				}
			}
		
			return ".\$templateEngine->$name( ".implode( ',', $parameter )." ).";
		}
		else {
			$newparameter = array($this);
			
			foreach( $parameter as $key=>$param ) {
				if( trim($param) != "" ) {
					$newparameter[] = trim($param);
				}
			}
			
			if( function_exists('__templatefunction_pre_'.$name) ) {			
				return call_user_func_array( '__templatefunction_pre_'.$name, $newparameter );
			}
			else {
				$this->error('Die compile-time Funktion &gt;'.$name.'&lt; konnte nicht lokalisiert werden');
			}
		}
	}	
	
	private function parse_functions( &$block ) {
		$block = preg_replace("/\{#([^\s^\}^\?]+)([^}]*)\}/e", "\$this->parse_function('\\1','\\2')", $block);
		$block = preg_replace("/\{!([^\s^\}^\?]+)([^}]*)\}/e", "\$this->parse_function('\\1','\\2',true)", $block);
	}
	
	private function parse_vars( &$block ) {
		$block = preg_replace("/\{([^\}^\s^\?]+)\}/siU", "\"); str.append(templateEngine.getVar(\"\\1\")); str.append(\"", $block);
	}
		
	private function parse_blocks( &$block, $parent ) {
		$reg = "/<!--\s+BEGIN ([^\s]*)\s+-->/sm";
		$result = array();
		preg_match($reg, $block, $result );
		
		$blocklist = array();
		
		while( count($result) ) {
			// ok...wir haben einen block
			// nun wollen wir mal das Ende suchen und ihn rausoperieren
			
			$name = $result[1];
			
			$newblock = array();
			preg_match_all( "/<!--\s+BEGIN $name\s+-->(.*)\s*<!--\s+END $name\s+-->/sm", $block, $newblock);
			$block = preg_replace( "/<!--\s+BEGIN $name\s+-->(.*)\s*<!--\s+END $name\s+-->/sm", "\"); str.append(templateEngine.getBlockReplacementVar(\"$name\")); str.append(\"", $block);
			
			$extractedblock = $newblock[1][0];
								
			$subblocks = $this->parse_blocks( $extractedblock, $name );
			
			preg_match_all( "/templateEngine.getVar\(\"([^\"]*)\"([^\)]*)\)/", $extractedblock, $varlist);

			$newvarlist = array();
			foreach( $varlist[1] as $avar ) {
				$newvarlist[] = $avar;
			}
			
			preg_match_all( "/templateEngine.getBlockReplacementVar\(\"([^\"]*)\"([^\)]*)\)/", $extractedblock, $varlist);

			$newvarlist = array();
			foreach( $varlist[1] as $avar ) {
				$newvarlist[] = $avar;
			}
								
			$blocklist[] = array( $name, $extractedblock, $newvarlist, $parent );
			$blocklist = array_merge($blocklist, $subblocks);
								
			$result = array();
			preg_match($reg, $block, $result );
		}
		
		return $blocklist;
	}
	
	private function parse_getChildVars( $blocks, $name ) {
		$result = array();
		
		foreach( $blocks as $ablock ) {
			if( $ablock[3] == $name ) {
				$result = array_merge($result,$ablock[2],$this->parse_getChildVars( $blocks, $ablock[0]));
			}	
		}
		
		return $result;	
	}
	
	public function compile( $fname ) {
		$filename = $this->getFileName($fname);
		$basefilename = basename($filename,'.html');
		
		$str = implode('', @file($filename));
		
		$str = str_replace("\"", "\\\"", $str);
		$str = str_replace("\\'","\\\\'", $str);
		
		$this->parse_functions($str);
		
		$this->parse_control_structures($str);
		
		$this->parse_vars($str);
		
		$varlist = array();
		preg_match_all( "/templateEngine.getVar\(\"([^\"]*)\"([^\)]*)\)/", $str, $varlist);

		$completevarlist = array();
		foreach( $varlist[1] as $avar ) {
			$completevarlist[] = $avar;
		}
		
		preg_match_all( "/templateEngine.getBlockReplacementVar\(\"([^\"]*)\"([^\)]*)\)/", $str, $varlist);

		foreach( $varlist[1] as $avar ) {
			$completevarlist[] = $avar;
		}

		$result = $this->parse_blocks($str, 'MAIN');
		
		$bfname = str_replace('.', '', $basefilename);

		$newfile = "package net.driftingsouls.ds2.server.templates;\n";
		$newfile .= "import net.driftingsouls.ds2.server.framework.templates.Template;\n";
		$newfile .= "import net.driftingsouls.ds2.server.framework.templates.TemplateBlock;\n";
		$newfile .= "import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;\n\n";
		
		$newfile .= "/* Dieses Script wurde automatisch generiert\n";
		$newfile .= "   Alle Aenderungen gehen daher bei der naechten Generierung\n";
		$newfile .= "   verloren. Wenn sie Aenderungen machen wollen, tun sie dies\n";
		$newfile .= "   bitte im entsprechenden Ursprungstemplate */\n\n";
		
		$newfile .= "public class ".$bfname." implements Template {\n";
		$newfile .= "\tpublic void prepare( TemplateEngine templateEngine, String filehandle ) {\n";
		foreach( $result as $block ) {
			if( $block[3] != 'MAIN' ) {
				$parent = "\"".$block[3]."\"";
			}
			else {
				$parent = "filehandle";	
			}
			$newfile .= "\t\ttemplateEngine.registerBlockItrnl(\"".$block[0]."\",filehandle,$parent);\n";
		}
		$newfile .= "\t}\n\n";
		
		foreach( $result as $block ) {
			$newfile .= "\tclass ".str_replace('.','',$block[0])." implements TemplateBlock {\n";
			$newfile .= "\t\tpublic String[] getBlockVars(boolean all) {\n";
			$newfile .= "\t\t\tif( !all ) {\n";
			
			if( count($block[2]) ) {
				$newfile .= "\t\t\t\treturn new String[] {\"".implode('","',$block[2])."\"};\n";
			}
			else {
				$newfile .= "\t\t\t\treturn new String[] {};\n";
			}
			
			$newfile .= "\t\t\t} else {\n";
			
			$varlist = $block[2];
			$varlist = array_merge($block[2], $this->parse_getChildVars($result, $block[0]));
			
			if( count($varlist) ) {
				$newfile .= "\t\t\t\treturn new String[] {\"".implode('","',$varlist)."\"};\n";
			}
			else {
				$newfile .= "\t\t\t\treturn new String[] {};\n";
			}
			$newfile .= "\t\t\t}\n\t\t}\n";
			$newfile .= "\t\tpublic String output(TemplateEngine templateEngine) {\n";
			$newfile .= "\t\tStringBuilder str = new StringBuilder(".strlen($block[1]).");\n";
			$blockstr = str_replace("\r\n", "\n", $block[1]);
			$blockstr = explode("\n", $blockstr);
			for( $i=0; $i < count($blockstr); $i++ ) {
				if( $i < count($blockstr) - 1 ) {
					$newfile .= "\t\tstr.append(\"".$blockstr[$i]."\\n\");\n";
				}
				else {
					$newfile .= "\t\tstr.append(\"".$blockstr[$i]."\");\n";
				}
			}
			$newfile .= "\t\treturn str.toString();";

			$newfile .= "\t\t}\n";
			$newfile .= "\t}\n";
		}

		$newfile .= "\tpublic TemplateBlock getBlock(String block) {\n";
		foreach( $result as $block ) {
			$newfile .= "\t\tif( block.equals(\"".$block[0]."\") ) {\n";
			$newfile .= "\t\t\treturn new ".str_replace('.','',$block[0])."();\n";
			$newfile .= "\t\t}\n";
		}
		$newfile .= "\t\treturn null;\n";
		$newfile .= "\t}\n";
		
		$varlist = array();
		preg_match_all( "/templateEngine.getVar\(\"([^\"]*)\"([^\)]*)\)/", $str, $varlist);

		$newvarlist = array();
		foreach( $varlist[1] as $avar ) {
			$newvarlist[] = $avar;
		}
		
		preg_match_all( "/templateEngine.getBlockReplacementVar\(\"([^\"]*)\"([^\)]*)\)/", $str, $varlist);

		foreach( $varlist[1] as $avar ) {
			$newvarlist[] = $avar;
		}
		
		$newfile .= "\tpublic String[] getVarList(boolean all) {\n";
		$newfile .= "\t\tif( !all ) {\n";
			
		if( count($newvarlist) ) {
			$newfile .= "\t\t\treturn new String[] {\"".implode('","',$newvarlist)."\"};\n";
		}
		else {
			$newfile .= "\t\t\treturn new String[] {};\n";
		}
			
		$newfile .= "\t\t} else {\n";

		if( count($completevarlist) ) {
			$newfile .= "\t\t\treturn new String[] {\"".implode('","',$completevarlist)."\"};\n";
		}
		else {
			$newfile .= "\t\t\treturn new String[] {};\n";
		}
		$newfile .= "\t\t}\n\t}\n";
		
		$newfile .= "\tpublic String main( TemplateEngine templateEngine ) {\n";
		$newfile .= "\t\tStringBuilder str = new StringBuilder(".strlen($str).");\n";
		$str = str_replace("\r\n", "\n", $str);
		$str = explode("\n", $str);
		for( $i=0; $i < count($str); $i++ ) {
			if( $i < count($str) - 1 ) {
				$newfile .= "\t\tstr.append(\"".$str[$i]."\\n\");\n";
			}
			else {
				$newfile .= "\t\tstr.append(\"".$str[$i]."\");\n";
			}
		}
		$newfile .= "\t\treturn str.toString();";

		$newfile .= "\t}\n";
		$newfile .= "}";
		
		$filename = $this->overlay."/".$fname;
		$dir = "";
		if( $this->overlay=="" || !file_exists($filename) ) {
   			$dir = $this->base;
		}
		else {
			$dir = $this->overlay;	
		}
		
		$f = fopen( $dir."/".$bfname.".java", "w+" );
		fwrite( $f, $newfile );
		fclose($f);

		return $bfname;
	}
	
	/***************************************************************************/
	/* 
	 *	getFileName($file)
	 *		$file -> name to be completed.
	 */
	private function getFileName( $file, $quiet=false, $overlayonly=false ) {
		if( $file{0} != '/' ) {
			$filename = $this->overlay.'/'.$file;

			if( ($this->overlay == '' || !file_exists($filename)) && !$overlayonly ) {
   				$filename = $this->base.'/'.$file;	//now try $this->base
				if( !file_exists($filename) ) {
					if( !$quiet ) {
						$this->error("getFileName: Die Datei &gt;".$file."&lt; existiert nicht");
					}
					return false;
				}
			}
			elseif( $this->overlay == '' || !file_exists($filename) ) {
				if( !$quiet ) {
					$this->error("getFileName: Die Datei &gt;".$file."&lt; existiert nicht");
				}
				return false;
			}
		}
		elseif( !file_exists($file) ) {
			if( !$quiet ) {
				$this->error("getFileName: Die Datei &gt;".$file."&lt; existiert nicht");
			}
			return false;
		}

		return $filename;
	}

	/***************************************************************************/
	/*	
	 *	error($msg)
	 *		$msg -> send an error message to the controller
	 */
	private function error($msg) {
		CController::setError('Template: '.$msg);
		
		return false;
 	}
 	
 	/***************************************************************************/
 	/*	
	 *	__call($method, $parameter)
	 *		php standard method
	 */
 	function __call( $method, $parameter ) {
 		if( $method == 'table_begin' ) {
 			echo call_user_func_array('table_begin', $parameter);
 		}
 		elseif( $method == 'table_end' ) {
 			echo table_end();	
 		}	
 	}
}


if( $argc < 2 ) {
	echo "Template.php filename\n";
	exit();
}

$filename = $argv[1];
$t = new Template(null,dirname($filename));
$bfname = $t->compile(basename($filename));
@unlink('src/net/driftingsouls/ds2/server/templates/'.$bfname.'.java');
copy(dirname($filename).'/'.$bfname.'.java','src/net/driftingsouls/ds2/server/templates/'.$bfname.'.java');
@unlink(dirname($filename).'/'.$bfname.'.java');
?>
