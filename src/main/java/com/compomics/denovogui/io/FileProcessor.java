/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.io;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
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
 * @author Thilo Muth
 */
public class FileProcessor {
    
    /**
     * Returns the number of spectra in multiple files.
     * @param files MGF files.
     * @return Number of spectra in multiple files.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static int getNumberOfSpectra(List<File> files) throws FileNotFoundException, IOException {
        BufferedReader br = null;
        String line;
        int counter = 0;
        // Iterate over all the files.
        for (File file : files) {
            br = new BufferedReader(new FileReader(file));
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
     * @throws IOException
     */
    public static List<File> chunkFiles(List<File> files, int chunkSize) throws IOException {
        File output;
        final String path;
        String outputFilename, line, filename;
        int start, spectrumCounter = 0;
        int chunkNumber = 1;
        BufferedReader br = null;
        BufferedWriter bos = null;
        path = files.get(0).getParent();

        List<File> chunkedFiles = new ArrayList<File>();
        // Iterate over all the files.
        for (File file: files) {
            br = new BufferedReader(new FileReader(file));

            // Write new output file for the first time.
            if (spectrumCounter == 0) {
                // Read the filename.
                filename = file.getName();

                start = filename.lastIndexOf(".");
                outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
                //  outputFilename = outputFilename.replaceAll(" ", "");
                output = new File(path + File.separator + outputFilename);
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
                        filename = file.getName();
                        start = filename.lastIndexOf(".");
                        outputFilename = filename.substring(0, start) + "_" + chunkNumber + filename.substring(start);
                        //outputFilename = outputFilename.replaceAll(" ", "");                                          
                        output = new File(path + File.separator + outputFilename);
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
}
