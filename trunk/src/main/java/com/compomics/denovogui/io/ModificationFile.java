/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.io;

import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import java.awt.Color;
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
     * Comma / semi-colon separated format.
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
    private static HashMap<Integer, Color> ptmTypeColorMap;

    /**
     * This method writes the modifications to a file.
     *
     * @param filePath The file path to the exported file.
     * @param mods The modifications.
     * @throws java.io.IOException Exception thrown when the file access fails.
     */
    public static void writeFile(String filePath, List<String> mods) throws IOException {
        // Init the buffered writer.
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)));

        // PTM file header
        writer.append(getHeader());
        writer.newLine();

        // Get the PTMFactory
        PTMFactory ptmFactory = PTMFactory.getInstance();
        modIdMap = new HashMap<String, String>();
        // Connector string: plus for positive modifications, minus for negative ones
        String connector = "";

        // Write the modifications
        for (String mod : mods) {
            PTM ptm = ptmFactory.getPTM(mod);
            if (ptm.getMass() > 0) {
                connector = "+";
            } else {
                connector = "";
            }

            // Mass offset maximum -50 Da

            // Write a line for each residue
            for (AminoAcid residue : ptmFactory.getPTM(mod).getPattern().getAminoAcidsAtTarget()) {

                if (ptm.getMass() > -50) {
                    if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                        writer.append("N_TERM" + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        writer.append(VARIABLE_PTM + SPACE);
                        writer.append("N_TERM" + SPACE);
                        writer.append("^" + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptm.getMass())));
                    } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                        writer.append("C_TERM" + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        writer.append(VARIABLE_PTM + SPACE);
                        writer.append("C_TERM" + SPACE);
                        writer.append("$" + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptm.getMass())));
                    } else {
                        writer.append(residue.singleLetterCode + SEP);
                        writer.append(Double.toString(ptm.getMass()) + SPACE);
                        if (ptm.getMass() == 57.021464) {
                            writer.append(FIXED_PTM + SPACE);
                        } else {
                            writer.append(VARIABLE_PTM + SPACE);
                        }
                        writer.append(ALL_LOCATIONS + SPACE);
                        writer.append(residue.singleLetterCode + connector + Long.toString(Math.round(ptm.getMass())) + SPACE);
                        modIdMap.put(ptm.getName(), residue.singleLetterCode + connector + Long.toString(Math.round(ptm.getMass())));
                    }
                    writer.append(ptm.getName().toUpperCase());
                    writer.newLine();
                }
            }
        }

        writer.flush();
        writer.close();
    }

    private static void fillModIdMap() {
        modIdMap = new HashMap<String, String>();
        PTMFactory ptmFactory = PTMFactory.getInstance();
        List<String> mods = new ArrayList<String>();
        mods.addAll(ptmFactory.getDefaultModifications());
        mods.addAll(ptmFactory.getUserModifications());
        // Connector string: plus for positive modifications, minus for negative ones
        String connector = "";

        // Write the modifications
        for (String mod : mods) {
            PTM ptm = ptmFactory.getPTM(mod);
            if (ptm.getMass() > 0) {
                connector = "+";
            } else {
                connector = "";
            }

            // Write a line for each residue
            for (AminoAcid residue : ptmFactory.getPTM(mod).getPattern().getAminoAcidsAtTarget()) {
                if (ptm.getMass() > -50) {
                    if (ptmFactory.getPTM(mod).getType() == PTM.MODN || ptmFactory.getPTM(mod).getType() == PTM.MODNAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODNP || ptmFactory.getPTM(mod).getType() == PTM.MODNPAA) {
                        modIdMap.put(ptm.getName(), "^" + connector + Long.toString(Math.round(ptm.getMass())));
                    } else if (ptmFactory.getPTM(mod).getType() == PTM.MODC || ptmFactory.getPTM(mod).getType() == PTM.MODCAA
                            || ptmFactory.getPTM(mod).getType() == PTM.MODCP || ptmFactory.getPTM(mod).getType() == PTM.MODCPAA) {
                        modIdMap.put(ptm.getName(), "$" + connector + Long.toString(Math.round(ptm.getMass())));
                    } else {
                        modIdMap.put(ptm.getName(), residue.singleLetterCode + connector + Long.toString(Math.round(ptm.getMass())));
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
     * Returns the modification name to ID map.
     *
     * @return The modification name to ID map
     */
    public static Map<String, String> getModIdMap() {
        if (modIdMap == null) {
            fillModIdMap();
        }
        return modIdMap;
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
     * Returns the color for a specific modification.
     *
     * @param ptm
     * @return the color
     */
    private static Color getColorForModification(PTM ptm) {
        return getColorTypeMap().get(ptm.getType());
    }

    /**
     * Returns the color type map.
     *
     * @return the color type map.
     */
    public static HashMap<Integer, Color> getColorTypeMap() {
        if (ptmTypeColorMap == null) {
            ptmTypeColorMap = new HashMap<Integer, Color>();
            ptmTypeColorMap.put(PTM.MODMAX, Color.lightGray);
            ptmTypeColorMap.put(PTM.MODAA, new Color(110, 196, 97));
            ptmTypeColorMap.put(PTM.MODC, Color.CYAN);
            ptmTypeColorMap.put(PTM.MODCAA, Color.MAGENTA);
            ptmTypeColorMap.put(PTM.MODCP, Color.RED);
            ptmTypeColorMap.put(PTM.MODCPAA, Color.ORANGE);
            ptmTypeColorMap.put(PTM.MODN, Color.YELLOW);
            ptmTypeColorMap.put(PTM.MODNAA, Color.PINK);
            ptmTypeColorMap.put(PTM.MODNP, Color.BLUE);
            ptmTypeColorMap.put(PTM.MODNPAA, Color.GRAY);
        }
        return ptmTypeColorMap;
    }

    /**
     * Returns the list of modification in a pep novo format for search
     * @param modifications the list of modifications to export
     * @return 
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
