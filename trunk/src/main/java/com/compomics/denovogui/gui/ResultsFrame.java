package com.compomics.denovogui.gui;

import com.compomics.denovogui.PepNovoIdfileReader;
import com.compomics.denovogui.gui.tablemodels.SpectrumMatchTableModel;
import com.compomics.denovogui.gui.tablemodels.SpectrumTableModel;
import com.compomics.denovogui.io.TextExporter;
import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.denovo.PeptideAssumptionDetails;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.Atom;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.IonFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialog;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialogParent;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import java.awt.Color;
import java.awt.Component;
import static java.awt.Frame.MAXIMIZED_BOTH;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
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
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Frame for showing the results of de novo sequencing.
 *
 * @author Harald Barsnes
 */
public class ResultsFrame extends javax.swing.JFrame implements ExportGraphicsDialogParent {

    /**
     * A references to the main frame.
     */
    private DeNovoGUI deNovoGUI;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The current peptide assumptions.
     */
    private ArrayList<PeptideAssumption> assumptions = new ArrayList<PeptideAssumption>();
    /**
     * The annotation preferences.
     */
    private AnnotationPreferences annotationPreferences = new AnnotationPreferences();
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
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
    private PTMFactory ptmFactory = PTMFactory.getInstance();
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
     * The location of the folder used for caching.
     */
    public final static String CACHE_DIRECTORY = "resources/matches";
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
     * The actually identified modifications.
     */
    private ArrayList<String> identifiedModifications = null;

