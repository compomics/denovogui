

#!/usr/bin/perl
use strict;
use warnings;
use subs;
use POSIX;
use Getopt::Std;

my $BLAST_DIR      = "~/BLAST";
my $BLASTP		    = "$BLAST_DIR/blastp";
my $MAX_SEQLEN      = 100000000;
my $MIN_DECOY_RATIO = 0.125;
my $TARGET_FDR		= 0.05;


my (%user_opts,@additional_BLAST_opts);

## default values
$user_opts{'m'}='blosum62';
$user_opts{'R'}=25000;
$user_opts{'S'}=30;
$user_opts{'E'}=500000;
$user_opts{'f'}='seg';

$user_opts{'C'}='XXX';
$user_opts{'F'}=$TARGET_FDR;
	
### READ OPTIONS
usage() if (@ARGV<2);
usage() unless getopts('Q:D:m:O:V:B:S:f:h:E:a:LF:X:R:', \%user_opts);

if (defined($user_opts{'X'}))
{
	$BLAST_DIR = $user_opts{'X'};
	$BLASTP	   = "$BLAST_DIR/blastp";
}

die "Could not find BLAST dir: $BLAST_DIR\nPlease set the variable \$BLAST_DIR to the correct path!"
	if (! -d $BLAST_DIR);	
die "Could not find blastp: $BLASTP\nPlease set the variable $BLASTP to the correct path!\n"
	if (! -f $BLASTP);

die "Must provide valid query file path (-Q)\n" 
	if (! defined($user_opts{'L'}) && (! defined($user_opts{'Q'}) || ! -f $user_opts{'Q'}));
die "Must provide valid db file path (-D)\n"
	if (! defined($user_opts{'D'}));
die "Must provide output path (-O)\n"
	if (! defined($user_opts{'O'}));
	
$user_opts{'h'} = $user_opts{'R'} if (! defined($user_opts{'h'}));
$user_opts{'V'} = $user_opts{'R'} if (! defined($user_opts{'V'}));
$user_opts{'B'} = $user_opts{'R'} if (! defined($user_opts{'B'}));

if (defined($user_opts{'F'}))
{
	die "Must supply 0<=FDR<0.5!\n" if ($user_opts{'F'}<0.0 || $user_opts{'F'}>=0.5);
	$TARGET_FDR = $user_opts{'F'};
	
}
	
	
my $db_path = $user_opts{'D'};
my $out_path = $user_opts{'O'};
$out_path .= "output" if ($out_path =~ /\/$/);

my ($blast_cmd,$result_path,$query_path);
if (defined($user_opts{'L'}))
{
	$query_path   = $out_path."_final.query";
	$result_path  = $out_path."_final.res";
	&make_final_query($out_path);
	my $blast_params = &make_blast_params(\%user_opts);
	$blast_cmd = "$BLASTP $db_path $query_path $blast_params > $result_path";
	$out_path .= "_final";
}
else
{
	$query_path  = $out_path.".query";
	$result_path	= $out_path.".res";
	
	&process_query_seq($user_opts{'Q'}, $query_path);
	my $blast_params = &make_blast_params(\%user_opts);
	$blast_cmd = "$BLASTP $db_path $query_path $blast_params > $result_path";
}

system("uname -a");
print "\n",&get_now_str(),"\nCOMMAND: $blast_cmd\n";
my $blast_ret = 0;
$blast_ret=system($blast_cmd); # if (! -f $result_path);
die "BLAST returned value: $blast_ret, see $result_path for details.\n" if ($blast_ret !=0);
print "\n",&get_now_str(),"\n";
print "\nParsing results...\n";
&parse_blastp_res($out_path, $user_opts{'C'});
print "\nDone.  ";
print &get_now_str(),"\n";

exit(0);

	

