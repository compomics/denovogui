package com.compomics.denovogui;

import com.compomics.denovogui.preferences.DenovoguiPathPreferences;
import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import java.io.File;
import java.io.IOException;

/**
 * Wrapper to start the tool.
 *
 * @author Marc Vaudel
 */
public class DeNovoGUIWrapper extends CompomicsWrapper {

    /**
     * The name of the jar file. Must be equal to the name given in the pom
     * file.
     */
    public static final String toolName = "DeNovoGUI";

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     */
    public DeNovoGUIWrapper() {
        this(null);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments (ignored if null)
     */
    public DeNovoGUIWrapper(String[] args) {

        // get the version number set in the pom file
        String jarFileName = toolName + "-" + new Properties().getVersion() + ".jar";
        String path = getJarFilePath();
        File jarFile = new File(path, jarFileName);
        // get the splash 
        String splash = "denovogui-splash.png";
        String mainClass = "com.compomics.denovogui.gui.DeNovoGUI";
        // Set path for utilities preferences
        try {
            setPathConfiguration();
        } catch (Exception e) {
            System.out.println("Impossible to load path configuration, default will be used.");
        }

        launchTool(toolName, jarFile, splash, mainClass, args);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new DeNovoGUIWrapper(args);
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    protected String getJarFilePath() {
        return DeNovoGUIWrapper.getJarFilePath(this.getClass().getResource("DeNovoGUIWrapper.class").getPath(), DeNovoGUIWrapper.toolName);
    }

    /**
     * Sets the path configuration.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(getJarFilePath(), DenovoguiPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            DenovoguiPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
    }
}
