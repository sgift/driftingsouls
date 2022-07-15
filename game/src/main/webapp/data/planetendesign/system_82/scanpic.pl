use GD;
use DBI;

$system_name=$ARGV[0];
$system_number=$ARGV[1];
print "Bearbeite $system_name.png.\n";
print "Bearbeitung erfolgt als DS-System: $system_number.\n\n";

$image = GD::Image->newFromPng("./".$system_name.".png",1);
die "Ungueltiges PNG-bild ($image_path)\n" if($image==undef);

$truecolor=0;
# Benutzte Eingabebilder sollen zunächst nur mit schwarzem Hintergrund benutzt werden, Alpha wird nicht ausgewertet
#if(!defined($image->colorsTotal)){
#	$truecolor=1;
#}

($breite,$hoehe) = $image->getBounds();



print "Das Bild ist $breite breit und $hoehe hoch.\n";
print "-------------------------------------------\n";


open (IN, "<./scanpic.cfg");
open (my $out, ">>./system".$system_number.".sql");

$l = 0;
while ($zeile = <IN>) {
	chomp $zeile;
	$lc[$l] = $zeile;

	@cfgv = split(/\|/, $lc[$l]);

	for ($k = 0; $k < 10; $k++) {
		$cfgval[$l][$k] = $cfgv[$k];
	}
	print $cfgval[$l][0]." - Farbcode: ".$cfgval[$l][1]." fuer ".$cfgval[$l][9]."\n";
	
	$l++;
}
$lastcfg = $l;
close (IN);
print "-------------------------------------------\n";
print "\n";

for ($j = 0; $j < $lastcfg; $j++) {
	for ($i = 0; $i < 10; $i++) {
		print $cfgval[$j][$i]."|";
	}
	print "\n-------------------------------------------\n";
}
print "\n-------------------------------------------\n";
print "\n";

#my $dbh = DBI->connect("DBI:mysql:database=web1_1;host=localhost","ds","",{'RaiseError' => 1});

for ($j = 0; $j < $hoehe; $j++) {
  for ($i = 0; $i < $breite; $i++) {
            
    
    $index = $image->getPixel($i,$j);
    # Angabe erfolgt in x,y - was gefühlt falsch ist, wenn man von Reihen und Spalten ausgeht
    
    ($red,$green,$blue) = $image->rgb($index);
    
    $hexval = sprintf('%.6X', $index);
    $hexred = sprintf('%.2X', $red);
    $hexgreen = sprintf('%.2X', $green);
    $hexblue = sprintf('%.2X', $blue);
    $hexcode = $hexred.$hexgreen.$hexblue;
    
    $match = 0;
    for ($m = 0; $m < $lastcfg; $m++) {
    
    	if ($hexcode eq $cfgval[$m][1]) {
    		print "($j,$i) Farbcode $hexcode - ".$cfgval[$m][9]."\n";
    		if ($cfgval[$m][2] eq "A") {
    			print "Asti Typ ".$cfgval[$m][3]." setzen\n";
    			print $out "INSERT INTO bases (x ,y ,system ,klasse,width,height,maxtiles,maxe,maxcargo,cargo)VALUES ($i,$j,$system_number,".$cfgval[$m][3].",".$cfgval[$m][4].",".$cfgval[$m][5].",".$cfgval[$m][6].",".$cfgval[$m][7].",".$cfgval[$m][8].",\"0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,\");\n";
    			# $query = "INSERT INTO bases (x ,y ,system ,klasse,width,height,maxtiles,maxe,maxcargo,cargo)VALUES ($i,$j,$system_number,".$cfgval[$m][3].",".$cfgval[$m][4].",".$cfgval[$m][5].",".$cfgval[$m][6].",".$cfgval[$m][7].",".$cfgval[$m][8].",\"0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,\");";
    			# $sth = $dbh->prepare ($query);
          # $sth->execute();
          }
    		if ($cfgval[$m][2] eq "N") {
    			print "Nebel Typ ".$cfgval[$m][3]." setzen\n";
    			print $out "INSERT INTO nebel (x ,y ,system ,type)VALUES ($i,$j,$system_number,".$cfgval[$m][3].");\n";
    			# $query = "INSERT INTO nebel (x ,y ,system ,type)VALUES ($i,$j,$system_number,".$cfgval[$m][3].");";
    			# $sth = $dbh->prepare ($query);
          # $sth->execute();
    		}
    		$match++;
    	}
    
    }
    
    if ($match == 0) {
    		print "Fehler bei ($j,$i) Farbcode $hexcode - unbekannt\n";
    	}
    
    
    
    
    
    
    print "-------------------------------------------\n";	
    
  }
}
close $out;
close IN;