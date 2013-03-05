/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.idfilereaders.PepNovoIdfileReader;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the PepNovoIDfileReader.
 * 
 * @author Thilo Muth
 */
public class PepNovoIdfileReaderTest extends TestCase {
    
    private PepNovoIdfileReader idfileReader;
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    @Before
    public void setUp() throws ClassNotFoundException, IOException, InterruptedException {
        idfileReader = new PepNovoIdfileReader(new File("src/test/resources/test.mgf.out"));        
    }

    @Test
    public void testGetAllSpectrumMatches() throws Exception {
        HashSet<SpectrumMatch> allSpectrumMatches = idfileReader.getAllSpectrumMatches(null);
        //get the Iterator
        Iterator iter = allSpectrumMatches.iterator();
        SpectrumMatch sm = (SpectrumMatch) iter.next();
        assertEquals("test.mgf.out_cus_1530: Scan 2818 (rt=24.5551) [ELEEIVQPIISK]", sm.getKey());
    }
}