sub usage {
print "\nPepNovo for MS-BLAST v2.0

Developed by Ari Frank		                <arf\@cs.ucsd.edu>
Based on code for MS-Blast by Ivan Adzhubey.

Usage: pnv_msblast.pl <options>

Required:
---------
	-Q    Query file path (already formatted for Blast)
	-D    DB file path	  (already formated for Blast)
	-O    output path     (dir + stub)
	
Final round:
------------
	-L    If queires are split (due to large size), you should run a final last Blast run to consolidate the results. Use the -L flag (with no argument) instead of the -Q flag.
	
Optional:
---------
	-R    maximum number of results for BLAST search (sets -h, -V, -B, to this value), default -R=$user_opts{'R'}
	-E    E (Expect) paramater, default E=$user_opts{'E'}
	-h    hspmax for BLAST, default h=$user_opts{'R'}
	-V    V parameter for blastp (number of descriptions), default V=$user_opts{'R'}
	-B    B parameter for blastp (number of alignments), default B=$user_opts{'R'}
	-S    S parameter for blastp, default S=$user_opts{'S'}
	-m    matrix name (from options installed by BLAST in \$BLAST_DIR/matrix (default $user_opts{'m'})
	-f    filter for BLAST runs, default=$user_opts{'f'}, (typical options: seg, xnu, seg+xnu, etc.)
	-C    decoy protein name prefix (default $user_opts{'C'})
	-F    FDR level for protein ids (default $user_opts{'F'});
	-a    additional BLAST parameter, use format NAME1=value1,NAME2=values2,...
	
See Blast help page for more details about the various parameters:
http://blast.advbiocomp.com/doc/parameters.html

IMPORTANT: if the script complains about to few decoy hits you should increase the -E (and possibly -R) parameters (e.g., double their values).

For questions or bug reports contact Ari Frank: arf\@cs.ucsd.edu

";
exit -1;
}

sub get_now_str {
    my $now_string = strftime "%a %b %e %H:%M:%S %Y", localtime;
    return $now_string;
}

sub process_query_seq {
	my $seq_path = shift;
	my $qfile	 = shift;
	
	die "Missing query file path!\n" if (! defined($seq_path) || ! -e $seq_path);
	die "Missing path to tmp processed query\n" if (! defined($qfile));
	
	open(SEQ, $seq_path) || die "Can't open sequence file: $seq_path\n";
	my $all_seq;
	{ local $/; $all_seq = <SEQ>; }
	close(SEQ);
	die "Empty sequence file" unless length $all_seq;
	
	my $length = 0;
	my $seq='';
	if ($all_seq =~ /\w+/) 
	{
		my @seqs = split /\n/, $all_seq;
		$seq = '';

		if ($seqs[0] =~ /\s*\>/) 
		{
			$seq = $seqs[0] . "\n";
			shift @seqs;

			grep s/[\d]//g,@seqs;
			grep s/\*/1/g,@seqs;
			grep s/\-/45/g,@seqs;
			grep s/\W//g, @seqs;
			grep s/45/\-/g,@seqs;
			grep s/1/\*/g,@seqs;


			$length = length(join '', @seqs);
			$seq .= join "\n",@seqs;
		}
		else 
		{
			$seq = ">query_sequence\n";

			grep s/[\d]//g,@seqs;
			grep s/\*/1/g,@seqs;
			grep s/\-/45/g,@seqs;
			grep s/\W//g, @seqs;
			grep s/45/\-/g,@seqs;
			grep s/1/\*/g,@seqs;

			$length = length(join '', @seqs);
			$seq .= join "\n", @seqs;
			$seq .= "\n";
		}
	}
	die "Query length is $length and that exceeds the length limit of $MAX_SEQLEN!\n"
		if ($length > $MAX_SEQLEN);
		

	open(QFILE,">$qfile") || die "Could not open query file for writing: $qfile [$!]\n";
	print QFILE $seq;
	close QFILE;
	
	return $qfile;
}

sub make_final_query {
	my $out_path = shift;
	my ($dir,$name)=("","");
	if ($out_path =~ /^(\S+)[\/\\]([^\/\\]+)$/)
	{
		$dir = $1;
		$name = $2;
	}
	else
	{
		$dir  = ".";
		$name = $out_path;
	}
	
	opendir(DIR,$dir) || die "Couldn't open dir $dir : $!\n";
	my @pep_files = grep {/^$name\_pt\_\d+.*peps$/} readdir DIR;
	closedir DIR;
	
	die "No peprtide summary files (.peps) where found starting with $name in directory $dir !\n" if (@pep_files == 0);
	print "Making final query from ",scalar(@pep_files)," summary files.\n";
	
	my %all_peps;
	foreach my $pep (sort @pep_files)
	{
		print $pep," ... \t";
		open(PEP,"$dir/$pep") || die;
		while (my $line=<PEP>)
		{
			last if ($line =~ /^PEPTIDE\s+SUMMARY/);
		}
		
		my $n=0;
		while (my $line = <PEP>)
		{
			next if ($line !~ /^\d+\t\d+\t\S+\t(\S+)/);
			$all_peps{$1}++;
			$n++;
		}
		close PEP;
		print " $n\tpeptides\n";
	}
	
	my $final_path = $out_path."_final.query";
	my $np = scalar(keys %all_peps);
	die "No peptides where read!\n" if ($np<=0);
	print "\nWriting final query to $final_path with $np peptides\n";
	open(FINAL,">$final_path") || die "Could not open query for writing $final_path : $!\n";
	print FINAL ">query_sequence\n";

	my $c=0;
	foreach my $p (sort keys %all_peps)
	{
		print FINAL "-$p";
		if ($c++ >6)
		{
			$c=0;
			print FINAL "\n";
		}
	}
	close FINAL;
}

sub make_blast_params {
	# blast command line options hash
	#   keys:
	#     'caps' for 'B=NNN' options
	#     'dash' for '-opt=NNN' options
	my $r_user_opts     = shift;
	
	my %opts;
	$opts{caps}{E}      = $$r_user_opts{'E'};
	$opts{caps}{S}      = $$r_user_opts{'S'};
	$opts{caps}{V}      = $$r_user_opts{'V'};
	$opts{caps}{B}      = $$r_user_opts{'B'};
	
	$opts{dash}{-matrix}     = $$r_user_opts{'m'};
	$opts{dash}{-filter}     = $$r_user_opts{'f'};
	
	# other opts that are default behaviour
	#$opts{dash}{-echofilter} = '';
	$opts{dash}{-stats}      = '';
	$opts{dash}{-qtype}      = '';
	$opts{dash}{-qres}       = '';
	$opts{dash}{-nogaps}	 = '';
	$opts{dash}{-hspmax}	 = $$r_user_opts{'h'};
	$opts{dash}{-sort_by_totalscore} = '';
	$opts{dash}{-span1}				 = '';
	
	if (defined($$r_user_opts{'a'}))
	{
		$$r_user_opts{'a'} =~ s/^\s+//;
		$$r_user_opts{'a'} =~ s/\s+$//;
	}
	
	if (defined($user_opts{'a'}) && $$r_user_opts{'a'})
	{
		# List (hash) of AB-BLAST options that DO NOT take values
		my %toggles = (
		nogaps=>1, sort_by_totalscore=>1, span1=>1, consistency=>1,
		echofilter=>1, errors=>1, evalues=>1, gaps=>1, gapall=>1, gi=>1,
		kap=>1, lcfilter=>1, lcmask=>1, links=>1, nogaps=>1, nosegs=>1,
		noseqs=>1, notes=>1, poissonp=>1, postsw=>1, prune=>1, pvalues=>1,
		qres=>1, qtype=>1, restest=>1, seqtest=>1, shortqueryok=>1,
		sort_by_count=>1, sort_by_highscore=>1, sort_by_pvalue=>1,
		sort_by_subjectlength=>1, span=>1, span2=>1, stats=>1, sump=>1,
		top=>1, warnings=>1, wstrict=>1, xmlcompact=>1,
		);
		
		my @more_opts = split /\,/, $$r_user_opts{'a'};
		while (@more_opts) 
		{
			my $token = shift @more_opts;
			my $dash = 0;
			$dash = 1 if $token =~ s/^-//; # get rid of the leading dash
			my ($opt, $val);
			# This option does not take a value
			if ($toggles{$token}) 
			{
				$opt = $token; $val = '';
			# Value is mandatory for this option
			} 
			else 
			{
				# Do we have a '=' separator?
				($opt, $val) = split /=/, $token;
				# No equal sign separator, shift to next token
				$val = shift @more_opts unless defined $val;
				die "Mandatory value missing for option: $opt\n" 
					unless defined $val && length $val;
			}
	
			$val = '' unless defined $val;
			die "Illegal option name: $opt\n" if $opt =~ /\W/;
			die "Illegal option value: $val\n" if $val =~ /\W/;
			if ($dash) 
			{
			  $opts{dash}{'-'.$opt} = $val;
			} 
			else 
			{
			  $opts{caps}{$opt} = $val;
			}
		}
	}

	my $blast_params = '';
	foreach my $o (sort keys %{ $opts{caps} }) 
	{
	  $blast_params .= ' ' . uc($o);
	  $blast_params .= '=' . $opts{caps}{$o} if length($opts{caps}{$o});
	}
	foreach my $o (sort keys %{ $opts{dash} }) 
	{
	  $blast_params .= ' ' . lc($o);
	  $blast_params .= '=' . $opts{dash}{$o} if length($opts{dash}{$o});
	}	
	return $blast_params;
}

sub parse_blastp_res {
	my $output_stub  = shift;
	my $decoy_prefix = shift;
	
	my $res_file	 = $output_stub.".res";
	my $prot_file	 = $output_stub.".prots";
	my $pep_file	 = $output_stub.".peps";
	my $group_file	 = $output_stub.".groups";
	my $align_file   = $output_stub.".align";
	
	open(RES,$res_file) || die;
	my @res_lines;
	my $line;
	while ($line=<RES>)
	{
		last if ($line =~ /^Sequences producing High-scoring Segment Pairs/);
	}
	die "Did not find \"Sequences producing High-scoring Segment Pairs\"\n"
		if ($line !~ /^Sequences producing High-scoring Segment Pairs/);
		
	while (my $line=<RES>)
	{
		push @res_lines,$line;
	}
	close RES;
	
	my (@protein_results,@prot_scores,@pep_scores, %all_peps);
	my ($n_forward_peps,$n_decoy_peps)=(0,0);
	
	for (my $idx=0; $idx<@res_lines; $idx++)
	{
		my $prot_line = "";
		next if ($res_lines[$idx] !~ /^\>(.*)/);
		my $prot   = $1;
		my $length = 0;
		my $is_good_prot = 1;
		$is_good_prot = 0 if ($prot =~ /$decoy_prefix/);
		$prot_line .= $res_lines[$idx++];
		for ( ; $idx<@res_lines; $idx++)
		{
			$prot_line .= $res_lines[$idx];
			next if ($res_lines[$idx] !~ /Length\s*=\s*(\d+)/);
			$length = $1;
			last;
		}
		
		my $total_score = 0;
		my @query_peps;
		for ( ; $idx<@res_lines; $idx++)
		{
			if ($res_lines[$idx] =~ /^\>/)
			{
				$idx--;
				last;
			}
			
			my ($score,$pep,$match,$subj)=(-1,"","","");
			next if ($res_lines[$idx] !~ /^\s*Score\s+=\s+(\d+)/);
			$score = $1;
			$total_score+=$score;
			$match .= $res_lines[$idx++];
			
			die "Bad line, missing \"Identities\"\n" if ($res_lines[$idx] !~ /^\s*Identities/);
			$match .= $res_lines[$idx++];
			$match .= $res_lines[$idx++];
			die "Missing Query\n" if ($res_lines[$idx] !~ /^Query:\s*\d+\s+(\S+)/);
			$pep = $1;
			die "Bad peptide in alignment\n" if (length($pep)<3);
			$match .= $res_lines[$idx++];
			$match .= $res_lines[$idx++];
			
			die "Missing Subject\n" if ($res_lines[$idx] !~ /^Sbjct:\s*\d+\s+(\S+)/);
			$subj=$1;
			
			$match .= $res_lines[$idx++];
			push @query_peps,[$pep,$score,$match];
			$all_peps{$pep}=[$score,scalar(@protein_results),$subj] if (! defined($all_peps{$pep}) || $score>${$all_peps{$pep}}[0]);
		}
		
		my $max_pep_score = -1000;
		foreach my $p (@query_peps)
		{
			push @pep_scores,[$$p[1],$is_good_prot];
			$max_pep_score=$$p[1] if ($$p[1]>$max_pep_score);
		}
		
		push @protein_results,{name => $prot, prot_line => $prot_line, score => $total_score, peptides => \@query_peps, is_good => $is_good_prot, max_pep_score => $max_pep_score};	
	#	
		
		if ($is_good_prot)
		{
			$n_forward_peps += scalar(@query_peps);
		}
		else
		{
			$n_decoy_peps += scalar(@query_peps);
		}
	}
	
	my $sum_peps =  $n_decoy_peps+$n_forward_peps;
	die "No peptide results parsed from $res_file\n" if ($sum_peps <=0);
	my $ratio = $n_decoy_peps/$sum_peps;
	print "Read resuls for ",scalar(@protein_results)," proteins.\n";
	print "Found $n_decoy_peps/$sum_peps hits to decoy.\n";
	
	
	my $r_pep_fdr_hash  = &make_fdr_hash(\@pep_scores);
	
	## removing results of bad peptides (only incude peptides with fdr at least TARGET_FDR)
	foreach my $r (@protein_results)
	{
		my @new_query_peps;
		my $total = 0;
		my $max_pep_score = 0;
		foreach my $p (@{$$r{peptides}})
		{
			my $score = $$p[1];
			if ($$r_pep_fdr_hash{$score}<=$TARGET_FDR)
			{
				push @new_query_peps,$p;
				$total += $score;
				$max_pep_score = $score if ($$p[1]>$max_pep_score);
			}
		}
		my $num_peps = scalar(@new_query_peps);
		my $adj_score = $total - ($num_peps-1)*35;
		$adj_score -= ($num_peps - 5)*3 if ($num_peps>5);
		$$r{peptides}=\@new_query_peps;
		$$r{score}=$total;
		$$r{adj_score}=$adj_score;
		$$r{max_pep_score}=$max_pep_score;
		push @prot_scores,[$adj_score,$$r{is_good},$max_pep_score];
	}
	
	my $r_prot_fdr_hash = &make_fdr_hash(\@prot_scores, $r_pep_fdr_hash);
	
	open (PROT,">$prot_file") || die "Could not open summary for writing $prot_file: $!\n";
	open (PEP,">$pep_file") || die "Could not open summary for writing $pep_file: $!\n";
	open (GROUP,">$group_file") || die "Could not open summary for writing $group_file: $!\n";
	if ($ratio < $MIN_DECOY_RATIO)
	{
		my $msg="\n### WARNING: too few hits were made to decoy database! It is suggested rerun BLAST with increased E-value (-E X, X>$user_opts{'E'}) ####\n\n";
		print $msg;
		print PROT $msg;
		print PEP  $msg;
		print GROUP $msg;
	}
	
	open(ALIGN,">$align_file") || die;
	
	print PROT "#PROTEIN SUMMARY:\n";
	print PROT "#idx\t#peps\tfdr\tAScore\tScore\tProtein\n";
	my $c=0;
	my $gp_counter=0;
	my %peptide_group_hash;
	my @groups;
	foreach my $r (sort {$$b{adj_score} <=> $$a{adj_score}} @protein_results)
	{
		## make sure that protein has at least 1 peptide with high enough score (i.e., FDR below $TARGET_FDR) ##
		my $max_score = 0;
		
		## check where these peptides were previously assigned (in what protein groups)
		my %prev_assigns;
		foreach my $p (@{$$r{peptides}})
		{
			$max_score = $$p[1] if ($$p[1]>$max_score);
			if (defined($peptide_group_hash{$$p[0]}))
			{
				foreach my $idx (@{$peptide_group_hash{$$p[0]}})
				{
					$prev_assigns{$idx}++;
				}
			}
		}
		my $num_peptides = scalar(@{$$r{peptides}});
		
		my $assign_group=-1;
		my $max_num_peps_assigned=0;
		foreach my $idx (sort keys %prev_assigns)
		{
			if ($prev_assigns{$idx}>$max_num_peps_assigned)
			{
				$assign_group=$idx;
				$max_num_peps_assigned = $prev_assigns{$idx};
			}
		}
		
		# assign a protein to a gorup if 50\% or more of its peptides are assigned to that group
		if (0.5*$num_peptides<=$max_num_peps_assigned)
		{
			push @{$groups[$assign_group]},$r;
		}
		else
		{
			push @groups,[$r]; ## new group
			my $group_idx = scalar(@groups)-1;
			foreach my $p (@{$$r{peptides}})
			{
				push @{$peptide_group_hash{$$p[0]}},$group_idx;
			}
		}

		next if ($$r_pep_fdr_hash{$max_score}>$TARGET_FDR);
		my $prot_fdr = $$r_prot_fdr_hash{$$r{adj_score}};
		$gp_counter++ if ($prot_fdr<= $TARGET_FDR);
		$$r{fdr}=$prot_fdr;
		printf PROT "%d\t%d\t%.4f\t%d\t%d\t%s\n",++$c,scalar(@{$$r{peptides}}),$prot_fdr,$$r{adj_score},$$r{score},$$r{name};
		
		print ALIGN "==================================================================\n";
		printf ALIGN "PROTEIN $c\t(FDR %.4f)\t",$prot_fdr;
		print ALIGN  "Peptides: ",scalar(@{$$r{peptides}}),"\tAdjusted score: $$r{adj_score}\t Score: $$r{score}\n";
		print ALIGN $$r{prot_line},"\n";
		my $cc=0;
		foreach my $p (@{$$r{peptides}})
		{
			$cc++;
			my $m = $$p[2];
			my $f = sprintf("%.4f",$$r_pep_fdr_hash{$$p[1]});
			$m =~ s/bits\)/bits\),\tfdr = $f/;
			print ALIGN "$c.$cc\t$m\n";
		}
		print ALIGN "\n\n";
	}
	
	## write protein groups
	print GROUP "Protein group summary:\n";
	print GROUP "#Group_idx\t\#Prots_in_group\t#Peps\tFDR\tAScore\tScore\tTop_Protein\tAll_protein_accesions\n";
	$c=0;
	foreach my $g (@groups)
	{
		my $r = $$g[0];
		printf GROUP "%d\t%d\t%d\t%.4f\t%d\t%d\t%s\t",++$c,scalar(@{$g}),scalar(@{$$r{peptides}}),$$r{fdr},$$r{adj_score},$$r{score},$$r{name};
		my $first=1;
		foreach my $p (@{$g})
		{
			$$p{name} =~ /^([^\|]+)/;
			my $code = $1;
			print GROUP "," if (! $first);
			print GROUP "$code";
			$first=0;
		}
		print GROUP "\n";
	}
	
	print "Found $gp_counter proteins with fdr below $TARGET_FDR\n";
	print "These proteins belong to ",scalar(@groups)," groups.\n";
	print PEP "\nPEPTIDE SUMMARY:\n";
	print PEP "#idx\tscore\tfdr\tDenovoSequence\tProtSequence\tProtein\n";
	$c=0;
	my $max_score = 0;
	foreach my $pep (sort {${$all_peps{$b}}[0] <=> ${$all_peps{$a}}[0]} keys %all_peps)
	{
		my $score = ${$all_peps{$pep}}[0];
		$max_score=$score if ($max_score==0 && $$r_pep_fdr_hash{$score}>2*$TARGET_FDR);
		last if ($score<$max_score);
		print  PEP ++$c,"\t$score\t";
		printf PEP "%.3f\t",$$r_pep_fdr_hash{$score};
		print  PEP $pep,"\t",${$all_peps{$pep}}[2],"\t",
						${$protein_results[${$all_peps{$pep}}[1]]}{name},"\n";
	}
	print "Found $c peptides with fdr below $TARGET_FDR\n";
	close PROT;
	close PEP;
	close ALIGN;
	
	print "\nWrote following result files:\n";
	print   "-----------------------------\n";
	print "All proteins     - $prot_file\n";
	print "Grouped proteins - $group_file\n";
	print "All peptides     - $pep_file\n";
	print "All alignments   - $align_file\n";
}

