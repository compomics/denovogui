package com.compomics.denovogui.util;

import java.io.InputStream;

/**
 * This class contains many of the properties that are used during the use of
 * the tool.
 *
 * @author Harald Barsnes
 */
public class Properties {

    /**
     * Creates a new empty Properties object.
     */
    public Properties() {
    }

    /**
     * Retrieves the version number set in the pom file.
     *
     * @return the version number of the software
     */
    public static String getVersion() {

        java.util.Properties p = new java.util.Properties();

        try {
            InputStream is = (new Properties()).getClass().getClassLoader().getResourceAsStream("denovogui.properties");
            p.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return p.getProperty("denovogui.version");
    }
}
