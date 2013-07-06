package com.compomics.denovogui.cmd;

import com.compomics.util.experiment.identification.SearchParametersCLIParams;
import org.apache.commons.cli.Options;

/**
 * Command line option parameters for DeNovoCLI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class DeNovoCLIParams {

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {

        aOptions.addOption(SearchParametersCLIParams.SPECTRUM_FILES.id, true, SearchParametersCLIParams.SPECTRUM_FILES.description);
        aOptions.addOption(SearchParametersCLIParams.OUTPUT_FOLDER.id, true, SearchParametersCLIParams.OUTPUT_FOLDER.description);
        aOptions.addOption(SearchParametersCLIParams.SEARCH_PARAMETERS.id, true, SearchParametersCLIParams.SEARCH_PARAMETERS.description);
        aOptions.addOption(SearchParametersCLIParams.PREC_TOL.id, true, SearchParametersCLIParams.PREC_TOL.description);
        aOptions.addOption(SearchParametersCLIParams.FRAG_TOL.id, true, SearchParametersCLIParams.FRAG_TOL.description);
        aOptions.addOption(SearchParametersCLIParams.ENZYME.id, true, SearchParametersCLIParams.ENZYME.description);
        aOptions.addOption(SearchParametersCLIParams.FIXED_MODS.id, true, SearchParametersCLIParams.FIXED_MODS.description);
        aOptions.addOption(SearchParametersCLIParams.VARIABLE_MODS.id, true, SearchParametersCLIParams.VARIABLE_MODS.description);
        aOptions.addOption(SearchParametersCLIParams.MIN_CHARGE.id, true, SearchParametersCLIParams.MIN_CHARGE.description);
        aOptions.addOption(SearchParametersCLIParams.MAX_CHARGE.id, true, SearchParametersCLIParams.MAX_CHARGE.description);
        aOptions.addOption(SearchParametersCLIParams.HITLIST_LENGTH_DE_NOVO.id, true, SearchParametersCLIParams.HITLIST_LENGTH_DE_NOVO.description);
        aOptions.addOption(SearchParametersCLIParams.REMOVE_PREC.id, true, SearchParametersCLIParams.REMOVE_PREC.description);
        aOptions.addOption(SearchParametersCLIParams.SCALE_PREC.id, true, SearchParametersCLIParams.SCALE_PREC.description);
        aOptions.addOption(SearchParametersCLIParams.ESTIMATE_CHARGE_DE_NOVO.id, true, SearchParametersCLIParams.ESTIMATE_CHARGE_DE_NOVO.description);
        aOptions.addOption(SearchParametersCLIParams.MGF_SPLITTING_LIMIT.id, true, SearchParametersCLIParams.MGF_SPLITTING_LIMIT.description);
        aOptions.addOption(SearchParametersCLIParams.MGF_MAX_SPECTRA.id, true, SearchParametersCLIParams.MGF_MAX_SPECTRA.description);
        aOptions.addOption(SearchParametersCLIParams.PEP_NOVO_LOCATION.id, true, SearchParametersCLIParams.PEP_NOVO_LOCATION.description);
        aOptions.addOption(SearchParametersCLIParams.THREADS.id, true, SearchParametersCLIParams.THREADS.description);

        // note: remember to add new parameters to the getOptionsAsString below as well!!
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
        output += "-" + String.format(formatter, SearchParametersCLIParams.SPECTRUM_FILES.id) + SearchParametersCLIParams.SPECTRUM_FILES.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.OUTPUT_FOLDER.id) + SearchParametersCLIParams.OUTPUT_FOLDER.description + "\n";

        output += "\n\nOptional common parameters:\n\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.SEARCH_PARAMETERS.id) + SearchParametersCLIParams.SEARCH_PARAMETERS.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.PREC_TOL.id) + SearchParametersCLIParams.PREC_TOL.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.FRAG_TOL.id) + SearchParametersCLIParams.FRAG_TOL.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.ENZYME.id) + SearchParametersCLIParams.ENZYME.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.FIXED_MODS.id) + SearchParametersCLIParams.FIXED_MODS.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.VARIABLE_MODS.id) + SearchParametersCLIParams.VARIABLE_MODS.description + "\n";

        output += "\n\nOptional advanced parameters:\n\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.HITLIST_LENGTH_DE_NOVO.id) + SearchParametersCLIParams.HITLIST_LENGTH_DE_NOVO.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.DISCARD_SPECTRA.id) + SearchParametersCLIParams.DISCARD_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.CORRECT_PRECURSOR_MASS.id) + SearchParametersCLIParams.CORRECT_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.ESTIMATE_CHARGE.id) + SearchParametersCLIParams.ESTIMATE_CHARGE.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.FRAGMENTATION_MODEL.id) + SearchParametersCLIParams.FRAGMENTATION_MODEL.description + "\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.THREADS.id) + SearchParametersCLIParams.THREADS.description + "\n\n";
        output += "-" + String.format(formatter, SearchParametersCLIParams.PEP_NOVO_LOCATION.id) + SearchParametersCLIParams.PEP_NOVO_LOCATION.description + "\n";

        return output;
    }
}
