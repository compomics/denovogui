package com.compomics.denovogui.io;

import com.compomics.util.io.IoUtil;
import com.compomics.util.waiting.WaitingHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import uk.ac.ebi.pride.tools.braf.BufferedRandomAccessFile;

/**
 * Dirty helper class for counting the number of spectra and chunking.
 *
 * @author Thilo Muth.
 */
public class FileProcessor {

    /**
     * Writes the chunk/merged output files.
     *
     * @param file the file to chunk
     * @param chunkSize the chunk size
     * @param remaining no. of remaining spectra (not fitting in file number
     * division)
     * @param nSpectra no. of all spectra.
     * @param waitingHandler waiting handler displaying the progress and
     * allowing the user to cancel the progress
     *
     * @return the chunk files.
     * @throws IOException thrown if there are problems with the random access
     * file
     */
    public static ArrayList<File> chunkFile(File file, int chunkSize, int remaining, int nSpectra, WaitingHandler waitingHandler) throws IOException {

        final String path;
        String line;
        int spectrumCounter = 0;
        int chunkNumber = 1;
        BufferedRandomAccessFile br = new BufferedRandomAccessFile(file, "r", 1024 * 100);
        path = file.getParent();

        ArrayList<File> chunkedFiles = new ArrayList<File>();

        long progressStep = br.length() / 100;
        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxSecondaryProgressCounter(100);
            waitingHandler.setSecondaryProgressCounter(0);
        }
        long progress = 0;
        boolean streamClosed = false;

