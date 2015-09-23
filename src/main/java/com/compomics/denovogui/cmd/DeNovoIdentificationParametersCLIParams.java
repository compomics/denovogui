package com.compomics.denovogui.cmd;

import com.compomics.util.experiment.identification.search_parameters_cli.IdentificationParametersCLIParams;
import org.apache.commons.cli.Options;

/**
 * This class provides the parameters which can be used for the identification
 * parameters cli in DeNovoGUI.
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

        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.id, true, IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.id, true, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.id, true, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.id, true, IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.id, true, IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.id, true, IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.id, true, IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.id, true, IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.id, true, IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MAX_PRECUSOR_ADJUSTMENT.id, true, IdentificationParametersCLIParams.DIRECTAG_MAX_PRECUSOR_ADJUSTMENT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.id, true, IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MIN_PRECUSOR_ADJUSTMENT.id, true, IdentificationParametersCLIParams.DIRECTAG_MIN_PRECUSOR_ADJUSTMENT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.id, true, IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.id, true, IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.id, true, IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.id, true, IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_PRECUSOR_ADJUSTMENT_STEP.id, true, IdentificationParametersCLIParams.DIRECTAG_PRECUSOR_ADJUSTMENT_STEP.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_TAG_LENGTH.id, true, IdentificationParametersCLIParams.DIRECTAG_TAG_LENGTH.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.id, true, IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.description);
        aOptions.addOption(IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.id, true, IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.description);

        aOptions.addOption(IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.id, true, IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.description);
        aOptions.addOption(IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.id, true, IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.id, true, IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.description);
        aOptions.addOption(IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.id, true, IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.description);
        
        aOptions.addOption(IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.id, true, IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.description);
        aOptions.addOption(IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.id, true, IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.description);

        aOptions.addOption(IdentificationParametersCLIParams.USAGE.id, false, IdentificationParametersCLIParams.USAGE.description);
        aOptions.addOption(IdentificationParametersCLIParams.USAGE_2.id, false, IdentificationParametersCLIParams.USAGE_2.description);
        aOptions.addOption(IdentificationParametersCLIParams.USAGE_3.id, false, IdentificationParametersCLIParams.USAGE_3.description);
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

        output += "\n\nPepNovo+ advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.id) + IdentificationParametersCLIParams.PEPNOVO_CORRECT_PREC_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.id) + IdentificationParametersCLIParams.PEPNOVO_DISCARD_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_FRAGMENTATION_MODEL.id) + IdentificationParametersCLIParams.PEPNOVO_FRAGMENTATION_MODEL.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.id) + IdentificationParametersCLIParams.PEPNOVO_GENERATE_BLAST.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.id) + IdentificationParametersCLIParams.PEPNOVO_HITLIST_LENGTH.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PEPTNOVO_ESTIMATE_CHARGE.id) + IdentificationParametersCLIParams.PEPTNOVO_ESTIMATE_CHARGE.description + "\n";

        output += "\n\nDirecTag advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.id) + IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.id) + IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.id) + IdentificationParametersCLIParams.DIRECTAG_COMPLEMENT_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.id) + IdentificationParametersCLIParams.DIRECTAG_DEISOTOPING_MODE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.id) + IdentificationParametersCLIParams.DIRECTAG_DUPLICATE_SPECTRA.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.id) + IdentificationParametersCLIParams.DIRECTAG_INTENSITY_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.id) + IdentificationParametersCLIParams.DIRECTAG_ISOTOPE_MZ_TOLERANCE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.id) + IdentificationParametersCLIParams.DIRECTAG_MAX_DYNAMIC_MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.id) + IdentificationParametersCLIParams.DIRECTAG_MAX_PEAK_COUNT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_PRECUSOR_ADJUSTMENT.id) + IdentificationParametersCLIParams.DIRECTAG_MAX_PRECUSOR_ADJUSTMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.id) + IdentificationParametersCLIParams.DIRECTAG_MAX_TAG_COUNT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MIN_PRECUSOR_ADJUSTMENT.id) + IdentificationParametersCLIParams.DIRECTAG_MIN_PRECUSOR_ADJUSTMENT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.id) + IdentificationParametersCLIParams.DIRECTAG_MZ_FIDELITY_SCORE_WEIGHT.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.id) + IdentificationParametersCLIParams.DIRECTAG_NUM_CHARGE_STATES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.id) + IdentificationParametersCLIParams.DIRECTAG_NUM_INTENSITY_CLASSES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.id) + IdentificationParametersCLIParams.DIRECTAG_OUTPUT_SUFFIX.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_PRECUSOR_ADJUSTMENT_STEP.id) + IdentificationParametersCLIParams.DIRECTAG_PRECUSOR_ADJUSTMENT_STEP.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_TAG_LENGTH.id) + IdentificationParametersCLIParams.DIRECTAG_ADJUST_PRECURSOR_MASS.DIRECTAG_TAG_LENGTH + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.id) + IdentificationParametersCLIParams.DIRECTAG_TIC_CUTOFF_PERCENTAGE.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.id) + IdentificationParametersCLIParams.DIRECTAG_USE_CHARGE_STATE_FROM_MS.description + "\n";

        output += "\n\npNovo+ advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.id) + IdentificationParametersCLIParams.PNOVO_NUMBER_OF_PEPTIDES.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.id) + IdentificationParametersCLIParams.PNOVO_LOWER_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.id) + IdentificationParametersCLIParams.PNOVO_UPPER_PRECURSOR_MASS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.id) + IdentificationParametersCLIParams.PNOVO_ACTIVATION_TYPE.description + "\n";

        output += "\n\nNovor advanced parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.id) + IdentificationParametersCLIParams.NOVOR_FRAGMENTATION.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.id) + IdentificationParametersCLIParams.NOVOR_MASS_ANALYZER.description + "\n";
        
        output += "\n\nHelp:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.MODS.id) + IdentificationParametersCLIParams.MODS.description + "\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.USAGE.id) + IdentificationParametersCLIParams.USAGE.description + "\n";

        return output;
    }
}