sub make_fdr_hash {
	my $r_scores     = shift;
	my $r_pep_fdr_hash = shift;	
	
	my @sorted_scores = sort {$$b[0] <=> $$a[0]} @{$r_scores};
	my %fdr_hash;
	
	my ($n_good,$n_bad)=(0,0);
	
	my $last_fdr=0.0;
	for (my $i=0; $i<@sorted_scores; $i++)
	{
		## if these are FDR for proteins then ignore any protein that doesn't have 
		## at least one strong peptide
		next if (defined($r_pep_fdr_hash) && 
				 $$r_pep_fdr_hash{${$sorted_scores[$i]}[2]} > $TARGET_FDR);
		if (${$sorted_scores[$i]}[1] == 1)
		{
			$n_good++;
		}
		else
		{
			$n_bad++;
		}
		
		# make sure FDR is non decreasing
		$fdr_hash{${$sorted_scores[$i]}[0]} = $n_bad/($n_good+$n_bad);
		$fdr_hash{${$sorted_scores[$i]}[0]} = $last_fdr if ($last_fdr>$fdr_hash{${$sorted_scores[$i]}[0]});
		$last_fdr = $fdr_hash{${$sorted_scores[$i]}[0]};
	}
	
	# fill in missing scores into fdr hash (and use min local fdr)
	my $max_score = ${$sorted_scores[0]}[0];
	my $min_fdr = 1.0;
	for (my $score=0; $score<$max_score; $score++)
	{
		if (defined($fdr_hash{$score}) && $min_fdr>$fdr_hash{$score})
		{
			$min_fdr = $fdr_hash{$score};
		}
		else
		{
			$fdr_hash{$score}=$min_fdr;
	#		print "ND: $score ==> $fdr\n";
		}
		
	#	print "$score --> $fdr_hash{$score}\n";
	}
	
	
	return \%fdr_hash;
}