        try {
            // Read the filename.
            String filename = file.getName();

            int start = filename.lastIndexOf(".");
            String outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
            //  outputFilename = outputFilename.replaceAll(" ", "");
            File output = new File(path + File.separator + outputFilename);
            chunkedFiles.add(output);
            BufferedWriter bos = new BufferedWriter(new FileWriter(output));

            try {
                boolean addedRemaining = false;
                int offset = 0;
                // Cycle the file.
                while ((line = br.getNextLine()) != null) {
                    line = line.trim();
                    if (!streamClosed) {
                        bos.write(line);
                        bos.newLine();
                    }
                    if (line.contains("END IONS")) {
                        // Increment the spectrumCounter by one.
                        spectrumCounter++;

                        // Increase the chunkSize for each remaining spectrum.
                        if (!addedRemaining && remaining > 0) {
                            chunkSize++;
                            remaining--;
                            addedRemaining = true;
                        }

                        if (spectrumCounter % (chunkSize + offset) == 0) {
                            chunkNumber++;
                            bos.flush();
                            bos.close();
                            streamClosed = true;

                            if (spectrumCounter != nSpectra) {
                                outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
                                //outputFilename = outputFilename.replaceAll(" ", "");                                          
                                output = new File(path + File.separator + outputFilename);
                                chunkedFiles.add(output);
                                bos = new BufferedWriter(new FileWriter(output));
                                streamClosed = false;
                                offset += chunkSize;
                                if (addedRemaining) {
                                    chunkSize--;
                                    addedRemaining = false;
                                }
                            }
                        }
                    }
                    long readIndex = br.getFilePointer();
                    progress += readIndex;
                    if (waitingHandler != null) {
                        if (progress > progressStep) {
                            waitingHandler.increaseSecondaryProgressCounter();
                            progress = 0;
                        }
                        if (waitingHandler.isRunCanceled()) {
                            break;
                        }
                    }
                }
            } finally {
                bos.close();
            }
        } finally {
            br.close();
        }
        return chunkedFiles;
    }

    /**
     * Deletes the chunk files.
     *
     * @param mgfFiles the mgf file chunks.
     * @param waitingHandler the waiting handler
     * @throws IOException thrown if a file cannot be deleted
     */
    public static void deleteChunkFiles(List<File> mgfFiles, WaitingHandler waitingHandler) throws IOException {

        for (File file : mgfFiles) {
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    waitingHandler.appendReport("Failed to delete: " + file, true, true);
                    System.out.println("Failed to delete: " + file);
                }
            }
        }
    }

    /**
     * Merges and deletes the (split) output files.
     *
     * @param outFiles The output files to be merged.
     * @throws IOException thrown if there are problems with the reading/writing
     * to the file
     */
    public static void mergeAndDeleteOutputFiles(List<File> outFiles) throws IOException {

        File first = outFiles.get(0);
        File mergedFile = new File(first.getParent(), first.getName().substring(0, first.getName().lastIndexOf("_")) + ".mgf.out");
        BufferedWriter bWriter = new BufferedWriter(new FileWriter(mergedFile));

        try {
            String line;

            for (File file : outFiles) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                boolean isContent = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(">>")) {
                        isContent = true;
                    }
                    if (isContent) {
                        if (line.contains("#Problem")) {
                            bWriter.write(line.substring(0, line.indexOf("#Problem")));
                            bWriter.newLine();
                            bWriter.write("#Problem reading spectrum...");
                            bWriter.newLine();
                        } else if (!line.startsWith("#Processed")) {
                            bWriter.write(line);
                            bWriter.newLine();
                        }
                    }
                }
                reader.close();

                // Delete redundant output files.
                if (file.exists()) {
                    file.delete();
                }
            }
            bWriter.flush();
        } finally {
            bWriter.close();
        }
    }

    /**
     * Returns the PepNovo result file corresponding to the given spectrum file
     * and output folder.
     *
     * @param outFolder the output folder
     * @param spectrumFile the spectrum file
     * @return the corresponding out file
     */
    public static File getOutFile(File outFolder, File spectrumFile) {
        return new File(outFolder, IoUtil.getFileName(spectrumFile) + ".out");
    }

    /**
     * Returns the pNovo result file corresponding to the given spectrum file
     * and output folder.
     *
     * @param outFolder the output folder
     * @param spectrumFile the spectrum file
     * @return the corresponding out file
     */
    public static File getPNovoResultFile(File outFolder, File spectrumFile) {
        String mgfName = spectrumFile.getName().substring(0, spectrumFile.getName().lastIndexOf("."));
        return new File(outFolder, mgfName + ".pnovo.txt");
    }

    /**
     * Returns the Novor result file corresponding to the given spectrum file
     * and output folder.
     *
     * @param outFolder the output folder
     * @param spectrumFile the spectrum file
     * @return the corresponding out file
     */
    public static File getNovorResultFile(File outFolder, File spectrumFile) {
        String mgfName = spectrumFile.getName().substring(0, spectrumFile.getName().lastIndexOf("."));
        return new File(outFolder, mgfName + ".novor.csv");
    }

    /**
     * Returns the DirectTag result file corresponding to the given spectrum
     * file and output folder.
     *
     * @param outFolder the output folder
     * @param spectrumFile the spectrum file
     * @return the corresponding out file
     */
    public static File getTagsFile(File outFolder, File spectrumFile) {
        String mgfName = spectrumFile.getName().substring(0, spectrumFile.getName().lastIndexOf("."));
        return new File(outFolder, mgfName + ".tags");
    }

    /**
     * Returns the mgf file corresponding to the given identification file.
     *
     * @param outFile the out file
     * @return the corresponding mgf file
     * @throws IllegalArgumentException thrown if the file format is not
     * recognized
     */
    public static File getMgfFile(File outFile) throws IllegalArgumentException {
        String fileName = IoUtil.getFileName(outFile);
        if (fileName.endsWith(".out")) {
            String mgfName = fileName.substring(0, fileName.lastIndexOf("."));
            return new File(outFile.getParent(), mgfName);
        } else if (fileName.endsWith(".tags")) {
            String mgfName = fileName.substring(0, fileName.lastIndexOf("."));
            return new File(outFile.getParent(), mgfName + ".mgf");
        }
        throw new IllegalArgumentException("Output file " + fileName + " format not recognized.");
    }

    /**
     * Returns a list of PepNovo out files expected from a list of spectrum
     * files.
     *
     * @param outFolder the output folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getOutFiles(File outFolder, List<File> spectrumFiles) {
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            File tempFile = getOutFile(outFolder, file);
            if (tempFile.exists()) {
                outFiles.add(tempFile);
            }
        }
        return outFiles;
    }

    /**
     * Returns a list of DirecTag tags files expected from a list of spectrum
     * files.
     *
     * @param outFolder the output folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getTagsFiles(File outFolder, List<File> spectrumFiles) {
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            File tempFile = getTagsFile(outFolder, file);
            if (tempFile.exists()) {
                outFiles.add(tempFile);
            }
        }
        return outFiles;
    }

    /**
     * Returns a list of pNovo result files expected from a list of spectrum
     * files.
     *
     * @param outFolder the output folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getPNovoResultFiles(File outFolder, List<File> spectrumFiles) {
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            File tempFile = getPNovoResultFile(outFolder, file);
            if (tempFile.exists()) {
                outFiles.add(tempFile);
            }
        }
        return outFiles;
    }

    /**
     * Returns a list of Novor result files expected from a list of spectrum
     * files.
     *
     * @param outFolder the output folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getNovorResultFiles(File outFolder, List<File> spectrumFiles) {
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            File tempFile = getNovorResultFile(outFolder, file);
            if (tempFile.exists()) {
                outFiles.add(tempFile);
            }
        }
        return outFiles;
    }

    /**
     * Returns a list of result files expected from a list of spectrum files.
     *
     * @param resultsFolder the results folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getAllResultFiles(File resultsFolder, List<File> spectrumFiles) {
        return getAllResultFiles(resultsFolder, spectrumFiles, true, true, true, true);
    }

    /**
     * Returns a list of result files expected from a list of spectrum files.
     *
     * @param resultsFolder the results folder
     * @param spectrumFiles list of spectrum files
     * @param pepNovo add PepNovo files
     * @param direcTag add DirecTag files
     * @param pNovo add pNovo files
     * @param novor add Novor files
     * @return expected list of out files
     */
    public static ArrayList<File> getAllResultFiles(File resultsFolder, List<File> spectrumFiles, 
            boolean pepNovo, boolean direcTag, boolean pNovo, boolean novor) {
        ArrayList<File> resultFiles = new ArrayList<File>();
        if (pepNovo) {
            resultFiles.addAll(getOutFiles(resultsFolder, spectrumFiles));
        }
        if (direcTag) {
            resultFiles.addAll(getTagsFiles(resultsFolder, spectrumFiles));
        }
        if (pNovo) {
            resultFiles.addAll(getPNovoResultFiles(resultsFolder, spectrumFiles));
        }
        if (novor) {
            resultFiles.addAll(getNovorResultFiles(resultsFolder, spectrumFiles));
        }
        return resultFiles;
    }
}
