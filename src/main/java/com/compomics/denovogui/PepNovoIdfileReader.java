package com.compomics.denovogui;

import com.compomics.util.Util;
import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.personalization.ExperimentObject;
import com.compomics.util.gui.waiting.WaitingHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import uk.ac.ebi.pride.tools.braf.BufferedRandomAccessFile;

/**
 * This class can be used to parse PepNovo identification files.
 *
 * @author Marc Vaudel
 */
public class PepNovoIdfileReader extends ExperimentObject implements IdfileReader {

    /**
     * A map of all spectrum titles and the associated index in the random
     * access file.
     */
    private HashMap<String, Long> index;
    /**
     * The result file in random access.
     */
    private BufferedRandomAccessFile bufferedRandomAccessFile = null;
    /**
     * The name of the result file.
     */
    private String fileName;
    /**
     * The standard format.
     */
    public static final String tableHeader = "#Index	RnkScr	PnvScr	N-Gap	C-Gap	[M+H]	Charge	Sequence";
    /**
     * The minimum PepNovo score.
     */
    private double minPepNovoScore = Double.MAX_VALUE;
    /**
     * The maximum PepNovo score.
     */
    private double maxPepNovoScore = Double.MIN_VALUE;
    /**
     * The minimum rank score.
     */
    private double minRankScore = Double.MAX_VALUE;
    /**
     * The maximum rank score.
     */
    private double maxRankScore = Double.MIN_VALUE;
    /**
     * The minimum N-terminal gap.
     */
    private double minNGap = Double.MAX_VALUE;
    /**
     * The maximum N-terminal gap.
     */
    private double maxNGap = Double.MIN_VALUE;
    /**
     * The minimum C-terminal gap.
     */
    private double minCGap = Double.MAX_VALUE;
    /**
     * The maximum C-terminal gap.
     */
    private double maxCGap = Double.MIN_VALUE;
    /**
     * The minimum charge.
     */
    private int minCharge = Integer.MAX_VALUE;
    /**
     * The maximum charge.
     */
    private int maxCharge = Integer.MIN_VALUE;
    /**
     * The minimum m/z value.
     */
    private double minMz = Double.MAX_VALUE;
    /**
     * The maximum m/z value.
     */
    private double maxMz = Double.MIN_VALUE;

    /**
     * Default constructor for the purpose of instantiation.
     */
    public PepNovoIdfileReader() {
    }

    /**
     * Constructor, initiate the parser. The close() method shall be used when
     * the file reader is no longer used.
     *
     * @param identificationFile the identification file to parse
     * @throws FileNotFoundException exception thrown whenever the provided file
     * was not found
     * @throws IOException exception thrown whenever an error occurred while
     * reading the file
     */
    public PepNovoIdfileReader(File identificationFile) throws FileNotFoundException, IOException {
        this(identificationFile, null);
    }

