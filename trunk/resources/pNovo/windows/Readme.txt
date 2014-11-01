pNovo+ v1.3 de novo sequencing for MS/MS. 

Programmed by Chen Haifeng and Hao Chi, please send any questions or bug reports to chihao@ict.ac.cn or smhe@ict.ac.cn.

The readme file covers the following topics:
---------------------------------------------
1. Running pNovo

2. Key parameter settings
	
3. Result format

4. Citation(s)
---------------------------------------------



1. Running pNovo
---------------
Windows: input 'pNovoplus.exe parameter_file_name [folder_name]' in the command line.

REQUIRED:
parameter_file_name: a file containing all parameters needed in pNovo+.
OPTIONAL:
folder_name: The program will create a folder named folder_name to which the result files will be output. The default value is 'pnovo'.

2. Key parameter settings

	please see the details in param.txt

3. Result format
------------------
	In the result file(.TXT), a list is generated for each spectrum as follows:
	
	S1	TEST1.dta	
	P1	AHAAHVK	2.53875	
	P2	AHAAVHK	2.43762	
	P3	HAAAHVK	1.87265		
	
	S2	TEST2.dta
	
	S3	TEST3.dta
	P1	SEGLTPR	4.82763	
	P2	DTGLTPR	4.82763	
	P3	ESGLTPR	4.82763	
	P4	TDGLTPR	4.82763	
	P5	TDAVTPR	4.82763	

	The peptides are sorted in descending order by their scores.
	
4. Citation(s)
------------------

1) pNovo+: De Novo Peptide Sequencing Using Complementary HCD and ETD Tandem Mass Spectra. Hao Chi, Hai-Feng Chen, Kun He, Long Wu, Bing Yang, Rui-Xiang Sun, Jian-Yun Liu, Wen-Feng Zeng, Chun-Qing Song, Si-Min He, Meng-Qiu Dong. Journal of Proteome Research, DOI: 10.1021/pr3006843, 2012.