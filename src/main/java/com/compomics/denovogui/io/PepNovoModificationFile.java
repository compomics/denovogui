package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
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
     * Fixed Modification type.
     */
    private static final String FIXED_PTM = "FIXED";
    /**
     * Variable Modification type.
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

        // Modification file header
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
     * Writes the lines corresponding to the given Modification name.
     *
     * @param writer the writer used to write
     * @param mod the name of the modifications of interest
     * @param variable a string (see static fields) indicating whether the
     * modification is fixed or variable
     * @throws IOException
     */
    private static void writePtmLine(BufferedWriter writer, String mod, String variable) throws IOException {

        // Get the ModificationFactory
        ModificationFactory ptmFactory = ModificationFactory.getInstance();
        Modification ptm = ptmFactory.getModification(mod);
        double ptmMass = ptm.getRoundedMass();

        if (ptmMass > maxMassOffsetValue) {

            String connector = "";
            if (ptmMass > 0) {
                connector = "+";
            }

            // Write a line for each residue
            if (ptm.getPattern() == null || ptm.getPattern().getAminoAcidsAtTarget().isEmpty()) {

                if (variable.equalsIgnoreCase(FIXED_PTM)) {
                    variable = VARIABLE_PTM; // PepNovo+ does not support fixed PTMs at the terminals...
                }

                if (ptmFactory.getModification(mod).getType() == Modification.MODN || ptmFactory.getModification(mod).getType() == Modification.MODNAA
                        || ptmFactory.getModification(mod).getType() == Modification.MODNP || ptmFactory.getModification(mod).getType() == Modification.MODNPAA) {
                    writer.append("N_TERM" + SEP);
                    writer.append(ptmMass + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("N_TERM" + SPACE);
                    writer.append("^" + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                    modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptmMass)));
                    writer.append(ptm.getName().toUpperCase());
                    writer.newLine();
                } else if (ptmFactory.getModification(mod).getType() == Modification.MODC || ptmFactory.getModification(mod).getType() == Modification.MODCAA
                        || ptmFactory.getModification(mod).getType() == Modification.MODCP || ptmFactory.getModification(mod).getType() == Modification.MODCPAA) {
                    writer.append("C_TERM" + SEP);
                    writer.append(ptmMass + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("C_TERM" + SPACE);
                    writer.append("$" + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                    modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptmMass)));
                    writer.append(ptm.getName().toUpperCase());
                    writer.newLine();
                }
            } else {

                for (Character residue : ptm.getPattern().getAminoAcidsAtTarget()) {

                    if (ptmFactory.getModification(mod).getType() == Modification.MODN || ptmFactory.getModification(mod).getType() == Modification.MODNAA
                            || ptmFactory.getModification(mod).getType() == Modification.MODNP || ptmFactory.getModification(mod).getType() == Modification.MODNPAA) {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("+1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else if (ptmFactory.getModification(mod).getType() == Modification.MODC || ptmFactory.getModification(mod).getType() == Modification.MODCAA
                            || ptmFactory.getModification(mod).getType() == Modification.MODCP || ptmFactory.getModification(mod).getType() == Modification.MODCPAA) {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("-1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else {
                        writer.append(residue + SEP);
                        writer.append(ptmMass + SPACE);
                        writer.append(variable + SPACE);
                        writer.append(ALL_LOCATIONS + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptmMass)) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    }

                    writer.append(ptm.getName().toUpperCase());
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
        ModificationFactory ptmFactory = ModificationFactory.getInstance();
        List<String> mods = new ArrayList<String>();
        mods.addAll(ptmFactory.getDefaultModifications());
        mods.addAll(ptmFactory.getUserModifications());
        // Connector string: plus for positive modifications, minus for negative ones
        String connector;

        // Write the modifications
        for (String mod : mods) {
            Modification ptm = ptmFactory.getModification(mod);
            double ptmMass = ptm.getRoundedMass();

            if (ptmMass > 0) {
                connector = "+";
            } else {
                connector = "";
            }

            if (ptm.getPattern() == null || ptm.getPattern().getAminoAcidsAtTarget().isEmpty()) {
                if (ptmFactory.getModification(mod).getType() == Modification.MODN || ptmFactory.getModification(mod).getType() == Modification.MODNAA
                        || ptmFactory.getModification(mod).getType() == Modification.MODNP || ptmFactory.getModification(mod).getType() == Modification.MODNPAA) {
                    modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptmMass)));
                } else if (ptmFactory.getModification(mod).getType() == Modification.MODC || ptmFactory.getModification(mod).getType() == Modification.MODCAA
                        || ptmFactory.getModification(mod).getType() == Modification.MODCP || ptmFactory.getModification(mod).getType() == Modification.MODCPAA) {
                    modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptmMass)));
                }
            } else {
                for (Character residue : ptmFactory.getModification(mod).getPattern().getAminoAcidsAtTarget()) {
                    if (ptmFactory.getModification(mod).getType() == Modification.MODN || ptmFactory.getModification(mod).getType() == Modification.MODNAA
                            || ptmFactory.getModification(mod).getType() == Modification.MODNP || ptmFactory.getModification(mod).getType() == Modification.MODNPAA) {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else if (ptmFactory.getModification(mod).getType() == Modification.MODC || ptmFactory.getModification(mod).getType() == Modification.MODCAA
                            || ptmFactory.getModification(mod).getType() == Modification.MODCP || ptmFactory.getModification(mod).getType() == Modification.MODCPAA) {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
                    } else {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptmMass)));
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
        return "#AA  offset      type    locations  symbol  Modification name";
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
