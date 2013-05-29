package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class allows exporting the results in a text file.
 *
 * @author Thilo Muth
 */
public class TextExporter {

    /**
     * Separator used for the export.
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
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportPSMs(String filePath, Identification identification) throws IOException, SQLException, ClassNotFoundException, InterruptedException {
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
     * This method exports the assumptions for a selected spectrum.
     *
     * @param selectedSpectrumFile Selected MGF file name.
     * @param selectedSpectrumTitle Selected spectrum title.
     * @param selectedFile Selected output file.
     * @param identification De novo identification.
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void exportSingleAssumptions(String selectedSpectrumFile, String selectedSpectrumTitle, File selectedFile, Identification identification) throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException {

        String psmKey = Spectrum.getSpectrumKey(selectedSpectrumFile, selectedSpectrumTitle);
        if (identification.matchExists(psmKey)) {
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);

            HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

            if (selectedFile != null) {
                FileWriter w = new FileWriter(selectedFile);
                BufferedWriter bw = new BufferedWriter(w);

                // Write MGF file name.
                bw.write("MGF=" + selectedSpectrumFile);
                bw.newLine();
                // Write spectrum title.
                bw.write("TITLE=" + selectedSpectrumTitle);
                bw.newLine();

                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                for (int i = 0; i < scores.size(); i++) {
                    for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                        bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                        bw.newLine();
                    }
                }
                bw.close();
                w.close();
            }
        }
    }

    /**
     * This method exports the de novo assumptions.
     *
     * @param selectedFile The file to export to.
     * @param identification The identification result.
     * @throws IOException Exception thrown when the file access fails.
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public static void exportAssumptions(File selectedFile, Identification identification) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        FileWriter w = new FileWriter(selectedFile);
        BufferedWriter bw = new BufferedWriter(w);
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        // Iterate the spectrum files.
        for (String fileName : spectrumFactory.getMgfFileNames()) {
            // Iterate the spectrum titles.
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {
                String psmKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);

                if (identification.matchExists(psmKey)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                    HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

                    if (selectedFile != null) {
                        // Write MGF file name.
                        bw.write("MGF=" + fileName);
                        bw.newLine();

                        // Write spectrum title.
                        bw.write("TITLE=" + spectrumTitle);
                        bw.newLine();

                        ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                        Collections.sort(scores, Collections.reverseOrder());
                        for (int i = 0; i < scores.size(); i++) {
                            for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                                bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                                bw.newLine();
                            }
                        }
                    }
                    bw.newLine();
                }
            }
        }
        bw.close();
        w.close();
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
