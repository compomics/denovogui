package com.compomics.denovogui.execution.jobs;

import com.compomics.denovogui.io.FileProcessor;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.InstaNovoParameters;
import java.io.File;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for InstaNovo job setup.
 *
 * @author CompOmics
 */
public class InstaNovoJobTest {

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Tests Unix virtual environment executable resolution.
     */
    @Test
    public void testUnixVirtualEnvironmentExecutableResolution() throws Exception {

        File instaNovoFolder = temporaryFolder.newFolder("InstaNovo");
        File executable = new File(instaNovoFolder, ".venv/bin/instanovo");
        Assert.assertTrue(executable.getParentFile().mkdirs());
        Assert.assertTrue(executable.createNewFile());

        Assert.assertEquals(executable, InstaNovoJob.getExecutable(instaNovoFolder));
    }

    /**
     * Tests Windows virtual environment executable resolution.
     */
    @Test
    public void testWindowsVirtualEnvironmentExecutableResolution() throws Exception {

        File instaNovoFolder = temporaryFolder.newFolder("InstaNovo");
        File executable = new File(instaNovoFolder, ".venv/Scripts/instanovo.exe");
        Assert.assertTrue(executable.getParentFile().mkdirs());
        Assert.assertTrue(executable.createNewFile());

        Assert.assertEquals(executable, InstaNovoJob.getExecutable(instaNovoFolder));
    }

    /**
     * Tests result file naming.
     */
    @Test
    public void testResultFileNaming() throws Exception {

        File outputFolder = temporaryFolder.newFolder("output");
        File spectrumFile = temporaryFolder.newFile("sample.mgf");

        Assert.assertEquals(new File(outputFolder, "sample.instanovo.csv"), FileProcessor.getInstaNovoResultFile(outputFolder, spectrumFile));
        Assert.assertEquals(new File(outputFolder, "sample.instanovoplus.csv"), FileProcessor.getInstaNovoPlusResultFile(outputFolder, spectrumFile));
        Assert.assertEquals(new File(outputFolder, "sample.instanovo.refined.csv"), FileProcessor.getInstaNovoRefinedResultFile(outputFolder, spectrumFile));
    }

    /**
     * Tests refined command construction.
     */
    @Test
    public void testRefinedCommandConstruction() throws Exception {

        File instaNovoFolder = temporaryFolder.newFolder("InstaNovo");
        File executable = new File(instaNovoFolder, ".venv/bin/instanovo");
        Assert.assertTrue(executable.getParentFile().mkdirs());
        Assert.assertTrue(executable.createNewFile());

        File spectrumFile = temporaryFolder.newFile("sample.mgf");
        File outputFolder = temporaryFolder.newFolder("output");

        InstaNovoParameters parameters = new InstaNovoParameters();
        parameters.setInstaNovoModel("instanovo-v1.2.0");
        parameters.setInstaNovoPlusModel("instanovoplus-v1.1.0");
        parameters.setNumberOfBeams(7);
        parameters.setUseKnapsack(true);
        parameters.setSaveAllPredictions(true);
        parameters.setBatchSize(8);
        parameters.setForceCpu(true);

        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setIdentificationAlgorithmParameter(Advocate.instanovo.getIndex(), parameters);

        InstaNovoJob job = new InstaNovoJob(
                instaNovoFolder,
                spectrumFile,
                outputFolder,
                searchParameters,
                InstaNovoJob.Mode.refined,
                new WaitingHandlerCLIImpl(),
                new CommandLineExceptionHandler()
        );

        List<String> command = job.getCommand();

        Assert.assertEquals(executable.getPath(), command.get(0));
        Assert.assertTrue(command.contains("predict"));
        Assert.assertTrue(command.contains("--with-refinement"));
        Assert.assertTrue(command.contains("--instanovo-model"));
        Assert.assertTrue(command.contains("instanovo-v1.2.0"));
        Assert.assertTrue(command.contains("--instanovo-plus-model"));
        Assert.assertTrue(command.contains("instanovoplus-v1.1.0"));
        Assert.assertTrue(command.contains("num_beams=7"));
        Assert.assertTrue(command.contains("use_knapsack=true"));
        Assert.assertTrue(command.contains("save_all_predictions=true"));
        Assert.assertTrue(command.contains("batch_size=8"));
        Assert.assertTrue(command.contains("force_cpu=true"));
        Assert.assertTrue(command.contains("log_interval=1"));
        Assert.assertTrue(command.contains(FileProcessor.getInstaNovoRefinedResultFile(outputFolder, spectrumFile).getAbsolutePath()));
    }
}
