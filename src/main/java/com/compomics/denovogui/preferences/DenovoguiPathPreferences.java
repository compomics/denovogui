package com.compomics.denovogui.preferences;

import com.compomics.denovogui.DeNovoSequencingHandler;
import com.compomics.denovogui.gui.ResultsFrame;
import com.compomics.util.preferences.UtilitiesPathPreferences;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class sets the path preferences for the files to read/write
 *
 * @author Marc
 */
public class DenovoguiPathPreferences {

    /**
     * Default name for the path configuration file
     */
    public static final String configurationFileName = "resources/conf/paths.txt";
    /**
     * Enum of the paths which can be set in DenovoGUI
     */
    public enum DenovoguiPathKey {

        /**
         * Directory where identification matches are temporarily saved to
         * reduce the memory footprint
         */
        matchesDirectory("denovogui_matches_directory", "Directory where identification matches are temporarily saved to reduce the memory footprint.", "", true);
        /**
         * The key used to refer to this path
         */
        private String id;
        /**
         * The description of the path usage
         */
        private String description;
        /**
         * The default sub directory or file to use in case all paths should be
         * included in a single directory.
         */
        private String defaultSubDirectory;
        /**
         * Indicates whether the path should be a folder
         */
        private boolean isDirectory;

        /**
         * Constructor
         *
         * @param id the id used to refer to this path key
         * @param description the description of the path usage
         * @param defaultSubDirectory the sub directory to use in case all paths
         * should be included in a single directory
         * @param isDirectory boolean indicating whether a folder is expected
         */
        private DenovoguiPathKey(String id, String description, String defaultSubDirectory, boolean isDirectory) {
            this.id = id;
            this.description = description;
            this.defaultSubDirectory = defaultSubDirectory;
            this.isDirectory = isDirectory;
        }

        /**
         * Returns the key from its id. Null if not found.
         *
         * @param id the id of the key of interest
         *
         * @return the key of interest
         */
        public static DenovoguiPathKey getKeyFromId(String id) {
            for (DenovoguiPathKey pathKey : values()) {
                if (pathKey.id.equals(id)) {
                    return pathKey;
                }
            }
            return null;
        }

        /**
         * Returns the id of the path.
         *
         * @return the id of the path
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the description of the path.
         *
         * @return the description of the path
         */
        public String getDescription() {
            return description;
        }

    }

    /**
     * Loads the path preferences from a text file.
     *
     * @param inputFile the file to load the path preferences from
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void loadPathPreferencesFromFile(File inputFile) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.equals("") && !line.startsWith("#")) {
                    loadPathPreferenceFromLine(line);
                }
            }
        } finally {
            br.close();
        }
    }

    /**
     * Loads a path to be set from a line
     *
     * @param line the line where to read the path from
     * @throws java.io.FileNotFoundException
     */
    public static void loadPathPreferenceFromLine(String line) throws FileNotFoundException {
        String id = UtilitiesPathPreferences.getPathID(line);
        if (id.equals("")) {
            throw new IllegalArgumentException("Impossible to parse path in " + line + ".");
        }
        DenovoguiPathKey denovoguiPathKey = DenovoguiPathKey.getKeyFromId(id);
        if (denovoguiPathKey == null) {
            UtilitiesPathPreferences.loadPathPreferenceFromLine(line);
        } else {
            String path = UtilitiesPathPreferences.getPath(line);
            File file = new File(path);
            if (!file.exists()) {
                throw new FileNotFoundException("File " + path + " not found.");
            }
            if (denovoguiPathKey.isDirectory && !file.isDirectory()) {
                throw new FileNotFoundException("Found a file when expecting a directory for " + denovoguiPathKey.id + ".");
            }
            setPathPreference(denovoguiPathKey, path);
        }
    }

    /**
     * Sets the path according to the given key and path
     *
     * @param denovoguiPathKey the key of the path
     * @param path the path to be set
     */
    public static void setPathPreference(DenovoguiPathKey denovoguiPathKey, String path) {
        switch (denovoguiPathKey) {
            case matchesDirectory:
                ResultsFrame.setCacheDirectoryParent(path);
                return;
            default:
                throw new UnsupportedOperationException("Path " + denovoguiPathKey.id + " not implemented.");
        }
    }

    /**
     * Sets all the paths inside a given folder.
     *
     * @param path the path of the folder where to redirect all paths.
     *
     * @throws FileNotFoundException
     */
    public static void setAllPathsIn(String path) throws FileNotFoundException {
        for (DenovoguiPathKey denovoguiPathKey : DenovoguiPathKey.values()) {
            String subDirectory = denovoguiPathKey.defaultSubDirectory;
            File newFile = new File(path, subDirectory);
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
            if (!newFile.exists()) {
                throw new FileNotFoundException(newFile.getAbsolutePath() + "could not be created.");
            }
            setPathPreference(denovoguiPathKey, newFile.getAbsolutePath());
        }
        UtilitiesPathPreferences.setAllPathsIn(path);
    }

    /**
     * Writes all path configurations to the given file.
     *
     * @param file the destination file
     *
     * @throws IOException
     */
    public static void writeConfigurationToFile(File file) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        try {
            writeConfigurationToFile(bw);
        } finally {
            bw.close();
        }
    }

    /**
     *
     * @param bw the writer to use for writing
     *
     * @throws IOException
     */
    public static void writeConfigurationToFile(BufferedWriter bw) throws IOException {
        for (DenovoguiPathKey pathKey : DenovoguiPathKey.values()) {
            writePathToFile(bw, pathKey);
        }
        UtilitiesPathPreferences.writeConfigurationToFile(bw);
    }

    /**
     * Writes the path of interest using the provided buffered writer.
     *
     * @param bw the writer to use for writing
     * @param pathKey the key of the path of interest
     *
     * @throws IOException
     */
    public static void writePathToFile(BufferedWriter bw, DenovoguiPathKey pathKey) throws IOException {
        bw.write(pathKey.id + UtilitiesPathPreferences.separator);
        switch (pathKey) {
            case matchesDirectory:
                bw.write(ResultsFrame.getCacheDirectoryParent());
                break;
            default:
                throw new UnsupportedOperationException("Path " + pathKey.id + " not implemented.");
        }
        bw.newLine();
    }
}
