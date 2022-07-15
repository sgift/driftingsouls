#!/usr/bin/perl

use GD;
use POSIX;
use Switch;

$kolonie_nr=$ARGV[0];

$tile_width=25;
$tile_height=25;
$fontname='/srv/www/web1/tomcat/dsj/data/bnkgothm.ttf';
@suffixes[0,1,2,3,4,5,6,7,8]=('','_fo','_fa','_fe','_fo_fa','_fo_fe','_fa_fe','_fo_fa_fe');

$image = GD::Image->newFromPng("kolonie".$kolonie_nr."_lrs.png",1);
die "Ungueltiges PNG-bild ($image_path)\n" if($image==undef);
$truecolor=0;
if(!defined($image->colorsTotal)){
	$truecolor=1;
}
($width,$height) = $image->getBounds();

$red = $image->colorAllocate(255,95,95);

@F_bounds=GD::Image->stringFT($red,$fontname,12,0,0,0,'F');
#if(!exists($bounds[0])){ die "Konnte Font nicht rendern\n";}
$F_width=$F_bounds[4]-$F_bounds[0];
$F_height=$F_bounds[1]-$F_bounds[5];
$F_y=int(($tile_height-$F_height)/2+$F_height);
$F1_x=int(($tile_width-$F_width)/2);
$F2_x=int(($tile_width-$F_width*2)/2);
$F2_width=$F_width;
if($F2_x<0){
	$F2_width=int($F_width-abs($F2_x)/2);
	$F2_x=-1;
}
$F3_x=int(($tile_width-$F_width*3)/2);
$F3_width=$F_width;
if($F3_x<0){
	$F3_width=int($F_width-abs($F3_x)/2);
	$F3_x=-1;
}

$v_tiles=$height/$tile_height;
$h_tiles=$width/$tile_width;

$tile_number=0;
for($v=0;$v<$v_tiles;$v++){
TILE:	for($h=0;$h<$h_tiles;$h++){
		$tile_name="kolonie".$kolonie_nr."_lrs".$tile_number;
		for($c=0;$c<=7;$c++){
			$tile=GD::Image->new($tile_width,$tile_height,$truecolor);
#			$tile->saveAlpha(1);
#			$tile->alphaBlending(0);
			$tile->copy($image,0,0,$h*$tile_width,$v*$tile_height,$tile_width,$tile_height);
			$temp=$tile->clone();
			$temp->trueColorToPalette();
			print $temp->colorsTotal()."\n";
			if($temp->colorsTotal()==1){
				next TILE;
			}
			$red = $image->colorAllocate(255,95,95);
			$green = $image->colorAllocate(55,255,55);
			$blue = $image->colorAllocate(127,146,255);
			switch($c){
				case 0 {} #leeres feld
				case 1 {@bounds = $tile->stringFT($green,$fontname,12,0,$F1_x,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 2 {@bounds = $tile->stringFT($blue,$fontname,12,0,$F1_x,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 3 {@bounds = $tile->stringFT($red,$fontname,12,0,$F1_x,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 4 {@bounds = $tile->stringFT($green,$fontname,12,0,$F2_x,$F_y,'F'); if(!exists($bounds[0])){ die;} @bounds = $tile->stringFT($blue,$fontname,12,0,$F2_x+$F2_width,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 5 {@bounds = $tile->stringFT($green,$fontname,12,0,$F2_x,$F_y,'F'); if(!exists($bounds[0])){ die;} @bounds = $tile->stringFT($red,$fontname,12,0,$F2_x+$F2_width,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 6 {@bounds = $tile->stringFT($blue,$fontname,12,0,$F2_x,$F_y,'F'); if(!exists($bounds[0])){ die;} @bounds = $tile->stringFT($red,$fontname,12,0,$F2_x+$F2_width,$F_y,'F'); if(!exists($bounds[0])){ die;}}
				case 7 {@bounds = $tile->stringFT($green,$fontname,12,0,$F3_x,$F_y,'F'); if(!exists($bounds[0])){ die;} @bounds = $tile->stringFT($blue,$fontname,12,0,$F3_x+$F3_width,$F_y,'F'); if(!exists($bounds[0])){ die;} @bounds = $tile->stringFT($red,$fontname,12,0,$F3_x+$F3_width*2,$F_y,'F'); if(!exists($bounds[0])){ die;}}
			}
			$png_data = $tile->png;
			open (NEW_IMAGE,'>',$tile_name.$suffixes[$c].".png") || die "Konnte Ziel nicht oeffnen\n";
			binmode NEW_IMAGE;
			print NEW_IMAGE $png_data;
			close NEW_IMAGE;
		}
		$tile_number++;
	}
}
