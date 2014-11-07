package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.preferences.ModificationProfile;
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
 */
public class ModificationFile {

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
     * @param filePath The folder where the file shall be saved.
     * @param modificationProfile The modification profile of the search.
     * @throws java.io.IOException Exception thrown when the file access fails.
     */
    public static void writeFile(File filePath, ModificationProfile modificationProfile) throws IOException {
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

        // Get the PTMFactory
        PTMFactory ptmFactory = PTMFactory.getInstance();
        PTM ptm = ptmFactory.getPTM(mod);

        if (ptm.getMass() > maxMassOffsetValue) {

            String connector = "";
            if (ptm.getMass() > 0) {
                connector = "+";
            }

            // Write a line for each residue
            if (ptm.getPattern().getAminoAcidsAtTarget().isEmpty()) {

                if (variable.equalsIgnoreCase(FIXED_PTM)) {
                    variable = VARIABLE_PTM; // PepNovo+ does not support fixed PTMs at the terminals...
                }

                if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                        || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                    writer.append("N_TERM" + SEP);
                    writer.append(Double.toString(ptm.getMass()) + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("N_TERM" + SPACE);
                    writer.append("^" + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                    modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptm.getMass())));
                    writer.append(ptm.getName().toUpperCase());
                    writer.newLine();
                } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                        || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                    writer.append("C_TERM" + SEP);
                    writer.append(Double.toString(ptm.getMass()) + SPACE);
                    writer.append(variable + SPACE);
                    writer.append("C_TERM" + SPACE);
                    writer.append("$" + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                    modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptm.getMass())));
                    writer.append(ptm.getName().toUpperCase());
                    writer.newLine();
                }
            } else {

                for (Character residue : ptm.getPattern().getAminoAcidsAtTarget()) {

                    if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                        writer.append(residue + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("+1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
                    } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                        writer.append(residue + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        writer.append(variable + SPACE);
                        writer.append("-1" + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
                    } else {
                        writer.append(residue + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        writer.append(variable + SPACE);
                        writer.append(ALL_LOCATIONS + SPACE);
                        writer.append(residue + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
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
        
        // @TODO: is this method really needed anymore?

        modIdMap = new HashMap<String, String>();
        PTMFactory ptmFactory = PTMFactory.getInstance();
        List<String> mods = new ArrayList<String>();
        mods.addAll(ptmFactory.getDefaultModifications());
        mods.addAll(ptmFactory.getUserModifications());
        // Connector string: plus for positive modifications, minus for negative ones
        String connector;

        // Write the modifications
        for (String mod : mods) {
            PTM ptm = ptmFactory.getPTM(mod);
            if (ptm.getMass() > 0) {
                connector = "+";
            } else {
                connector = "";
            }

            if (ptm.getPattern().getAminoAcidsAtTarget().isEmpty()) {
                if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                        || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                    modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptm.getMass())));
                } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                        || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                    modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptm.getMass())));
                }
            } else {
                for (Character residue : ptmFactory.getPTM(mod).getPattern().getAminoAcidsAtTarget()) {
                    if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
                    } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
                    } else {
                        modIdMap.put(ptm.getName(), residue + connector + Long.toString(Math.round(ptm.getMass())));
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
        if (modIdMap == null) {
            fillModIdMap();
        }

        // Lazy loading
        if (invertedModIdMap == null) {
            invertedModIdMap = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : modIdMap.entrySet()) {
                invertedModIdMap.put(entry.getValue(), entry.getKey());
            }
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
        if (modIdMap == null) {
            fillModIdMap();
        }
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
