/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import static junit.framework.Assert.assertEquals;
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
        while(iter.hasNext()){
            SpectrumMatch sm = (SpectrumMatch) iter.next();
            if(sm.getKey().contains("Scan 835")){
               assertEquals("test.mgf_cus_7: Scan 835 (rt=12.4589) [NQIGDKEK]", sm.getKey());         
            }
        }
        
    }
}
