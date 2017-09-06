package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.refinementparameters.PepnovoAssumptionDetails;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class allows exporting the results in a text file.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class TextExporter {

    /**
     * Separator used for the export.
     */
    private static final String SEPARATOR = "\t";
    /**
     * Separator used for the export.
     */
    private static final String SEPARATOR_2 = ";";

    /**
     * Exports the peptide matching results to a given file.
     *
     * @param destinationFile the destination file
     * @param identification the identification object containing identification
     * details
     * @param searchParameters the search parameters used for the search
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing to cancel the process
     * @param scoreThreshold de novo score threshold
     * @param greaterThan use greater than threshold
     * @param aNumberOfMatches the maximum number of matches to export per
     * spectrum
     *
     * @throws IOException thrown if an IO exception occurs
     * @throws SQLException thrown if an SQL exception occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if a precursor cannot be
     * extracted from a spectrum
     * @throws InterruptedException thrown if the process is interrupted
     */
    public static void exportPeptides(File destinationFile, Identification identification, SearchParameters searchParameters,
            WaitingHandler waitingHandler, Double scoreThreshold, boolean greaterThan, Integer aNumberOfMatches)
            throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);

        double threshold = 0;
        if (scoreThreshold != null) {
            threshold = scoreThreshold;
        }
        int numberOfMatches = 10;
        if (aNumberOfMatches != null) {
            numberOfMatches = aNumberOfMatches;
        }

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {
                b.write("File Name" + SEPARATOR + "Spectrum Title" + SEPARATOR + "Retention Time (s)" + SEPARATOR + "Measured m/z" + SEPARATOR + "Measured Charge" + SEPARATOR
                        + "Rank" + SEPARATOR + "Protein(s)" + SEPARATOR + "Peptide" + SEPARATOR + "Peptide Variable Modifications" + SEPARATOR + "Modified Sequence"
                        + SEPARATOR + "Tag" + SEPARATOR + "Longest Amino Acid Sequence" + SEPARATOR + "Tag Variable Modifications" + SEPARATOR + "Modified tag sequence" + SEPARATOR
                        + "PepNovo RankScore" + SEPARATOR + "PepNovo Score" + SEPARATOR + "DirecTag E-value" + SEPARATOR + "pNovo+ Score" + SEPARATOR + "Novor Score" + SEPARATOR
                        + "N-Gap" + SEPARATOR + "C-Gap" + SEPARATOR + "Theoretic m/z" + SEPARATOR + "Identification Charge" + SEPARATOR + "Tag Mass Error (Da)"
                        + SEPARATOR + "Tag Mass Error (ppm)" + SEPARATOR + "Peptide Mass Error (Da)" + SEPARATOR + "Peptide Mass Error (ppm)" + SEPARATOR + "Isotope");
                b.newLine();

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            StringBuilder spectrumDetails = new StringBuilder();

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            spectrumDetails.append(mgfFile).append(SEPARATOR).append(spectrumTitle).append(SEPARATOR);

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails.append(precursor.getRt()).append(SEPARATOR).append(precursor.getMz()).append(SEPARATOR).append(precursor.getPossibleChargesAsString()).append(SEPARATOR);

                            ArrayList<PeptideAssumption> assumptions = new ArrayList<PeptideAssumption>();
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumKey);

                            for (int algorithmId : assumptionsMap.keySet()) {
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMap = assumptionsMap.get(algorithmId);
                                if (advocateMap != null) {
                                    ArrayList<Double> scores = new ArrayList<Double>(advocateMap.keySet());
                                    Collections.sort(scores, Collections.reverseOrder());
                                    for (Double score : scores) {
                                        for (SpectrumIdentificationAssumption assumption : advocateMap.get(score)) {
                                            if (assumption instanceof PeptideAssumption) {
                                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                                assumptions.add(peptideAssumption);
                                            }
                                        }
                                    }
                                }
                            }

                            // export all matches above the score threshold up to the given user selected amount
                            for (int i = 0; i < assumptions.size() && i < numberOfMatches; i++) {

                                PeptideAssumption peptideAssumption = assumptions.get(i);

                                boolean passesThreshold;

                                if (greaterThan) {
                                    passesThreshold = peptideAssumption.getScore() >= threshold;
                                } else { // less than
                                    passesThreshold = peptideAssumption.getScore() <= threshold;
                                }

                                if (passesThreshold) {

                                    b.write(spectrumDetails.toString());
                                    b.write(peptideAssumption.getRank() + SEPARATOR);

                                    Peptide peptide = peptideAssumption.getPeptide();
                                    String proteinText = "";
                                    ArrayList<String> proteins = peptide.getParentProteinsNoRemapping();
                                    if (proteins != null) {
                                        Collections.sort(proteins);
                                        for (String accession : proteins) {
                                            if (!proteinText.equals("")) {
                                                proteinText += SEPARATOR_2;
                                            }
                                            proteinText += accession;
                                        }
                                    }
                                    b.write(proteinText + SEPARATOR);

                                    b.write(peptide.getSequence() + SEPARATOR);
                                    b.write(getPeptideModificationsAsString(peptide) + SEPARATOR);
                                    b.write(peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false) + SEPARATOR);

                                    // tag section if any
                                    TagAssumption tagAssumption = new TagAssumption();
                                    tagAssumption = (TagAssumption) peptideAssumption.getUrParam(tagAssumption);
                                    if (tagAssumption != null) {
                                        Tag tag = tagAssumption.getTag();
                                        b.write(tag.asSequence() + SEPARATOR);
                                        b.write(tag.getLongestAminoAcidSequence() + SEPARATOR);
                                        b.write(Tag.getTagModificationsAsString(tag) + SEPARATOR);
                                        b.write(tag.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false) + SEPARATOR);
                                        if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                                            PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                            pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                                            b.write(pepnovoAssumptionDetails.getRankScore() + SEPARATOR);
                                            b.write(tagAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR);
                                        } else if (tagAssumption.getAdvocate() == Advocate.direcTag.getIndex()) {
                                            b.write(SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR);
                                        } else if (tagAssumption.getAdvocate() == Advocate.pNovo.getIndex()) {
                                            b.write(SEPARATOR + SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR + SEPARATOR);
                                        } else if (tagAssumption.getAdvocate() == Advocate.novor.getIndex()) {
                                            b.write(SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR);
                                        }
                                        b.write(tag.getNTerminalGap() + SEPARATOR);
                                        b.write(tag.getCTerminalGap() + SEPARATOR);
                                        b.write(tag.getMass() + SEPARATOR);
                                        b.write(tagAssumption.getIdentificationCharge().value + SEPARATOR);
                                        double massDeviation = tagAssumption.getDeltaMass(precursor.getMz(), false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                                        b.write(massDeviation + SEPARATOR);
                                        massDeviation = tagAssumption.getDeltaMass(precursor.getMz(), true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                                        b.write(massDeviation + SEPARATOR);
                                    } else if (peptideAssumption.getAdvocate() == Advocate.novor.getIndex()) {
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + peptideAssumption.getScore() + SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);  
                                    } else {
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                        b.write(SEPARATOR);
                                    }
                                    Double massDeviation = peptideAssumption.getDeltaMass(precursor.getMz(), false, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                                    b.write(massDeviation + SEPARATOR);
                                    massDeviation = peptideAssumption.getDeltaMass(precursor.getMz(), true, searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection());
                                    b.write(massDeviation + SEPARATOR);
                                    b.write(peptideAssumption.getIsotopeNumber(precursor.getMz(), searchParameters.getMinIsotopicCorrection(), searchParameters.getMaxIsotopicCorrection()) + SEPARATOR);
                                    b.newLine();
                                }
                            }
                            if (waitingHandler != null) {
                                waitingHandler.increaseSecondaryProgressCounter();
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            } finally {
                b.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Exports the identification results to a given file.
     *
     * @param destinationFile the destination file
     * @param identification the identification object containing identification
     * details
     * @param searchParameters the search parameters used for the search
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing to cancel the process
     * @param scoreThreshold de novo score threshold
     * @param greaterThan use greater than threshold
     * @param aNumberOfMatches the maximum number of matches to export per
     * spectrum
     *
     * @throws IOException thrown if an IO exception occurs
     * @throws SQLException thrown if an SQL exception occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if a precursor cannot be
     * extracted from a spectrum
     * @throws InterruptedException thrown if the process is interrupted
     */
    public static void exportTags(File destinationFile, Identification identification, SearchParameters searchParameters,
            WaitingHandler waitingHandler, Double scoreThreshold, boolean greaterThan, Integer aNumberOfMatches)
            throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);

        double threshold = 0;
        if (scoreThreshold != null) {
            threshold = scoreThreshold;
        }
        int numberOfMatches = 10;
        if (aNumberOfMatches != null) {
            numberOfMatches = aNumberOfMatches;
        }

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {
                b.write("File Name" + SEPARATOR + "Spectrum Title" + SEPARATOR + "Retention Time (s)" + SEPARATOR + "Measured m/z" + SEPARATOR + "Measured Charge" + SEPARATOR
                        + "Rank" + SEPARATOR + "Tag" + SEPARATOR + "Longest AminoAcid sequence" + SEPARATOR + "Variable Modifications" + SEPARATOR + "Modified Sequence" + SEPARATOR
                        + "PepNovo RankScore" + SEPARATOR + "PepNovo Score" + SEPARATOR + "DirecTag E-value" + SEPARATOR + "pNovo+ Score" + SEPARATOR + "Novor Score" + SEPARATOR
                        + "N-Gap" + SEPARATOR + "C-Gap" + SEPARATOR + "Theoretic m/z" + SEPARATOR + "Identification Charge");
                b.newLine();

                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            StringBuilder spectrumDetails = new StringBuilder();

                            spectrumDetails.append(mgfFile).append(SEPARATOR).append(spectrumTitle).append(SEPARATOR);

                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails.append(precursor.getRt()).append(SEPARATOR).append(precursor.getMz()).append(SEPARATOR).append(precursor.getPossibleChargesAsString()).append(SEPARATOR);

                            ArrayList<SpectrumIdentificationAssumption> allAssumptions = new ArrayList<SpectrumIdentificationAssumption>();
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumKey);

                            for (int algorithmId : assumptionsMap.keySet()) {
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMap = assumptionsMap.get(algorithmId);
                                if (advocateMap != null) {
                                    ArrayList<Double> scores = new ArrayList<Double>(advocateMap.keySet());
                                    Collections.sort(scores, Collections.reverseOrder());
                                    for (Double score : scores) {
                                        for (SpectrumIdentificationAssumption assumption : advocateMap.get(score)) {
                                            allAssumptions.add(assumption);
                                        }
                                    }
                                }
                            }

                            int rank = 0;

                            // export all matches above the score threshold up to the given user selected amount
                            for (int i = 0; i < allAssumptions.size() && i < numberOfMatches; i++) {

                                SpectrumIdentificationAssumption assumption = allAssumptions.get(i);

                                boolean passesThreshold;

                                if (greaterThan) {
                                    passesThreshold = assumption.getScore() >= threshold;
                                } else { // less than
                                    passesThreshold = assumption.getScore() <= threshold;
                                }

                                if (passesThreshold) {
                                    b.write(spectrumDetails.toString());
                                    b.write(++rank + SEPARATOR);
                                    writeTagExportLine(b, assumption, searchParameters);
                                    b.newLine();
                                }
                            }
                            if (allAssumptions.isEmpty()) {
                                b.newLine();
                            }
                            if (waitingHandler != null) {
                                waitingHandler.increaseSecondaryProgressCounter();
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                            }
                        }

                        b.newLine();
                    }
                }
            } finally {
                b.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Writes the details on the given assumption to the given writer in the
     * form of a tag export.
     *
     * @param b the writer
     * @param assumption the assumption to write
     * @param searchParameters the search parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing.
     */
    public static void writeTagExportLine(BufferedWriter b, SpectrumIdentificationAssumption assumption, SearchParameters searchParameters) throws IOException {
        if (assumption instanceof TagAssumption) {
            TagAssumption tagAssumption = (TagAssumption) assumption;
            writeTagExportLine(b, tagAssumption, searchParameters);
        } else if (assumption instanceof PeptideAssumption) {
            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
            writeTagExportLine(b, peptideAssumption, searchParameters);
        } else {
            throw new UnsupportedOperationException("Export not implemented for assumption of type " + assumption.getClass() + ".");
        }
    }

    /**
     * Writes the details on the given peptide assumption to the given writer in
     * the form of a tag export.
     *
     * @param b the writer
     * @param peptideAssumption the peptide assumption to write
     * @param searchParameters the search parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing.
     */
    public static void writeTagExportLine(BufferedWriter b, PeptideAssumption peptideAssumption, SearchParameters searchParameters) throws IOException {

        Peptide peptide = peptideAssumption.getPeptide();
        b.write(peptide.getSequence() + SEPARATOR);
        b.write(peptide.getSequence() + SEPARATOR);
        b.write(Peptide.getPeptideModificationsAsString(peptide, true) + SEPARATOR);
        b.write(peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false) + SEPARATOR);
        if (peptideAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
            PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
            pepnovoAssumptionDetails = (PepnovoAssumptionDetails) peptideAssumption.getUrParam(pepnovoAssumptionDetails);
            b.write(pepnovoAssumptionDetails.getRankScore() + SEPARATOR);
            b.write(peptideAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR);
        } else if (peptideAssumption.getAdvocate() == Advocate.direcTag.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + peptideAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR);
        } else if (peptideAssumption.getAdvocate() == Advocate.pNovo.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + SEPARATOR + peptideAssumption.getScore() + SEPARATOR + SEPARATOR);
        } else if (peptideAssumption.getAdvocate() == Advocate.novor.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + peptideAssumption.getScore() + SEPARATOR);
        }
        b.write(0 + SEPARATOR);
        b.write(0 + SEPARATOR);
        b.write(peptide.getMass() + SEPARATOR);
        b.write(peptideAssumption.getIdentificationCharge().value + SEPARATOR);
    }

    /**
     * Writes the details on the given tag assumption to the given writer in the
     * form of a tag export.
     *
     * @param b the writer
     * @param tagAssumption the tag assumption to write
     * @param searchParameters the search parameters
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing.
     */
    public static void writeTagExportLine(BufferedWriter b, TagAssumption tagAssumption, SearchParameters searchParameters) throws IOException {

        Tag tag = tagAssumption.getTag();
        b.write(tag.asSequence() + SEPARATOR);
        b.write(tag.getLongestAminoAcidSequence() + SEPARATOR);
        b.write(Tag.getTagModificationsAsString(tag) + SEPARATOR);
        b.write(tag.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false) + SEPARATOR);
        if (tagAssumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
            PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
            pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
            b.write(pepnovoAssumptionDetails.getRankScore() + SEPARATOR);
            b.write(tagAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR);
        } else if (tagAssumption.getAdvocate() == Advocate.direcTag.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR + SEPARATOR + SEPARATOR);
        } else if (tagAssumption.getAdvocate() == Advocate.pNovo.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR + SEPARATOR);
        } else if (tagAssumption.getAdvocate() == Advocate.novor.getIndex()) {
            b.write(SEPARATOR + SEPARATOR + SEPARATOR + SEPARATOR + tagAssumption.getScore() + SEPARATOR);
        }
        b.write(tag.getNTerminalGap() + SEPARATOR);
        b.write(tag.getCTerminalGap() + SEPARATOR);
        b.write(tag.getMass() + SEPARATOR);
        b.write(tagAssumption.getIdentificationCharge().value + SEPARATOR);
    }

    /**
     * Exports the BLAST-compatible identification results to a given file.
     *
     * @param destinationFile the destination file
     * @param identification the identification object containing identification
     * details
     * @param searchParameters the search parameters used for the search
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing to cancel the process.
     * @param scoreThreshold de novo score threshold
     * @param greaterThan use greater than threshold
     * @param aNumberOfMatches the maximum number of matches to export per
     * spectrum
     *
     * @throws IOException thrown if an IO exception occurs
     * @throws SQLException thrown if an SQL exception occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws MzMLUnmarshallerException thrown if a precursor cannot be
     * extracted from a spectrum
     * @throws InterruptedException thrown if the process is interrupted
     */
    public static void exportBlastPSMs(File destinationFile, Identification identification, SearchParameters searchParameters, WaitingHandler waitingHandler,
            Double scoreThreshold, boolean greaterThan, Integer aNumberOfMatches) throws IOException, SQLException, ClassNotFoundException, MzMLUnmarshallerException, InterruptedException {

        FileWriter f = new FileWriter(destinationFile);
        double threshold = 0;
        if (scoreThreshold != null) {
            threshold = scoreThreshold;
        }
        int numberOfMatches = 10;
        if (aNumberOfMatches != null) {
            numberOfMatches = aNumberOfMatches;
        }

        try {
            BufferedWriter b = new BufferedWriter(f);

            try {
                if (waitingHandler != null) {
                    waitingHandler.setWaitingText("Exporting Spectra - Writing File. Please Wait...");
                    // reset the progress bar
                    waitingHandler.resetSecondaryProgressCounter();
                    waitingHandler.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
                }

                for (String mgfFile : identification.getSpectrumFiles()) {
                    for (String spectrumKey : identification.getSpectrumIdentification(mgfFile)) {
                        if (identification.matchExists(spectrumKey)) {

                            String spectrumDetails = ">";
                            String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
                            spectrumDetails += mgfFile + SEPARATOR_2 + spectrumTitle + SEPARATOR_2;
                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);
                            spectrumDetails += precursor.getMz() + SEPARATOR_2 + precursor.getPossibleChargesAsString() + SEPARATOR_2;
                            ArrayList<SpectrumIdentificationAssumption> assumptions = new ArrayList<SpectrumIdentificationAssumption>();
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = identification.getAssumptions(spectrumKey);

                            for (int algorithmId : assumptionsMap.keySet()) {
                                HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateMap = assumptionsMap.get(algorithmId);
                                if (advocateMap != null) {
                                    ArrayList<Double> scores = new ArrayList<Double>(advocateMap.keySet());
                                    Collections.sort(scores, Collections.reverseOrder());
                                    for (Double score : scores) {
                                        for (SpectrumIdentificationAssumption assumption : advocateMap.get(score)) {
                                            assumptions.add(assumption);
                                        }
                                    }
                                }
                            }

                            // export all matches above the score threshold up to the given user selected amount
                            for (int i = 0; i < assumptions.size() && i < numberOfMatches; i++) {

                                SpectrumIdentificationAssumption assumption = assumptions.get(i);

                                boolean passesThreshold;

                                if (greaterThan) {
                                    passesThreshold = assumption.getScore() >= threshold;
                                } else { // less than
                                    passesThreshold = assumption.getScore() <= threshold;
                                }

                                if (passesThreshold) {
                                    b.write(spectrumDetails);
                                    if (assumption.getAdvocate() == Advocate.pepnovo.getIndex()) {
                                        PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                        pepnovoAssumptionDetails = (PepnovoAssumptionDetails) assumption.getUrParam(pepnovoAssumptionDetails);
                                        b.write(pepnovoAssumptionDetails.getRankScore() + SEPARATOR_2);
                                    } else {
                                        b.write(SEPARATOR_2);
                                    }
                                    b.write(assumption.getScore() + "");
                                    b.newLine();
                                    if (assumption instanceof TagAssumption) {
                                        TagAssumption tagAssumption = (TagAssumption) assumption;
                                        b.write(tagAssumption.getTag().getLongestAminoAcidSequence());
                                    } else if (assumption instanceof PeptideAssumption) {
                                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                        Peptide peptide = peptideAssumption.getPeptide();
                                        b.write(peptide.getSequence());
                                    } else {
                                        throw new UnsupportedOperationException("Export not implemented for assumption of type " + assumption.getClass() + ".");
                                    }

                                    b.newLine();
                                }
                            }

                            if (assumptions.isEmpty()) {
                                b.newLine(); //This should not happen. Should.
                            }

                            if (waitingHandler != null) {
                                waitingHandler.increaseSecondaryProgressCounter();
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            } finally {
                b.close();
            }
        } finally {
            f.close();
        }
    }

    /**
     * Returns the peptide modifications as a string.
     *
     * @param peptide the peptide
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide) {

        StringBuilder result = new StringBuilder();

        if (peptide.isModified()) {
            HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>(peptide.getNModifications());
            if (peptide.isModified()) {
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                            modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                        }
                        modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                    }
                }
            }

            boolean first = true, first2;
            ArrayList<String> mods = new ArrayList<String>(modMap.keySet());

            Collections.sort(mods);
            for (String mod : mods) {
                if (first) {
                    first = false;
                } else {
                    result.append(", ");
                }
                first2 = true;
                result.append(mod);
                result.append(" (");
                for (int aa : modMap.get(mod)) {
                    if (first2) {
                        first2 = false;
                    } else {
                        result.append(", ");
                    }
                    result.append(aa);
                }
                result.append(")");
            }
        }

        return result.toString();
    }
}
