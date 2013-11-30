package com.compomics.denovogui.io;

/**
 * Enum of the available export types.
 *
 * @author Marc Vaudel
 */
public enum ExportType {

    /**
     * Tags.
     */
    tags(0, "Tag", "Export de novo tag sequences results."),
    /**
     * Peptides.
     */
    peptides(1, "Peptides", "Export de novo tag sequences mapped to protein sequences."),
    /**
     * BLAST.
     */
    blast(0, "Tag", "Export de novo tag sequences results blasted to protein sequences.");
    /**
     * The id number of this export type.
     */
    private int id;
    /**
     * The name of this export type.
     */
    private String name;
    /**
     * The description of this export type.
     */
    private String description;

    /**
     * Constructor.
     *
     * @param id the id number of this export type
     * @param name the name of this export type
     * @param description the description of this export type
     */
    private ExportType(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the id number of this export type.
     *
     * @return the id number of this export type
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of this export type.
     *
     * @return the name of this export type
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of this export type.
     *
     * @return the description of this export type
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a list of available export types.
     *
     * @return a list of available export types
     */
    public static ExportType[] getExportTypes() {
        ExportType[] options = new ExportType[3];
        options[0] = tags;
        options[1] = peptides;
        options[2] = blast;
        return options;
    }

    /**
     * Returns the different implemented exports as list of command line option.
     *
     * @return the different implemented exports as list of command line option
     */
    public static String getCommandLineOptions() {
        String result = "";
        for (ExportType option : getExportTypes()) {
            if (!result.equals("")) {
                result += ", ";
            }
            result += option.getId() + ": " + option.getDescription();
        }
        return result;
    }
}
