/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui;

import com.compomics.denovogui.util.Properties;
import com.compomics.software.CompomicsWrapper;
import java.io.File;

/**
 * Wrapper to start the tool
 *
 * @author Marc
 */
public class DenovoguiWrapper extends CompomicsWrapper {

    /**
     * The name of the jar file. Must be equal to the name given in the pom
     * file.
     */
    public static final String toolName = "DenovoGUI";
    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     */
    public DenovoguiWrapper() {
        this(null);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments (ignored if null)
     */
    public DenovoguiWrapper(String[] args) {

        // get the version number set in the pom file
        String jarFileName = toolName + "-" + new Properties().getVersion() + ".jar";
        String path = this.getClass().getResource("DenovoguiWrapper.class").getPath();
        // remove starting 'file:' tag if there
        if (path.startsWith("file:")) {
            path = path.substring("file:".length(), path.indexOf(jarFileName));
        } else {
            path = path.substring(0, path.indexOf(jarFileName));
        }
        path = path.replace("%20", " ");
        path = path.replace("%5b", "[");
        path = path.replace("%5d", "]");
        File jarFile = new File(path, jarFileName);
        // get the splash 
        String splash = "denovogui-splash.png";
        String mainClass = "denovogui.gui.DeNovoGUI";

        launchTool(toolName, jarFile, splash, mainClass, args);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new DenovoguiWrapper(args);
    }
    
}