    /**
     * Creates a new ResultsPanel.
     *
     * @param deNovoGUI a references to the main frame
     * @param outFiles the out files
     * @param searchParameters the search parameters
     */
    public ResultsFrame(DeNovoGUI deNovoGUI, ArrayList<File> outFiles, SearchParameters searchParameters) {
        initComponents();
        this.deNovoGUI = deNovoGUI;
        this.searchParameters = searchParameters;
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        // set the title of the frame and add the icon
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));
        setUpGUI();
        if (outFiles != null) {
            setVisible(true);
            displayResults(outFiles);
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

        // Add default neutral losses to display
        //IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.H2O); // @TODO: uncomment to add neutral losses. but results in rather messy spectra without a lower annotation intensity threshold...
        //IonFactory.getInstance().addDefaultNeutralLoss(NeutralLoss.NH3);
        annotationPreferences.setNeutralLossesSequenceDependant(true);

        spectrumAnnotationMenuPanel.add(annotationMenuBar);
        updateAnnotationPreferences();

        // make sure that the scroll panes are see-through
        querySpectraTableScrollPane.getViewport().setOpaque(false);
        deNovoPeptidesTableScrollPane.getViewport().setOpaque(false);

        querySpectraTable.getTableHeader().setReorderingAllowed(false);
        deNovoPeptidesTable.getTableHeader().setReorderingAllowed(false);

        querySpectraTable.setAutoCreateRowSorter(true);
        deNovoPeptidesTable.setAutoCreateRowSorter(true);

        // correct the color for the upper right corner
        JPanel queryCorner = new JPanel();
        queryCorner.setBackground(querySpectraTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, queryCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(deNovoPeptidesTable.getTableHeader().getBackground());
        deNovoPeptidesTableScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);

        // hide items that are not implemented
        annotationColorsJMenuItem.setVisible(false);
        jSeparator14.setVisible(false);

        // setup the column header tooltips
        querySpectraTableToolTips = new ArrayList<String>();
        querySpectraTableToolTips.add(null);
        querySpectraTableToolTips.add("Spectrum Title");
        querySpectraTableToolTips.add("Precusor m/z");
        querySpectraTableToolTips.add("Precursor Charge");
        querySpectraTableToolTips.add("Precursor Intensity");
        querySpectraTableToolTips.add("Retention Time");
        querySpectraTableToolTips.add("Number of Peaks");
        querySpectraTableToolTips.add("Max De Novo Score");
        querySpectraTableToolTips.add("De Novo Solution");

        deNovoPeptidesTableToolTips = new ArrayList<String>();
        deNovoPeptidesTableToolTips.add(null);
        deNovoPeptidesTableToolTips.add("Peptide Sequences");
        deNovoPeptidesTableToolTips.add("Precursor m/z");
        deNovoPeptidesTableToolTips.add("Precursor Charge");
        deNovoPeptidesTableToolTips.add("N-terminal Gap");
        deNovoPeptidesTableToolTips.add("C-terminal Gap");
        deNovoPeptidesTableToolTips.add("PepNovo Rank Score");
        deNovoPeptidesTableToolTips.add("PepNovo Score");
        deNovoPeptidesTableToolTips.add("BLAST Sequence");

        // set the title
        this.setTitle("DeNovoGUI " + deNovoGUI.getVersion());
    }

    /**
     * Set the table properties.
     */
    private void setTableProperties() {

        double maxMz = Math.max(spectrumFactory.getMaxMz(), maxIdentificationMz);
        double maxCharge = Math.max(spectrumFactory.getMaxCharge(), maxIdentificationCharge);

        querySpectraTable.getColumn(" ").setMaxWidth(50);
        querySpectraTable.getColumn(" ").setMinWidth(50);
        querySpectraTable.getColumn("  ").setMaxWidth(30);
        querySpectraTable.getColumn("  ").setMinWidth(30);

        querySpectraTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "De Novo Solution", "No De Novo Solution"));
        querySpectraTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCharge, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, labelWidth - 30);
        querySpectraTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxMz, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, spectrumFactory.getMaxIntensity(), sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, labelWidth + 20);
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Int").getCellRenderer()).setLogScale(true);
        querySpectraTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, spectrumFactory.getMinRT(),
                spectrumFactory.getMaxRT(), spectrumFactory.getMaxRT() / 50, sparklineColor, sparklineColor));
        ((JSparklinesIntervalChartTableCellRenderer) querySpectraTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);
        ((JSparklinesIntervalChartTableCellRenderer) querySpectraTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
        querySpectraTable.getColumn("#Peaks").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, (double) spectrumFactory.getMaxPeakCount(), sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("#Peaks").getCellRenderer()).showNumberAndChart(true, labelWidth);
        querySpectraTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPepnovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, labelWidth);


        deNovoPeptidesTable.getColumn(" ").setMaxWidth(50);
        deNovoPeptidesTable.getColumn(" ").setMinWidth(50);
        deNovoPeptidesTable.getColumn("  ").setMaxWidth(30);
        deNovoPeptidesTable.getColumn("  ").setMinWidth(30);

        deNovoPeptidesTable.getColumn("Rank Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, minRankScore, maxRankScore, Color.BLUE, Color.RED));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Rank Score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxPepnovoScore, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoPeptidesTable.getColumn("N-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxNGap, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("N-Gap").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoPeptidesTable.getColumn("C-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCGap, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("C-Gap").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoPeptidesTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxMz, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, labelWidth);
        deNovoPeptidesTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, maxCharge, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, labelWidth - 30);
        deNovoPeptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/blast.png")),
                null,
                "Click to BLAST peptide sequence", null));

        querySpectraTable.revalidate();
        querySpectraTable.repaint();
        deNovoPeptidesTable.revalidate();
        deNovoPeptidesTable.repaint();

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
    public Identification getPepNovoIdentifications() {
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
        splitterMenu9 = new javax.swing.JMenu();
        settingsMenu = new javax.swing.JMenu();
        allCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        automaticAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
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
        deNovoChargeButtonGroup = new javax.swing.ButtonGroup();
        bcakgroundPanel = new javax.swing.JPanel();
        debovoResultsPanel = new javax.swing.JPanel();
        spectrumViewerPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
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
        deNovoPeptidesPanel = new javax.swing.JPanel();
        deNovoPeptidesTableScrollPane = new javax.swing.JScrollPane();
        deNovoPeptidesTable = new JTable() {
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
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        exportMenu = new javax.swing.JMenu();
        exportMatchesMenuItem = new javax.swing.JMenuItem();
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
        settingsMenu.add(jSeparator5);

        automaticAnnotationCheckBoxMenuItem.setSelected(true);
        automaticAnnotationCheckBoxMenuItem.setText("Automatic Annotation");
        automaticAnnotationCheckBoxMenuItem.setToolTipText("Use automatic annotation");
        automaticAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                automaticAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(automaticAnnotationCheckBoxMenuItem);
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("De Novo Results");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        bcakgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        debovoResultsPanel.setBackground(new java.awt.Color(255, 255, 255));

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
                    .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 948, Short.MAX_VALUE)
                    .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        spectrumViewerPanelLayout.setVerticalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

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
                .addContainerGap()
                .addGroup(querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 948, Short.MAX_VALUE)
                    .addGroup(querySpectraPanelLayout.createSequentialGroup()
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
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                .addContainerGap())
        );

        deNovoPeptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("De Novo Peptides"));
        deNovoPeptidesPanel.setOpaque(false);

        deNovoPeptidesTable.setModel(new SpectrumMatchTableModel());
        deNovoPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        deNovoPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                deNovoPeptidesTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deNovoPeptidesTableMouseExited(evt);
            }
        });
        deNovoPeptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                deNovoPeptidesTableMouseMoved(evt);
            }
        });
        deNovoPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                deNovoPeptidesTableKeyReleased(evt);
            }
        });
        deNovoPeptidesTableScrollPane.setViewportView(deNovoPeptidesTable);

        javax.swing.GroupLayout deNovoPeptidesPanelLayout = new javax.swing.GroupLayout(deNovoPeptidesPanel);
        deNovoPeptidesPanel.setLayout(deNovoPeptidesPanelLayout);
        deNovoPeptidesPanelLayout.setHorizontalGroup(
            deNovoPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 956, Short.MAX_VALUE)
                .addContainerGap())
        );
        deNovoPeptidesPanelLayout.setVerticalGroup(
            deNovoPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout debovoResultsPanelLayout = new javax.swing.GroupLayout(debovoResultsPanel);
        debovoResultsPanel.setLayout(debovoResultsPanelLayout);
        debovoResultsPanelLayout.setHorizontalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(querySpectraPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        debovoResultsPanelLayout.setVerticalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        exportMenu.setMnemonic('E');
        exportMenu.setText("Export");

        exportMatchesMenuItem.setText("Export Matches");
        exportMatchesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMatchesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportMatchesMenuItem);

        exportBlastMatchesMenuItem.setText("Export BLAST Matches");
        exportBlastMatchesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportBlastMatchesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportBlastMatchesMenuItem);

        menuBar.add(exportMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");

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
            .addGap(0, 1008, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bcakgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 766, Short.MAX_VALUE)
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
    private void deNovoPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableMouseReleased

        int row = deNovoPeptidesTable.rowAtPoint(evt.getPoint());
        int column = deNovoPeptidesTable.columnAtPoint(evt.getPoint());

        // check of the user clicked the blast columns
        if (evt.getButton() == 1 && row != -1 && column == deNovoPeptidesTable.getColumn("  ").getModelIndex()) {
            PeptideAssumption peptideAssumption = assumptions.get(deNovoPeptidesTable.convertRowIndexToModel(deNovoPeptidesTable.getSelectedRow()));
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            BareBonesBrowserLaunch.openURL("http://blast.ncbi.nlm.nih.gov/Blast.cgi?PROGRAM=blastp&BLAST_PROGRAMS=blastp&"
                    + "PAGE_TYPE=BlastSearch&SHOW_DEFAULTS=on&LINK_LOC=blasthome&QUERY=" + peptideAssumption.getPeptide().getSequence());
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }

        updateSpectrum();
    }//GEN-LAST:event_deNovoPeptidesTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void deNovoPeptidesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_deNovoPeptidesTableMouseExited

    /**
     * Shows a tooltip with modification details if over the sequence column.
     *
     * @param evt
     */
    private void deNovoPeptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableMouseMoved
        int row = deNovoPeptidesTable.rowAtPoint(evt.getPoint());
        int column = deNovoPeptidesTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && deNovoPeptidesTable.getValueAt(row, column) != null) {
            if (column == deNovoPeptidesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                String sequence = (String) deNovoPeptidesTable.getValueAt(row, column);

                if (sequence.indexOf("<span") != -1) {
                    try {
                        PeptideAssumption peptideAssumption = assumptions.get(row);
                        String tooltip = getPeptideModificationTooltipAsHtml(peptideAssumption.getPeptide());
                        deNovoPeptidesTable.setToolTipText(tooltip);
                    } catch (Exception e) {
                        e.printStackTrace();
                        deNovoGUI.catchException(e);
                    }
                } else {
                    deNovoPeptidesTable.setToolTipText(null);
                }
            }
            if (column == deNovoPeptidesTable.getColumn("  ").getModelIndex()) { // the BLAST column
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                deNovoPeptidesTable.setToolTipText(null);
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            deNovoPeptidesTable.setToolTipText(null);
        }
    }//GEN-LAST:event_deNovoPeptidesTableMouseMoved

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableKeyReleased
        updateSpectrum();
    }//GEN-LAST:event_deNovoPeptidesTableKeyReleased

    /**
     * @see #updateAnnotationPreferences()
     */
    private void aIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_aIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void bIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_bIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void cIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_cIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void xIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_xIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void yIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_yIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void zIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_zIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void precursorCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_precursorCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void immoniumIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_immoniumIonsCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_immoniumIonsCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void reporterIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterIonsCheckMenuActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_reporterIonsCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void adaptCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_adaptCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void forwardIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void rewindIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void deNovoChargeOneJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void deNovoChargeTwoJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void allCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allCheckBoxMenuItemActionPerformed
        updateAnnotationPreferences();
    }//GEN-LAST:event_allCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void automaticAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_automaticAnnotationCheckBoxMenuItemActionPerformed
        if (automaticAnnotationCheckBoxMenuItem.isSelected()) {
            adaptCheckBoxMenuItem.setSelected(true);
            try {
                annotationPreferences.resetAutomaticAnnotation();
            } catch (Exception e) {
                deNovoGUI.catchException(e);
            }

            for (int availableCharge : chargeMenus.keySet()) {
                chargeMenus.get(availableCharge).setSelected(annotationPreferences.getValidatedCharges().contains(availableCharge));
            }
        }

        updateAnnotationPreferences();
    }//GEN-LAST:event_automaticAnnotationCheckBoxMenuItemActionPerformed

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
        new ExportGraphicsDialog(deNovoGUI, this, true, (Component) spectrumJPanel);
    }//GEN-LAST:event_exportSpectrumGraphicsJMenuItemActionPerformed

    /**
     * Export spectrum as MGF.
     *
     * @param evt
     */
    private void exportSpectrumValuesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumValuesJMenuItemActionPerformed

        String spectrumAsMgf = getSpectrumAsMgf();

        if (spectrumAsMgf != null) {

            File selectedFile = Util.getUserSelectedFile(this, ".mgf", "(Mascot Generic Format) *.mgf", deNovoGUI.getLastSelectedFolder(), "Save As...", false);

            if (selectedFile != null) {

                deNovoGUI.setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());

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
                    JOptionPane.showMessageDialog(this, "An error occured while saving " + selectedFile.getPath() + ".\n"
                            + "See resources/PeptideShaker.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
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
                "Spectrum - Help");
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Update the spectrum data displayed.
     *
     * @param evt
     */
    private void spectrumFileComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumFileComboBoxActionPerformed
        displayResults(0, 0);
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
        new BugReport(this, deNovoGUI.getLastSelectedFolder(), "DeNovoGUI", "denovogui", deNovoGUI.getVersion(), new File(deNovoGUI.getJarFilePath() + "/resources/DeNovoGUI.log"));
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
     * Export the matches to file.
     *
     * @param evt
     */
    private void exportMatchesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMatchesMenuItemActionPerformed
        File selectedFile = Util.getUserSelectedFile(this, ".txt", "Text file (.txt)", "Select File", deNovoGUI.getLastSelectedFolder(), false);
        if (selectedFile != null) {
            deNovoGUI.setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
            exportIdentification(selectedFile, false);
        }
    }//GEN-LAST:event_exportMatchesMenuItemActionPerformed

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
        int selectedRow = deNovoPeptidesTable.getSelectedRow();
        ((SpectrumMatchTableModel) deNovoPeptidesTable.getModel()).setExcludeAllFixedPtms(!fixedPtmsCheckBoxMenuItem.isSelected());
        ((SpectrumMatchTableModel) deNovoPeptidesTable.getModel()).fireTableDataChanged();
        if (selectedRow != -1) {
            deNovoPeptidesTable.setRowSelectionInterval(selectedRow, selectedRow);
            updateSpectrum();
        }
    }//GEN-LAST:event_fixedPtmsCheckBoxMenuItemActionPerformed

    private void exportBlastMatchesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportBlastMatchesMenuItemActionPerformed
        File selectedFile = Util.getUserSelectedFile(this, ".txt", "Text file (.txt)", "Select File", deNovoGUI.getLastSelectedFolder(), false);
        if (selectedFile != null) {
            deNovoGUI.setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
            exportIdentification(selectedFile, true);
        }
    }//GEN-LAST:event_exportBlastMatchesMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem aIonCheckBoxMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JCheckBoxMenuItem adaptCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem allCheckBoxMenuItem;
    private javax.swing.JMenuItem annotationColorsJMenuItem;
    private javax.swing.JMenuBar annotationMenuBar;
    private javax.swing.JCheckBoxMenuItem automaticAnnotationCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JPanel bcakgroundPanel;
    private javax.swing.JMenuItem bugReportMenu;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.ButtonGroup deNovoChargeButtonGroup;
    private javax.swing.JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem;
    private javax.swing.JMenu deNovoMenu;
    private javax.swing.JPanel deNovoPeptidesPanel;
    private javax.swing.JTable deNovoPeptidesTable;
    private javax.swing.JScrollPane deNovoPeptidesTableScrollPane;
    private javax.swing.JPanel debovoResultsPanel;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportBlastMatchesMenuItem;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenuItem exportMatchesMenuItem;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenuItem exportSpectrumGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumValuesJMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JCheckBoxMenuItem fixedPtmsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpMainMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JCheckBoxMenuItem immoniumIonsCheckMenu;
    private javax.swing.JMenu ionsMenu;
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
    private javax.swing.JCheckBoxMenuItem reporterIonsCheckMenu;
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
     * Shows a dialog allowing the selection of a new result file and displays
     * the results.
     */
    private void openNewFile() {
        final SelectResultsDialog selectResultsDialog = new SelectResultsDialog(this, deNovoGUI.getLastSelectedFolder());
        if (!selectResultsDialog.isCanceled() && selectResultsDialog.getMgfFile() != null && selectResultsDialog.getOutFile() != null && selectResultsDialog.getSearchParameters() != null) {
            deNovoGUI.setLastSelectedFolder(selectResultsDialog.getLastSelectedFolder());
            searchParameters = selectResultsDialog.getSearchParameters();
            setVisible(true);
            deNovoGUI.setVisible(false);
            openNewFile(selectResultsDialog.getOutFile(), selectResultsDialog.getMgfFile());
        } else {
            dispose();
        }
    }

    /**
     * Opens a new out file.
     *
     * @param outFile the .out file
     * @param spectrumFile the corresponding mgf file
     */
    private void openNewFile(File outFile, File spectrumFile) {
        final File finalOutFile = outFile;
        final File finalMgfFile = spectrumFile;
        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Spectra. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("importThread") {
            public void run() {
                try {
                    spectrumFactory.addSpectra(finalMgfFile, progressDialog);
                    String[] filesArray = {finalMgfFile.getName()};
                    spectrumFileComboBox.setModel(new DefaultComboBoxModel(filesArray));
                    progressDialog.setRunFinished();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while loading the spectra.", "Mgf File Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                    return;
                }
                progressDialog.setRunFinished();
                if (!progressDialog.isRunCanceled()) {
                    ArrayList<File> outFiles = new ArrayList<File>();
                    outFiles.add(finalOutFile);
                    try {
                        displayResults(outFiles);
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while importing the results.", "Out File Error", JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Displays new results.
     *
     * @param peptideRowSelection the peptide row to select
     * @param psmRowSelection the PSM row to select
     */
    private void displayResults(final int peptideRowSelection, final int psmRowSelection) {

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
            public void run() {
                orderedSpectrumTitles = null;
                try {
                    orderedSpectrumTitles = orderTitlesByScore(progressDialog);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while sorting the results.", "Out File Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                progressDialog.setTitle("Updating Display. Please Wait...");
                TableModel tableModel = new SpectrumTableModel(getSelectedSpectrumFile(), identification, orderedSpectrumTitles);
                querySpectraTable.setModel(tableModel);

                if (querySpectraTable.getRowCount() > 0) {
                    if (peptideRowSelection != -1 && peptideRowSelection < querySpectraTable.getRowCount()) {
                        querySpectraTable.setRowSelectionInterval(peptideRowSelection, peptideRowSelection);
                    } else {
                        querySpectraTable.setRowSelectionInterval(0, 0);
                    }
                }

                if (identification.getSpectrumIdentification(getSelectedSpectrumFile()) == null) {
                    ((TitledBorder) querySpectraPanel.getBorder()).setTitle("Query Spectra (?/"
                            + querySpectraTable.getRowCount() + ")");
                } else {
                    ((TitledBorder) querySpectraPanel.getBorder()).setTitle("Query Spectra ("
                            + identification.getSpectrumIdentification(getSelectedSpectrumFile()).size() + "/"
                            + querySpectraTable.getRowCount() + ")");
                }

                querySpectraPanel.repaint();

                updateAssumptionsTable(psmRowSelection);
                progressDialog.setRunFinished();
                findPanel.setEnabled(true);

                // change the icon to the normal version (should not be needed, but added as an extra safty)
                setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png")));

                // the spectrum file and the results file do not match...
                if (identification.getSpectrumIdentification(getSelectedSpectrumFile()) == null) {
                    JOptionPane.showMessageDialog(ResultsFrame.this, "No identifications for the selected spectrum file."
                            + "\nPlease check that you loaded the correct files.", "File Errors", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.start();
    }

    /**
     * Returns a list of the spectrum titles of the selected mgf file ordered by
     * max PepNovo score.
     *
     * @return a list of the spectrum titles of the selected mgf file ordered by
     * max PepMovo score
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private ArrayList<String> orderTitlesByScore(ProgressDialogX progressDialog) throws SQLException, IOException, ClassNotFoundException {

        String spectrumFile = getSelectedSpectrumFile();
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.resetPrimaryProgressCounter();
        progressDialog.setMaxPrimaryProgressCounter(spectrumFactory.getNSpectra(spectrumFile));
        HashMap<Double, ArrayList<String>> titlesMap = new HashMap<Double, ArrayList<String>>();
        ArrayList<String> noId = new ArrayList<String>();

        for (String spectrumTitle : spectrumFactory.getSpectrumTitles(spectrumFile)) {
            String spectrumKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
            if (identification.matchExists(spectrumKey)) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                double maxScore = 0;
                for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions()) {
                    if (peptideAssumption.getScore() > maxScore) {
                        maxScore = peptideAssumption.getScore();
                    }
                }
                if (!titlesMap.containsKey(maxScore)) {
                    titlesMap.put(maxScore, new ArrayList<String>());
                }
                titlesMap.get(maxScore).add(spectrumTitle);
            } else {
                noId.add(spectrumTitle);
            }
            progressDialog.increasePrimaryProgressCounter();
        }

        ArrayList<Double> scores = new ArrayList<Double>(titlesMap.keySet());
        Collections.sort(scores, Collections.reverseOrder());
        ArrayList<String> orderedTitles = new ArrayList<String>();

        for (double score : scores) {
            orderedTitles.addAll(titlesMap.get(score));
        }

        orderedTitles.addAll(noId);
        return orderedTitles;
    }

    /**
     * Exports the identification results in a text file.
     *
     * @param file the destination file
     * @param blast BLAST-compatible export flag
     */
    public void exportIdentification(File file, final boolean blast) {

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
                    if (blast) {
                        TextExporter.exportBlastPSMs(finalFile, identification, searchParameters, progressDialog);
                    } else {
                        TextExporter.exportPSMs(finalFile, identification, searchParameters, progressDialog);
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
            return spectrumFactory.getSpectrumTitles(getSelectedSpectrumFile()).get(modelRow);
        }
    }

    /**
     * Updates the assumption table based on the selected line.
     *
     * @param selectedPsmRow the selected PSM row
     */
    public void updateAssumptionsTable(int selectedPsmRow) {

        try {
            assumptions = new ArrayList<PeptideAssumption>();
            if (querySpectraTable.getRowCount() > 0) {
                String psmKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

                if (identification.matchExists(psmKey)) {
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                    HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);
                    if (assumptionsMap != null) {
                        ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                        Collections.sort(scores, Collections.reverseOrder());
                        for (Double score : scores) {
                            assumptions.addAll(assumptionsMap.get(score));
                        }
                    }
                }

                TableModel tableModel = new SpectrumMatchTableModel(assumptions, searchParameters.getModificationProfile(), !fixedPtmsCheckBoxMenuItem.isSelected());
                deNovoPeptidesTable.setModel(tableModel);

                ((DefaultTableModel) deNovoPeptidesTable.getModel()).fireTableDataChanged();
                setTableProperties();

                if (deNovoPeptidesTable.getRowCount() > 0) {
                    if (selectedPsmRow != -1 && selectedPsmRow < deNovoPeptidesTable.getRowCount()) {
                        deNovoPeptidesTable.setRowSelectionInterval(selectedPsmRow, selectedPsmRow);
                        deNovoPeptidesTable.scrollRectToVisible(deNovoPeptidesTable.getCellRect(selectedPsmRow, 0, false));
                    } else {
                        deNovoPeptidesTable.setRowSelectionInterval(0, 0);
                    }
                }

                ((TitledBorder) deNovoPeptidesPanel.getBorder()).setTitle("De Novo Peptides (" + deNovoPeptidesTable.getRowCount() + ")");
                deNovoPeptidesPanel.repaint();

                updateSpectrum();
            }

        } catch (Exception e) {
            deNovoGUI.catchException(e);
        }
    }

    /**
     * Update the spectrum and annotations.
     */
    private void updateSpectrum() {

        spectrumJPanel.removeAll();
        String spectrumKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

        ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle("Spectrum Viewer");
        spectrumViewerPanel.repaint();

        if (spectrumFactory.spectrumLoaded(spectrumKey)) {
            try {
                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                // add the data to the spectrum panel
                Precursor precursor = currentSpectrum.getPrecursor();
                if (deNovoPeptidesTable.getSelectedRow() != -1) {
                    PeptideAssumption peptideAssumption = assumptions.get(deNovoPeptidesTable.convertRowIndexToModel(deNovoPeptidesTable.getSelectedRow()));

                    SpectrumPanel spectrumPanel = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), peptideAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrumPanel.setBorder(null);

                    Peptide currentPeptide = peptideAssumption.getPeptide();

                    // add the annotations
                    annotationPreferences.setCurrentSettings(
                            currentPeptide, peptideAssumption.getIdentificationCharge().value,
                            !currentSpectrumKey.equalsIgnoreCase(spectrumKey));

                    annotationPreferences.setFragmentIonAccuracy(deNovoGUI.getSearchParameters().getFragmentIonAccuracy());

                    SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();

                    // get the terminal gaps
                    PeptideAssumptionDetails peptideAssumptionDetails = new PeptideAssumptionDetails();
                    peptideAssumptionDetails = (PeptideAssumptionDetails) peptideAssumption.getUrParam(peptideAssumptionDetails);

                    double nTermGap = peptideAssumptionDetails.getNTermGap();
                    double cTermGap = peptideAssumptionDetails.getCTermGap();
                    if (nTermGap > 0) {
                        //nTermGap -= 1.0079; // @TODO: should there be anything here??
                    }
                    if (cTermGap > 0) {
                        double temp = Atom.O.mass + Atom.H.mass + ElementaryIon.proton.getTheoreticMass() * 2; // @TODO: figure out why this is the correct number...
                        cTermGap -= temp;
                    }
                    spectrumAnnotator.setTerminalMassShifts(nTermGap, cTermGap);

                    ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            peptideAssumption.getIdentificationCharge().value,
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(0.0), //annotationPreferences.getAnnotationIntensityLimit() // @TODO: set from the GUI
                            annotationPreferences.getFragmentIonAccuracy(),
                            false);
                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                    spectrumPanel.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());

                    if (!currentSpectrumKey.equalsIgnoreCase(spectrumKey)) {
                        if (annotationPreferences.useAutomaticAnnotation()) {
                            annotationPreferences.setNeutralLossesSequenceDependant(true);
                        }
                    }

                    // add de novo sequencing
                    spectrumPanel.addAutomaticDeNovoSequencing(currentPeptide, annotations,
                            PeptideFragmentIon.B_ION, // @TODO: choose the fragment ion types from the annotation menu bar?
                            PeptideFragmentIon.Y_ION,
                            annotationPreferences.getDeNovoCharge(),
                            annotationPreferences.showForwardIonDeNovoTags(),
                            annotationPreferences.showRewindIonDeNovoTags(),
                            0.75, 1.0, !fixedPtmsCheckBoxMenuItem.isSelected());

                    // show all or just the annotated peaks
                    spectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                    spectrumJPanel.add(spectrumPanel);
                    updateAnnotationMenus(peptideAssumption.getIdentificationCharge().value, currentPeptide);

                    String modifiedSequence = currentPeptide.getTaggedModifiedSequence(deNovoGUI.getSearchParameters().getModificationProfile(), false, false, true);

                    // replace the terminals if there are terminal gaps
                    if (peptideAssumptionDetails.getNTermGap() > 0) {
                        modifiedSequence = modifiedSequence.replaceAll("NH2-", "...");
                    }
                    if (peptideAssumptionDetails.getCTermGap() > 0) {
                        modifiedSequence = modifiedSequence.replaceAll("-COOH", "...");
                    }

                    ((TitledBorder) spectrumViewerPanel.getBorder()).setTitle(
                            "Spectrum Viewer (" + modifiedSequence
                            + "   " + peptideAssumption.getIdentificationCharge().toString() + "   "
                            + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 4) + " m/z)");
                    spectrumViewerPanel.repaint();
                } else {
                    // Show spectrum without identification.
                    SpectrumPanel spectrumPanel = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                            precursor.getMz(), "",
                            "", 40, false, false, false, 2, false);
                    spectrumPanel.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumJPanel.add(spectrumPanel);
                }
                currentSpectrumKey = spectrumKey;

            } catch (Exception e) {
                deNovoGUI.catchException(e);
            }
        }

        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();
    }

    /**
     * Loads the results of the given spectrum files and loads everything in the
     * identification.
     *
     * @param outFiles the out files
     */
    public void displayResults(ArrayList<File> outFiles) {

        final ArrayList<File> finalOutFiles = outFiles;

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
                    // import the PepNovo results
                    identification = importPepNovoResults(finalOutFiles, searchParameters, progressDialog);
                    progressDialog.setRunFinished();
                    if (identification != null) {
                        displayResults(0, 0);
                    }
                } catch (Exception e) {
                    deNovoGUI.catchException(e);
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Update the annotation menu bar with the current annotation preferences.
     *
     * @param precursorCharge
     * @param peptide
     */
    public void updateAnnotationMenus(int precursorCharge, Peptide peptide) {

        aIonCheckBoxMenuItem.setSelected(false);
        bIonCheckBoxMenuItem.setSelected(false);
        cIonCheckBoxMenuItem.setSelected(false);
        xIonCheckBoxMenuItem.setSelected(false);
        yIonCheckBoxMenuItem.setSelected(false);
        zIonCheckBoxMenuItem.setSelected(false);
        precursorCheckMenu.setSelected(false);
        immoniumIonsCheckMenu.setSelected(false);
        reporterIonsCheckMenu.setSelected(false);

        for (Ion.IonType ionType : annotationPreferences.getIonTypes().keySet()) {
            if (ionType == Ion.IonType.IMMONIUM_ION) {
                immoniumIonsCheckMenu.setSelected(true);
            } else if (ionType == Ion.IonType.PRECURSOR_ION) {
                precursorCheckMenu.setSelected(true);
            } else if (ionType == Ion.IonType.REPORTER_ION) {
                reporterIonsCheckMenu.setSelected(true);
            } else if (ionType == Ion.IonType.PEPTIDE_FRAGMENT_ION) {
                for (int subtype : annotationPreferences.getIonTypes().get(ionType)) {
                    if (subtype == PeptideFragmentIon.A_ION) {
                        aIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.B_ION) {
                        bIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.C_ION) {
                        cIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.X_ION) {
                        xIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.Y_ION) {
                        yIonCheckBoxMenuItem.setSelected(true);
                    } else if (subtype == PeptideFragmentIon.Z_ION) {
                        zIonCheckBoxMenuItem.setSelected(true);
                    }
                }
            }
        }

        boolean selected;

        ArrayList<String> selectedLosses = new ArrayList<String>();

        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {

            if (lossMenuItem.isSelected()) {
                selectedLosses.add(lossMenuItem.getText());
            }

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
        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            PTM ptm = ptmFactory.getPTM(modMatch.getTheoreticPtm());
            for (NeutralLoss neutralLoss : ptm.getNeutralLosses()) {
                neutralLosses.put(neutralLoss.name, neutralLoss);
            }
        }

        ArrayList<String> names = new ArrayList<String>(neutralLosses.keySet());
        Collections.sort(names);

        ArrayList<String> finalSelectedLosses = selectedLosses;

        if (names.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {
            for (int i = 0; i < names.size(); i++) {

                if (annotationPreferences.areNeutralLossesSequenceDependant()) {
                    selected = false;
                    for (NeutralLoss neutralLoss : annotationPreferences.getNeutralLosses().getAccountedNeutralLosses()) {
                        if (neutralLoss.isSameAs(neutralLoss)) {
                            selected = true;
                            break;
                        }
                    }
                } else {
                    if (finalSelectedLosses.contains(names.get(i))) {
                        selected = true;
                    } else {
                        selected = false;
                    }
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(names.get(i));
                lossMenuItem.setSelected(selected);
                lossMenuItem.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        annotationPreferences.useAutomaticAnnotation(false);
                        annotationPreferences.setNeutralLossesSequenceDependant(false);
                        updateAnnotationPreferences();
                    }
                });
                lossMenus.put(neutralLosses.get(names.get(i)), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
        }

        ArrayList<String> selectedCharges = new ArrayList<String>();

        for (JCheckBoxMenuItem chargeMenuItem : chargeMenus.values()) {

            if (chargeMenuItem.isSelected()) {
                selectedCharges.add(chargeMenuItem.getText());
            }

            chargeMenu.remove(chargeMenuItem);
        }

        chargeMenus.clear();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        final ArrayList<String> finalSelectedCharges = selectedCharges;

        for (int charge = 1; charge < precursorCharge; charge++) {

            JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            if (annotationPreferences.useAutomaticAnnotation()) {
                chargeMenuItem.setSelected(annotationPreferences.getValidatedCharges().contains(charge));
            } else {
                if (finalSelectedCharges.contains(charge + "+")) {
                    chargeMenuItem.setSelected(true);
                } else {
                    chargeMenuItem.setSelected(false);
                }
            }

            chargeMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    annotationPreferences.useAutomaticAnnotation(false);
                    updateAnnotationPreferences();
                }
            });

            chargeMenus.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        automaticAnnotationCheckBoxMenuItem.setSelected(annotationPreferences.useAutomaticAnnotation());
        adaptCheckBoxMenuItem.setSelected(annotationPreferences.areNeutralLossesSequenceDependant());

        // disable/enable the neutral loss options
        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {
            lossMenuItem.setEnabled(!annotationPreferences.areNeutralLossesSequenceDependant());
        }

        allCheckBoxMenuItem.setSelected(annotationPreferences.showAllPeaks());
    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    public void updateAnnotationPreferences() {

        annotationPreferences.clearIonTypes();
        if (aIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
        }
        if (bIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
        }
        if (cIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
        }
        if (xIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
        }
        if (yIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
        }
        if (zIonCheckBoxMenuItem.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
        }
        if (precursorCheckMenu.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.PRECURSOR_ION);
        }
        if (immoniumIonsCheckMenu.isSelected()) {
            annotationPreferences.addIonType(Ion.IonType.IMMONIUM_ION);
        }
        if (reporterIonsCheckMenu.isSelected()) {
            for (int subtype : getReporterIons()) {
                annotationPreferences.addIonType(Ion.IonType.REPORTER_ION, subtype);
            }
        }

        annotationPreferences.clearNeutralLosses();

        for (NeutralLoss neutralLoss : lossMenus.keySet()) {
            if (lossMenus.get(neutralLoss).isSelected()) {
                annotationPreferences.addNeutralLoss(neutralLoss);
            }
        }

        annotationPreferences.clearCharges();

        for (int charge : chargeMenus.keySet()) {
            if (chargeMenus.get(charge).isSelected()) {
                annotationPreferences.addSelectedCharge(charge);
            }
        }

        annotationPreferences.useAutomaticAnnotation(automaticAnnotationCheckBoxMenuItem.isSelected());
        annotationPreferences.setNeutralLossesSequenceDependant(adaptCheckBoxMenuItem.isSelected());

        annotationPreferences.setShowAllPeaks(allCheckBoxMenuItem.isSelected());

        annotationPreferences.setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
        annotationPreferences.setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

        if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
            annotationPreferences.setDeNovoCharge(1);
        } else {
            annotationPreferences.setDeNovoCharge(2);
        }

        if (deNovoPeptidesTable.getSelectedRow() != -1) {
            updateSpectrum();
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
            PTM ptm = ptmFactory.getPTM(mod);
            for (ReporterIon reporterIon : ptm.getReporterIons()) {
                int subType = reporterIon.getSubType();
                if (!reporterIonsSubtypes.contains(subType)) {
                    reporterIonsSubtypes.add(subType);
                }
            }
        }

        return reporterIonsSubtypes;
    }

    @Override
    public void setSelectedExportFolder(String selectedFolder) {
        deNovoGUI.setLastSelectedFolder(selectedFolder);
    }

    @Override
    public String getDefaultExportFolder() {
        return deNovoGUI.getLastSelectedFolder();
    }

    @Override
    public Image getNormalIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui.png"));
    }

    @Override
    public Image getWaitingIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/denovogui_orange.png"));
    }

    /**
     * Returns the current spectrum as an MGF string.
     *
     * @return the current spectrum as an MGF string
     */
    public String getSpectrumAsMgf() {

        String spectrumKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

        if (spectrumFactory.spectrumLoaded(spectrumKey) && deNovoPeptidesTable.getSelectedRow() != -1) {

            StringBuilder spectraAsMgf = new StringBuilder();

            try {
                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                spectraAsMgf.append(currentSpectrum.asMgf());
                return spectraAsMgf.toString();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occured when reading the spectrum.", "File Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occured when reading the spectrum.", "File Error", JOptionPane.ERROR_MESSAGE);
            } catch (MzMLUnmarshallerException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occured when reading the spectrum.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        return null;
    }

    /**
     * Returns a String with the HTML tooltip for the peptide indicating the
     * modification details.
     *
     * @param peptide
     * @return a String with the HTML tooltip for the peptide
     */
    public String getPeptideModificationTooltipAsHtml(Peptide peptide) {

        // @TODO: should be merged with the same method on PeptideShaker - DisplayFeaturesGenerator -  and moved to utilities

        String tooltip = "<html>";
        ArrayList<String> alreadyAnnotated = new ArrayList<String>();

        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            String modName = modMatch.getTheoreticPtm();
            PTM ptm = ptmFactory.getPTM(modName);

            if ((ptm.getType() == PTM.MODAA && modMatch.isVariable()) || (!modMatch.isVariable()) && fixedPtmsCheckBoxMenuItem.isSelected()) {

                int modSite = modMatch.getModificationSite();

                if (modSite > 0) {
                    char affectedResidue = peptide.getSequence().charAt(modSite - 1);
                    Color ptmColor = deNovoGUI.getSearchParameters().getModificationProfile().getColor(modName);

                    if (!alreadyAnnotated.contains(modName + "_" + affectedResidue)) {
                        tooltip += "<span style=\"color:#" + Util.color2Hex(Color.WHITE) + ";background:#" + Util.color2Hex(ptmColor) + "\">"
                                + affectedResidue
                                + "</span>"
                                + ": " + modName + "<br>";

                        alreadyAnnotated.add(modName + "_" + affectedResidue);
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
     * Imports the PepNovo results from the given files and puts all matches in
     * the identification.
     *
     * @param outFiles the out files
     * @param searchParameters the search parameters
     * @param waitingHandler the waiting handler
     * @return the Identification object
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public Identification importPepNovoResults(ArrayList<File> outFiles, SearchParameters searchParameters, WaitingHandler waitingHandler)
            throws SQLException, FileNotFoundException, IOException, IllegalArgumentException, ClassNotFoundException, Exception {

        // @TODO: let the user reference his project

        String projectReference = "DenovoGUI";
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
        tempIdentification.setIsDB(true);

        // The cache used whenever the identification becomes too big
        String dbFolder = new File(deNovoGUI.getJarFilePath(), CACHE_DIRECTORY).getAbsolutePath();
        ObjectsCache objectsCache = new ObjectsCache();
        objectsCache.setAutomatedMemoryManagement(true);
        try {
            tempIdentification.establishConnection(dbFolder, true, objectsCache);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ResultsFrame.this, "An error occurred while creating the identification database. "
                    + "Please make sure that no other instance of DenovoGUI is running.", "Database Connection error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
            return null;
        }

        for (int i = 0; i < outFiles.size(); i++) {

            File outFile = outFiles.get(i);

            // initiate the parser
            String loadingText = "Loading Results. Loading File. Please Wait...";
            if (outFiles.size() > 1) {
                loadingText += " (" + (i + 1) + "/" + outFiles.size() + ")";
            }
            progressDialog.setTitle(loadingText);
            PepNovoIdfileReader idfileReader = new PepNovoIdfileReader(outFile, searchParameters, waitingHandler);

            // get spectrum matches
            loadingText = "Loading Results. Loading Matches. Please Wait...";
            if (outFiles.size() > 1) {
                loadingText += " (" + (i + 1) + "/" + outFiles.size() + ")";
            }
            progressDialog.setTitle(loadingText);
            HashSet<SpectrumMatch> spectrumMatches = idfileReader.getAllSpectrumMatches(waitingHandler);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

            // put the matches in the identification object
            tempIdentification.addSpectrumMatch(spectrumMatches);

            // get gui min/max values
            if (idfileReader.getMinRankScore() < minRankScore) {
                minRankScore = idfileReader.getMinRankScore();
            }
            if (idfileReader.getMaxRankScore() > maxRankScore) {
                maxRankScore = idfileReader.getMaxRankScore();
            }
            if (idfileReader.getMaxPepNovoScore() > maxPepnovoScore) {
                maxPepnovoScore = idfileReader.getMaxPepNovoScore();
            }
            if (idfileReader.getMaxNGap() > maxNGap) {
                maxNGap = idfileReader.getMaxNGap();
            }
            if (idfileReader.getMaxCGap() > maxCGap) {
                maxCGap = idfileReader.getMaxCGap();
            }
            if (idfileReader.getMaxMz() > maxIdentificationMz) {
                maxIdentificationMz = idfileReader.getMaxMz();
            }
            if (idfileReader.getMaxCharge() > maxIdentificationCharge) {
                maxIdentificationCharge = idfileReader.getMaxCharge();
            }

            idfileReader.close();
        }

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
                File matchFolder = new File(deNovoGUI.getJarFilePath(), CACHE_DIRECTORY);
                File[] tempFiles = matchFolder.listFiles();

                if (tempFiles != null) {
                    for (File currentFile : tempFiles) {
                        Util.deleteDir(currentFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ResultsFrame.this, "An error occured when closing the identification database.", "File Error", JOptionPane.ERROR_MESSAGE);
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
                updateAssumptionsTable(deNovoPeptidesTable.convertRowIndexToView(psmRow));
            }
        });
    }

    /**
     * Returns the modifications found in this project.
     *
     * @return the modifications found in this project
     */
    public ArrayList<String> getFoundModifications() {
        if (identifiedModifications == null) {
            identifiedModifications = new ArrayList<String>();

            if (identification != null) {
                for (String peptideKey : identification.getPeptideIdentification()) {

                    boolean modified = false;

                    for (String modificationName : Peptide.getModificationFamily(peptideKey)) {
                        if (!identifiedModifications.contains(modificationName)) {
                            identifiedModifications.add(modificationName);
                            modified = true;
                        }
                    }
                    if (!modified && !identifiedModifications.contains("no modification")) {
                        identifiedModifications.add("no modification");
                    }
                }
            }
        }
        return identifiedModifications;
    }
}
