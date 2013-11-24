package com.compomics.denovogui;

import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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

    @Before
    public void setUp() throws ClassNotFoundException, IOException, InterruptedException {
        idfileReader = new PepNovoIdfileReader(new File("src/test/resources/test.mgf.out"), new SearchParameters());
    }

    @Test
    public void testGetAllSpectrumMatches() throws Exception {
        HashSet<SpectrumMatch> allSpectrumMatches = idfileReader.getAllSpectrumMatches(null);
        for (SpectrumMatch sm : allSpectrumMatches) {
            if (sm.getKey().contains("Scan 835")) {
                assertEquals("test.mgf_cus_7: Scan 835 (rt=12.4589) [NQIGDKEK]", sm.getKey());         
            }
        }
    }
}
