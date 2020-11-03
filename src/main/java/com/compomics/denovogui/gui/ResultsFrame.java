package com.compomics.denovogui.gui;

import com.compomics.denovogui.DeNovoGUIWrapper;
import com.compomics.denovogui.gui.tablemodels.AssumptionsTableModel;
import com.compomics.denovogui.gui.tablemodels.SpectrumTableModel;
import com.compomics.denovogui.io.ExportType;
import com.compomics.denovogui.io.TextExporter;
import com.compomics.util.Util;
import com.compomics.util.db.object.ObjectsCache;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.biology.aminoacids.AminoAcid;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidPattern;
import com.compomics.util.experiment.biology.aminoacids.sequence.AminoAcidSequence;
import com.compomics.util.experiment.biology.ions.Ion;
import static com.compomics.util.experiment.biology.ions.Ion.IonType.IMMONIUM_ION;
import static com.compomics.util.experiment.biology.ions.Ion.IonType.REPORTER_ION;
import com.compomics.util.experiment.biology.ions.IonFactory;
import com.compomics.util.experiment.biology.ions.NeutralLoss;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.biology.ions.impl.TagFragmentIon;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.modifications.ModificationType;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.amino_acid_tags.MassGap;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.amino_acid_tags.TagComponent;
import com.compomics.util.experiment.identification.protein_inference.PeptideProteinMapping;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.io.identification.IdfileReader;
import com.compomics.util.experiment.io.identification.IdfileReaderFactory;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.refinement_parameters.PepnovoAssumptionDetails;
import com.compomics.util.gui.file_handling.FileChooserUtil;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.advanced.PeptideVariantsParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.identification.tool_specific.PepnovoParameters;
import com.compomics.util.pride.prideobjects.Sample;
import java.awt.Color;
import java.awt.Component;
import static java.awt.Frame.MAXIMIZED_BOTH;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Frame for showing the results of de novo sequencing.
 *
 * @author Harald Barsnes
 */
public class ResultsFrame extends javax.swing.JFrame {

    /**
     * A references to the main frame.
     */
    private DeNovoGUI deNovoGUI;
    /**
     * The current assumptions.
     */
    private ArrayList<SpectrumIdentificationAssumption> assumptions = new ArrayList<SpectrumIdentificationAssumption>();
    /**
     * The annotation parameters.
     */
    private AnnotationParameters annotationParameters = new AnnotationParameters();
    /**
     * The specific annotation parameters.
     */
    private SpecificAnnotationParameters specificAnnotationParameters;
    /**
     * The charge menus.
     */
    private HashMap<Integer, JCheckBoxMenuItem> chargeMenus = new HashMap<Integer, JCheckBoxMenuItem>();
    /**
     * The neutral loss menus.
     */
    private HashMap<NeutralLoss, JCheckBoxMenuItem> lossMenus = new HashMap<NeutralLoss, JCheckBoxMenuItem>();
    /**
     * The compomics PTM factory.
     */
    private ModificationFactory modFactory = ModificationFactory.getInstance();
    /**
     * The query spectra table column header tooltips.
     */
    private ArrayList<String> querySpectraTableToolTips;
    /**
     * The de novo peptides table column header tooltips.
     */
    private ArrayList<String> deNovoPeptidesTableToolTips;
    /**
     * The sequencing parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The name of the folder used for caching.
     */
    private static String CACHE_DIRECTORY_NAME = "matches";
    /**
     * The parent directory of the folder used for caching.
     */
    private static String CACHE_PARENT_DIRECTORY = "resources";
    /**
     * De novo identification.
     */
    private Identification identification;
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * The label with for the numbers in the jsparklines columns.
     */
    private int labelWidth = 50;
    /**
     * The minimal rank score.
     */
    private double minRankScore = Double.MAX_VALUE;
    /**
     * The maximal rank score.
     */
    private double maxRankScore = Double.MIN_VALUE;
    /**
     * The maximal PepNovo score.
     */
    private double maxPepnovoScore = Double.MIN_VALUE;
    /**
     * The minimal direct tag e-value.
     */
    private double minDirectTagEvalue = Double.MAX_VALUE;
    /**
     * The maximal direct tag e-value.
     */
    private double maxDirectTagEvalue = Double.MIN_VALUE;
    /**
     * The minimal pNovo score.
     */
    private double minPNovoScore = Double.MAX_VALUE;
    /**
     * The maximal pNovo score.
     */
    private double maxPNovoScore = Double.MIN_VALUE;
    /**
     * The minimal Novor score.
     */
    private double minNovorScore = Double.MAX_VALUE;
    /**
     * The maximal Novor score.
     */
    private double maxNovorScore = Double.MIN_VALUE;
    /**
     * The maximal n gap.
     */
    private double maxNGap = 0;
    /**
     * The maximal c gap.
     */
    private double maxCGap = 0;
    /**
     * The maximal theoretic peptide mass.
     */
    private double maxIdentificationMz = 0;
    /**
     * The maximal charge identified.
     */
    private double maxIdentificationCharge = 0;
    /**
     * The ordered spectrum keys.
     */
    private ArrayList<String> orderedSpectrumTitles = null;
    /**
     * The Find panel.
     */
    private FindPanel findPanel;
    /**
     * The object cache used for the identification.
     */
    private ObjectsCache objectsCache = new ObjectsCache();
    /**
     * The number of advocate types loaded.
     */
    private int numAdvocatesLoaded = 0;
    /**
     * The export settings dialog.
     */
    private ExportSettingsDialog exportSettingsDialog;
    /**
     * Exception handler.
     */
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/compomics/denovogui/issues");
    /**
     * The spectrum annotator to use for tags.
     */
    private TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();
    /**
     * The spectrum annotator to use for peptides.
     */
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();

