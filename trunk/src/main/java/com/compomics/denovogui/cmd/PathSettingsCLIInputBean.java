package com.compomics.denovogui.cmd;

import com.compomics.denovogui.preferences.DenovoguiPathPreferences;
import com.compomics.util.preferences.UtilitiesPathPreferences;
import java.util.HashMap;
import org.apache.commons.cli.CommandLine;

/**
 * Parses the command line and retrieves the user input.
 *
 * @author Marc Vaudel
 */
public class PathSettingsCLIInputBean {

    /**
     * The path set to the temp folder.
     */
    private String tempFolder = "";
    /**
     * The specific paths sets for every option.
     */
    private HashMap<String, String> paths = new HashMap<String, String>();

    /**
     * Construct a FollowUpCLIInputBean from an Apache CLI instance.
     *
     * @param aLine the command line
     */
    public PathSettingsCLIInputBean(CommandLine aLine) {

        if (aLine.hasOption(PathSettingsCLIParams.ALL.id)) {
            tempFolder = aLine.getOptionValue(PathSettingsCLIParams.ALL.id);
        }

        for (DenovoguiPathPreferences.DenovoguiPathKey denovoguiPathKey : DenovoguiPathPreferences.DenovoguiPathKey.values()) {
            String id = denovoguiPathKey.getId();
            if (aLine.hasOption(id)) {
                paths.put(id, aLine.getOptionValue(id));
            }
        }
        for (UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey : UtilitiesPathPreferences.UtilitiesPathKey.values()) {
            String id = utilitiesPathKey.getId();
            if (aLine.hasOption(id)) {
                paths.put(id, aLine.getOptionValue(id));
            }
        }
    }

    /**
     * Returns the temp folder, an empty string if not set.
     *
     * @return the temp folder
     */
    public String getTempFolder() {
        return tempFolder;
    }

    /**
     * Returns the specific paths provided by the user in a map: Path id ->
     * path.
     *
     * @return the specific paths provided by the user
     */
    public HashMap<String, String> getPaths() {
        return paths;
    }

    /**
     * Indicates whether the user gave some path configuration input.
     *
     * @return a boolean indicating whether the user gave some path
     * configuration input.
     */
    public boolean hasInput() {
        return !tempFolder.equals("") || !paths.isEmpty();
    }
}