    /**
     * Constructor, initiate the parser. Displays the progress using the waiting
     * handler The close() method shall be used when the file reader is no
     * longer used.
     *
     * @param identificationFile the identification file to parse
     * @param waitingHandler a waiting handler providing progress feedback to
     * the user
     * @throws FileNotFoundException exception thrown whenever the provided file
     * was not found
     * @throws IOException exception thrown whenever an error occurred while
     * reading the file
     */
    public PepNovoIdfileReader(File identificationFile, WaitingHandler waitingHandler) throws FileNotFoundException, IOException {

        bufferedRandomAccessFile = new BufferedRandomAccessFile(identificationFile, "r", 1024 * 100);

        fileName = Util.getFileName(identificationFile);

        if (waitingHandler != null) {
            waitingHandler.setMaxSecondaryProgressValue(100);
        }

        long progressUnit = bufferedRandomAccessFile.length() / 100;

        index = new HashMap<String, Long>();

        String line, spectrumTitle;
        while ((line = bufferedRandomAccessFile.readLine()) != null) {
            if (line.startsWith(">>")) {
                long currentIndex = bufferedRandomAccessFile.getFilePointer();

                String[] temp = line.split("\\s+");
                String formatted = "";
                for (int i = 3; i < temp.length; i++) {
                    formatted += (temp[i] + " ");
                }
                int endIndex = formatted.lastIndexOf("#Problem");
                if (endIndex == -1) {
                    endIndex = formatted.lastIndexOf("(SQS");
                }
                
                // Condition: Skip problematic spectra not containing (SQS) at the end of the line.
                if(endIndex > -1){
                    spectrumTitle = formatted.substring(0, endIndex).trim();
                    index.put(spectrumTitle, currentIndex);                                  
                } else {                    
                }
                
                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }
                    waitingHandler.setSecondaryProgressValue((int) (currentIndex / progressUnit));
                }
            }
        }
    }

    @Override
    public HashSet<SpectrumMatch> getAllSpectrumMatches(WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, Exception {

        if (bufferedRandomAccessFile == null) {
            throw new IllegalStateException("The identification file was not set. Please use the appropriate constructor.");
        }

        HashSet<SpectrumMatch> spectrumMatches = new HashSet<SpectrumMatch>();

        if (waitingHandler != null) {
            waitingHandler.setMaxSecondaryProgressValue(index.size());
        }

        for (String title : index.keySet()) {

            String decodedTitle = URLDecoder.decode(title, "utf-8");
            SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(getMgfFileName(), decodedTitle));

            int cpt = 1;
            bufferedRandomAccessFile.seek(index.get(title));
            String line = bufferedRandomAccessFile.getNextLine();       
            boolean solutionsFound = true;
            if (line.startsWith("# No") || line.startsWith("# Charge") || line.startsWith("#Problem") || line.startsWith("# too")) {
               solutionsFound = false;
            } else if (!line.equals(tableHeader)) {
                System.out.println("line: " + line);
                throw new IllegalArgumentException("Unrecognized table format. Expected: \"" + tableHeader + "\", found:\"" + line + "\".");
            }

            while ((line = bufferedRandomAccessFile.getNextLine()) != null
                    && !line.equals("") && !line.startsWith(">>")) {
                currentMatch.addHit(Advocate.PEPNOVO, getAssumptionFromLine(line, cpt));
                cpt++;
            }
            if(solutionsFound){
                spectrumMatches.add(currentMatch);
            } 
            
            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    break;
                }
                waitingHandler.increaseSecondaryProgressValue();
            }
        }

        return spectrumMatches;
    }

    /**
     * Returns the spectrum file name. This method assumes that the pepnovo
     * output file is the mgf file name + ".out"
     *
     * @return the spectrum file name
     */
    public String getMgfFileName() {
        return fileName.substring(0, fileName.length() - 4);
    }

    @Override
    public String getExtension() {
        return ".out";
    }

    @Override
    public void close() throws IOException {
        bufferedRandomAccessFile.close();
    }

    /**
     * Returns a Peptide Assumption from a pep novo result line. the rank score
     * is taken as reference score. All additional parameters are attached as
     * PeptideAssumptionDetails.
     *
     * @param line the line to parse
     * @param rank the rank of the assumption
     * @return the corresponding assumption
     */
    private PeptideAssumption getAssumptionFromLine(String line, int rank) {

        String[] lineComponents = line.trim().split("\t");

        Double rankScore = new Double(lineComponents[1]);
        if (rankScore < minRankScore) {
            minRankScore = rankScore;
        }
        if (rankScore > maxRankScore) {
            maxRankScore = rankScore;
        }
        Double pepNovoScore = new Double(lineComponents[2]);
        if (pepNovoScore < minPepNovoScore) {
            minPepNovoScore = pepNovoScore;
        }
        if (pepNovoScore > maxPepNovoScore) {
            maxPepNovoScore = pepNovoScore;
        }
        Double nGap = new Double(lineComponents[3]);
        if (nGap < minNGap) {
            minNGap = nGap;
        }
        if (nGap > maxNGap) {
            maxNGap = nGap;
        }
        Double cGap = new Double(lineComponents[4]);
        if (cGap < minCGap) {
            minCGap = cGap;
        }
        if (cGap > maxCGap) {
            maxCGap = cGap;
        }
        Integer charge = new Integer(lineComponents[6]);
        if (charge < minCharge) {
            minCharge = charge;
        }
        if (charge > maxCharge) {
            maxCharge = charge;
        }
        String sequence = lineComponents[7];
        ArrayList<ModificationMatch> modificationMatches = new ArrayList<ModificationMatch>();

        Peptide peptide = new Peptide(sequence, new ArrayList<String>(), modificationMatches);
        PeptideAssumption result = new PeptideAssumption(peptide, rank, Advocate.PEPNOVO, new Charge(Charge.PLUS, charge), rankScore, fileName);
        double theoreticMz = result.getTheoreticMz();
        if (theoreticMz < minMz) {
            minMz = theoreticMz;
        }
        if (theoreticMz > maxMz) {
            maxMz = theoreticMz;
        }
        PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();
        peptideAssumptionDetails.setPepNovoScore(pepNovoScore);
        peptideAssumptionDetails.setcTermGap(cGap);
        peptideAssumptionDetails.setnTermGap(nGap);
        result.addUrParam(peptideAssumptionDetails);

        return result;
    }

    /**
     * Returns the minimum PepNovo score.
     *
     * @return Minimum PepNovo score
     */
    public double getMinPepNovoScore() {
        return minPepNovoScore;
    }

    /**
     * Returns the maximum PepNovo score.
     *
     * @return Maximum PepNovo score
     */
    public double getMaxPepNovoScore() {
        return maxPepNovoScore;
    }

    /**
     * Returns the minimum rank score.
     *
     * @return Minimum rank score
     */
    public double getMinRankScore() {
        return minRankScore;
    }

    /**
     * Returns the maximum rank score.
     *
     * @return Maximum rank score
     */
    public double getMaxRankScore() {
        return maxRankScore;
    }

    /**
     * Returns the minimum N-terminal gap.
     *
     * @return Minimum N-terminal gap.
     */
    public double getMinNGap() {
        return minNGap;
    }

    /**
     * Returns the maximum N-terminal gap.
     *
     * @return Maximum N-terminal gap
     */
    public double getMaxNGap() {
        return maxNGap;
    }

    /**
     * Returns the minimum C-terminal gap.
     *
     * @return Minimum C-terminal gap.
     */
    public double getMinCGap() {
        return minCGap;
    }

    /**
     * Returns the maximum C-terminal gap.
     *
     * @return Maximum C-terminal gap.
     */
    public double getMaxCGap() {
        return maxCGap;
    }

    /**
     * Returns the minimum charge.
     *
     * @return Minimum charge.
     */
    public int getMinCharge() {
        return minCharge;
    }

    /**
     * Returns the maximum charge.
     *
     * @return Maximum charge.
     */
    public int getMaxCharge() {
        return maxCharge;
    }

    /**
     * Returns the minimum m/z value.
     *
     * @return Minimum m/z value.
     */
    public double getMinMz() {
        return minMz;
    }

    /**
     * Returns the maximum m/z value.
     *
     * @return Maximum m/z value.
     */
    public double getMaxMz() {
        return maxMz;
    }
}
