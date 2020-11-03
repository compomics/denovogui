package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class enable the export of the standardized modifications to a PepNovo+
 * specific format.
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class PepNovoModificationFile {

    /**
     * The name of the file.
     */
    private static final String name = "PepNovo_PTMs.txt";
    /**
     * Tab separated format.
     */
    private static final String SEP = "\t";
    /**
     * Space format.
     */
    private static final String SPACE = " ";
    /**
     * Fixed PTM type.
     */
    private static final String FIXED_PTM = "FIXED";
    /**
     * Variable PTM type.
     */
    private static final String VARIABLE_PTM = "OPTIONAL";
    /**
     * Modification at all locations.
     */
    private static final String ALL_LOCATIONS = "ALL";
    /**
     * Mappings from modification name to modification ID in PepNovo+, e.g.
     * C+57.
     */
    private static Map<String, String> modIdMap;
    /**
     * Inverted mapping from modification ID to modification names.
     */
    private static HashMap<String, String> invertedModIdMap;
    /**
     * Maximum mass offset value.
     */
    private static double maxMassOffsetValue = -50; // @TODO: why -50???

    /**
     * This method writes the modifications to a file.
     *
     * @param filePath the folder where the file shall be saved
     * @param modificationProfile the modification profile of the search
     * @throws java.io.IOException thrown if the file access fails
     */
    public static void writeFile(File filePath, ModificationParameters modificationProfile) throws IOException {
        // Init the buffered writer.
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath, name)));

        // PTM file header
        writer.append(getHeader());
        writer.newLine();

        modIdMap = new HashMap<String, String>();

        // Write the fixed modifications
        for (String mod : modificationProfile.getFixedModifications()) {
            writePtmLine(writer, mod, FIXED_PTM);
        }
        // Write the variable modifications
        for (String mod : modificationProfile.getVariableModifications()) {
            writePtmLine(writer, mod, VARIABLE_PTM);
        }

        writer.flush();
        writer.close();
    }

    /**
     * Writes the lines corresponding to the given PTM name.
     *
     * @param writer the writer used to write
     * @param mod the name of the modifications of interest
     * @param variable a string (see static fields) indicating whether the
     * modification is fixed or variable
     * @throws IOException
     */
    private static void writePtmLine(BufferedWriter writer, String mod, String variable) throws IOException {

        // Get the ModificationFactory
        ModificationFactory modificationFactory = ModificationFactory.getInstance();
        Modification tempMod = modificationFactory.getModification(mod);
        double ptmMass = tempMod.getRoundedMass();

        if (ptmMass > maxMassOffsetValue) {

            String connector = "";
            if (ptmMass > 0) {
                connector = "+";
            }

            // Write a line for each residue
            if (tempMod.getPattern() == null || tempMod.getPattern().getAminoAcidsAtTarget().isEmpty()) {

                if (variable.equalsIgnoreCase(FIXED_PTM)) {
                    variable = VARIABLE_PTM; // PepNovo+ does not support fixed PTMs at the terminals...
                }

                if (modificationFactory.getModification(mod).getModificationType() == ModificationType.modn_peptide || modificationFactory.getModification(mod).getModificationType() == ModificationType.modnaa_peptide
                        || modificationFactory.getModification(mod).getModificationType() == ModificationType.modn_protein || modificationFactory.getModification(mod).getModificationType() == ModificationType.modnaa_protein) {
                    writer.append("N_TERM" + SEP);
                    writer.append(ptmMass + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("N_TERM" + SPACE);
                    writer.append("^" + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                    modIdMap.put(tempMod.getName(), "^" + connector + Long.toString(Math.round(ptmMass)));
                    writer.append(tempMod.getName().toUpperCase());
                    writer.newLine();
                } else if (modificationFactory.getModification(mod).getModificationType() == ModificationType.modc_peptide || modificationFactory.getModification(mod).getModificationType() == ModificationType.modcaa_peptide
                        || modificationFactory.getModification(mod).getModificationType() == ModificationType.modc_protein || modificationFactory.getModification(mod).getModificationType() == ModificationType.modcaa_protein) {
                    writer.append("C_TERM" + SEP);
                    writer.append(ptmMass + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("C_TERM" + SPACE);
                    writer.append("$" + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                    modIdMap.put(tempMod.getName(), "$" + connector + Long.toString(Math.round(ptmMass)));
                    writer.append(tempMod.getName().toUpperCase());
                    writer.newLine();
                }
            } else {

                for (Character residue : tempMod.getPattern().getAminoAcidsAtTarget()) {

                    if (modificationFactory.getModification(mod).getModificationType() == ModificationType.modn_peptide || modificationFactory.getModification(mod).getModificationType() == ModificationType.modnaa_peptide
                            || modificationFactory.getModification(mod).getModificationType() == ModificationType.modn_protein || modificationFactory.getModification(mod).getModificationType() == ModificationType.modnaa_protein) {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("+1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else if (modificationFactory.getModification(mod).getModificationType() == ModificationType.modc_peptide || modificationFactory.getModification(mod).getModificationType() == ModificationType.modcaa_peptide
                            || modificationFactory.getModification(mod).getModificationType() == ModificationType.modc_protein || modificationFactory.getModification(mod).getModificationType() == ModificationType.modcaa_protein) {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("-1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append(ALL_LOCATIONS + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    }

                    writer.append(tempMod.getName().toUpperCase());
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Fill the modification ID map.
     */
    private static void fillModIdMap() {

        modIdMap = new HashMap<String, String>();
        ModificationFactory modFactory = ModificationFactory.getInstance();
        List<String> mods = new ArrayList<String>();
        mods.addAll(modFactory.getDefaultModifications());
        mods.addAll(modFactory.getUserModifications());
        // Connector string: plus for positive modifications, minus for negative ones
        String connector;

        // Write the modifications
        for (String mod : mods) {
            Modification tempMod = modFactory.getModification(mod);
            double ptmMass = tempMod.getRoundedMass();

            if (ptmMass > 0) {
                connector = "+";
            } else {
                connector = "";
            }

            if (tempMod.getPattern() == null || tempMod.getPattern().getAminoAcidsAtTarget().isEmpty()) {
                if (modFactory.getModification(mod).getModificationType() == ModificationType.modn_peptide || modFactory.getModification(mod).getModificationType() == ModificationType.modnaa_peptide
                        || modFactory.getModification(mod).getModificationType() == ModificationType.modn_protein || modFactory.getModification(mod).getModificationType() == ModificationType.modnaa_protein) {
                    modIdMap.put(tempMod.getName(), "^" + connector + Long.toString(Math.round(ptmMass)));
                } else if (modFactory.getModification(mod).getModificationType() == ModificationType.modc_peptide || modFactory.getModification(mod).getModificationType() == ModificationType.modcaa_peptide
                        || modFactory.getModification(mod).getModificationType() == ModificationType.modc_protein || modFactory.getModification(mod).getModificationType() == ModificationType.modcaa_protein) {
                    modIdMap.put(tempMod.getName(), "$" + connector + Long.toString(Math.round(ptmMass)));
                }
            } else {
                for (Character residue : modFactory.getModification(mod).getPattern().getAminoAcidsAtTarget()) {
                    if (modFactory.getModification(mod).getModificationType() == ModificationType.modn_peptide || modFactory.getModification(mod).getModificationType() == ModificationType.modnaa_peptide
                            || modFactory.getModification(mod).getModificationType() == ModificationType.modn_protein || modFactory.getModification(mod).getModificationType() == ModificationType.modnaa_protein) {
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else if (modFactory.getModification(mod).getModificationType() == ModificationType.modc_peptide || modFactory.getModification(mod).getModificationType() == ModificationType.modcaa_peptide
                            || modFactory.getModification(mod).getModificationType() == ModificationType.modc_protein || modFactory.getModification(mod).getModificationType() == ModificationType.modcaa_protein) {
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else {
                        modIdMap.put(tempMod.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    }
                }
            }
        }
    }

    /**
     * Returns the header.
     *
     * @return The header string.
     */
    private static String getHeader() {
        return "#AA  offset      type    locations  symbol  PTM name";
    }

    /**
     * Returns the inverted modification ID to name map.
     *
     * @return The modification ID to name map
     */
    public static Map<String, String> getInvertedModIdMap() {

        fillModIdMap();

        invertedModIdMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : modIdMap.entrySet()) {
            invertedModIdMap.put(entry.getValue(), entry.getKey());
        }

        return invertedModIdMap;
    }

    /**
     * Returns the list of modification in the PepNovo format for search.
     *
     * @param modifications the list of modifications to export
     * @return the list of modification in a pep novo format
     */
    public static String getModsString(ArrayList<String> modifications) {
        fillModIdMap();
        Collections.sort(modifications);
        String result = "";
        // Append the selected modifications
        for (String mod : modifications) {
            if (!result.equals("")) {
                result += ":";
            }
            String deNovoName = modIdMap.get(mod);
            if (deNovoName == null) {
                throw new IllegalArgumentException("No PepNovo name found for modification " + mod + ".");
            }
            result += deNovoName;
        }
        return result;
    }
}
