package com.compomics.denovogui.io;

import com.compomics.util.Util;
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
     * @throws IOException
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
                    if (line.indexOf("END IONS") >= 0) {
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
     * @param mgfFiles The MGF file chunks.
     * @param waitingHandler the waiting handler
     * @throws IOException
     */
    public static void deleteChunkFiles(List<File> files, WaitingHandler waitingHandler) throws IOException {

        for (File file : files) {            
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
     * @throws IOException
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
     * Returns the result file corresponding to the given spectrum file and
     * output folder.
     *
     * @param outFolder the output folder
     * @param spectrumFile the spectrum file
     * @return the corresponding out file
     */
    public static File getOutFile(File outFolder, File spectrumFile) {
        return new File(outFolder, Util.getFileName(spectrumFile) + ".out");
    }

    /**
     * Returns the mfg file corresponding to the given out file.
     *
     * @param outFile the out file
     * @return the corresponding mfg file
     */
    public static File getMgfFile(File outFile) {
        String fileName = Util.getFileName(outFile);
        if (!fileName.endsWith(".out")) {
            throw new IllegalArgumentException("Output file " + fileName + " does not have the '.out' extension.");
        }
        String outName = fileName.substring(0, fileName.lastIndexOf("."));
        return new File(outFile.getParent(), outName);
    }

    /**
     * Returns a list of out files expected from a list of spectrum files.
     *
     * @param outFolder the out folder
     * @param spectrumFiles list of spectrum files
     * @return expected list of out files
     */
    public static ArrayList<File> getOutFiles(File outFolder, ArrayList<File> spectrumFiles) {
        ArrayList<File> outFiles = new ArrayList<File>();
        for (File file : spectrumFiles) {
            outFiles.add(getOutFile(outFolder, file));
        }
        return outFiles;
    }
}
