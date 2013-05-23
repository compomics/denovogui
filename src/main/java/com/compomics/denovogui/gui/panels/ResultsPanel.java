package com.compomics.denovogui.gui.panels;

import com.compomics.denovogui.PepNovoIdfileReader;
import com.compomics.denovogui.gui.DeNovoGUI;
import com.compomics.denovogui.gui.tablemodels.SpectrumMatchTableModel;
import com.compomics.denovogui.gui.tablemodels.SpectrumTableModel;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.IonFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialog;
import com.compomics.util.gui.export_graphics.ExportGraphicsDialogParent;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.AnnotationPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 *
 * @author Thilo Muth
 * @author Harald Barsnes
 */
public class ResultsPanel extends javax.swing.JPanel implements ExportGraphicsDialogParent {

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
     * Creates a new ResultsPanel.
     *
     * @param deNovoGUI a references to the main frame
     */
    public ResultsPanel(DeNovoGUI deNovoGUI) {
        initComponents();
        this.deNovoGUI = deNovoGUI;
        setUpGUI();
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {

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
        querySpectraTableToolTips.add("Identified");

        deNovoPeptidesTableToolTips = new ArrayList<String>();
        deNovoPeptidesTableToolTips.add(null);
        deNovoPeptidesTableToolTips.add("Peptide Sequences");
        deNovoPeptidesTableToolTips.add("Rank Score");
        deNovoPeptidesTableToolTips.add("Score");
        deNovoPeptidesTableToolTips.add("N-Gap");
        deNovoPeptidesTableToolTips.add("C-Gap");
        deNovoPeptidesTableToolTips.add("m/z");
        deNovoPeptidesTableToolTips.add("Charge");
    }

    /**
     * Set the table properties.
     */
    private void setTableProperties() {
        PepNovoIdfileReader idfileReader = deNovoGUI.getIdfileReader();
        querySpectraTable.getColumn(" ").setMaxWidth(50);
        querySpectraTable.getColumn(" ").setMinWidth(50);
        querySpectraTable.getColumn("m/z").setMaxWidth(80);
        querySpectraTable.getColumn("m/z").setMinWidth(80);
        querySpectraTable.getColumn("Charge").setMaxWidth(80);
        querySpectraTable.getColumn("Charge").setMinWidth(80);
        querySpectraTable.getColumn("  ").setMaxWidth(30);
        querySpectraTable.getColumn("  ").setMinWidth(30);

        querySpectraTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
        querySpectraTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, (double) idfileReader.getMaxCharge(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth() - 30);
        querySpectraTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMaxMz(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) querySpectraTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());

        deNovoPeptidesTable.getColumn(" ").setMaxWidth(50);
        deNovoPeptidesTable.getColumn(" ").setMinWidth(50);

        deNovoPeptidesTable.getColumn("RankScore").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMinRankScore(), idfileReader.getMaxRankScore(), Color.BLUE, Color.RED));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("RankScore").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMaxPepNovoScore(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("N-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMaxNGap(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("N-Gap").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("C-Gap").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMaxCGap(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("C-Gap").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, idfileReader.getMaxMz(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth());
        deNovoPeptidesTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, (double) idfileReader.getMaxCharge(), deNovoGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) deNovoPeptidesTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, deNovoGUI.getLabelWidth() - 30);
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
        exportAssumptionsMenu = new javax.swing.JMenu();
        exportSingleAssumptionsJMenuItem = new javax.swing.JMenuItem();
        exportAllAssumptionsJMenuItem = new javax.swing.JMenuItem();
        splitterMenu6 = new javax.swing.JMenu();
        helpJMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        splitterMenu7 = new javax.swing.JMenu();
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

        annotationMenuBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        annotationMenuBar.setOpaque(false);

        splitterMenu5.setText("|");
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
        annotationMenuBar.add(splitterMenu2);

        chargeMenu.setText("Charge");
        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        annotationMenuBar.add(splitterMenu3);

        deNovoMenu.setText("De Novo");

        forwardIonsDeNovoCheckBoxMenuItem.setSelected(true);
        forwardIonsDeNovoCheckBoxMenuItem.setText("f-ions");
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setSelected(true);
        rewindIonsDeNovoCheckBoxMenuItem.setText("r-ions");
        rewindIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rewindIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(rewindIonsDeNovoCheckBoxMenuItem);
        deNovoMenu.add(jSeparator19);

        deNovoChargeOneJRadioButtonMenuItem.setSelected(true);
        deNovoChargeOneJRadioButtonMenuItem.setText("Single Charge");
        deNovoChargeOneJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeOneJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeOneJRadioButtonMenuItem);

        deNovoChargeTwoJRadioButtonMenuItem.setText("Double Charge");
        deNovoChargeTwoJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deNovoChargeTwoJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(deNovoChargeTwoJRadioButtonMenuItem);

        annotationMenuBar.add(deNovoMenu);

        splitterMenu9.setText("|");
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

        exportAssumptionsMenu.setText("Assumptions");

        exportSingleAssumptionsJMenuItem.setText("Single");
        exportSingleAssumptionsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSingleAssumptionsJMenuItemActionPerformed(evt);
            }
        });
        exportAssumptionsMenu.add(exportSingleAssumptionsJMenuItem);

        exportAllAssumptionsJMenuItem.setText("All");
        exportAllAssumptionsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAllAssumptionsJMenuItemActionPerformed(evt);
            }
        });
        exportAssumptionsMenu.add(exportAllAssumptionsJMenuItem);

        exportGraphicsMenu.add(exportAssumptionsMenu);

        annotationMenuBar.add(exportGraphicsMenu);

        splitterMenu6.setText("|");
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
        annotationMenuBar.add(splitterMenu7);

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
                    .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 935, Short.MAX_VALUE)
                    .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        spectrumViewerPanelLayout.setVerticalGroup(
            spectrumViewerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumViewerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
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

        javax.swing.GroupLayout querySpectraPanelLayout = new javax.swing.GroupLayout(querySpectraPanel);
        querySpectraPanel.setLayout(querySpectraPanelLayout);
        querySpectraPanelLayout.setHorizontalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(querySpectraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                .addContainerGap())
        );
        querySpectraPanelLayout.setVerticalGroup(
            querySpectraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, querySpectraPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(querySpectraTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
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
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                .addContainerGap())
        );
        deNovoPeptidesPanelLayout.setVerticalGroup(
            deNovoPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deNovoPeptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(deNovoPeptidesTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout debovoResultsPanelLayout = new javax.swing.GroupLayout(debovoResultsPanel);
        debovoResultsPanel.setLayout(debovoResultsPanelLayout);
        debovoResultsPanelLayout.setHorizontalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                        .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        debovoResultsPanelLayout.setVerticalGroup(
            debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debovoResultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumViewerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(debovoResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deNovoPeptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(querySpectraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(debovoResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_querySpectraTableMouseReleased
        updateAssumptionsTable();
    }//GEN-LAST:event_querySpectraTableMouseReleased

    /**
     * Update the assumptions table.
     *
     * @param evt
     */
    private void querySpectraTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_querySpectraTableKeyReleased
        updateAssumptionsTable();
    }//GEN-LAST:event_querySpectraTableKeyReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void deNovoPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deNovoPeptidesTableMouseReleased
        updateSpectrum();
    }//GEN-LAST:event_deNovoPeptidesTableMouseReleased

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
     * Opens the export graphics dialog.
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

            File selectedFile = Util.getUserSelectedFile(this, ".mgf", "(Mascot Generic Format) *.mgf", deNovoGUI.getOutputFolder().getAbsolutePath(), "Save As...", false);

            if (selectedFile != null) {
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
     * Export assumptions for a single spectrum.
     *
     * @param evt
     */
    private void exportSingleAssumptionsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSingleAssumptionsJMenuItemActionPerformed

        Identification identification = deNovoGUI.getIdentification();
        String psmKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());
        if (identification.matchExists(psmKey)) {

            File selectedFile = Util.getUserSelectedFile(this, ".csv", "(Comma-Separated Values) *.csv", "Save As...", deNovoGUI.getOutputFolder().getAbsolutePath(), false);

            if (selectedFile != null) {
                deNovoGUI.setLastSelectedFolder(selectedFile.getParent());
            }

            SpectrumMatch spectrumMatch = null;
            try {
                spectrumMatch = identification.getSpectrumMatch(psmKey);

                HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

                if (selectedFile != null) {

                    FileWriter w = new FileWriter(selectedFile);
                    BufferedWriter bw = new BufferedWriter(w);

                    // Write spectrum title.
                    bw.write(">" + getSelectedSpectrumTitle());
                    bw.newLine();

                    ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                    Collections.sort(scores, Collections.reverseOrder());
                    for (int i = 0; i < scores.size(); i++) {
                        for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                            bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                            bw.newLine();                          
                        }
                    }
                    bw.close();
                    w.close();
                    JOptionPane.showMessageDialog(this, "Assumptions saved to " + selectedFile.getPath() + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occured while saving " + selectedFile.getPath() + ".\n"
                        + "See resources/DeNovoGUI.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                deNovoGUI.catchException(ex);
            }

        }

    }//GEN-LAST:event_exportSingleAssumptionsJMenuItemActionPerformed

    /**
     * Export assumptions for all spectra.
     *
     * @param evt
     */
    private void exportAllAssumptionsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAllAssumptionsJMenuItemActionPerformed

        File selectedFile = Util.getUserSelectedFile(this, ".csv", "(Comma-Separated Values) *.csv", "Save As...", deNovoGUI.getLastSelectedFolder(), false);

        if (selectedFile != null) {
            deNovoGUI.setLastSelectedFolder(selectedFile.getParent());
        }
        try {
            FileWriter w = new FileWriter(selectedFile);
            BufferedWriter bw = new BufferedWriter(w);

            Identification identification = deNovoGUI.getIdentification();
            // Iterate the spectrum files.
            for (String fileName : spectrumFactory.getMgfFileNames()) {
                // Iterate the spectrum titles.
                for (String spectrumTitle : spectrumFactory.getSpectrumTitles(fileName)) {
                    String psmKey = Spectrum.getSpectrumKey(fileName, spectrumTitle);

                    if (identification.matchExists(psmKey)) {
                        SpectrumMatch spectrumMatch = null;

                        spectrumMatch = identification.getSpectrumMatch(psmKey);

                        HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);

                        if (selectedFile != null) {
                            // Write spectrum title.
                            bw.write(">" + spectrumTitle);
                            bw.newLine();

                            ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                            Collections.sort(scores, Collections.reverseOrder());
                            for (int i = 0; i < scores.size(); i++) {
                                for (PeptideAssumption assumption : assumptionsMap.get(scores.get(i))) {
                                    bw.write(assumption.getPeptide().getSequence() + "\t" + assumption.getScore());
                                    bw.newLine();
                                }
                            }                            
                        }
                    }
                }
            }
            bw.close();
            w.close();
            JOptionPane.showMessageDialog(this, "Assumptions saved to " + selectedFile.getPath() + ".",
                    "File Saved", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while saving " + selectedFile.getPath() + ".\n"
                    + "See resources/DeNovoGUI.log for details.", "Save Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            deNovoGUI.catchException(ex);
        }
    }//GEN-LAST:event_exportAllAssumptionsJMenuItemActionPerformed

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        new HelpDialog(deNovoGUI, getClass().getResource("/html/SpectrumPanel.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                "Spectrum - Help", 500, 0);
    }//GEN-LAST:event_helpMenuItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem aIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem adaptCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem allCheckBoxMenuItem;
    private javax.swing.JMenuItem annotationColorsJMenuItem;
    private javax.swing.JMenuBar annotationMenuBar;
    private javax.swing.JCheckBoxMenuItem automaticAnnotationCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem;
    private javax.swing.JMenu deNovoMenu;
    private javax.swing.JPanel deNovoPeptidesPanel;
    private javax.swing.JTable deNovoPeptidesTable;
    private javax.swing.JScrollPane deNovoPeptidesTableScrollPane;
    private javax.swing.JPanel debovoResultsPanel;
    private javax.swing.JMenuItem exportAllAssumptionsJMenuItem;
    private javax.swing.JMenu exportAssumptionsMenu;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenuItem exportSingleAssumptionsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumGraphicsJMenuItem;
    private javax.swing.JMenuItem exportSpectrumValuesJMenuItem;
    private javax.swing.JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu helpJMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JCheckBoxMenuItem immoniumIonsCheckMenu;
    private javax.swing.JMenu ionsMenu;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JMenu lossMenu;
    private javax.swing.JMenu lossSplitter;
    private javax.swing.JMenu otherMenu;
    private javax.swing.JCheckBoxMenuItem precursorCheckMenu;
    private javax.swing.JPanel querySpectraPanel;
    private javax.swing.JTable querySpectraTable;
    private javax.swing.JScrollPane querySpectraTableScrollPane;
    private javax.swing.JCheckBoxMenuItem reporterIonsCheckMenu;
    private javax.swing.JCheckBoxMenuItem rewindIonsDeNovoCheckBoxMenuItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
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
    private javax.swing.JCheckBoxMenuItem xIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem yIonCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem zIonCheckBoxMenuItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays new results.
     */
    public void diplayResults() {
        TableModel tableModel = new SpectrumTableModel(getSelectedSpectrumFile(), deNovoGUI.getIdentification());
        querySpectraTable.setModel(tableModel);
        querySpectraTable.setRowSelectionInterval(0, 0);
        updateAssumptionsTable();
    }

    /**
     * Returns the name of the spectrum file displayed.
     *
     * @return the name of the spectrum file displayed
     */
    public String getSelectedSpectrumFile() {
        return spectrumFactory.getMgfFileNames().get(0); //@TODO: allow the user to chose the file
    }

    /**
     * Returns the title of the selected spectrum.
     *
     * @return the title of the selected spectrum
     */
    public String getSelectedSpectrumTitle() {
        int selectedRow = querySpectraTable.getSelectedRow();
        int modelRow = querySpectraTable.convertRowIndexToModel(selectedRow);
        return spectrumFactory.getSpectrumTitles(getSelectedSpectrumFile()).get(modelRow);
    }

    /**
     * Updates the assumption table based on the selected line.
     */
    public void updateAssumptionsTable() {

        try {
            assumptions = new ArrayList<PeptideAssumption>();
            Identification identification = deNovoGUI.getIdentification();
            String psmKey = Spectrum.getSpectrumKey(getSelectedSpectrumFile(), getSelectedSpectrumTitle());

            if (identification.matchExists(psmKey)) {
                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);
                HashMap<Double, ArrayList<PeptideAssumption>> assumptionsMap = spectrumMatch.getAllAssumptions(SearchEngine.PEPNOVO);
                ArrayList<Double> scores = new ArrayList<Double>(assumptionsMap.keySet());
                Collections.sort(scores, Collections.reverseOrder());
                for (Double score : scores) {
                    assumptions.addAll(assumptionsMap.get(score));
                }
            }
            TableModel tableModel = new SpectrumMatchTableModel(assumptions);
            deNovoPeptidesTable.setModel(tableModel);

            ((DefaultTableModel) deNovoPeptidesTable.getModel()).fireTableDataChanged();
            setTableProperties();

            if (deNovoPeptidesTable.getRowCount() > 0) {
                deNovoPeptidesTable.setRowSelectionInterval(0, 0);
            }

            updateSpectrum();

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

        if (spectrumFactory.spectrumLoaded(spectrumKey) && deNovoPeptidesTable.getSelectedRow() != -1) {

            try {
                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);

                // add the data to the spectrum panel
                Precursor precursor = currentSpectrum.getPrecursor();
                PeptideAssumption peptideAssumption = assumptions.get(deNovoPeptidesTable.getSelectedRow());

                SpectrumPanel spectrumPanel = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), peptideAssumption.getIdentificationCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrumPanel.setBorder(null);

                Peptide currentPeptide = peptideAssumption.getPeptide();

                // add the annotations
                SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
                annotationPreferences.setCurrentSettings(
                        currentPeptide, peptideAssumption.getIdentificationCharge().value,
                        !currentSpectrumKey.equalsIgnoreCase(spectrumKey));

                annotationPreferences.setFragmentIonAccuracy(deNovoGUI.getSearchParameters().getFragmentIonAccuracy());

                ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                        annotationPreferences.getNeutralLosses(),
                        annotationPreferences.getValidatedCharges(),
                        peptideAssumption.getIdentificationCharge().value,
                        currentSpectrum, currentPeptide,
                        currentSpectrum.getIntensityLimit(0.0), //annotationPreferences.getAnnotationIntensityLimit() // @TODO: set from the GUI
                        annotationPreferences.getFragmentIonAccuracy(), false);
                spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                // add de novo sequencing
                spectrumPanel.addAutomaticDeNovoSequencing(currentPeptide, annotations,
                        PeptideFragmentIon.B_ION, // @TODO: choose the reverse fragment ion type from the annotation menu bar?
                        PeptideFragmentIon.Y_ION,
                        annotationPreferences.getDeNovoCharge(),
                        annotationPreferences.showForwardIonDeNovoTags(),
                        annotationPreferences.showRewindIonDeNovoTags());

                // show all or just the annotated peaks
                spectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                spectrumJPanel.add(spectrumPanel);
                updateAnnotationMenus(peptideAssumption.getIdentificationCharge().value, currentPeptide);
                currentSpectrumKey = spectrumKey;

            } catch (Exception e) {
                deNovoGUI.catchException(e);
            }
        }

        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();
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

        ArrayList<String> modifications = new ArrayList<String>(); //getFoundModifications(); // @TODO: re-add ptms
        ArrayList<Integer> reporterIonsSubtypes = new ArrayList<Integer>();

//        for (String mod : modifications) {
//            PTM ptm = ptmFactory.getPTM(mod);
//            for (ReporterIon reporterIon : ptm.getReporterIons()) {
//                int subType = reporterIon.getSubType();
//                if (!reporterIonsSubtypes.contains(subType)) {
//                    reporterIonsSubtypes.add(subType);
//                }
//            }
//        }

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
}
