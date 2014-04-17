package com.compomics.denovogui.cmd;

import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.preferences.DenovoguiPathPreferences;
import com.compomics.util.preferences.UtilitiesPathPreferences;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Allows the user to set the path settings in command line.
 *
 * @author Marc Vaudel
 */
public class PathSettingsCLI {

    /**
     * The input bean containing the user parameters.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;

    /**
     * Constructor.
     *
     * @param pathSettingsCLIInputBean an input bean containing the user
     * parameters
     */
    public PathSettingsCLI(PathSettingsCLIInputBean pathSettingsCLIInputBean) {
        this.pathSettingsCLIInputBean = pathSettingsCLIInputBean;
    }

    public Object call() {
        setPathSettings();
        return null;
    }

    /**
     * Sets the path settings according to the pathSettingsCLIInputBean.
     */
    public void setPathSettings() {

        String path = pathSettingsCLIInputBean.getTempFolder();
        if (!path.equals("")) {
            try {
                DenovoguiPathPreferences.setAllPathsIn(path);
            } catch (Exception e) {
                System.out.println("An error occurred when setting the temporary folder path.");
                e.printStackTrace();
            }
        }

        HashMap<String, String> pathInput = pathSettingsCLIInputBean.getPaths();
        for (String id : pathInput.keySet()) {
            try {
                DenovoguiPathPreferences.DenovoguiPathKey denovoguiPathKey = DenovoguiPathPreferences.DenovoguiPathKey.getKeyFromId(id);
                if (denovoguiPathKey == null) {
                    UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey = UtilitiesPathPreferences.UtilitiesPathKey.getKeyFromId(id);
                    if (utilitiesPathKey == null) {
                        System.out.println("Path id " + id + " not recognized.");
                    } else {
                        UtilitiesPathPreferences.setPathPreference(utilitiesPathKey, path);
                    }
                } else {
                    DenovoguiPathPreferences.setPathPreference(denovoguiPathKey, path);
                }
            } catch (Exception e) {
                System.out.println("An error occurred when setting the path " + id + ".");
                e.printStackTrace();
            }
        }

        // Write path file preference
        File destinationFile = new File(getJarFilePath(), DenovoguiPathPreferences.configurationFileName);
        try {
            DenovoguiPathPreferences.writeConfigurationToFile(destinationFile);
        } catch (Exception e) {
            System.out.println("An error occurred when saving the path preference to " + destinationFile.getAbsolutePath() + ".");
            e.printStackTrace();
        }

        System.out.println("Path configuration completed.");
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    protected String getJarFilePath() {
        return DeNovoGUIWrapper.getJarFilePath(this.getClass().getResource("PathSettingsCLI.class").getPath(), DeNovoGUIWrapper.toolName);
    }

    /**
     * DeNovoGUI path settings CLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "The DenovoGUI path settings command line allows setting the path of every configuration file created by DenovoGUI or set a temporary folder where all files will be stored." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://denovogui.googlecode.com and http://code.google.com/p/denovogui/wiki/DenovoGUICLI." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/denovogui." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            Options lOptions = new Options();
            PathSettingsCLIParams.createOptionsCLI(lOptions);
            BasicParser parser = new BasicParser();
            CommandLine line = parser.parse(lOptions, args);

            if (args.length == 0) {
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "========================================" + System.getProperty("line.separator"));
                lPrintWriter.print("DenovoGUI Path Settings - Command Line" + System.getProperty("line.separator"));
                lPrintWriter.print("========================================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(PathSettingsCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);
            } else {
                PathSettingsCLIInputBean cliInputBean = new PathSettingsCLIInputBean(line);
                PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(cliInputBean);
                pathSettingsCLI.call();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("DenovoGUI used up all the memory and had to be stopped. See the DenovoGUI log for details.");
            System.err.println("Ran out of memory!");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("DenovoGUI processing failed. See the DenovoGUI log for details.");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "PathSettingsCLI{"
                + ", cliInputBean=" + pathSettingsCLIInputBean
                + '}';
    }
}
