package com.compomics.util.experiment.identification.protein_sequences;

import java.io.File;

/**
 * Minimal DeNovoGUI compatibility facade for the removed Utilities 4 sequence
 * factory.
 */
public class SequenceFactory {

    /**
     * The singleton instance.
     */
    private static final SequenceFactory instance = new SequenceFactory();
    /**
     * The selected FASTA file.
     */
    private File currentFastaFile;

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static SequenceFactory getInstance() {
        return instance;
    }

    /**
     * Clears the selected FASTA file.
     */
    public void clearFactory() {
        currentFastaFile = null;
    }

    /**
     * Returns the currently selected FASTA file.
     *
     * @return the currently selected FASTA file
     */
    public File getCurrentFastaFile() {
        return currentFastaFile;
    }

    /**
     * Returns the number of loaded sequences.
     *
     * @return the number of loaded sequences
     */
    public int getNSequences() {
        return currentFastaFile == null ? 0 : 1;
    }

    /**
     * Returns the number of target sequences.
     *
     * @return the number of target sequences
     */
    public int getNTargetSequences() {
        return getNSequences();
    }

    /**
     * Indicates whether decoys are reversed by default.
     *
     * @return true
     */
    public boolean isDefaultReversed() {
        return true;
    }

    /**
     * Legacy compatibility method.
     *
     * @param decoyInMemory ignored
     */
    public void setDecoyInMemory(boolean decoyInMemory) {
    }

    /**
     * Legacy compatibility method.
     *
     * @param cacheSize ignored
     */
    public void setnCache(int cacheSize) {
    }
}
