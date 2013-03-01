/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.io;

import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class allows exporting the results in a text file
 *
 * @author Thilo Muth
 */
public class TextExporter {

    /**
     * Separator used for the export
     */
    private static final String SEP = "\t";

    /**
     * The different export types.
     */
    public enum ExportType {

        PSMS, ASSUMPTIONS
    }

    /**
     * This method exports the PSM results.
     *
     * @param filePath The file path to the exported file.
     * @param identification The identification result.
     * @throws IOException Exception thrown when the file access fails.
     */
    public static void exportPSMs(String filePath, Identification identification) throws IOException, SQLException, ClassNotFoundException {
        // Init the buffered writer.
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));

        int count = 1;

        // Peptide header.
        writer.write(getPSMHeader());
        writer.newLine();

        for (String spectrumFile : identification.getSpectrumFiles()) {

            identification.loadSpectrumMatches(spectrumFile, null);

            for (String spectrumTitle : identification.getSpectrumIdentification(spectrumFile)) {

                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                PeptideAssumption peptideAssumption = spectrumMatch.getFirstHit(Advocate.PEPNOVO);

                writer.write(count++ + SEP);
                writer.write(spectrumFile + SEP);
                writer.write(spectrumTitle + SEP);
                Peptide peptide = peptideAssumption.getPeptide();
                //@TODO add modifications
                writer.write(peptide.getSequence() + SEP);
                String proteinList = "";
                for (String accession : peptide.getParentProteins()) {
                    if (!proteinList.equals("")) {
                        proteinList += ", ";
                    }
                    proteinList += accession;
                }
                writer.write(proteinList + SEP);
                //@TODO: implement other stuffs
                writer.newLine();
            }
        }

        writer.close();
    }

    /**
     * This method exports the denovo assumptions.
     *
     * @param filePath The file path to the exported file.
     * @param identification The identification result.
     * @throws IOException Exception thrown when the file access fails.
     */
    public static void exportAssumptions(String filePath, Identification identification) throws IOException, SQLException, ClassNotFoundException {
        // Init the buffered writer.
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));

        int count = 1;

        // header
        writer.append(getAssumptionHeader());
        writer.newLine();

        PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();

        for (String spectrumFile : identification.getSpectrumFiles()) {
            identification.loadSpectrumMatches(spectrumFile, null);
            for (String spectrumTitle : identification.getSpectrumIdentification(spectrumFile)) {
                String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                int rank = 1;
                HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(Advocate.PEPNOVO);
                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                for (double score : scores) {
                    for (PeptideAssumption peptideAssumption : assumptionsMap.get(score)) {
                        if (rank == 1) {
                            writer.write(++count + SEP);
                            writer.write(spectrumFile + SEP);
                            writer.write(spectrumTitle + SEP);
                        } else {
                            writer.write(SEP + SEP + SEP);
                        }
                        writer.write(++rank + SEP);
                        Peptide peptide = peptideAssumption.getPeptide();
                        //@TODO: add modifications
                        writer.write(peptide.getSequence() + SEP);
                        writer.write(peptide.getSequence().length() + SEP);
                        writer.write(peptideAssumption.getIdentificationCharge().toString() + SEP);
                        peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);
                        writer.write(peptideAssumptionDetails.getnTermGap() + SEP);
                        writer.write(peptideAssumptionDetails.getcTermGap() + SEP);
                        writer.write(peptideAssumption.getScore() + SEP);
                        writer.write(peptideAssumptionDetails.getPepNovoScore() + SEP);
                        writer.newLine();
                    }
                }
            }
        }
        writer.close();
    }

            /**
             * Returns the PSM header string.
             *
             * @return The PSM header string.
             */
    

    private static String getPSMHeader() {
        return "#" + SEP
                + "File" + SEP
                + "Spectrum title" + SEP
                + "Peptide Sequence" + SEP
                + "Protein Accession" + SEP;
    }

    /**
     * Returns the score header string.
     *
     * @return The score header string.
     */
    private static String getAssumptionHeader() {
        return "PSM No." + SEP
                + "File" + SEP
                + "Title" + SEP
                + "Rank" + SEP
                + "sequence" + SEP
                + "length" + SEP
                + "identification charge" + SEP
                + "N-term gap" + SEP
                + "C-term gap" + SEP
                + "rank score" + SEP
                + "Pepnovo score" + SEP;
    }
    
}
