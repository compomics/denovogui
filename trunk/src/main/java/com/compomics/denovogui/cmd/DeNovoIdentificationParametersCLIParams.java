package com.compomics.denovogui.cmd;

import com.compomics.util.experiment.identification.search_parameters_cli.IdentificationParametersCLIParams;
import org.apache.commons.cli.Options;

/**
 * This class provides the parameters which can be used for the identification
 * parameters cli in SeachGUI.
 *
 * @author Marc Vaudel
 */
public class DeNovoIdentificationParametersCLIParams {

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {

        aOptions.addOption(IdentificationParametersCLIParams.OUTPUT.id, true, IdentificationParametersCLIParams.OUTPUT.description);
        aOptions.addOption(IdentificationParametersCLIParams.MODS.id, false, IdentificationParametersCLIParams.MODS.description);

        aOptions.addOption(IdentificationParametersCLIParams.PREC_PPM.id, true, IdentificationParametersCLIParams.PREC_PPM.description);
        aOptions.addOption(IdentificationParametersCLIParams.PREC_TOL_DA.id, true, IdentificationParametersCLIParams.PREC_TOL_DA.description);
        aOptions.addOption(IdentificationParametersCLIParams.FRAG_TOL.id, true, IdentificationParametersCLIParams.FRAG_TOL.description);
        aOptions.addOption(IdentificationParametersCLIParams.ENZYME.id, true, IdentificationParametersCLIParams.ENZYME.description);
        aOptions.addOption(IdentificationParametersCLIParams.FIXED_MODS.id, true, IdentificationParametersCLIParams.FIXED_MODS.description);
        aOptions.addOption(IdentificationParametersCLIParams.VARIABLE_MODS.id, true, IdentificationParametersCLIParams.VARIABLE_MODS.description);
        aOptions.addOption(IdentificationParametersCLIParams.MIN_CHARGE.id, true, IdentificationParametersCLIParams.MIN_CHARGE.description);
        aOptions.addOption(IdentificationParametersCLIParams.MAX_CHARGE.id, true, IdentificationParametersCLIParams.MAX_CHARGE.description);
        aOptions.addOption(IdentificationParametersCLIParams.MC.id, true, IdentificationParametersCLIParams.MC.description);
        aOptions.addOption(IdentificationParametersCLIParams.FI.id, true, IdentificationParametersCLIParams.FI.description);
        aOptions.addOption(IdentificationParametersCLIParams.RI.id, true, IdentificationParametersCLIParams.RI.description);
        aOptions.addOption(IdentificationParametersCLIParams.DB.id, true, IdentificationParametersCLIParams.DB.description);

        aOptions.addOption(IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PEPNOVO_FRAGMENTATION_MODEL.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PEPTNOVO_ESTIMATE_CHARGE.id, true, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description);
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";
        String formatter = "%-25s";

        output += "Mandatory parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.OUTPUT.id) + IdentificationParametersCLIParams.OUTPUT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_PPM.id) + IdentificationParametersCLIParams.PREC_PPM.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PREC_TOL.id) + IdentificationParametersCLIParams.PREC_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FRAG_TOL.id) + IdentificationParametersCLIParams.FRAG_TOL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.ENZYME.id) + IdentificationParametersCLIParams.ENZYME.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FIXED_MODS.id) + IdentificationParametersCLIParams.FIXED_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.VARIABLE_MODS.id) + IdentificationParametersCLIParams.VARIABLE_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MIN_CHARGE.id) + IdentificationParametersCLIParams.MIN_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MAX_CHARGE.id) + IdentificationParametersCLIParams.MAX_CHARGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MC.id) + IdentificationParametersCLIParams.MC.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.FI.id) + IdentificationParametersCLIParams.FI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.RI.id) + IdentificationParametersCLIParams.RI.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DB.id) + IdentificationParametersCLIParams.DB.description + "\n";

        output += "\n\nPepnovo advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.id) + IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.id) + IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_FRAGMENTATION_MODEL.id) + IdentificationParametersCLIParams.PEPNOVO_FRAGMENTATION_MODEL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.id) + IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.id) + IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPTNOVO_ESTIMATE_CHARGE.id) + IdentificationParametersCLIParams.PEPTNOVO_ESTIMATE_CHARGE.description + "\n";

        output += "\n\nHelp:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MODS.id) + IdentificationParametersCLIParams.MODS.description + "\n";

        return output;
    }
}