    /**
     * Creates a new ResultsPanel.
     *
     * @param deNovoGUI a references to the main frame
     * @param resultFiles the result files
     * @param searchParameters the search parameters
     */
    public ResultsFrame(DeNovoGUI deNovoGUI, ArrayList<File> resultFiles, SearchParameters searchParameters) {
        initComponents();
        this.deNovoGUI = deNovoGUI;
        this.searchParameters = searchParameters;
        annotationParameters.setParametersFromSearchParameters(searchParameters);
        annotationParameters.setIntensityLimit(0.0); // annotate all peaks by default
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        // set the title of the frame and add the icon
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));
        setUpGUI();
        if (resultFiles != null) {
            setVisible(true);
            displayResults(resultFiles);
        } else {
            openNewFile();
        }
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {

        // add the find panel
        findPanel = new FindPanel(this);
        findPanel.setEnabled(false);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(findPanel);

        spectrumFileComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // get the spectrum files
        ArrayList<File> spectrumFileNames = deNovoGUI.getSpectrumFiles();
        String[] filesArray = new String[spectrumFileNames.size()];
        int cpt = 0;
        for (File tempFile : spectrumFileNames) {
            filesArray[cpt++] = tempFile.getName();
        }
        spectrumFileComboBox.setModel(new DefaultComboBoxModel(filesArray));

        // add default neutral losses to display
        spectrumAnnotationMenuPanel.add(annotationMenuBar);

        // make sure that the scroll panes are see-through
        querySpectraTableScrollPane.getViewport().setOpaque(false);
        deNovoPeptidesTableScrollPane.getViewport().setOpaque(false);

        querySpectraTable.getTableHeader().setReorderingAllowed(false);
        deNovoMatchesTable.getTableHeader().setReorderingAllowed(false);

        querySpectraTable.setAutoCreateRowSorter(true);
        deNovoMatchesTable.setAutoCreateRowSorter(true);

        // correct the color for the upper right corner
        JPanel queryCorner = new JPanel();
        queryCorner.setBackground(querySpectraTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, queryCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(deNovoMatchesTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);

        // hide items that are not implemented
        annotationColorsJMenuItem.setVisible(false);
        jSeparator14.setVisible(false);

        // setup the column header tooltips
        querySpectraTableToolTips = new ArrayList<String>();
        querySpectraTableToolTips.add(null);
        querySpectraTableToolTips.add("ID");
        querySpectraTableToolTips.add("Spectrum Title");
        querySpectraTableToolTips.add("Precusor m/z");
        querySpectraTableToolTips.add("Precursor Charge");
        querySpectraTableToolTips.add("Precursor Intensity");
        querySpectraTableToolTips.add("Retention Time");
        querySpectraTableToolTips.add("Number of Peaks");
        querySpectraTableToolTips.add("Max PepNovo+ Score");
        querySpectraTableToolTips.add("Min DirecTag E-Value");
        querySpectraTableToolTips.add("Max pNovo+ Score");
        querySpectraTableToolTips.add("Max Novor Score");
        querySpectraTableToolTips.add("De Novo Solution");

        deNovoPeptidesTableToolTips = new ArrayList<String>();
        deNovoPeptidesTableToolTips.add(null);
        deNovoPeptidesTableToolTips.add("Sequencing Algorithm");
        deNovoPeptidesTableToolTips.add("De Novo Sequences");
        deNovoPeptidesTableToolTips.add("Precursor m/z");
        deNovoPeptidesTableToolTips.add("Precursor Charge");
        deNovoPeptidesTableToolTips.add("N-terminal Gap");
        deNovoPeptidesTableToolTips.add("C-terminal Gap");
        deNovoPeptidesTableToolTips.add("PepNovo+ Rank Score");
        deNovoPeptidesTableToolTips.add("PepNovo+ Score");
        deNovoPeptidesTableToolTips.add("DirecTag E-Value");
        deNovoPeptidesTableToolTips.add("pNovo+ Score");
        deNovoPeptidesTableToolTips.add("Novor Score");
        deNovoPeptidesTableToolTips.add("BLAST Sequence");

        // set the title
        this.setTitle("DeNovoGUI " + deNovoGUI.getVersion());
    }

    /**
     * Set the spectrum table properties.
     */
    private void setSpectrumTableProperties() {

        double maxMz = Math.max(deNovoGUI.getMsFileHandler().getMaxPrecMz(), maxIdentificationMz);
        double maxCharge = Math.max(deNovoGUI.getMsFileHandler().getMaxCharge(), maxIdentificationCharge);

        querySpectraTable.getColumn(" ").setMaxWidth(50);
        querySpectraTable.getColumn(" ").setMinWidth(50);
        querySpectraTable.getColumn("ID").setMaxWidth(37);
        querySpectraTable.getColumn("ID").setMinWidth(37);
        querySpectraTable.getColumn("  ").setMaxWidth(30);
        querySpectraTable.getColumn("  ").setMinWidth(30);

        // set up the id column color map and tooltips
        HashMap<Integer, Color> idColorMap = new HashMap<Integer, Color>();
        HashMap<Integer, String> idTooltipMap = new HashMap<Integer, String>();
        if (numAdvocatesLoaded > 1) {
            idColorMap.put(0, Color.LIGHT_GRAY);
            idTooltipMap.put(0, "No de novo solutions");
            for (int i = 1; i < numAdvocatesLoaded; i++) {
                idColorMap.put(i, Color.YELLOW);
                idTooltipMap.put(i, "One or more de novo algorithm missing");
            }
            idColorMap.put(numAdvocatesLoaded, sparklineColor);
            if (numAdvocatesLoaded == 2) {
                idTooltipMap.put(numAdvocatesLoaded, "Found de novo solutions for both algorithms");
            } else {
                idTooltipMap.put(numAdvocatesLoaded, "Found de novo solutions for all algorithms");
            }
        } else {
            idColorMap.put(0, Color.LIGHT_GRAY);
            idColorMap.put(1, sparklineColor);
            idTooltipMap.put(0, "No de novo solutions");
            idTooltipMap.put(1, "De novo solution found");
        }

        querySpectraTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.LIGHT_GRAY, idColorMap, idTooltipMap));

        querySpectraTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept-new.png")),
                new ImageIcon(this.getClass().getResource("/icons/error-new.png")),
                "De Novo Solution", "No De Novo Solution"));
        querySpectraTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCharge, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, labelWidth - 30);
        querySpectraTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxMz, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, deNovoGUI.getMsFileHandler().getMaxPrecInt(), sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, labelWidth + 20);
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Int").getCellRenderer()).setLogScale(true);
        querySpectraTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, deNovoGUI.getMsFileHandler().getMinRT(),
                deNovoGUI.getMsFileHandler().getMaxPrecRT(), deNovoGUI.getMsFileHandler().getMaxPrecRT() / 50, sparklineColor, sparklineColor));
        ((JSparklinesIntervalChartTableCellRenderer) querySpectraTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);
        ((JSparklinesIntervalChartTableCellRenderer) querySpectraTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
        querySpectraTable.getColumn("#Peaks").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, (double) deNovoGUI.getMsFileHandler().getMaxPeakCount(), sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("#Peaks").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Score (P)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPepnovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Score (P)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Score (D)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxDirectTagEvalue, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Score (D)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Score (p)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPNovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Score (p)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Score (N)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNovorScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Score (N)").getCellRenderer()).showNumberAndChart(true, labelWidth);

        // make sure that the user is made aware that the tool is doing something during sorting of the query table
        querySpectraTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            @Override
            public void sorterChanged(RowSorterEvent e) {

                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    querySpectraTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the icon to a "waiting version"
                    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")));
                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    querySpectraTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    // change the icon to the normal version
                    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));
                }
            }
        });
    }

    /**
     * Set the assumptions table properties.
     */
    private void setAssumptionsTableProperties() {

        double maxMz = Math.max(deNovoGUI.getMsFileHandler().getMaxPrecMz(), maxIdentificationMz);
        double maxCharge = Math.max(deNovoGUI.getMsFileHandler().getMaxCharge(), maxIdentificationCharge);

        deNovoMatchesTable.getColumn("").setMaxWidth(50);
        deNovoMatchesTable.getColumn("").setMinWidth(50);
        deNovoMatchesTable.getColumn("SA").setMaxWidth(37);
        deNovoMatchesTable.getColumn("SA").setMinWidth(37);
        deNovoMatchesTable.getColumn("  ").setMaxWidth(30);
        deNovoMatchesTable.getColumn("  ").setMinWidth(30);

        if (minRankScore > maxRankScore) {
            minRankScore = maxRankScore;
        }

        deNovoMatchesTable.getColumn("Rank Score (P)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, minRankScore, maxRankScore, Color.BLUE, Color.RED));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Rank Score (P)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("Score (D)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxDirectTagEvalue, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Score (D)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("Score (P)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPepnovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Score (P)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("Score (p)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPNovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Score (p)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("Score (N)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNovorScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Score (N)").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("N-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNGap, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("N-Gap").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("C-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCGap, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("C-Gap").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxMz, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoMatchesTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCharge, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoMatchesTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, labelWidth - 30);
        deNovoMatchesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/blast.png")),
                null,
                "Click to BLAST tag sequence", null));

        deNovoMatchesTable.getColumn("SA").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.LIGHT_GRAY, Advocate.getAdvocateColorMap(), Advocate.getAdvocateToolTipMap()));
    }

    /**
     * Returns a reference to the main DeNovoGUI frame.
     *
     * @return a reference to the main DeNovoGUI frame.
     */
    public DeNovoGUI getDeNovoGUI() {
        return deNovoGUI;
    }

    /**
     * Returns the de novo identifications.
     *
     * @return the de novo identifications
     */
    public Identification getIdentifications() {
        return identification;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        annotationMenuBar = new javax.swing.JMenuBar();
        splitterMenu5 = new javax.swing.JMenu();
        ionsMenu = new javax.swing.JMenu();
        aIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        bIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        cIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        xIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        yIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        zIonCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu8 = new javax.swing.JMenu();
        otherMenu = new javax.swing.JMenu();
        precursorCheckMenu = new javax.swing.JCheckBoxMenuItem();
        immoniumIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
        relatedIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
        reporterIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
        lossSplitter = new javax.swing.JMenu();
        lossMenu = new javax.swing.JMenu();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        adaptCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu2 = new javax.swing.JMenu();
        chargeMenu = new javax.swing.JMenu();
        splitterMenu3 = new javax.swing.JMenu();
        deNovoMenu = new javax.swing.JMenu();
        forwardIonsDeNovoCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        rewindIonsDeNovoCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        deNovoChargeOneJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        deNovoChargeTwoJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        individualDeNovoCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        splitterMenu9 = new javax.swing.JMenu();
        settingsMenu = new javax.swing.JMenu();
        allCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        highResAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        defaultAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        annotationColorsJMenuItem = new javax.swing.JMenuItem();
        splitterMenu4 = new javax.swing.JMenu();
        exportGraphicsMenu = new javax.swing.JMenu();
        exportSpectrumGraphicsJMenuItem = new javax.swing.JMenuItem();
        exportSpectrumValuesJMenuItem = new javax.swing.JMenuItem();
        splitterMenu6 = new javax.swing.JMenu();
        helpJMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        splitterMenu7 = new javax.swing.JMenu();
        resetAnnotationMenu = new javax.swing.JMenu();
        deNovoChargeButtonGroup = new javax.swing.ButtonGroup();
        bcakgroundPanel = new javax.swing.JPanel();
        debovoResultsPanel = new javax.swing.JPanel();
        querySpectraPanel = new javax.swing.JPanel();
        querySpectraTableScrollPane = new javax.swing.JScrollPane();
        querySpectraTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) querySpectraTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumFileLabel = new javax.swing.JLabel();
        spectrumFileComboBox = new javax.swing.JComboBox();
        deNovoMatchesPanel = new javax.swing.JPanel();
        deNovoPeptidesTableScrollPane = new javax.swing.JScrollPane();
        deNovoMatchesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) deNovoPeptidesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumViewerPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        annotationsMenuItem = new javax.swing.JMenuItem();
        exportMenu = new javax.swing.JMenu();
        exportTagMatchesMenuItem = new javax.swing.JMenuItem();
        exportPeptideMatchesMenuItem = new javax.swing.JMenuItem();
        exportBlastMatchesMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        fixedPtmsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMainMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        bugReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();

        annotationMenuBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        annotationMenuBar.setOpaque(false);

        splitterMenu5.setText("|");
        splitterMenu5.setEnabled(false);
        annotationMenuBar.add(splitterMenu5);

        ionsMenu.setText("Ions");

        aIonCheckBoxMenuItem.setText("a");
        aIonCheckBoxMenuItem.setToolTipText("a-ions");
        aIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(aIonCheckBoxMenuItem);

        bIonCheckBoxMenuItem.setSelected(true);
        bIonCheckBoxMenuItem.setText("b");
        bIonCheckBoxMenuItem.setToolTipText("b-ions");
        bIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(bIonCheckBoxMenuItem);

        cIonCheckBoxMenuItem.setText("c");
        cIonCheckBoxMenuItem.setToolTipText("c-ions");
        cIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(cIonCheckBoxMenuItem);
        ionsMenu.add(jSeparator6);

        xIonCheckBoxMenuItem.setText("x");
        xIonCheckBoxMenuItem.setToolTipText("x-ions");
        xIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(xIonCheckBoxMenuItem);

        yIonCheckBoxMenuItem.setSelected(true);
        yIonCheckBoxMenuItem.setText("y");
        yIonCheckBoxMenuItem.setToolTipText("y-ions");
        yIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(yIonCheckBoxMenuItem);

        zIonCheckBoxMenuItem.setText("z");
        zIonCheckBoxMenuItem.setToolTipText("z-ions");
        zIonCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonCheckBoxMenuItemActionPerformed(evt);
            }
        });
        ionsMenu.add(zIonCheckBoxMenuItem);

        annotationMenuBar.add(ionsMenu);

        splitterMenu8.setText("|");
        splitterMenu8.setEnabled(false);
        annotationMenuBar.add(splitterMenu8);

        otherMenu.setText("Other");

        precursorCheckMenu.setSelected(true);
        precursorCheckMenu.setText("Precursor");
        precursorCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(precursorCheckMenu);

        immoniumIonsCheckMenu.setSelected(true);
        immoniumIonsCheckMenu.setText("Immonium");
        immoniumIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                immoniumIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(immoniumIonsCheckMenu);

        relatedIonsCheckMenu.setSelected(true);
        relatedIonsCheckMenu.setText("Related");
        relatedIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                relatedIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(relatedIonsCheckMenu);

        reporterIonsCheckMenu.setSelected(true);
        reporterIonsCheckMenu.setText("Reporter");
        reporterIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reporterIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(reporterIonsCheckMenu);

        annotationMenuBar.add(otherMenu);

        lossSplitter.setText("|");
        annotationMenuBar.add(lossSplitter);

        lossMenu.setText("Loss");
        lossMenu.add(jSeparator7);

        adaptCheckBoxMenuItem.setSelected(true);
        adaptCheckBoxMenuItem.setText("Adapt");
        adaptCheckBoxMenuItem.setToolTipText("Adapt losses to sequence and modifications");
        adaptCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adaptCheckBoxMenuItemActionPerformed(evt);
            }
        });
        lossMenu.add(adaptCheckBoxMenuItem);

        annotationMenuBar.add(lossMenu);

        splitterMenu2.setText("|");
        splitterMenu2.setEnabled(false);
        annotationMenuBar.add(splitterMenu2);

        chargeMenu.setText("Charge");
        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        splitterMenu3.setEnabled(false);
        annotationMenuBar.add(splitterMenu3);

        deNovoMenu.setText("De Novo");

        forwardIonsDeNovoCheckBoxMenuItem.setSelected(true);
        forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setSelected(true);
        rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
        rewindIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rewindIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(rewindIonsDeNovoCheckBoxMenuItem);
        deNovoMenu.add(jSeparator19);

        deNovoChargeButtonGroup.add(deNovoChargeOneJRadioButtonMenuItem);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(true);
        deNovoChargeOneJRadioButtonMenuItem.setText("Single Charge");
        deNovoChargeOneJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeOneJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeOneJRadioButtonMenuItem);

        deNovoChargeButtonGroup.add(deNovoChargeTwoJRadioButtonMenuItem);
        deNovoChargeTwoJRadioButtonMenuItem.setText("Double Charge");
        deNovoChargeTwoJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeTwoJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeTwoJRadioButtonMenuItem);
        deNovoMenu.add(jSeparator1);

        individualDeNovoCheckBoxMenuItem.setSelected(true);
        individualDeNovoCheckBoxMenuItem.setText("Amino Acid Scores");
        individualDeNovoCheckBoxMenuItem.setToolTipText("<html>\nColor indicates the amino acid scores.<br>\nLight color: low score, darker color: high score.\n</html>");
        individualDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                individualDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(individualDeNovoCheckBoxMenuItem);

        annotationMenuBar.add(deNovoMenu);

        splitterMenu9.setText("|");
        splitterMenu9.setEnabled(false);
        annotationMenuBar.add(splitterMenu9);

        settingsMenu.setText("Settings");

        allCheckBoxMenuItem.setText("Show All Peaks");
        allCheckBoxMenuItem.setToolTipText("Show all peaks or just the annotated peaks");
        allCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(allCheckBoxMenuItem);

        highResAnnotationCheckBoxMenuItem.setSelected(true);
        highResAnnotationCheckBoxMenuItem.setText("High Resolution");
        highResAnnotationCheckBoxMenuItem.setToolTipText("Use high resolution annotation");
        highResAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highResAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(highResAnnotationCheckBoxMenuItem);
        settingsMenu.add(jSeparator5);

        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItem.setText("Default Annotation");
        defaultAnnotationCheckBoxMenuItem.setToolTipText("Use default annotation");
        defaultAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(defaultAnnotationCheckBoxMenuItem);
        settingsMenu.add(jSeparator14);

        annotationColorsJMenuItem.setText("Annotation Colors");
        annotationColorsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationColorsJMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(annotationColorsJMenuItem);

        annotationMenuBar.add(settingsMenu);

        splitterMenu4.setText("|");
        splitterMenu4.setEnabled(false);
        annotationMenuBar.add(splitterMenu4);

        exportGraphicsMenu.setText("Export");

        exportSpectrumGraphicsJMenuItem.setText("Spectrum");
        exportSpectrumGraphicsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumGraphicsJMenuItemActionPerformed(evt);
            }
        });
        exportGraphicsMenu.add(exportSpectrumGraphicsJMenuItem);

        exportSpectrumValuesJMenuItem.setText("Spectrum as MGF");
        exportSpectrumValuesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumValuesJMenuItemActionPerformed(evt);
            }
        });
        exportGraphicsMenu.add(exportSpectrumValuesJMenuItem);

        annotationMenuBar.add(exportGraphicsMenu);

        splitterMenu6.setText("|");
        splitterMenu6.setEnabled(false);
        annotationMenuBar.add(splitterMenu6);

        helpJMenu.setText("Help");

        helpMenuItem.setText("Help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpJMenu.add(helpMenuItem);

        annotationMenuBar.add(helpJMenu);

        splitterMenu7.setText("|");
        splitterMenu7.setEnabled(false);
        annotationMenuBar.add(splitterMenu7);

        resetAnnotationMenu.setText("<html><a href>Reset Annotation</a></html>");
        resetAnnotationMenu.setFocusable(false);
        resetAnnotationMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                resetAnnotationMenuMenuSelected(evt);
            }
        });
        annotationMenuBar.add(resetAnnotationMenu);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("De Novo Results");
        setMinimumSize(new java.awt.Dimension(600, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bcakgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        debovoResultsPanel.setBackground(new java.awt.Color(255, 255, 255));

        querySpectraPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Query Spectra"));
        querySpectraPanel.setOpaque(false);

        querySpectraTable.setModel(new SpectrumTableModel());
        querySpectraTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        querySpectraTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                querySpectraTableMouseReleased(evt);
            }
        });
        querySpectraTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                querySpectraTableKeyReleased(evt);
            }
        });
        querySpectraTableScrollPane.setViewportView(querySpectraTable);

        spectrumFileLabel.setText("Spectrum File");

        spectrumFileComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        spectrumFileComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumFileComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout querySpectraPanelLayout = new javax.swing.GroupLayout(querySpectraPanel);
        querySpectraPanel.setLayout(querySpectraPanelLayout);
        querySpectraPanelLayout.setHorizontalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(querySpectraPanelLayout.createSequentialGroup()
                .addGroup(querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(querySpectraPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1013, Short.MAX_VALUE))
                    .addGroup(querySpectraPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(spectrumFileLabel)
                        .addGap(18, 18, 18)
                        .addComponent(spectrumFileComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        querySpectraPanelLayout.setVerticalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, querySpectraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectrumFileLabel)
                    .addComponent(spectrumFileComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );

        deNovoMatchesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("De Novo Matches"));
        deNovoMatchesPanel.setOpaque(false);

        deNovoMatchesTable.setModel(new com.compomics.denovogui.gui.tablemodels.AssumptionsTableModel());
        deNovoMatchesTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        deNovoMatchesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                deNovoMatchesTableMouseMoved(evt);
            }
        });
        deNovoMatchesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deNovoMatchesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                deNovoMatchesTableMouseReleased(evt);
            }
        });
        deNovoMatchesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                deNovoMatchesTableKeyReleased(evt);
            }
        });
        deNovoPeptidesTableScrollPane.setViewportView(deNovoMatchesTable);

        javax.swing.GroupLayout deNovoMatchesPanelLayout = new javax.swing.GroupLayout(deNovoMatchesPanel);
        deNovoMatchesPanel.setLayout(deNovoMatchesPanelLayout);
        deNovoMatchesPanelLayout.setHorizontalGroup(
            deNovoMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoMatchesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1013, Short.MAX_VALUE)
                .addContainerGap())
        );
        deNovoMatchesPanelLayout.setVerticalGroup(
            deNovoMatchesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoMatchesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumViewerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Viewer"));
        spectrumViewerPanel.setOpaque(false);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJPanel.setLayout(new javax.swing.BoxLayout(spectrumJPanel, javax.swing.BoxLayout.LINE_AXIS));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        javax.swing.GroupLayout spectrumViewerPanelLayout = new javax.swing.GroupLayout(spectrumViewerPanel);
        spectrumViewerPanel.setLayout(spectrumViewerPanelLayout);
        spectrumViewerPanelLayout.setHorizontalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1013, Short.MAX_VALUE)
                    .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        spectrumViewerPanelLayout.setVerticalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout debovoResultsPanelLayout = new javax.swing.GroupLayout(debovoResultsPanel);
        debovoResultsPanel.setLayout(debovoResultsPanelLayout);
        debovoResultsPanelLayout.setHorizontalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deNovoMatchesPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(querySpectraPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        debovoResultsPanelLayout.setVerticalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deNovoMatchesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout bcakgroundPanelLayout = new javax.swing.GroupLayout(bcakgroundPanel);
        bcakgroundPanel.setLayout(bcakgroundPanelLayout);
        bcakgroundPanelLayout.setHorizontalGroup(
            bcakgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bcakgroundPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        bcakgroundPanelLayout.setVerticalGroup(
            bcakgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        exitMenuItem.setText("Close");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        annotationsMenuItem.setMnemonic('S');
        annotationsMenuItem.setText("Spectrum Annotation");
        annotationsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(annotationsMenuItem);

        menuBar.add(editMenu);

        exportMenu.setMnemonic('X');
        exportMenu.setText("Export");

        exportTagMatchesMenuItem.setMnemonic('T');
        exportTagMatchesMenuItem.setText("Tag Matches");
        exportTagMatchesMenuItem.setToolTipText("Export the tag matches as text");
        exportTagMatchesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTagMatchesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportTagMatchesMenuItem);

        exportPeptideMatchesMenuItem.setMnemonic('P');
        exportPeptideMatchesMenuItem.setText("Peptide Matches (Beta)");
        exportPeptideMatchesMenuItem.setToolTipText("Export the peptide matches as text");
        exportPeptideMatchesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPeptideMatchesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportPeptideMatchesMenuItem);

        exportBlastMatchesMenuItem.setMnemonic('B');
        exportBlastMatchesMenuItem.setText("BLAST");
        exportBlastMatchesMenuItem.setToolTipText("Export the matches in a BLAST format");
        exportBlastMatchesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportBlastMatchesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportBlastMatchesMenuItem);

        menuBar.add(exportMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");

        fixedPtmsCheckBoxMenuItem.setMnemonic('F');
        fixedPtmsCheckBoxMenuItem.setText("Fixed Modifications");
        fixedPtmsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixedPtmsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(fixedPtmsCheckBoxMenuItem);

        menuBar.add(viewMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpMainMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpMainMenuItem.setMnemonic('H');
        helpMainMenuItem.setText("Help");
        helpMainMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMainMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMainMenuItem);
        helpMenu.add(jSeparator17);

        bugReportMenu.setMnemonic('B');
        bugReportMenu.setText("Bug Report");
        bugReportMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bugReportMenuActionPerformed(evt);
            }
        });
        helpMenu.add(bugReportMenu);
        helpMenu.add(jSeparator16);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1065, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bcakgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 831, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bcakgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the results display.
     *
     * @param evt
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        closeConnectionsAndEmptyTempFolder();
        deNovoGUI.setVisible(true);
        dispose();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_querySpectraTableMouseReleased
        updateAssumptionsTable(0);
    }//GEN-LAST:event_querySpectraTableMouseReleased

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_querySpectraTableKeyReleased
        updateAssumptionsTable(0);
    }//GEN-LAST:event_querySpectraTableKeyReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoMatchesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoMatchesTableMouseReleased

        int row = deNovoMatchesTable.rowAtPoint(evt.getPoint());
        int column = deNovoMatchesTable.columnAtPoint(evt.getPoint());

        // check of the user clicked the blast columns
        if (evt.getButton() == 1 && row != -1 && column == deNovoMatchesTable.getColumn("  ").getModelIndex()) {
            SpectrumIdentificationAssumption assumption = assumptions.get(deNovoMatchesTable.convertRowIndexToModel(deNovoMatchesTable.getSelectedRow()));
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            if (assumption instanceof TagAssumption) {
                TagAssumption tagAssumption = (TagAssumption) assumption;
                BareBonesBrowserLaunch.openURL("http://blast.ncbi.nlm.nih.gov/Blast.cgi?PROGRAM=blastp&BLAST_PROGRAMS=blastp&"
                        + "PAGE_TYPE=BlastSearch&SHOW_DEFAULTS=on&LINK_LOC=blasthome&QUERY=" + tagAssumption.getTag().getLongestAminoAcidSequence());
            } else if (assumption instanceof PeptideAssumption) {
                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                BareBonesBrowserLaunch.openURL("http://blast.ncbi.nlm.nih.gov/Blast.cgi?PROGRAM=blastp&BLAST_PROGRAMS=blastp&"
                        + "PAGE_TYPE=BlastSearch&SHOW_DEFAULTS=on&LINK_LOC=blasthome&QUERY=" + peptideAssumption.getPeptide().getSequence());
            } else {
                throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }

        updateSpectrum();
    }//GEN-LAST:event_deNovoMatchesTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void deNovoMatchesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoMatchesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deNovoMatchesTableMouseExited

    /**
     * Shows a tooltip with modification details if over the sequence column.
     *
     * @param evt
     */
    private void deNovoMatchesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoMatchesTableMouseMoved
        int row = deNovoMatchesTable.rowAtPoint(evt.getPoint());
        int column = deNovoMatchesTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && deNovoMatchesTable.getValueAt(row, column) != null) {
            if (column == deNovoMatchesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                String sequence = (String) deNovoMatchesTable.getValueAt(row, column);

                if (sequence.contains("<span")) {
                    try {
                        SpectrumIdentificationAssumption assumption = assumptions.get(row);
                        if (assumption instanceof TagAssumption) {
                            TagAssumption tagAssumption = (TagAssumption) assumption;
                            String tooltip = getTagModificationTooltipAsHtml(tagAssumption.getTag());
                            deNovoMatchesTable.setToolTipText(tooltip);
                        } else if (assumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            String tooltip = getPeptideModificationTooltipAsHtml(peptideAssumption.getPeptide());
                            deNovoMatchesTable.setToolTipText(tooltip);
                        } else {
                            throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                        }
                    } catch (Exception e) {
                        catchException(e);
                    }
                } else {
                    deNovoMatchesTable.setToolTipText(null);
                }
            } else if (column == deNovoMatchesTable.getColumn("  ").getModelIndex()) { // the BLAST column
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                deNovoMatchesTable.setToolTipText(null);
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            deNovoMatchesTable.setToolTipText(null);
        }
    }//GEN-LAST:event_deNovoMatchesTableMouseMoved

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoMatchesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_deNovoMatchesTableKeyReleased
        updateSpectrum();
    }//GEN-LAST:event_deNovoMatchesTableKeyReleased

    /**
     * @see #updateSpectrum()
     */
    private void aIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_aIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void bIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_bIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void cIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_cIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void xIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_xIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void yIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_yIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void zIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_zIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void precursorCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_precursorCheckMenuActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void immoniumIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_immoniumIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_immoniumIonsCheckMenuActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void reporterIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_reporterIonsCheckMenuActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void adaptCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_adaptCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void forwardIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void rewindIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void deNovoChargeOneJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void deNovoChargeTwoJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void allCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allCheckBoxMenuItemActionPerformed
        annotationParameters.setShowAllPeaks(allCheckBoxMenuItem.isSelected());
        updateSpectrum();
    }//GEN-LAST:event_allCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void defaultAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultAnnotationCheckBoxMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_defaultAnnotationCheckBoxMenuItemActionPerformed

    /**
     * Update the spectrum colors.
     *
     * @param evt
     */
    private void annotationColorsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationColorsJMenuItemActionPerformed
        //new SpectrumColorsDialog(this); // @TODO: re-add me!
    }//GEN-LAST:event_annotationColorsJMenuItemActionPerformed

    /**
     * Export spectrum as MGF.
     *
     * @param evt
     */
    private void exportSpectrumGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumGraphicsJMenuItemActionPerformed
        new ExportGraphicsDialog(deNovoGUI, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true, (Component) spectrumJPanel, deNovoGUI.getLastSelectedFolder());
    }//GEN-LAST:event_exportSpectrumGraphicsJMenuItemActionPerformed

    /**
     * Export spectrum as MGF.
     *
     * @param evt
     */
    private void exportSpectrumValuesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumValuesJMenuItemActionPerformed

        String spectrumAsMgf = getSpectrumAsMgf();

        if (spectrumAsMgf != null) {

            String spectrumKey = getSelectedSpectrumFile() + "_" + getSelectedSpectrumTitle();
            File selectedFile = FileChooserUtil.getUserSelectedFile(this, ".mgf", "(Mascot Generic Format) *.mgf", deNovoGUI.getLastSelectedFolder().getLastSelectedFolder(), spectrumKey + ".mgf", "Save As...", false);

            if (selectedFile != null) {

                deNovoGUI.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());

                try {
                    FileWriter w = new FileWriter(selectedFile);
                    BufferedWriter bw = new BufferedWriter(w);
                    bw.write(spectrumAsMgf);
                    bw.close();
                    w.close();

                    JOptionPane.showMessageDialog(this, "Spectrum saved to " + selectedFile.getPath() + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "An error occurred while saving " + selectedFile.getPath() + ".\n"
                            + "See resources/DeNovoGUI.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_exportSpectrumValuesJMenuItemActionPerformed

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/html/SpectrumPanel.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                "Spectrum Panel - Help");
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Update the spectrum data displayed.
     *
     * @param evt
     */
    private void spectrumFileComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumFileComboBoxActionPerformed

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Sorting Spectrum Table. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {
                displayResults();
                progressDialog.setRunFinished();
            }
        }.start();
    }//GEN-LAST:event_spectrumFileComboBoxActionPerformed

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpMainMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMainMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/html/DeNovoGUI.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.pgn")),
                "DeNovoGUI - Help");
    }//GEN-LAST:event_helpMainMenuItemActionPerformed

    /**
     * Opens a new bug report dialog.
     *
     * @param evt
     */
    private void bugReportMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bugReportMenuActionPerformed
        new BugReport(this, deNovoGUI.getLastSelectedFolder(), "DeNovoGUI", "denovogui", deNovoGUI.getVersion(),
                "denovogui", "DeNovoGUI", new File(deNovoGUI.getJarFilePath() + "/resources/DeNovoGUI.log"));
    }//GEN-LAST:event_bugReportMenuActionPerformed

    /**
     * Open the about dialog.
     *
     * @param evt
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/html/AboutDeNovoGUI.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.pgn")),
                "About DeNovoGUI");
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    /**
     * Export the tag matches to file.
     *
     * @param evt
     */
    private void exportTagMatchesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportTagMatchesMenuItemActionPerformed

        exportSettingsDialog = new ExportSettingsDialog(this, true);

        if (!exportSettingsDialog.canceled()) {
            File selectedFile = FileChooserUtil.getUserSelectedFile(this, ".txt", "Text file (.txt)", "Select File", deNovoGUI.getLastSelectedFolder().getLastSelectedFolder(), "tag_matches.txt", false);
            if (selectedFile != null) {
                deNovoGUI.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
                exportIdentification(selectedFile, ExportType.tags, exportSettingsDialog.getThreshold(), exportSettingsDialog.isGreaterThenThreshold(), exportSettingsDialog.getNumberOfPeptides());
            }
        }
    }//GEN-LAST:event_exportTagMatchesMenuItemActionPerformed

    /**
     * Empty the matches folder and close the window.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitMenuItemActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Hide/show the fixed PTMs in the peptides and spectrum annotation.
     *
     * @param evt
     */
    private void fixedPtmsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixedPtmsCheckBoxMenuItemActionPerformed
        int selectedRow = deNovoMatchesTable.getSelectedRow();
        ((AssumptionsTableModel) deNovoMatchesTable.getModel()).setExcludeAllFixedPtms(!fixedPtmsCheckBoxMenuItem.isSelected());
        ((AssumptionsTableModel) deNovoMatchesTable.getModel()).fireTableDataChanged();
        if (selectedRow != -1) {
            deNovoMatchesTable.setRowSelectionInterval(selectedRow, selectedRow);
            updateSpectrum();
        }
    }//GEN-LAST:event_fixedPtmsCheckBoxMenuItemActionPerformed

    /**
     * Export the matches in a BLAST supported format.
     *
     * @param evt
     */
    private void exportBlastMatchesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBlastMatchesMenuItemActionPerformed

        exportSettingsDialog = new ExportSettingsDialog(this, true);

        if (!exportSettingsDialog.canceled()) {
            File selectedFile = FileChooserUtil.getUserSelectedFile(this, ".txt", "Text file (.txt)", "Select File", deNovoGUI.getLastSelectedFolder().getLastSelectedFolder(), "blast_output.txt", false);
            if (selectedFile != null) {
                deNovoGUI.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
                exportIdentification(selectedFile, ExportType.blast, exportSettingsDialog.getThreshold(), exportSettingsDialog.isGreaterThenThreshold(), exportSettingsDialog.getNumberOfPeptides());
            }
        }
    }//GEN-LAST:event_exportBlastMatchesMenuItemActionPerformed

    /**
     * Open the spectrum annotation preferences dialog.
     *
     * @param evt
     */
    private void annotationsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationsMenuItemActionPerformed
        new AnnotationPreferencesDialog(this);
    }//GEN-LAST:event_annotationsMenuItemActionPerformed

    /**
     * Export the peptide matches to file.
     *
     * @param evt
     */
    private void exportPeptideMatchesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPeptideMatchesMenuItemActionPerformed

        exportSettingsDialog = new ExportSettingsDialog(this, true);

        if (!exportSettingsDialog.canceled()) {

            final File selectedFile = FileChooserUtil.getUserSelectedFile(this, ".txt", "Text file (.txt)", "Select File", deNovoGUI.getLastSelectedFolder().getLastSelectedFolder(), "peptide_matches.txt", false);

            if (selectedFile != null) {

                final boolean needMapping = sequenceFactory.getCurrentFastaFile() == null
                        || JOptionPane.showConfirmDialog(ResultsFrame.this,
                                "A protein mapping was already found, use the previous results?", "Mapping Already Exists",
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION;
                final ProteinMappingDialog mappingDialog;
                if (needMapping) {
                    mappingDialog = new ProteinMappingDialog(this, searchParameters.getModificationParameters());
                    if (mappingDialog.isCanceled() || sequenceFactory.getCurrentFastaFile() == null) {
                        return;
                    }
                } else {
                    mappingDialog = null;
                }

                progressDialog = new ProgressDialogX(this,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                        true);
                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                progressDialog.setTitle("Loading Protein Mapping. Please Wait...");

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("ExportPeptidesThread") {
                    @Override
                    public void run() {

                        try {
                            boolean peptideFound = true;
                            if (needMapping) {
                                SequenceMatchingParameters sequenceMatchingParameters = new SequenceMatchingParameters(); //@TODO add dialog
                                sequenceMatchingParameters.setPeptideMapperType(PeptideMapperType.fm_index);
                                peptideFound = matchInProteins(sequenceMatchingParameters, mappingDialog.getFixedModifications(), mappingDialog.getVariableModifications(), progressDialog,
                                        exportSettingsDialog.getThreshold(), exportSettingsDialog.isGreaterThenThreshold(), exportSettingsDialog.getNumberOfPeptides());
                            }
                            if (!progressDialog.isRunCanceled()) {
                                if (!peptideFound) {
                                    JOptionPane.showMessageDialog(ResultsFrame.this, "No peptide could be matched to the database.", "File Saved", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    progressDialog.setTitle("Exporting Matches. Please Wait...");
                                    deNovoGUI.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
                                    TextExporter.exportPeptides(selectedFile, deNovoGUI.getMsFileHandler(), identification, searchParameters, progressDialog,
                                            exportSettingsDialog.getThreshold(), exportSettingsDialog.isGreaterThenThreshold(), exportSettingsDialog.getNumberOfPeptides());
                                    if (!progressDialog.isRunCanceled()) {
                                        progressDialog.setRunFinished();
                                        JOptionPane.showMessageDialog(ResultsFrame.this, "Matches exported to " + selectedFile.getAbsolutePath() + ".", "File Saved", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            catchException(e);
                        } catch (OutOfMemoryError error) {
                            progressDialog.setRunCanceled();
                            System.out.println("DeNovoGUI ran out of memory! See the DeNovoGUI log for details.");
                            System.err.println("Ran out of memory!");
                            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
                            System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
                            System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");
                            Runtime.getRuntime().gc();

                            JOptionPane.showMessageDialog(ResultsFrame.this, JOptionEditorPane.getJOptionEditorPane(
                                    "DeNovoGUI used up all the available memory and had to be stopped.<br>"
                                    + "Memory boundaries are changed via the Edit menu (Edit Java Options)<br>. "
                                    + "See also <a href=\"http://compomics.github.io/projects/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>."),
                                    "Out Of Memory", JOptionPane.ERROR_MESSAGE);

                            error.printStackTrace();
                        } finally {
                            progressDialog.setRunFinished();
                        }
                    }
                }.start();
            }
        }
    }//GEN-LAST:event_exportPeptideMatchesMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void highResAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highResAnnotationCheckBoxMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_highResAnnotationCheckBoxMenuItemActionPerformed

    /**
     * Reset the annotation to the default annotation.
     *
     * @param evt
     */
    private void resetAnnotationMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_resetAnnotationMenuMenuSelected
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItemActionPerformed(null);
    }//GEN-LAST:event_resetAnnotationMenuMenuSelected

    /**
     * @see #updateSpectrum()
     */
    private void individualDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_individualDeNovoCheckBoxMenuItemActionPerformed
        updateSpectrum();
    }//GEN-LAST:event_individualDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateSpectrum()
     */
    private void relatedIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_relatedIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrum();
    }//GEN-LAST:event_relatedIonsCheckMenuActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem aIonCheckBoxMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JCheckBoxMenuItem adaptCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem allCheckBoxMenuItem;
    private javax.swing.JMenuItem annotationColorsJMenuItem;
    private javax.swing.JMenuBar annotationMenuBar;
    private javax.swing.JMenuItem annotationsMenuItem;
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JPanel bcakgroundPanel;
    private javax.swing.JMenuItem bugReportMenu;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.ButtonGroup deNovoChargeButtonGroup;
    private javax.swing.JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem;
    private javax.swing.JPanel deNovoMatchesPanel;
    private javax.swing.JTable deNovoMatchesTable;
    private javax.swing.JMenu deNovoMenu;
    private javax.swing.JScrollPane deNovoPeptidesTableScrollPane;
    private javax.swing.JPanel debovoResultsPanel;
    private javax.swing.JCheckBoxMenuItem defaultAnnotationCheckBoxMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportBlastMatchesMenuItem;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenuItem exportPeptideMatchesMenuItem;
    private javax.swing.JMenuItem exportSpectrumGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumValuesJMenuItem;
    private javax.swing.JMenuItem exportTagMatchesMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBoxMenuItem fixedPtmsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpMainMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JCheckBoxMenuItem highResAnnotationCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem immoniumIonsCheckMenu;
    private javax.swing.JCheckBoxMenuItem individualDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu ionsMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JMenu lossMenu;
    private javax.swing.JMenu lossSplitter;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu otherMenu;
    private javax.swing.JCheckBoxMenuItem precursorCheckMenu;
    private javax.swing.JPanel querySpectraPanel;
    private javax.swing.JTable querySpectraTable;
    private javax.swing.JScrollPane querySpectraTableScrollPane;
    private javax.swing.JCheckBoxMenuItem relatedIonsCheckMenu;
    private javax.swing.JCheckBoxMenuItem reporterIonsCheckMenu;
    private javax.swing.JMenu resetAnnotationMenu;
    private javax.swing.JCheckBoxMenuItem rewindIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JComboBox spectrumFileComboBox;
    private javax.swing.JLabel spectrumFileLabel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumViewerPanel;
    private javax.swing.JMenu splitterMenu2;
    private javax.swing.JMenu splitterMenu3;
    private javax.swing.JMenu splitterMenu4;
    private javax.swing.JMenu splitterMenu5;
    private javax.swing.JMenu splitterMenu6;
    private javax.swing.JMenu splitterMenu7;
    private javax.swing.JMenu splitterMenu8;
    private javax.swing.JMenu splitterMenu9;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JCheckBoxMenuItem xIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem yIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem zIonCheckBoxMenuItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Matches the tags to a protein database.
     *
     * @param sequenceMatchingParameters the sequence matching preferences
     * @param fixedModifications the fixed modifications
     * @param variableModifications the variable modifications
     * @param waitingHandler a waiting handler
     * @param scoreThreshold the score threshold
     * @param greaterThan a boolean indicating whether the score has to be
     * higher than the threshold
     * @param aNumberOfMatches the limit in number of matches
     *
     * @return a boolean indicating whether the peptide could be mapped
     *
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing an object
     * @throws InterruptedException exception thrown whenever a threading error
     * occurred
     * @throws SQLException exception thrown whenever an error occurs while
     * interacting with the back-end database
     */
    private boolean matchInProteins(SequenceMatchingParameters sequenceMatchingParameters, ArrayList<String> fixedModifications, ArrayList<String> variableModifications, WaitingHandler waitingHandler,
            Double scoreThreshold, boolean greaterThan, Integer aNumberOfMatches) throws IOException, ClassNotFoundException, InterruptedException, SQLException {

        double threshold = 0;
        if (scoreThreshold != null) {
            threshold = scoreThreshold;
        }
        int numberOfMatches = 10;
        if (aNumberOfMatches != null) {
            numberOfMatches = aNumberOfMatches;
        }

        waitingHandler.setWaitingText("Importing FASTA File (Step 1 of 2). Please Wait...");
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);

        if (waitingHandler.isRunCanceled()) {
            return false;
        }

        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        UtilitiesUserParameters userParameters = UtilitiesUserParameters.loadUserParameters();
        int memoryPreference = userParameters.getMemoryParameter();
        int nSequences = sequenceFactory.getNSequences();
        int cacheSizeInMb = nSequences * 112 / 1048576; // 112 is the size taken by one protein
        if (!sequenceFactory.isDefaultReversed() || cacheSizeInMb < memoryPreference / 4) {
            nSequences = sequenceFactory.getNSequences();
            sequenceFactory.setDecoyInMemory(true);
        } else {
            nSequences = sequenceFactory.getNTargetSequences();
            sequenceFactory.setDecoyInMemory(false);
        }
        long availableCachSize = 1048576l * memoryPreference / 112; // 112 is the size taken by one protein
        availableCachSize *= 0.8;
        if (availableCachSize > nSequences) {
            availableCachSize = nSequences;
        } else {
            waitingHandler.appendReport("Warning: DeNovoGUI cannot load your FASTA file into memory. This will slow down the processing. "
                    + "Note that using large large databases also increases the number of false positives. "
                    + "Try to either (i) use a smaller database, (ii) increase the memory provided to DeNovoGUI, or (iii) improve the reading speed by using an SSD disc. "
                    + "(See also http://compomics.github.io/projects/compomics-utilities/wiki/proteininference.html.)", true, true);

        }
        int cacheSize = (int) availableCachSize;
        sequenceFactory.setnCache(cacheSize);

        PeptideMapper peptideMapper;
        try {
            peptideMapper = sequenceFactory.getDefaultPeptideMapper(sequenceMatchingParameters, searchParameters, PeptideVariantsParameters.getNoVariantPreferences(), waitingHandler, exceptionHandler);
        } catch (SQLException e) {
            waitingHandler.appendReport("Database " + sequenceFactory.getCurrentFastaFile().getName() + " could not be accessed, make sure that the file is not used by another program.", true, true);
            e.printStackTrace();
            waitingHandler.setRunCanceled();
            return false;
        }
        int treeKeyLength = 0;
        if (sequenceMatchingParameters.getPeptideMapperType() == PeptideMapperType.tree) {
            treeKeyLength = ((ProteinTree) peptideMapper).getInitialTagSize();
        }

        waitingHandler.setWaitingText("Mapping Tags (Step 2 of 2). Please Wait...");
        waitingHandler.resetSecondaryProgressCounter();
        waitingHandler.setSecondaryProgressCounterIndeterminate(false);
        int total = identification.getSpectrumIdentificationSize();
        waitingHandler.setMaxSecondaryProgressCounter(total);
        ((SpectrumTableModel) querySpectraTable.getModel()).setUpdate(false); // @TODO: remove when the objectDB is stable
        TagMatcher tagMatcher = new TagMatcher(fixedModifications, variableModifications, deNovoGUI.getSequenceMatchingParameters());

        int progress = 0;
        boolean peptideFound = false;
        for (String spectrumFile : deNovoGUI.getMsFileHandler().getOrderedFileNamesWithoutExtensions()) {

            PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, true, waitingHandler);
            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = psmIterator.next()) != null) {

                String spectrumKey = spectrumMatch.getKey();
                HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);

                for (Advocate advocate : DeNovoGUI.implementedAlgorithms) {

                    int advocateIndex = advocate.getIndex();
                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = allAssumptions.get(advocateIndex);

                    if (assumptionsMap != null) {
                        ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                        if (advocate.getIndex() == Advocate.pepnovo.getIndex()
                                || advocate.getIndex() == Advocate.pNovo.getIndex()
                                || advocate.getIndex() == Advocate.novor.getIndex()) {
                            Collections.sort(scores, Collections.reverseOrder());
                        } else {
                            Collections.sort(scores);
                        }

                        for (int i = 0; i < scores.size() && i < numberOfMatches; i++) {

                            double score = scores.get(i);
                            ArrayList<SpectrumIdentificationAssumption> tempAssumptions = assumptionsMap.get(score);
                            ArrayList<SpectrumIdentificationAssumption> denovoAssumptions = new ArrayList<SpectrumIdentificationAssumption>(tempAssumptions);

                            for (SpectrumIdentificationAssumption assumption : denovoAssumptions) {

                                boolean passesThreshold;

                                if (greaterThan) {
                                    passesThreshold = assumption.getScore() >= threshold;
                                } else { // less than
                                    passesThreshold = assumption.getScore() <= threshold;
                                }

                                if (passesThreshold) {
                                    if (assumption instanceof TagAssumption) {
                                        TagAssumption tagAssumption = (TagAssumption) assumption;
                                        int longestAminoAcidSequence = tagAssumption.getTag().getLongestAminoAcidSequence().length();
                                        if (longestAminoAcidSequence >= treeKeyLength) {
                                            ArrayList<PeptideProteinMapping> proteinMapping = peptideMapper.getProteinMapping(
                                                    tagAssumption.getTag(), tagMatcher, deNovoGUI.getSequenceMatchingParameters(), searchParameters.getFragmentIonAccuracy());
                                            for (Peptide peptide : PeptideProteinMapping.getPeptides(proteinMapping, deNovoGUI.getSequenceMatchingParameters())) {
                                                if (!peptideFound) {
                                                    peptideFound = true;
                                                }
                                                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, tagAssumption.getRank(),
                                                        advocateIndex, assumption.getIdentificationCharge(), score, assumption.getIdentificationFile());
                                                peptideAssumption.addUrParam(tagAssumption);
                                                tempAssumptions.add(peptideAssumption);
                                            }
                                        }
                                    } else if (assumption instanceof PeptideAssumption) {
                                        if (!peptideFound) {
                                            peptideFound = true;
                                        }
                                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                        ArrayList<PeptideProteinMapping> proteinMapping = peptideMapper.getProteinMapping(
                                                peptideAssumption.getPeptide().getSequence(), deNovoGUI.getSequenceMatchingParameters());
                                        HashSet<String> proteins = new HashSet<String>(proteinMapping.size());
                                        for (PeptideProteinMapping peptideProteinMapping : proteinMapping) {
                                            proteins.add(peptideProteinMapping.getProteinAccession());
                                        }
                                        ArrayList<String> sortedProteinList = new ArrayList<String>(proteins);
                                        Collections.sort(sortedProteinList);
                                        peptideAssumption.getPeptide().setParentProteins(sortedProteinList);
                                    } else {
                                        throw new UnsupportedOperationException("Peptide mapping not supported for assumption of type " + assumption.getClass() + ".");
                                    }
                                }
                            }
                        }
                    }
                }

                identification.updateAssumptions(spectrumKey, allAssumptions);

                // free memory if needed
                if (memoryUsed() > 0.8 && !objectsCache.isEmpty()) {
                    objectsCache.reduceMemoryConsumption(0.5, null);
                }
                waitingHandler.increaseSecondaryProgressCounter();
                waitingHandler.setWaitingText("Mapping Tags (Step 2 of 2, Spectrum " + ++progress + " of " + total + "). Please Wait...");

                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }

        if (waitingHandler.isRunCanceled()) {
            return false;
        }

        ((SpectrumTableModel) querySpectraTable.getModel()).setUpdate(true); //@TODO: remove when the objectDB is stable
        waitingHandler.setRunFinished();

        return peptideFound;
    }

    /**
     * Returns the share of memory being used.
     *
     * @return the share of memory being used
     */
    public double memoryUsed() {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long givenMemory = Runtime.getRuntime().maxMemory();
        double ratio = usedMemory / givenMemory;
        return ratio;
    }

    /**
     * Shows a dialog allowing the selection of a new result file and displays
     * the results.
     */
    private void openNewFile() {
        final SelectResultsDialog selectResultsDialog = new SelectResultsDialog(this, deNovoGUI.getLastSelectedFolder().getLastSelectedFolder());
        if (!selectResultsDialog.isCanceled() && selectResultsDialog.getMgfFiles() != null && selectResultsDialog.getResultFiles() != null && selectResultsDialog.getSearchParameters() != null) {
            deNovoGUI.getLastSelectedFolder().setLastSelectedFolder(selectResultsDialog.getLastSelectedFolder());
            searchParameters = selectResultsDialog.getSearchParameters();
            setVisible(true);
            deNovoGUI.setVisible(false);
            displayResults(selectResultsDialog.getResultFiles(), selectResultsDialog.getMgfFiles());
        } else {
            dispose();
        }
    }

    /**
     * Displays new results.
     *
     * @param peptideRowSelection the peptide row to select
     * @param psmRowSelection the PSM row to select
     */
    private void displayResults() {

        orderedSpectrumTitles = null;
        try {
            orderedSpectrumTitles = orderTitlesByScore(progressDialog);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while sorting the results.", "Out File Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Display. Please Wait...");

        TableModel tableModel = new SpectrumTableModel(getSelectedSpectrumFile(), deNovoGUI.getMsFileHandler(), identification, orderedSpectrumTitles);
        querySpectraTable.setModel(tableModel);
        setSpectrumTableProperties();

        ((DefaultTableModel) querySpectraTable.getModel()).fireTableDataChanged();

        if (identification.getSpectrumIdentification(getSelectedSpectrumFile()) == null) {
            ((TitledBorder) querySpectraPanel.getBorder()).setTitle("Query Spectra (?/"
                    + deNovoGUI.getMsFileHandler().getSpectrumTitles(getSelectedSpectrumFile()).length + ")");
        } else {
            ((TitledBorder) querySpectraPanel.getBorder()).setTitle("Query Spectra ("
                    + identification.getSpectrumIdentification(getSelectedSpectrumFile()).size() + "/"
                    + deNovoGUI.getMsFileHandler().getSpectrumTitles(getSelectedSpectrumFile()).length + ")");
        }

        querySpectraPanel.repaint();

        if (querySpectraTable.getRowCount() > 0) {
            querySpectraTable.setRowSelectionInterval(0, 0);
        }

        // select the first assumption
        updateAssumptionsTable(0);

        findPanel.setEnabled(true);

        // change the icon to the normal version (should not be needed, but added as an extra safty)
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));

        // the spectrum file and the results file do not match...
        if (identification.getSpectrumIdentification(getSelectedSpectrumFile()) == null) {
            JOptionPane.showMessageDialog(ResultsFrame.this, "No identifications for the selected spectrum file."
                    + "\nPlease check that you loaded the correct files.", "File Errors", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns a list of the spectrum titles of the selected mgf file ordered by
     * max score.
     *
     * @return a list of the spectrum titles of the selected mgf file ordered by
     * max score
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private ArrayList<String> orderTitlesByScore(ProgressDialogX progressDialog) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        String spectrumFile = getSelectedSpectrumFile();
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.resetPrimaryProgressCounter();
        progressDialog.setMaxPrimaryProgressCounter(deNovoGUI.getMsFileHandler().getSpectrumTitles(spectrumFile).length);

        // score to title map: advocate -> score -> titles
        HashMap<Integer, HashMap<Double, ArrayList<String>>> titlesMap = new HashMap<Integer, HashMap<Double, ArrayList<String>>>();
        ArrayList<String> noId = new ArrayList<String>();

        for (String spectrumTitle : deNovoGUI.getMsFileHandler().getSpectrumTitles(spectrumFile)) {

            String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);

            if (identification.matchExists(spectrumKey)) {

                for (Advocate advocate : DeNovoGUI.implementedAlgorithms) {

                    int advocateId = advocate.getIndex();

                    HashMap<Double, ArrayList<String>> advocateMap = titlesMap.get(advocateId);
                    if (advocateMap == null) {
                        advocateMap = new HashMap<Double, ArrayList<String>>();
                        titlesMap.put(advocateId, advocateMap);
                    }

                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(spectrumKey);
                    HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = allAssumptions.get(advocateId);

                    if (assumptionsMap != null) {
                        double bestScore = DeNovoGUI.getBestScore(advocate, assumptionsMap.keySet());
                        ArrayList<String> titles = advocateMap.get(bestScore);
                        if (titles == null) {
                            titles = new ArrayList<String>();
                            advocateMap.put(bestScore, titles);
                        }
                        titles.add(spectrumTitle);
                        break;
                    }
                }
            } else {
                noId.add(spectrumTitle);
            }

            progressDialog.increasePrimaryProgressCounter();
        }

        ArrayList<String> orderedTitles = new ArrayList<String>();
        for (Advocate advocate : DeNovoGUI.implementedAlgorithms) {
            HashMap<Double, ArrayList<String>> advocateMap = titlesMap.get(advocate.getIndex());
            if (advocateMap != null) {
                ArrayList<Double> scores = new ArrayList<Double>(advocateMap.keySet());
                DeNovoGUI.sortScores(advocate, scores);
                for (double score : scores) {
                    orderedTitles.addAll(titlesMap.get(advocate.getIndex()).get(score));
                }
            }
        }

        orderedTitles.addAll(noId);
        return orderedTitles;
    }

    /**
     * Exports the identification results in a text file.
     *
     * @param file the destination file
     * @param exportType the type of export desired
     * @param scoreThreshold score threshold for BLAST-compatible export
     * @param greaterThan use a greater than threshold for the scores
     * @param numberOfMatches the maximum number of matches to export per
     * spectrum
     */
    public void exportIdentification(File file, final ExportType exportType, final Double scoreThreshold, final Boolean greaterThan, final Integer numberOfMatches) {

        final File finalFile = file;

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Exporting Matches. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("exportThread") {
            public void run() {
                try {
                    switch (exportType) {
                        case tags:
                            TextExporter.exportTags(finalFile, identification, searchParameters, progressDialog, scoreThreshold, greaterThan, numberOfMatches);
                            break;
                        case peptides:
                            TextExporter.exportPeptides(finalFile, deNovoGUI.getMsFileHandler(), identification, searchParameters, progressDialog, scoreThreshold, greaterThan, numberOfMatches);
                            break;
                        case blast:
                            TextExporter.exportBlastPSMs(finalFile, identification, searchParameters, progressDialog, scoreThreshold, greaterThan, numberOfMatches);
                    }

                    boolean cancelled = progressDialog.isRunCanceled();
                    progressDialog.setRunFinished();

                    if (!cancelled) {
                        JOptionPane.showMessageDialog(ResultsFrame.this, "Matches exported to " + finalFile.getAbsolutePath() + ".", "File Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while exporting the results.", "Export Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Returns the name of the spectrum file displayed.
     *
     * @return the name of the spectrum file displayed
     */
    public String getSelectedSpectrumFile() {
        return (String) spectrumFileComboBox.getSelectedItem();
    }

    /**
     * Returns the title of the selected spectrum.
     *
     * @return the title of the selected spectrum
     */
    public String getSelectedSpectrumTitle() {
        int selectedRow = querySpectraTable.getSelectedRow();
        int modelRow = querySpectraTable.convertRowIndexToModel(selectedRow);
        if (orderedSpectrumTitles != null) {
            return orderedSpectrumTitles.get(modelRow);
        } else {
            return deNovoGUI.getMsFileHandler().getSpectrumTitles(getSelectedSpectrumFile())[modelRow];
        }
    }

    /**
     * Updates the assumption table based on the selected line.
     *
     * @param selectedPsmRow the selected PSM row
     */
    public void updateAssumptionsTable(int selectedPsmRow) {

        try {
            assumptions = new ArrayList<SpectrumIdentificationAssumption>();

            if (querySpectraTable.getRowCount() > 0) {

                String psmKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

                if (identification.matchExists(psmKey)) {

                    for (Advocate advocate : DeNovoGUI.implementedAlgorithms) {

                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> allAssumptions = identification.getAssumptions(psmKey);
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> assumptionsMap = allAssumptions.get(advocate.getIndex());

                        if (assumptionsMap != null) {

                            ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                            DeNovoGUI.sortScores(advocate, scores);

                            for (Double score : scores) {
                                for (SpectrumIdentificationAssumption assumption : assumptionsMap.get(score)) {
                                    assumptions.add(assumption);
                                }
                            }
                        }
                    }
                }

                TableModel tableModel = new AssumptionsTableModel(assumptions, searchParameters.getModificationParameters(), !fixedPtmsCheckBoxMenuItem.isSelected());
                deNovoMatchesTable.setModel(tableModel);

                ((DefaultTableModel) deNovoMatchesTable.getModel()).fireTableDataChanged();
                setAssumptionsTableProperties();

                if (deNovoMatchesTable.getRowCount() > 0) {
                    if (selectedPsmRow != -1 && selectedPsmRow < deNovoMatchesTable.getRowCount()) {
                        deNovoMatchesTable.setRowSelectionInterval(selectedPsmRow, selectedPsmRow);
                        deNovoMatchesTable.scrollRectToVisible(deNovoMatchesTable.getCellRect(selectedPsmRow, 0, false));
                    } else {
                        deNovoMatchesTable.setRowSelectionInterval(0, 0);
                    }
                }

                ((TitledBorder) deNovoMatchesPanel.getBorder()).setTitle("De Novo Matches (" + deNovoMatchesTable.getRowCount() + ")");
                deNovoMatchesPanel.repaint();

                updateSpectrum();
            }

        } catch (Exception e) {
            catchException(e);
        }
    }

    /**
     * Update the spectrum and annotations.
     */
    public void updateSpectrum() {

        spectrumJPanel.removeAll();

        ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle("Spectrum Viewer");
        spectrumViewerPanel.repaint();

        exportSpectrumValuesJMenuItem.setVisible(deNovoMatchesTable.getSelectedRowCount() < 2);

        if (deNovoMatchesTable.getSelectedRowCount() > 2) {
            JPanel tempPanel = new JPanel();
            tempPanel.setOpaque(false);
            JLabel tempLabel = new JLabel("Please select maximum two PSMs at the same time...");
            tempLabel.setFont(tempLabel.getFont().deriveFont(tempLabel.getFont().getStyle() | java.awt.Font.BOLD));
            tempPanel.setLayout(new GridBagLayout());
            tempPanel.add(tempLabel, new GridBagConstraints());
            spectrumJPanel.add(tempPanel);
        } else {

            if (deNovoGUI.getMsFileHandler().spectrumLoaded(spectrumKey)) {
                try {
                    int maxPrecursorCharge = 1;
                    ArrayList<ModificationMatch> allModifications = new ArrayList<ModificationMatch>();

                    Spectrum currentSpectrum = deNovoGUI.getMsFileHandler().getSpectrum(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

                    // add the data to the spectrum panel
                    Precursor precursor = currentSpectrum.getPrecursor();
                    if (deNovoMatchesTable.getSelectedRow() != -1) {

                        double[] intensitiesAsArray = currentSpectrum.intensity;

                        if (deNovoMatchesTable.getSelectedRowCount() == 2) {
                            intensitiesAsArray = currentSpectrum.getIntensityValuesNormalizedAsArray();
                        }

                        SpectrumPanel spectrumPanel = new SpectrumPanel(
                                currentSpectrum.mz, intensitiesAsArray,
                                precursor.mz, "",
                                "", 40, false, false, false, 2, false);
                        spectrumPanel.setBorder(null);

                        SpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                        spectrumPanel.setDeltaMassWindow(annotationParameters.getFragmentIonAccuracy());

                        // show all or just the annotated peaks
                        spectrumPanel.showAnnotatedPeaksOnly(!annotationParameters.showAllPeaks());
                        spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationParameters.yAxisZoomExcludesBackgroundPeaks());

                        // add the mirrored spectrum
                        if (deNovoMatchesTable.getSelectedRowCount() == 2) {
                            spectrumPanel.addMirroredSpectrum(
                                    currentSpectrum.mz, currentSpectrum.getIntensityValuesNormalizedAsArray(), precursor.mz,
                                    "", "", false,
                                    Color.BLUE, Color.BLUE); // @TODO: remove hardcoded colors!
                        }

                        String modifiedSequence = "";

                        // add the spectrum annotations
                        for (int i = 0; i < deNovoMatchesTable.getSelectedRowCount(); i++) {

                            SpectrumIdentificationAssumption assumption = assumptions.get(deNovoMatchesTable.convertRowIndexToModel(deNovoMatchesTable.getSelectedRows()[i]));
                            specificAnnotationParameters = annotationParameters.getSpecificAnnotationParameters(spectrumKey, assumption, SequenceMatchingParameters.DEFAULT_STRING_MATCHING, SequenceMatchingParameters.DEFAULT_STRING_MATCHING);
                            updateAnnotationPreferences();
                            ArrayList<IonMatch> annotations;
                            if (assumption instanceof TagAssumption) {
                                TagAssumption tagAssumption = (TagAssumption) assumption;
                                annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationParameters, specificAnnotationParameters, currentSpectrum, tagAssumption.getTag());
                            } else if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                annotations = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationParameters, specificAnnotationParameters, currentSpectrum, peptideAssumption.getPeptide());
                            } else {
                                throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                            }
                            if (i == 0) {
                                spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                                // convert the amino acid scores to alpha levels
                                ArrayList<float[]> alphaValues = null;
                                if (assumption.getAminoAcidScores() != null && individualDeNovoCheckBoxMenuItem.isSelected()) {
                                    alphaValues = convertAminoAcidScoresToAlphaValues(assumption.getAminoAcidScores());
                                }

                                // add de novo sequencing
                                if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    spectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                            TagFragmentIon.B_ION, // @TODO: choose the fragment ion types from the annotation menu bar?
                                            TagFragmentIon.Y_ION,
                                            annotationParameters.getDeNovoCharge(),
                                            annotationParameters.showForwardIonDeNovoTags(),
                                            annotationParameters.showRewindIonDeNovoTags(),
                                            0.75, 1.0, alphaValues, !fixedPtmsCheckBoxMenuItem.isSelected(), false);
                                } else if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    spectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                            PeptideFragmentIon.B_ION, // @TODO: choose the fragment ion types from the annotation menu bar?
                                            PeptideFragmentIon.Y_ION,
                                            annotationParameters.getDeNovoCharge(),
                                            annotationParameters.showForwardIonDeNovoTags(),
                                            annotationParameters.showRewindIonDeNovoTags(),
                                            0.75, 1.0, alphaValues, !fixedPtmsCheckBoxMenuItem.isSelected(), false);
                                } else {
                                    throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                                }
                            } else {
                                spectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                                // convert the amino acid scores to alpha levels
                                ArrayList<float[]> alphaValues = null;
                                if (assumption.getAminoAcidScores() != null && individualDeNovoCheckBoxMenuItem.isSelected()) {
                                    alphaValues = convertAminoAcidScoresToAlphaValues(assumption.getAminoAcidScores());
                                }

                                // add de novo sequencing
                                if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    spectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                            TagFragmentIon.B_ION, // @TODO: choose the fragment ion types from the annotation menu bar?
                                            TagFragmentIon.Y_ION,
                                            annotationParameters.getDeNovoCharge(),
                                            annotationParameters.showForwardIonDeNovoTags(),
                                            annotationParameters.showRewindIonDeNovoTags(),
                                            0.75, 1.0, alphaValues, !fixedPtmsCheckBoxMenuItem.isSelected(), true);
                                } else if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    spectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                            PeptideFragmentIon.B_ION, // @TODO: choose the fragment ion types from the annotation menu bar?
                                            PeptideFragmentIon.Y_ION,
                                            annotationParameters.getDeNovoCharge(),
                                            annotationParameters.showForwardIonDeNovoTags(),
                                            annotationParameters.showRewindIonDeNovoTags(),
                                            0.75, 1.0, alphaValues, !fixedPtmsCheckBoxMenuItem.isSelected(), false);
                                } else {
                                    throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                                }
                            }

                            spectrumJPanel.add(spectrumPanel);

                            // get the modifications and update the modified sequence for the panel title
                            if (assumption instanceof TagAssumption) {
                                TagAssumption tagAssumption = (TagAssumption) assumption;
                                Tag tag = tagAssumption.getTag();
                                for (TagComponent tagComponent : tag.getContent()) {
                                    if (tagComponent instanceof AminoAcidPattern) {
                                        AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;
                                        for (int site = 1; site <= aminoAcidPattern.length(); site++) {
                                            for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(site)) {
                                                allModifications.add(modificationMatch);
                                            }
                                        }
                                    } else if (tagComponent instanceof AminoAcidSequence) {
                                        AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;
                                        for (int site = 1; site <= aminoAcidSequence.length(); site++) {
                                            for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(site)) {
                                                allModifications.add(modificationMatch);
                                            }
                                        }
                                    } else if (tagComponent instanceof MassGap) {
                                        // Nothing to do here
                                    } else {
                                        throw new UnsupportedOperationException("Annotation not supported for the tag component " + tagComponent.getClass() + ".");
                                    }
                                }

                                if (!modifiedSequence.isEmpty()) {
                                    modifiedSequence += " vs. ";
                                }

                                modifiedSequence += tag.getTaggedModifiedSequence(deNovoGUI.getSearchParameters().getModificationParameters(), false, false, true, false);

                            } else if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                Peptide peptide = peptideAssumption.getPeptide();
                                if (peptide.isModified()) {
                                    for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                        allModifications.add(modificationMatch);
                                    }
                                }

                                if (!modifiedSequence.isEmpty()) {
                                    modifiedSequence += " vs. ";
                                }

                                modifiedSequence += peptide.getTaggedModifiedSequence(deNovoGUI.getSearchParameters().getModificationParameters(), false, false, true, false);
                            } else {
                                throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                            }

                            // Update max precursor charge
                            if (assumption.getIdentificationCharge() > maxPrecursorCharge) {
                                maxPrecursorCharge = assumption.getIdentificationCharge();
                            }
                        }

                        spectrumPanel.rescale(0.0, spectrumPanel.getMaxXAxisValue());

                        // update the spectrum title
                        if (deNovoMatchesTable.getSelectedRowCount() == 1) {
                            double theoreticMz;
                            SpectrumIdentificationAssumption assumption = assumptions.get(deNovoMatchesTable.convertRowIndexToModel(deNovoMatchesTable.getSelectedRow()));
                            if (assumption instanceof TagAssumption) {
                                TagAssumption tagAssumption = (TagAssumption) assumption;
                                theoreticMz = tagAssumption.getTheoreticMz(true, true);
                            } else if (assumption instanceof PeptideAssumption) {
                                PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                theoreticMz = peptideAssumption.getTheoreticMz();
                            } else {
                                throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                            }

                            ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle(
                                    "Spectrum Viewer (" + modifiedSequence
                                    + "   " + maxPrecursorCharge + "   "
                                    + Util.roundDouble(theoreticMz, 2) + " m/z)");
                        } else if (deNovoMatchesTable.getSelectedRowCount() == 2) {
                            ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle(
                                    "Spectrum Viewer (" + modifiedSequence + ")");
                        } else if (deNovoMatchesTable.getSelectedRowCount() > 2) {
                            ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle(
                                    "Spectrum Viewer (" + deNovoMatchesTable.getSelectedRowCount() + " PSMs)");
                        }

                        spectrumViewerPanel.repaint();
                    } else {
                        // Show spectrum without identification.
                        SpectrumPanel spectrumPanel = new SpectrumPanel(
                                currentSpectrum.mz, currentSpectrum.intensity,
                                precursor.mz, "",
                                "", 40, false, false, false, 2, false);
                        spectrumPanel.setDeltaMassWindow(annotationParameters.getFragmentIonAccuracy());
                        SpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                        spectrumPanel.setBorder(null);
                        spectrumJPanel.add(spectrumPanel);
                    }

                    updateAnnotationMenus(maxPrecursorCharge, allModifications);

                } catch (Exception e) {
                    catchException(e);
                }
            }
        }

        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();
    }

    /**
     * Loads the results of the given spectrum files and loads everything in the
     * identification.
     *
     * @param resultFiles the result files
     */
    public void displayResults(ArrayList<File> resultFiles) {
        displayResults(resultFiles, null);
    }

    /**
     * Loads the results of the given spectrum files and loads everything in the
     * identification.
     *
     * @param resultFiles the result files
     * @param spectrumFiles the spectrum files to be added in the spectrum
     * factory, ignored if null
     */
    public void displayResults(ArrayList<File> resultFiles, ArrayList<File> spectrumFiles) {

        final ArrayList<File> finalOutFiles = resultFiles;
        final ArrayList<File> finalMgfFiles = spectrumFiles;

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    if (finalMgfFiles != null) {
                        String[] fileNamesArray = new String[finalMgfFiles.size()];
                        for (int i = 0; i < finalMgfFiles.size(); i++) {
                            File mgfFile = finalMgfFiles.get(i);
                            deNovoGUI.getMsFileHandler().register(mgfFile, progressDialog);
                            fileNamesArray[i] = mgfFile.getName();
                        }
                        spectrumFileComboBox.setModel(new DefaultComboBoxModel(fileNamesArray));
                    }

                    // import the de novo results
                    identification = importDeNovoResults(finalOutFiles, searchParameters, progressDialog);

                    if (identification != null) {
                        displayResults();
                    }

                    progressDialog.setRunFinished();

                    // @TODO: catch out of memory...
                } catch (Exception e) {
                    catchException(e);
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Update the annotation menu bar with the current annotation preferences.
     *
     * @param precursorCharge the precursor charges
     * @param modificationMatches the modifications
     */
    public void updateAnnotationMenus(int precursorCharge, ArrayList<ModificationMatch> modificationMatches) {

        aIonCheckBoxMenuItem.setSelected(false);
        bIonCheckBoxMenuItem.setSelected(false);
        cIonCheckBoxMenuItem.setSelected(false);
        xIonCheckBoxMenuItem.setSelected(false);
        yIonCheckBoxMenuItem.setSelected(false);
        zIonCheckBoxMenuItem.setSelected(false);
        precursorCheckMenu.setSelected(false);
        immoniumIonsCheckMenu.setSelected(false);
        relatedIonsCheckMenu.setSelected(false);
        reporterIonsCheckMenu.setSelected(false);

        HashMap<Ion.IonType, HashSet<Integer>> ionTypes;
        if (specificAnnotationParameters != null) {
            ionTypes = specificAnnotationParameters.getIonTypes();
        } else {
            ionTypes = annotationParameters.getIonTypes();
        }

        for (Ion.IonType ionType : ionTypes.keySet()) {
            if (null != ionType) {
                switch (ionType) {
                    case IMMONIUM_ION:
                        immoniumIonsCheckMenu.setSelected(true);
                        break;
                    case RELATED_ION:
                        relatedIonsCheckMenu.setSelected(true);
                        break;
                    case PRECURSOR_ION:
                        precursorCheckMenu.setSelected(true);
                        break;
                    case REPORTER_ION:
                        reporterIonsCheckMenu.setSelected(true);
                        break;
                    case TAG_FRAGMENT_ION:
                        for (int subtype : ionTypes.get(ionType)) {
                            switch (subtype) {
                                case TagFragmentIon.A_ION:
                                    aIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.B_ION:
                                    bIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.C_ION:
                                    cIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.X_ION:
                                    xIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Y_ION:
                                    yIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Z_ION:
                                    zIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {
            lossMenu.remove(lossMenuItem);
        }
        lossMenu.setVisible(true);
        lossSplitter.setVisible(true);
        lossMenus.clear();

        HashMap<String, NeutralLoss> neutralLosses = new HashMap<String, NeutralLoss>();

        // add the general neutral losses
        for (NeutralLoss neutralLoss : IonFactory.getInstance().getDefaultNeutralLosses()) {
            neutralLosses.put(neutralLoss.name, neutralLoss);
        }

        // add the sequence specific neutral losses
        for (ModificationMatch modMatch : modificationMatches) {
            Modification mod = modFactory.getModification(modMatch.getModification());
            for (NeutralLoss neutralLoss : mod.getNeutralLosses()) {
                neutralLosses.put(neutralLoss.name, neutralLoss);
            }
        }

        ArrayList<String> names = new ArrayList<String>(neutralLosses.keySet());
        Collections.sort(names);

        if (neutralLosses.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {

            ArrayList<String> currentNeutralLosses;
            boolean neutralLossesAuto;
            if (specificAnnotationParameters != null) {
                currentNeutralLosses = specificAnnotationParameters.getNeutralLossesMap().getAccountedNeutralLosses();
                neutralLossesAuto = specificAnnotationParameters.isNeutralLossesAuto();
            } else {
                ArrayList<NeutralLoss> annotationNeutralLosses = annotationParameters.getNeutralLosses();
                if (annotationNeutralLosses != null) {
                    currentNeutralLosses = new ArrayList<String>(annotationNeutralLosses.size());
                    for (NeutralLoss neutralLoss : annotationNeutralLosses) {
                        currentNeutralLosses.add(neutralLoss.name);
                    }
                } else {
                    currentNeutralLosses = new ArrayList<String>(0);
                }
                neutralLossesAuto = true;
            }

            for (int i = 0; i < names.size(); i++) {

                String neutralLossName = names.get(i);
                NeutralLoss neutralLoss = neutralLosses.get(neutralLossName);

                boolean selected = false;
                for (String specificNeutralLossName : currentNeutralLosses) {
                    NeutralLoss specificNeutralLoss = NeutralLoss.getNeutralLoss(specificNeutralLossName);
                    if (neutralLoss.isSameAs(specificNeutralLoss)) {
                        selected = true;
                        break;
                    }
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(neutralLossName);
                lossMenuItem.setSelected(selected);
                lossMenuItem.setEnabled(!neutralLossesAuto);
                lossMenuItem.addActionListener(new java.awt.event.ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        deselectDefaultAnnotationMenuItem();
                        updateSpectrum();
                    }
                });
                lossMenus.put(neutralLosses.get(neutralLossName), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
            adaptCheckBoxMenuItem.setSelected(neutralLossesAuto);
        }

        chargeMenus.clear();
        chargeMenu.removeAll();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        HashSet<Integer> selectedCharges;
        if (specificAnnotationParameters != null) {
            selectedCharges = specificAnnotationParameters.getSelectedCharges();
        } else {
            selectedCharges = new HashSet<Integer>();
            selectedCharges.add(1);
        }

        for (Integer charge = 1; charge < precursorCharge; charge++) {

            final JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            chargeMenuItem.setSelected(selectedCharges.contains(charge));
            chargeMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    deselectDefaultAnnotationMenuItem();
                    updateSpectrum();
                }
            });

            chargeMenus.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        // General annotation settings
        highResAnnotationCheckBoxMenuItem.setSelected(annotationParameters.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz); //@TODO: change to a drop down menu
        allCheckBoxMenuItem.setSelected(annotationParameters.showAllPeaks());
    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    public void updateAnnotationPreferences() {

        if (!defaultAnnotationCheckBoxMenuItem.isSelected()) {

            specificAnnotationParameters.clearIonTypes();
            if (aIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.A_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
            }
            if (bIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
            }
            if (cIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.C_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
            }
            if (xIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.X_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
            }
            if (yIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
            }
            if (zIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.TAG_FRAGMENT_ION, TagFragmentIon.Z_ION);
                specificAnnotationParameters.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
            }
            if (precursorCheckMenu.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.PRECURSOR_ION);
            }
            if (immoniumIonsCheckMenu.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.IMMONIUM_ION);
            }
            if (relatedIonsCheckMenu.isSelected()) {
                specificAnnotationParameters.addIonType(Ion.IonType.RELATED_ION);
            }
            if (reporterIonsCheckMenu.isSelected()) {
                for (int subtype : getReporterIons()) {
                    specificAnnotationParameters.addIonType(Ion.IonType.REPORTER_ION, subtype);
                }
            }

            if (!adaptCheckBoxMenuItem.isSelected()) {
                specificAnnotationParameters.setNeutralLossesAuto(false);
                specificAnnotationParameters.clearNeutralLosses();
                for (NeutralLoss neutralLoss : lossMenus.keySet()) {
                    if (lossMenus.get(neutralLoss).isSelected()) {
                        specificAnnotationParameters.addNeutralLoss(neutralLoss);
                    }
                }
            } else {
                specificAnnotationParameters.clearNeutralLosses(); // Neutral losses are turned off by default in denovogui
            }

            specificAnnotationParameters.clearCharges();
            for (int charge : chargeMenus.keySet()) {
                if (chargeMenus.get(charge).isSelected()) {
                    specificAnnotationParameters.addSelectedCharge(charge);
                }
            }

        } else {
            specificAnnotationParameters.clearNeutralLosses(); // Neutral losses are turned off by default in denovogui
            selectDefaultAnnotationMenuItem();
        }

        // The following preferences are kept for all spectra
        SpectrumAnnotator.TiesResolution tiesResolution = highResAnnotationCheckBoxMenuItem.isSelected() ? SpectrumAnnotator.TiesResolution.mostAccurateMz : SpectrumAnnotator.TiesResolution.mostIntense;
        annotationParameters.setTiesResolution(tiesResolution); //@TODO: replace by a drop down menu
        annotationParameters.setShowAllPeaks(allCheckBoxMenuItem.isSelected());
        annotationParameters.setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
        annotationParameters.setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

        if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
            annotationParameters.setDeNovoCharge(1);
        } else {
            annotationParameters.setDeNovoCharge(2);
        }
    }

    /**
     * Returns the reporter ions possibly found in this project.
     *
     * @return the reporter ions possibly found in this project
     */
    public ArrayList<Integer> getReporterIons() {

        ArrayList<String> modifications = getFoundModifications();
        ArrayList<Integer> reporterIonsSubtypes = new ArrayList<Integer>();

        for (String mod : modifications) {
            Modification tempMod = modFactory.getModification(mod);
            for (ReporterIon reporterIon : tempMod.getReporterIons()) {
                int subType = reporterIon.getSubType();
                if (!reporterIonsSubtypes.contains(subType)) {
                    reporterIonsSubtypes.add(subType);
                }
            }
        }

        return reporterIonsSubtypes;
    }

    /**
     * Returns the normal icon.
     *
     * @return the normal icon.
     */
    public Image getNormalIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png"));
    }

    /**
     * Returns the waiting icon.
     *
     * @return the waiting icon
     */
    public Image getWaitingIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png"));
    }

    /**
     * Returns the current spectrum as an MGF string.
     *
     * @return the current spectrum as an MGF string
     */
    public String getSpectrumAsMgf() {

        if (deNovoGUI.getMsFileHandler().spectrumLoaded(spectrumKey) && deNovoMatchesTable.getSelectedRow() != -1) {

            StringBuilder spectraAsMgf = new StringBuilder();

            try {
                Spectrum currentSpectrum = deNovoGUI.getMsFileHandler().getSpectrum(getSelectedSpectrumFile(), getSelectedSpectrumTitle());
                spectraAsMgf.append(currentSpectrum.asMgf());
                return spectraAsMgf.toString();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred when reading the spectrum.", "File Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred when reading the spectrum.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        return null;
    }

    /**
     * Returns a string with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide the peptide
     * @return a string with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        String peptideSequence = peptide.getSequence();

        // @TODO: update to use the ptm scores
        // @TODO: merge with getPeptideModificationTooltipAsHtml in PeptideShaker and move to utilities
        String tooltip = "<html>";
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (int aa = 1; aa <= peptideSequence.length(); aa++) {

            int aaIndex = aa - 1;
            char aminoAcid = peptideSequence.charAt(aaIndex);

            if (peptide.isModified()) {
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                    if ((modificationMatch.isVariable() || (!modificationMatch.isVariable() && fixedPtmsCheckBoxMenuItem.isSelected())) && modificationMatch.getModificationSite() == aa) {
                        String ptmName = modificationMatch.getModification();

                        String ptmConfidence;
                        if (!modificationMatch.isVariable()) {
                            ptmConfidence = "fixed";
                        } else if (modificationMatch.isConfident()) {
                            ptmConfidence = "confident";
                        } else {
                            ptmConfidence = "not confident";
                        }

                        String temp = AminoAcidSequence.getTaggedResidue(aminoAcid, ptmName, searchParameters.getModificationParameters(), 1, true, true) + ": " + ptmName + " (" + ptmConfidence + ")<br>";
                        if (!alreadyAnnotated.contains(temp)) {
                            tooltip += temp;
                            alreadyAnnotated.add(temp);
                        }
                    }
                }
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Returns a String with the HTML tooltip for the tag indicating the
     * modification details.
     *
     * @param tag the tag
     * @return a String with the HTML tooltip for the tag
     */
    public String getTagModificationTooltipAsHtml(Tag tag) {

        // @TODO: update to use the ptm scores
        // @TODO: merge with getTagModificationTooltipAsHtml in PeptideShaker and move to utilities
        String tooltip = "<html>";

        for (TagComponent tagComponent : tag.getContent()) {
            if (tagComponent instanceof AminoAcidPattern) {
                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;
                for (int site = 1; site <= aminoAcidPattern.length(); site++) {
                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(site)) {
                        String affectedResidue = aminoAcidPattern.asSequence(site - 1);
                        String modName = modificationMatch.getModification();
                        if (modificationMatch.isVariable() || (!modificationMatch.isVariable() && fixedPtmsCheckBoxMenuItem.isSelected())) {
                            Color ptmColor = new Color(deNovoGUI.getSearchParameters().getModificationParameters().getColor(modName));
                            if (!modificationMatch.isVariable()) {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (fixed)<br>";
                            } else if (modificationMatch.isConfident()) {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (confident)<br>";
                            } else {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(ptmColor) + ";background:#" + Util.color2Hex(Color.WHITE) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (not confident)<br>";
                            }
                        }
                    }
                }
            } else if (tagComponent instanceof AminoAcidSequence) {
                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;
                for (int site = 1; site <= aminoAcidSequence.length(); site++) {
                    for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(site)) {
                        if (modificationMatch.isVariable() || (!modificationMatch.isVariable() && fixedPtmsCheckBoxMenuItem.isSelected())) {
                            char affectedResidue = aminoAcidSequence.charAt(site - 1);
                            String modName = modificationMatch.getModification();
                            Color ptmColor = new Color(deNovoGUI.getSearchParameters().getModificationParameters().getColor(modName));
                            if (!modificationMatch.isVariable()) {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (fixed)<br>";
                            } else if (modificationMatch.isConfident()) {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (confident)<br>";
                            } else {
                                tooltip += "<span style=\"color:#" + Util.color2Hex(ptmColor) + ";background:#" + Util.color2Hex(Color.WHITE) + "\">"
                                        + affectedResidue
                                        + "</span>"
                                        + ": " + modName + " (not confident)<br>";
                            }
                        }
                    }
                }
            } else if (tagComponent instanceof MassGap) {
                // Nothing to do here
            } else {
                throw new UnsupportedOperationException("Annotation not supported for the tag component " + tagComponent.getClass() + ".");
            }
        }

        if (!tooltip.equalsIgnoreCase("<html>")) {
            tooltip += "</html>";
        } else {
            tooltip = null;
        }

        return tooltip;
    }

    /**
     * Imports the de novo results from the given files and puts all matches in
     * the identification.
     *
     * @param resultFiles the result files
     * @param searchParameters the search parameters
     * @param waitingHandler the waiting handler
     * @return the Identification object
     * @throws Exception thrown if an exception occurs
     */
    public Identification importDeNovoResults(ArrayList<File> resultFiles, SearchParameters searchParameters, WaitingHandler waitingHandler) throws Exception {

        // @TODO: let the user reference his project
        String projectReference = "DeNovoGUI";
        String sampleReference = "sample reference";
        int replicateNumber = 0;
        String identificationReference = Identification.getDefaultReference(projectReference, sampleReference, replicateNumber);
        MsExperiment experiment = new MsExperiment(projectReference);
        Sample sample = new Sample(sampleReference);
        SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
        experiment.addAnalysisSet(sample, analysisSet);
        ProteomicAnalysis analysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
        analysis.addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification(identificationReference));

        // The identification object
        Identification tempIdentification = analysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);

        // The cache used whenever the identification becomes too big
        String dbFolder = getCacheDirectory(getJarFilePath()).getAbsolutePath();
        objectsCache.setAutomatedMemoryManagement(true);
        try {
            tempIdentification.establishConnection(dbFolder, true, objectsCache);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while creating the identification database. "
                    + "Please make sure that no other instance of DeNovoGUI is running.", "Database Connection error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
            return null;
        }

        numAdvocatesLoaded = 0;
        boolean pepNovoDataLoaded = false;
        boolean direcTagDataLoaded = false;
        boolean pNovoDataLoaded = false;
        boolean novorDataLoaded = false;

        for (int i = 0; i < resultFiles.size(); i++) {

            File resultFile = resultFiles.get(i);

            // initiate the parser
            String loadingText = "Loading Results. Loading File. Please Wait...";
            if (resultFiles.size() > 1) {
                loadingText += " (" + (i + 1) + "/" + resultFiles.size() + ")";
            }
            progressDialog.setTitle(loadingText);

            IdfileReader idfileReader = IdfileReaderFactory.getInstance().getFileReader(resultFile);

            if (idfileReader != null) {

                ArrayList<SpectrumMatch> spectrumMatches = idfileReader.getAllSpectrumMatches(deNovoGUI.getMsFileHandler(), waitingHandler, searchParameters);
                progressDialog.setPrimaryProgressCounterIndeterminate(true);

                // remap the ptms and set GUI min/max values
                for (SpectrumMatch spectrumMatch : spectrumMatches) {

                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptionsMap = spectrumMatch.getAssumptionsMap();

                    for (int advocate : assumptionsMap.keySet()) {

                        if (advocate == Advocate.pepnovo.getIndex()) {
                            pepNovoDataLoaded = true;
                        } else if (advocate == Advocate.direcTag.getIndex()) {
                            direcTagDataLoaded = true;
                        } else if (advocate == Advocate.pNovo.getIndex()) {
                            pNovoDataLoaded = true;
                        } else if (advocate == Advocate.novor.getIndex()) {
                            novorDataLoaded = true;
                        }

                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> tempAssumptions = assumptionsMap.get(advocate);

                        for (double score : tempAssumptions.keySet()) {
                            for (SpectrumIdentificationAssumption assumption : tempAssumptions.get(score)) {

                                if (assumption instanceof TagAssumption) {

                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    Tag tag = tagAssumption.getTag();

                                    // add the fixed PTMs
                                    modFactory.checkFixedModifications(searchParameters.getModificationParameters(), tag, deNovoGUI.getSequenceMatchingPreferences());

                                    // rename the variable modifications
                                    for (TagComponent tagComponent : tag.getContent()) {
                                        if (tagComponent instanceof AminoAcidPattern) {

                                            AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;

                                            for (int aa : aminoAcidPattern.getModificationIndexes()) {
                                                for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(aa)) {
                                                    if (modificationMatch.isVariable()) {
                                                        if (advocate == Advocate.pepnovo.getIndex()) {
                                                            String pepnovoPtmName = modificationMatch.getModification();
                                                            PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocate);
                                                            String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                                            if (utilitiesPtmName == null) {
                                                                throw new IllegalArgumentException("PepNovo PTM " + pepnovoPtmName + " not recognized in spectrum " + spectrumMatch.getKey() + ".");
                                                            }
                                                            modificationMatch.setModification(utilitiesPtmName);
                                                        } else if (advocate == Advocate.direcTag.getIndex()) {
                                                            // already mapped
                                                        } else if (advocate == Advocate.pNovo.getIndex()) {
                                                            // already mapped
                                                        } else if (advocate == Advocate.novor.getIndex()) {
                                                            // already mapped
                                                        } else {
                                                            Advocate notImplemented = Advocate.getAdvocate(advocate);
                                                            if (notImplemented == null) {
                                                                throw new IllegalArgumentException("Advocate of id " + advocate + " not recognized.");
                                                            }
                                                            throw new IllegalArgumentException("PTM mapping not implemented for " + Advocate.getAdvocate(advocate).getName() + ".");
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (tagComponent instanceof AminoAcidSequence) {

                                            AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;

                                            for (int aa : aminoAcidSequence.getModificationIndexes()) {
                                                for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(aa)) {
                                                    if (modificationMatch.isVariable()) {
                                                        if (advocate == Advocate.pepnovo.getIndex()) {
                                                            String pepnovoPtmName = modificationMatch.getModification();
                                                            PepnovoParameters pepnovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(advocate);
                                                            String utilitiesPtmName = pepnovoParameters.getUtilitiesPtmName(pepnovoPtmName);
                                                            if (utilitiesPtmName == null) {
                                                                throw new IllegalArgumentException("PepNovo PTM " + pepnovoPtmName + " not recognized in spectrum " + spectrumMatch.getKey() + ".");
                                                            }
                                                            modificationMatch.setModification(utilitiesPtmName);
                                                        } else if (advocate == Advocate.direcTag.getIndex()) {
                                                            // already mapped
                                                        } else if (advocate == Advocate.pNovo.getIndex()) {
                                                            // already mapped
                                                        } else if (advocate == Advocate.novor.getIndex()) {
                                                            // already mapped
                                                        } else {
                                                            Advocate notImplemented = Advocate.getAdvocate(advocate);
                                                            if (notImplemented == null) {
                                                                throw new IllegalArgumentException("Advocate of id " + advocate + " not recognized.");
                                                            }
                                                            throw new IllegalArgumentException("PTM mapping not implemented for " + Advocate.getAdvocate(advocate).getName() + ".");
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (tagComponent instanceof MassGap) {
                                            // Nothing to do here
                                        } else {
                                            throw new UnsupportedOperationException("Annotation not supported for the tag component " + tagComponent.getClass() + ".");
                                        }
                                    }

                                    // Set GUI min/max values
                                    double mz = tagAssumption.getTheoreticMz();
                                    if (mz > maxIdentificationMz) {
                                        maxIdentificationMz = mz;
                                    }
                                    if (tagAssumption.getIdentificationCharge() > maxIdentificationCharge) {
                                        maxIdentificationCharge = tagAssumption.getIdentificationCharge();
                                    }
                                    double nGap = tag.getNTerminalGap();
                                    if (nGap > maxNGap) {
                                        maxNGap = nGap;
                                    }
                                    double cGap = tag.getCTerminalGap();
                                    if (cGap > maxCGap) {
                                        maxCGap = cGap;
                                    }

                                    if (advocate == Advocate.pepnovo.getIndex()) {
                                        PepnovoAssumptionDetails pepnovoAssumptionDetails = new PepnovoAssumptionDetails();
                                        pepnovoAssumptionDetails = (PepnovoAssumptionDetails) tagAssumption.getUrParam(pepnovoAssumptionDetails);
                                        double rankScore = pepnovoAssumptionDetails.getRankScore();
                                        if (rankScore < minRankScore) {
                                            minRankScore = rankScore;
                                        }
                                        if (rankScore > maxRankScore) {
                                            maxRankScore = rankScore;
                                        }
                                        if (score > maxPepnovoScore) {
                                            maxPepnovoScore = score;
                                        }
                                    } else if (advocate == Advocate.direcTag.getIndex()) {
                                        if (score > maxDirectTagEvalue) {
                                            maxDirectTagEvalue = score;
                                        }
                                        if (score < minDirectTagEvalue) {
                                            minDirectTagEvalue = score;
                                        }
                                    } else if (advocate == Advocate.pNovo.getIndex()) {
                                        if (score > maxPNovoScore) {
                                            maxPNovoScore = score;
                                        }
                                        if (score < minPNovoScore) {
                                            minPNovoScore = score;
                                        }
                                    } else if (advocate == Advocate.novor.getIndex()) {
                                        if (score > maxNovorScore) {
                                            maxNovorScore = score;
                                        }
                                        if (score < minNovorScore) {
                                            minNovorScore = score;
                                        }
                                    } else {
                                        Advocate notImplemented = Advocate.getAdvocate(advocate);
                                        if (notImplemented == null) {
                                            throw new IllegalArgumentException("Advocate of id " + advocate + " not recognized.");
                                        }
                                        throw new IllegalArgumentException("Unsupported advocate " + Advocate.getAdvocate(advocate).getName() + ".");
                                    }
                                } else if (assumption instanceof PeptideAssumption) {
                                    //@TODO: implement other algorithms?
                                    if (advocate == Advocate.novor.getIndex()) {
                                        if (score > maxNovorScore) {
                                            maxNovorScore = score;
                                        }
                                        if (score < minNovorScore) {
                                            minNovorScore = score;
                                        }
                                    } else {
                                        Advocate notImplemented = Advocate.getAdvocate(advocate);
                                        if (notImplemented == null) {
                                            throw new IllegalArgumentException("Advocate of id " + advocate + " not recognized.");
                                        }
                                        throw new IllegalArgumentException("Unsupported advocate " + Advocate.getAdvocate(advocate).getName() + ".");
                                    }
                                } else {
                                    throw new UnsupportedOperationException("Operation not supported for assumption of type " + assumption.getClass() + ".");
                                }
                            }
                        }
                    }
                }

                // put the matches in the identification object
                tempIdentification.addSpectrumMatches(spectrumMatches);

                idfileReader.close();

                loadingText = "Loading Results. Loading Matches. Please Wait...";
                if (resultFiles.size() > 1) {
                    loadingText += " (" + (i + 1) + "/" + resultFiles.size() + ")";
                }
            }

            progressDialog.setTitle(loadingText);
        }

        int numberOfAdvocateLoaded = 0;
        if (pepNovoDataLoaded) {
            numberOfAdvocateLoaded++;
        }
        if (direcTagDataLoaded) {
            numberOfAdvocateLoaded++;
        }
        if (pNovoDataLoaded) {
            numberOfAdvocateLoaded++;
        }
        if (novorDataLoaded) {
            numberOfAdvocateLoaded++;
        }

        numAdvocatesLoaded = numberOfAdvocateLoaded;

        return tempIdentification;
    }

    /**
     * Close the DB connection and empty the temp folder.
     */
    private void closeConnectionsAndEmptyTempFolder() {

        if (identification != null) {

            // @TODO: the progress dialog setup below should work but results in thread issues...
//            progressDialog = new ProgressDialogX(this,
//                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
//                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
//                    true);
//            progressDialog.setTitle("Closing. Please Wait...");
//            progressDialog.setPrimaryProgressCounterIndeterminate(true);
//
//            new Thread(new Runnable() {
//                public void run() {
//                    try {
//                        progressDialog.setVisible(true);
//                    } catch (IndexOutOfBoundsException e) {
//                        // ignore
//                    }
//                }
//            }, "ProgressDialog").start();
//
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
            try {
                identification.close();
                DerbyUtil.closeConnection();
                File matchFolder = getCacheDirectory(getJarFilePath());
                File[] tempFiles = matchFolder.listFiles();

                if (tempFiles != null) {
                    for (File currentFile : tempFiles) {
                        Util.deleteDir(currentFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred when closing the identification database.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
//                    finally {
//                        progressDialog.setRunFinished();
//                    }
//                }
//            });
        }
    }

    /**
     * Set the currently selected PSM.
     *
     * @param spectrumFileName the spectrum file name
     * @param spectrumTitle the spectrum title
     * @param psmRow the row number of the PSM
     */
    public void setSelectedPsm(final String spectrumFileName, final String spectrumTitle, final int psmRow) {

        if (!((String) spectrumFileComboBox.getSelectedItem()).equalsIgnoreCase(spectrumFileName)) {
            spectrumFileComboBox.setSelectedItem(spectrumFileName);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int spectrumRowIndex = querySpectraTable.convertRowIndexToView(orderedSpectrumTitles.indexOf(spectrumTitle));
                querySpectraTable.setRowSelectionInterval(spectrumRowIndex, spectrumRowIndex);
                querySpectraTable.scrollRectToVisible(querySpectraTable.getCellRect(spectrumRowIndex, 0, false));
                updateAssumptionsTable(deNovoMatchesTable.convertRowIndexToView(psmRow));
            }
        });
    }

    /**
     * Returns the modifications found in this project.
     *
     * @return the modifications found in this project
     */
    public ArrayList<String> getFoundModifications() {
        return searchParameters.getModificationParameters().getAllModifications();
    }

    /**
     * Get the annotation parameters.
     *
     * @return the annotation parameters
     */
    public AnnotationParameters getAnnotationParameters() {
        return annotationParameters;
    }

    /**
     * Set the annotation parameters.
     *
     * @param annotationParameters the annotation parameters
     */
    public void setAnnotationParameters(AnnotationParameters annotationParameters) {
        this.annotationParameters = annotationParameters;
    }

    /**
     * Get the current delta masses for use when annotating the spectra.
     *
     * @return the current delta masses
     */
    public HashMap<Double, String> getCurrentMassDeltas() {

        HashMap<Double, String> knownMassDeltas = new HashMap<Double, String>();

        // add the monoisotopic amino acids masses
        knownMassDeltas.put(AminoAcid.A.getMonoisotopicMass(), "A");
        knownMassDeltas.put(AminoAcid.R.getMonoisotopicMass(), "R");
        knownMassDeltas.put(AminoAcid.N.getMonoisotopicMass(), "N");
        knownMassDeltas.put(AminoAcid.D.getMonoisotopicMass(), "D");
        knownMassDeltas.put(AminoAcid.C.getMonoisotopicMass(), "C");
        knownMassDeltas.put(AminoAcid.Q.getMonoisotopicMass(), "Q");
        knownMassDeltas.put(AminoAcid.E.getMonoisotopicMass(), "E");
        knownMassDeltas.put(AminoAcid.G.getMonoisotopicMass(), "G");
        knownMassDeltas.put(AminoAcid.H.getMonoisotopicMass(), "H");
        knownMassDeltas.put(AminoAcid.I.getMonoisotopicMass(), "I/L");
        knownMassDeltas.put(AminoAcid.K.getMonoisotopicMass(), "K");
        knownMassDeltas.put(AminoAcid.M.getMonoisotopicMass(), "M");
        knownMassDeltas.put(AminoAcid.F.getMonoisotopicMass(), "F");
        knownMassDeltas.put(AminoAcid.P.getMonoisotopicMass(), "P");
        knownMassDeltas.put(AminoAcid.S.getMonoisotopicMass(), "S");
        knownMassDeltas.put(AminoAcid.T.getMonoisotopicMass(), "T");
        knownMassDeltas.put(AminoAcid.W.getMonoisotopicMass(), "W");
        knownMassDeltas.put(AminoAcid.Y.getMonoisotopicMass(), "Y");
        knownMassDeltas.put(AminoAcid.V.getMonoisotopicMass(), "V");
        knownMassDeltas.put(AminoAcid.U.getMonoisotopicMass(), "U");
        knownMassDeltas.put(AminoAcid.O.getMonoisotopicMass(), "O");

        // add default neutral losses
//        knownMassDeltas.put(NeutralLoss.H2O.mass, "H2O");
//        knownMassDeltas.put(NeutralLoss.NH3.mass, "NH3");
//        knownMassDeltas.put(NeutralLoss.CH4OS.mass, "CH4OS");
//        knownMassDeltas.put(NeutralLoss.H3PO4.mass, "H3PO4");
//        knownMassDeltas.put(NeutralLoss.HPO3.mass, "HPO3");
//        knownMassDeltas.put(4d, "18O"); // @TODO: should this be added to neutral losses??
//        knownMassDeltas.put(44d, "PEG"); // @TODO: should this be added to neutral losses??
        // add the modifications
        ModificationParameters modificationProfile = searchParameters.getModificationParameters();
        ArrayList<String> modificationList = modificationProfile.getAllModifications();
        Collections.sort(modificationList);

        // iterate the modifications list and add the non-terminal modifications
        for (String modification : modificationList) {
            Modification ptm = modFactory.getModification(modification);

            if (ptm != null) {

                String shortName = ptm.getShortName();
                AminoAcidPattern ptmPattern = ptm.getPattern();
                double mass = ptm.getMass();

                if (ptm.getModificationType() == ModificationType.modaa && ptmPattern != null) {
                    for (Character aa : ptmPattern.getAminoAcidsAtTarget()) {
                        if (!knownMassDeltas.containsValue(aa + "<" + shortName + ">")) {
                            AminoAcid aminoAcid = AminoAcid.getAminoAcid(aa);
                            knownMassDeltas.put(mass + aminoAcid.getMonoisotopicMass(),
                                    aa + "<" + shortName + ">");
                        }
                    }
                }
            } else {
                System.out.println("Error: PTM not found: " + modification);
            }
        }

        return knownMassDeltas;
    }

    /**
     * Returns the folder used for caching identification objects.
     *
     * @param jarFilePath the path to the jar file
     *
     * @return the cache directory
     */
    public static File getCacheDirectory(String jarFilePath) {
        File parentFolder;
        if (CACHE_PARENT_DIRECTORY.equals("resources")) {
            parentFolder = new File(jarFilePath, CACHE_PARENT_DIRECTORY);
        } else {
            parentFolder = new File(CACHE_PARENT_DIRECTORY);
        }
        return new File(parentFolder, CACHE_DIRECTORY_NAME);
    }

    /**
     * Returns the directory used to save identification matches.
     *
     * @return the directory used to save identification matches
     */
    public static String getCacheDirectoryParent() {
        return CACHE_PARENT_DIRECTORY;
    }

    /**
     * Sets the directory used to save identification matches.
     *
     * @param cacheDirectory the directory used to save identification matches
     */
    public static void setCacheDirectoryParent(String cacheDirectory) {
        ResultsFrame.CACHE_PARENT_DIRECTORY = cacheDirectory;
    }

    /**
     * Method called whenever an exception is caught.
     *
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    protected String getJarFilePath() {
        return DeNovoGUIWrapper.getJarFilePath(this.getClass().getResource("DeNovoGUI.class").getPath(), DeNovoGUIWrapper.toolName);
    }

    /**
     * Selects the default annotation menu item.
     */
    private void selectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        resetAnnotationMenu.setVisible(false);
    }

    /**
     * Deselects the default annotation menu item.
     */
    private void deselectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(false);
        resetAnnotationMenu.setVisible(true);
    }

    /**
     * Converts the amino acid scores to alpha values. Assumes that the amino
     * acid scores are in the range [0-100].
     *
     * @param aminoAcidScores the amino acid scores
     * @return the alpha values
     */
    private ArrayList<float[]> convertAminoAcidScoresToAlphaValues(ArrayList<double[]> aminoAcidScores) {

        ArrayList<float[]> alphaValues = new ArrayList<float[]>();

        for (double[] tempAminoAcidScores : aminoAcidScores) {

            float[] tempAlphaValues = new float[tempAminoAcidScores.length];

            for (int i = 0; i < tempAminoAcidScores.length; i++) {
                tempAlphaValues[i] = 0.2f * ((float) tempAminoAcidScores[i] / 100);
            }

            alphaValues.add(tempAlphaValues);
        }

        return alphaValues;
    }
}
