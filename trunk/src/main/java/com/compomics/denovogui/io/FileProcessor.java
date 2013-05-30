package com.compomics.denovogui.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dirty helper class for counting the number of spectra and chunking.
 *
 * @author Thilo Muth.
 */
public class FileProcessor {

    /**
     * Returns the number of spectra in multiple files.
     *
     * @param files MGF files.
     * @return Number of spectra in multiple files.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static int getNumberOfSpectra(List<File> files) throws FileNotFoundException, IOException {
        String line;
        int counter = 0;
        // Iterate over all the files.
        for (File file : files) {
            BufferedReader br = new BufferedReader(new FileReader(file));
            // Cycle the file.
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.indexOf("END IONS") >= 0) {
                    // Increment the spectrumCounter by one.
                    counter++;
                }
            }
        }
        return counter;
    }

    /**
     * Writes the chunked/merged output files.
     *
     * @param files the files to chunk
     * @param chunkSize the chunk size
     * @return the chunked files.
     * @throws IOException
     */
    public static List<File> chunkFiles(List<File> files, int chunkSize) throws IOException {

        final String path;
        String line;
        int spectrumCounter = 0;
        int chunkNumber = 1;
        BufferedReader br = null;
        BufferedWriter bos = null;
        path = files.get(0).getParent();

        List<File> chunkedFiles = new ArrayList<File>();

        // Iterate over all the files.
        for (File file : files) {
            br = new BufferedReader(new FileReader(file));

            // Write new output file for the first time.
            if (spectrumCounter == 0) {
                // Read the filename.
                String filename = file.getName();

                int start = filename.lastIndexOf(".");
                String outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
                //  outputFilename = outputFilename.replaceAll(" ", "");
                File output = new File(path + File.separator + outputFilename);
                chunkedFiles.add(output);
                bos = new BufferedWriter(new FileWriter(output));
            }

            // Cycle the file.
            while ((line = br.readLine()) != null) {
                line = line.trim();
                bos.write(line);
                bos.newLine();
                if (line.indexOf("END IONS") >= 0) {
                    // Increment the spectrumCounter by one.
                    spectrumCounter++;

                    // Each specified offset the file gets chunked.
                    if (spectrumCounter % chunkSize == 0) {
                        chunkNumber++;
                        bos.flush();
                        bos.close();
                        String filename = file.getName();
                        int start = filename.lastIndexOf(".");
                        String outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
                        //outputFilename = outputFilename.replaceAll(" ", "");                                          
                        File output = new File(path + File.separator + outputFilename);
                        chunkedFiles.add(output);
                        bos = new BufferedWriter(new FileWriter(output));
                    }
                }
            }
        }
        br.close();
        bos.flush();
        bos.close();
        return chunkedFiles;
    }
    
    /**
     * Merges and deletes the (splitted) output files.
     *
     * @param outFiles The output files to be merged.     
     * @return Merged output file.
     * @throws IOException
     */
    public static File mergeAndDeleteOutputFiles(List<File> outFiles) throws IOException {
        File first = outFiles.get(0);
        File mergedFile = new File(first.getParent(), first.getName().substring(0, first.getName().lastIndexOf("_")) + ".mgf.out");
        BufferedWriter bWriter = new BufferedWriter(new FileWriter(mergedFile));
        String line;
        boolean isContent;
        
        for (File file : outFiles) {            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            isContent = false;
            while ((line = reader.readLine()) != null) {
                if(line.startsWith(">>")){
                    isContent = true;
                }
                if (isContent) {
                    if (!line.startsWith("#Processed")) {
                        bWriter.write(line);
                        bWriter.newLine();
                    }
                }
            }            
            reader.close();
            
            // Delete redundant output files.
            if(file.exists()) file.delete();
            
        }
        bWriter.flush();
        bWriter.close();
        
        return mergedFile;
    }
}
