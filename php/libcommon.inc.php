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
function table_begin( $width=420, $align='center', $imagepath='./' ) {	
	$output = '<table cellpadding="0" cellspacing="0" border="0" class="noBorderX" width="'.$width.'">';
	$output .= '<tr>';
	$output .= '<td class="noBorderXnBG" style="width:19px"><img src="'.$imagepath.'data/interface/border/border_topleft.gif" alt="" /></td>';
	$output .= '<td class="noBorderXnBG" style="background-image:url('.$imagepath.'data/interface/border/border_top.gif); background-repeat:repeat-x"></td>';
	$output .= '<td class="noBorderXnBG" style="width:19px"><img src="'.$imagepath.'data/interface/border/border_topright.gif" alt="" /></td>';
	$output .= '</tr>';
	$output .= '<tr>';
	$output .= '<td class="noBorderXnBG" rowspan="1" style="width:19px; background-image:url('.$imagepath.'data/interface/border/border_left.gif); background-repeat:repeat-y"></td>';
	$output .= '<td class="noBorderX" colspan="1" align="'.$align.'">';
	
	return $output;
}

function table_end( $imagepath='./' ) {
	$output = '</td>';
	$output .= '<td class="noBorderXnBG" rowspan="1" style="width:19px; background-image:url('.$imagepath.'data/interface/border/border_right.gif); background-repeat:repeat-y"></td>';
	$output .= '</tr>';
	$output .= '<tr>';
	$output .= '<td class="noBorderXnBG" style="width:19px"><img src="'.$imagepath.'data/interface/border/border_bottomleft.gif" alt="" /></td>';
	$output .= '<td class="noBorderXnBG" colspan="1" style="background-image:url('.$imagepath.'data/interface/border/border_bottom.gif); background-repeat:repeat-x"></td>';
	$output .= '<td class="noBorderXnBG" style="width:19px"><img src="'.$imagepath.'data/interface/border/border_bottomright.gif" alt="" /></td>';
	$output .= '</tr>';
	$output .= '</table>';
	
	return $output;
}

?>
