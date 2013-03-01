#!/usr/bin/perl
use strict;
use warnings;
use subs;


if (! @ARGV)
{
	print "Takes several fasta files and creates a single fasta file with a shuffled decoy appended to it.\n\n";
	print "create_fasta_with_decoy.pl [fasta 1] ... [fasta n]  >  my_new_fasta.fa\n\n";
	exit(0);
}

my $tmp_name = "TTfa.$$";
open(TMP,">$tmp_name") || die;
foreach my $fa (@ARGV)
{
	next if (! -e $fa);
	open(FASTA,$fa) || die "Could not open fasta file: $fa\n";
    my $prot=0;
    while (my $line=<FASTA>)
    {
		print $line;
        last if ($line =~ /^\>/);
    }

    while (! eof(FASTA))
    {
        my $seq="";
        my $eof=0;
        while (my $line = <FASTA>)
        {
			print $line;
            last if ($line =~ /^\>/);
            $line =~ /([A-Z]+)/;
            $seq .= $1;
        }
        my $len = length($seq);
		my @arr = ($seq =~/\S/g);
		&fisher_yates_shuffle(\@arr);
		print TMP ">XXX_$prot\n";
		my $c=0;
		for (my $i=0; $i<@arr; $i++)
		{
			$c++;
			if ($c == 80)
			{
				print TMP "\n";
				$c=0;
			}
			print TMP $arr[$i];
		}
        $prot++;
		print TMP "\n";
    }
    close FASTA;
}
close TMP;

open(TMP,$tmp_name) || die;
while (my $line=<TMP>)
{
	print $line;
}
close TMP;
unlink $tmp_name;
exit(0);

# fisher_yates_shuffle( \@array ) : generate a random permutation
# of @array in place
sub fisher_yates_shuffle {
    my $array = shift;
    my $i;
    for ($i = @$array; --$i; ) {
        my $j = int rand ($i+1);
        next if $i == $j;
        @$array[$i,$j] = @$array[$j,$i];
    }
}

