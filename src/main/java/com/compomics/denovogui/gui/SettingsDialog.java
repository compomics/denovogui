package com.compomics.denovogui.gui;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.DirecTagParameters;
import com.compomics.util.experiment.identification.identification_parameters.MsgfParameters;
import com.compomics.util.experiment.identification.identification_parameters.OmssaParameters;
import com.compomics.util.experiment.identification.identification_parameters.PepnovoParameters;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.ptm.ModificationsDialog;
import com.compomics.util.gui.ptm.PtmDialogParent;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Dialog for displaying and editing the search settings.
 *
 * @author Harald Barsnes
 */
public class SettingsDialog extends javax.swing.JDialog implements PtmDialogParent {

    /**
     * A references to the main DeNovoGUI.
     */
    private DeNovoGUI deNovoGUI;
    /**
     * The factory used to handle the modifications.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The search parameters.
     */
    private SearchParameters searchParameters;
    /**
     * The parameter file.
     */
    private File parametersFile = null;
    /**
     * The modification table column header tooltips.
     */
    private ArrayList<String> modificationTableToolTips;

    /**
     * Creates a new SettingsDialog.
     *
     * @param deNovoGUI references to the main DeNovoGUI
     * @param searchParameters
     * @param setVisible
     * @param modal
     */
    public SettingsDialog(DeNovoGUI deNovoGUI, SearchParameters searchParameters, boolean setVisible, boolean modal) {
        super(deNovoGUI, modal);
        this.deNovoGUI = deNovoGUI;
        this.searchParameters = searchParameters;
        initComponents();
        setUpGUI();
        insertData();
        setLocationRelativeTo(deNovoGUI);

        if (searchParameters.getParametersFile() != null) {
            setTitle("De Novo Settings - " + searchParameters.getParametersFile().getName());
        }

        if (setVisible) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setVisible(true);
                    validateParametersInput(true);
                }
            });
        }
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {

        modificationTypesSplitPane.setDividerLocation(0.5);

        // centrally align the comboboxes
        modificationsListCombo.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));
        duplicateSpectraPerChargeCmb.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));
        adjustPrecursorCmb.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));
        useSpectrumChargeCmb.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));

        fixedModsJScrollPane.getViewport().setOpaque(false);
        variableModsJScrollPane.getViewport().setOpaque(false);
        modificationsJScrollPane.getViewport().setOpaque(false);

        fixedModsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());
        variableModsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());

        fixedModsTable.getColumn(" ").setMaxWidth(35);
        fixedModsTable.getColumn(" ").setMinWidth(35);
        variableModsTable.getColumn(" ").setMaxWidth(35);
        variableModsTable.getColumn(" ").setMinWidth(35);

        fixedModsTable.getColumn("Mass").setMaxWidth(100);
        fixedModsTable.getColumn("Mass").setMinWidth(100);
        variableModsTable.getColumn("Mass").setMaxWidth(100);
        variableModsTable.getColumn("Mass").setMinWidth(100);

        modificationTableToolTips = new ArrayList<String>();
        modificationTableToolTips.add(null);
        modificationTableToolTips.add("Modification Name");
        modificationTableToolTips.add("Modification Mass");
        modificationTableToolTips.add("Default Modification");

        setAllModificationTableProperties();

        updateModificationList();

        // make sure that the scroll panes are see-through
        modificationsJScrollPane.getViewport().setOpaque(false);

        modificationsTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Set the properties of the all modification table.
     */
    private void setAllModificationTableProperties() {
        modificationsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());
        modificationsTable.getColumn(" ").setMaxWidth(35);
        modificationsTable.getColumn(" ").setMinWidth(35);
        modificationsTable.getColumn("Mass").setMaxWidth(100);
        modificationsTable.getColumn("Mass").setMinWidth(100);

        if (modificationsListCombo.getSelectedIndex() == 1) {
            modificationsTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
            modificationsTable.getColumn("  ").setMaxWidth(30);
            modificationsTable.getColumn("  ").setMinWidth(30);
        }
    }

    /**
     * Insert the search parameters into the GUI.
     */
    private void insertData() {

        if (searchParameters.getParametersFile() != null) {
            parametersFile = searchParameters.getParametersFile();
        }

        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), new OmssaParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), new XtandemParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), new MsgfParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), new PepnovoParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), new DirecTagParameters());
        }

        PepnovoParameters pepNovoParameters = (PepnovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex());
        DirecTagParameters direcTagParameters = (DirecTagParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex());

        // the general parameters
        fragmentMassToleranceSpinner.setValue(searchParameters.getFragmentIonAccuracy());
        precursorMassToleranceSpinner.setValue(searchParameters.getPrecursorAccuracyDalton());
        numberOfThreadsSpinner.setModel(new javax.swing.SpinnerNumberModel(deNovoGUI.getDeNovoSequencingHandler().getNThreads(), 1, Runtime.getRuntime().availableProcessors(), 1));
        numberOfSolutionsSpinner.setValue(pepNovoParameters.getHitListLength());
        if (pepNovoParameters.isCorrectPrecursorMass()) {
            adjustPrecursorCmb.setSelectedIndex(1);
        } else {
            adjustPrecursorCmb.setSelectedIndex(0);
        }
        if (pepNovoParameters.isEstimateCharge()) {
            useSpectrumChargeCmb.setSelectedIndex(1);
        } else {
            useSpectrumChargeCmb.setSelectedIndex(0);
        }

        // DirecTag specific parameters
        tagLengthTextField.setText(String.valueOf(direcTagParameters.getTagLength()));
        numVariableModsTextField.setText(String.valueOf(direcTagParameters.getMaxDynamicMods()));
        numberOfChargeStatesTextField.setText(String.valueOf(direcTagParameters.getNumChargeStates()));
        if (direcTagParameters.isDuplicateSpectra()) {
            duplicateSpectraPerChargeCmb.setSelectedIndex(0);
        } else {
            duplicateSpectraPerChargeCmb.setSelectedIndex(1);
        }
        deisptopingModeTextField.setText(String.valueOf(direcTagParameters.getDeisotopingMode()));
        isotopeToleranceTextField.setText(String.valueOf(direcTagParameters.getIsotopeMzTolerance()));
        numberOfIntensityClassesTextField.setText(String.valueOf(direcTagParameters.getNumIntensityClasses()));
        outputSuffixTextField.setText(String.valueOf(direcTagParameters.getOutputSuffix()));
        maxPeakCountTextField.setText(String.valueOf(direcTagParameters.getMaxPeakCount()));
        ticCutoffTextField.setText(String.valueOf(direcTagParameters.getTicCutoffPercentage()));
        complementToleranceTextField.setText(String.valueOf(direcTagParameters.getComplementMzTolerance()));
        precursorAdjustmentStepTextField.setText(String.valueOf(direcTagParameters.getPrecursorAdjustmentStep()));
        minPrecursorAdjustmentTextField.setText(String.valueOf(direcTagParameters.getMinPrecursorAdjustment()));
        maxPrecursorAdjustmentTextField.setText(String.valueOf(direcTagParameters.getMaxPrecursorAdjustment()));
        intensityScoreWeightTextField.setText(String.valueOf(direcTagParameters.getIntensityScoreWeight()));
        mzFidelityScoreWeightTextField.setText(String.valueOf(direcTagParameters.getMzFidelityScoreWeight()));
        complementScoreWeightTextField.setText(String.valueOf(direcTagParameters.getComplementScoreWeight()));

        // add the modifications
        ArrayList<String> missingPtms = new ArrayList<String>();
        ModificationProfile modificationProfile = searchParameters.getModificationProfile();
        if (modificationProfile != null) {
            ArrayList<String> fixedMods = modificationProfile.getFixedModifications();

            for (String ptmName : fixedMods) {
                if (!ptmFactory.containsPTM(ptmName)) {
                    missingPtms.add(ptmName);
                }
            }

            for (String missing : missingPtms) {
                fixedMods.remove(missing);
            }

            if (!missingPtms.isEmpty()) {
                if (missingPtms.size() == 1) {
                    JOptionPane.showMessageDialog(this, "The following modification is currently not recognized by DeNovoGUI: "
                            + missingPtms.get(0) + ".\nPlease import it in the Modification Editor.", "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                } else {
                    String output = "The following modifications are currently not recognized by DeNovoGUI:\n";
                    boolean first = true;

                    for (String ptm : missingPtms) {
                        if (first) {
                            first = false;
                        } else {
                            output += ", ";
                        }
                        output += ptm;
                    }

                    output += ".\nPlease import them in the Modification Editor.";
                    JOptionPane.showMessageDialog(this, output, "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }

            DefaultTableModel fixedModel = (DefaultTableModel) fixedModsTable.getModel();
            fixedModel.getDataVector().removeAllElements();

            for (String fixedMod : fixedMods) {
                ((DefaultTableModel) fixedModsTable.getModel()).addRow(new Object[]{searchParameters.getModificationProfile().getColor(fixedMod), fixedMod, ptmFactory.getPTM(fixedMod).getMass()});
            }
            ((DefaultTableModel) fixedModsTable.getModel()).fireTableDataChanged();
            fixedModsTable.repaint();
            fixedModificationsLabel.setText("Fixed Modifications (" + fixedMods.size() + ")");

            ArrayList<String> variableMods = modificationProfile.getVariableModifications();

            for (String ptmName : variableMods) {
                if (!ptmFactory.containsPTM(ptmName)) {
                    missingPtms.add(ptmName);
                }
            }

            for (String missing : missingPtms) {
                variableMods.remove(missing);
            }

            if (!missingPtms.isEmpty()) {
                if (missingPtms.size() == 1) {
                    JOptionPane.showMessageDialog(this, "The following modification is currently not recognized by DeNovohGUI: "
                            + missingPtms.get(0) + ".\nPlease import it in the Modification Editor.", "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                } else {
                    String output = "The following modifications are currently not recognized by DeNovohGUI:\n";
                    boolean first = true;

                    for (String ptm : missingPtms) {
                        if (first) {
                            first = false;
                        } else {
                            output += ", ";
                        }
                        output += ptm;
                    }

                    output += ".\nPlease import them in the Modification Editor.";
                    JOptionPane.showMessageDialog(this, output, "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }
            DefaultTableModel variableModel = (DefaultTableModel) variableModsTable.getModel();
            variableModel.getDataVector().removeAllElements();
            for (String variableMod : variableMods) {
                ((DefaultTableModel) variableModsTable.getModel()).addRow(
                        new Object[]{searchParameters.getModificationProfile().getColor(variableMod), variableMod, ptmFactory.getPTM(variableMod).getMass()});
            }
            ((DefaultTableModel) variableModsTable.getModel()).fireTableDataChanged();
            variableModsTable.repaint();
            variableModificationsLabel.setText("Variable Modifications (" + variableMods.size() + ")");

            updateModificationList();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        modificationsPanel1 = new javax.swing.JPanel();
        modificationTypesSplitPane = new javax.swing.JSplitPane();
        jPanel8 = new javax.swing.JPanel();
        fixedModificationsLabel = new javax.swing.JLabel();
        addFixedModification = new javax.swing.JButton();
        removeFixedModification = new javax.swing.JButton();
        fixedModsJScrollPane = new javax.swing.JScrollPane();
        fixedModsTable = new javax.swing.JTable();
        jPanel9 = new javax.swing.JPanel();
        variableModificationsLabel = new javax.swing.JLabel();
        addVariableModification = new javax.swing.JButton();
        removeVariableModification = new javax.swing.JButton();
        variableModsJScrollPane = new javax.swing.JScrollPane();
        variableModsTable = new javax.swing.JTable();
        availableModsPanel = new javax.swing.JPanel();
        modificationsListCombo = new javax.swing.JComboBox();
        modificationsJScrollPane = new javax.swing.JScrollPane();
        modificationsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) modificationTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        openModificationSettingsJButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();
        directTagPanel = new javax.swing.JPanel();
        numberOfChargeStatesLabel = new javax.swing.JLabel();
        duplicateSpectraLabel = new javax.swing.JLabel();
        deisotopingModeLabel = new javax.swing.JLabel();
        isotopeToleranceLabel = new javax.swing.JLabel();
        complementToleranceLabel = new javax.swing.JLabel();
        tagLengthLabel = new javax.swing.JLabel();
        numVariableModsLabel = new javax.swing.JLabel();
        intensityScoreWeightLabel = new javax.swing.JLabel();
        mzFidelityScoreWeightLabel = new javax.swing.JLabel();
        complementScoreWeightLabel = new javax.swing.JLabel();
        numberOfChargeStatesTextField = new javax.swing.JTextField();
        ticCutoffLabel = new javax.swing.JLabel();
        maxPeakCountTextField = new javax.swing.JTextField();
        maxPeakCountLabel = new javax.swing.JLabel();
        numberOfIntensityClassesTextField = new javax.swing.JTextField();
        numberOfIntensityClassesLabel = new javax.swing.JLabel();
        minPrecursorAdjustmentLabel = new javax.swing.JLabel();
        minPrecursorAdjustmentTextField = new javax.swing.JTextField();
        maxPrecursorAdjustmentLabel = new javax.swing.JLabel();
        maxPrecursorAdjustmentTextField = new javax.swing.JTextField();
        precursorAdjustmentStepLabel = new javax.swing.JLabel();
        precursorAdjustmentStepTextField = new javax.swing.JTextField();
        outputSuffixLabel = new javax.swing.JLabel();
        outputSuffixTextField = new javax.swing.JTextField();
        duplicateSpectraPerChargeCmb = new javax.swing.JComboBox();
        deisptopingModeTextField = new javax.swing.JTextField();
        isotopeToleranceTextField = new javax.swing.JTextField();
        complementToleranceTextField = new javax.swing.JTextField();
        tagLengthTextField = new javax.swing.JTextField();
        numVariableModsTextField = new javax.swing.JTextField();
        intensityScoreWeightTextField = new javax.swing.JTextField();
        mzFidelityScoreWeightTextField = new javax.swing.JTextField();
        complementScoreWeightTextField = new javax.swing.JTextField();
        ticCutoffTextField = new javax.swing.JTextField();
        generalPanel = new javax.swing.JPanel();
        fragmentMassToleranceLabel = new javax.swing.JLabel();
        fragmentMassToleranceSpinner = new javax.swing.JSpinner();
        precursorMassToleranceLabel = new javax.swing.JLabel();
        precursorMassToleranceSpinner = new javax.swing.JSpinner();
        numberOfSolutionsLabel = new javax.swing.JLabel();
        numberOfSolutionsSpinner = new javax.swing.JSpinner();
        numberOfThreadsLabel = new javax.swing.JLabel();
        numberOfThreadsSpinner = new javax.swing.JSpinner();
        adjustPrecursorLabel = new javax.swing.JLabel();
        adjustPrecursorCmb = new javax.swing.JComboBox();
        useSpectrumChargeStageLabel = new javax.swing.JLabel();
        useSpectrumChargeCmb = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("De Novo Settings");
        setMinimumSize(new java.awt.Dimension(800, 600));
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        modificationsPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Modifications"));
        modificationsPanel1.setOpaque(false);

        modificationTypesSplitPane.setBorder(null);
        modificationTypesSplitPane.setDividerSize(0);
        modificationTypesSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        modificationTypesSplitPane.setResizeWeight(0.5);
        modificationTypesSplitPane.setOpaque(false);
        modificationTypesSplitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                modificationTypesSplitPaneComponentResized(evt);
            }
        });

        jPanel8.setOpaque(false);

        fixedModificationsLabel.setFont(fixedModificationsLabel.getFont().deriveFont((fixedModificationsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        fixedModificationsLabel.setText("Fixed Modifications");

        addFixedModification.setText("<<");
        addFixedModification.setToolTipText("Add as fixed modification");
        addFixedModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFixedModificationActionPerformed(evt);
            }
        });

        removeFixedModification.setText(">>");
        removeFixedModification.setToolTipText("Remove as fixed modification");
        removeFixedModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFixedModificationActionPerformed(evt);
            }
        });

        fixedModsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        fixedModsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Name", "Mass"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        fixedModsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                fixedModsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fixedModsTableMouseExited(evt);
            }
        });
        fixedModsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                fixedModsTableMouseMoved(evt);
            }
        });
        fixedModsJScrollPane.setViewportView(fixedModsTable);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(fixedModificationsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(242, 242, 242))
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(fixedModsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(7, 7, 7)))
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeFixedModification, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addFixedModification, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fixedModificationsLabel)
                .addGap(6, 6, 6)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel8Layout.createSequentialGroup()
                        .addComponent(addFixedModification)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeFixedModification)
                        .addContainerGap(78, Short.MAX_VALUE))
                    .addComponent(fixedModsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        modificationTypesSplitPane.setLeftComponent(jPanel8);

        jPanel9.setOpaque(false);

        variableModificationsLabel.setFont(variableModificationsLabel.getFont().deriveFont((variableModificationsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        variableModificationsLabel.setText("Variable Modifications");

        addVariableModification.setText("<<");
        addVariableModification.setToolTipText("Add as variable modification");
        addVariableModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addVariableModificationActionPerformed(evt);
            }
        });

        removeVariableModification.setText(">>");
        removeVariableModification.setToolTipText("Remove as variable modification");
        removeVariableModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeVariableModificationActionPerformed(evt);
            }
        });

        variableModsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        variableModsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Name", "Mass"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        variableModsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                variableModsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                variableModsTableMouseExited(evt);
            }
        });
        variableModsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                variableModsTableMouseMoved(evt);
            }
        });
        variableModsJScrollPane.setViewportView(variableModsTable);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(variableModificationsLabel)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(variableModsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(addVariableModification, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(removeVariableModification, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(variableModificationsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(addVariableModification)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeVariableModification)
                        .addContainerGap())
                    .addComponent(variableModsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        modificationTypesSplitPane.setRightComponent(jPanel9);

        availableModsPanel.setOpaque(false);

        modificationsListCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Most Used Modifications", "All Modifications" }));
        modificationsListCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modificationsListComboActionPerformed(evt);
            }
        });

        modificationsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        modificationsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Name", "Mass", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        modificationsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                modificationsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                modificationsTableMouseExited(evt);
            }
        });
        modificationsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                modificationsTableMouseMoved(evt);
            }
        });
        modificationsJScrollPane.setViewportView(modificationsTable);

        openModificationSettingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit_gray.png"))); // NOI18N
        openModificationSettingsJButton.setToolTipText("Edit Modifications");
        openModificationSettingsJButton.setBorder(null);
        openModificationSettingsJButton.setBorderPainted(false);
        openModificationSettingsJButton.setContentAreaFilled(false);
        openModificationSettingsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit.png"))); // NOI18N
        openModificationSettingsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                openModificationSettingsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                openModificationSettingsJButtonMouseExited(evt);
            }
        });
        openModificationSettingsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openModificationSettingsJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout availableModsPanelLayout = new javax.swing.GroupLayout(availableModsPanel);
        availableModsPanel.setLayout(availableModsPanelLayout);
        availableModsPanelLayout.setHorizontalGroup(
            availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(modificationsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(availableModsPanelLayout.createSequentialGroup()
                .addComponent(modificationsListCombo, 0, 435, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openModificationSettingsJButton)
                .addContainerGap())
        );
        availableModsPanelLayout.setVerticalGroup(
            availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(availableModsPanelLayout.createSequentialGroup()
                .addGroup(availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(modificationsListCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(openModificationSettingsJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modificationsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout modificationsPanel1Layout = new javax.swing.GroupLayout(modificationsPanel1);
        modificationsPanel1.setLayout(modificationsPanel1Layout);
        modificationsPanel1Layout.setHorizontalGroup(
            modificationsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationTypesSplitPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(availableModsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        modificationsPanel1Layout.setVerticalGroup(
            modificationsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanel1Layout.createSequentialGroup()
                .addGroup(modificationsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modificationTypesSplitPane)
                    .addComponent(availableModsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        openDialogHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        openDialogHelpJButton.setToolTipText("Help");
        openDialogHelpJButton.setBorder(null);
        openDialogHelpJButton.setBorderPainted(false);
        openDialogHelpJButton.setContentAreaFilled(false);
        openDialogHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseExited(evt);
            }
        });
        openDialogHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDialogHelpJButtonActionPerformed(evt);
            }
        });

        directTagPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("DirecTag Settings"));
        directTagPanel.setOpaque(false);

        numberOfChargeStatesLabel.setText("Number of Charge States");

        duplicateSpectraLabel.setText("Duplicate Spectra per Charge");

        deisotopingModeLabel.setText("Deisptoping Mode");

        isotopeToleranceLabel.setText("Isotope MZ Tolerance (Da)");

        complementToleranceLabel.setText("Complement MZ Tolerance (Da)");

        tagLengthLabel.setText("Tag Length");

        numVariableModsLabel.setText("Max Number of Variable PTMs");

        intensityScoreWeightLabel.setText("Intensity Score Weight");

        mzFidelityScoreWeightLabel.setText("MZ Fidelity Score Weight");

        complementScoreWeightLabel.setText("Complement Score Weight");

        numberOfChargeStatesTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        numberOfChargeStatesTextField.setText("3");
        numberOfChargeStatesTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                numberOfChargeStatesTextFieldKeyPressed(evt);
            }
        });

        ticCutoffLabel.setText("TIC Cutoff Percentage");

        maxPeakCountTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        maxPeakCountTextField.setText("400");
        maxPeakCountTextField.setEnabled(false);
        maxPeakCountTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                maxPeakCountTextFieldKeyReleased(evt);
            }
        });

        maxPeakCountLabel.setText("Max Peak Count");

        numberOfIntensityClassesTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        numberOfIntensityClassesTextField.setText("3");
        numberOfIntensityClassesTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                numberOfIntensityClassesTextFieldKeyReleased(evt);
            }
        });

        numberOfIntensityClassesLabel.setText("Number of Intensity Classes");

        minPrecursorAdjustmentLabel.setText("Min Precursor Adjustment");

        minPrecursorAdjustmentTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        minPrecursorAdjustmentTextField.setText("-2.5");
        minPrecursorAdjustmentTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                minPrecursorAdjustmentTextFieldKeyReleased(evt);
            }
        });

        maxPrecursorAdjustmentLabel.setText("Max Precursor Adjustment");

        maxPrecursorAdjustmentTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        maxPrecursorAdjustmentTextField.setText("2.5");
        maxPrecursorAdjustmentTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                maxPrecursorAdjustmentTextFieldKeyReleased(evt);
            }
        });

        precursorAdjustmentStepLabel.setText("Precursor Adjustment Step");

        precursorAdjustmentStepTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorAdjustmentStepTextField.setText("0.1");
        precursorAdjustmentStepTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorAdjustmentStepTextFieldKeyReleased(evt);
            }
        });

        outputSuffixLabel.setText("Output Suffix");

        outputSuffixTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        outputSuffixTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                outputSuffixTextFieldKeyReleased(evt);
            }
        });

        duplicateSpectraPerChargeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        deisptopingModeTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        deisptopingModeTextField.setText("0");
        deisptopingModeTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                deisptopingModeTextFieldKeyReleased(evt);
            }
        });

        isotopeToleranceTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        isotopeToleranceTextField.setText("0.25");
        isotopeToleranceTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                isotopeToleranceTextFieldKeyReleased(evt);
            }
        });

        complementToleranceTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        complementToleranceTextField.setText("0.5");
        complementToleranceTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                complementToleranceTextFieldKeyReleased(evt);
            }
        });

        tagLengthTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        tagLengthTextField.setText("3");
        tagLengthTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tagLengthTextFieldKeyReleased(evt);
            }
        });

        numVariableModsTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        numVariableModsTextField.setText("2");
        numVariableModsTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                numVariableModsTextFieldKeyReleased(evt);
            }
        });

        intensityScoreWeightTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        intensityScoreWeightTextField.setText("1");
        intensityScoreWeightTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                intensityScoreWeightTextFieldKeyReleased(evt);
            }
        });

        mzFidelityScoreWeightTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mzFidelityScoreWeightTextField.setText("1");
        mzFidelityScoreWeightTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                mzFidelityScoreWeightTextFieldKeyReleased(evt);
            }
        });

        complementScoreWeightTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        complementScoreWeightTextField.setText("1");
        complementScoreWeightTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                complementScoreWeightTextFieldKeyReleased(evt);
            }
        });

        ticCutoffTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ticCutoffTextField.setText("85");
        ticCutoffTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ticCutoffTextFieldKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout directTagPanelLayout = new javax.swing.GroupLayout(directTagPanel);
        directTagPanel.setLayout(directTagPanelLayout);
        directTagPanelLayout.setHorizontalGroup(
            directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(directTagPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(directTagPanelLayout.createSequentialGroup()
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addGap(198, 198, 198)
                                .addComponent(tagLengthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(tagLengthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addComponent(numVariableModsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(numVariableModsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addComponent(numberOfChargeStatesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(numberOfChargeStatesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addComponent(duplicateSpectraLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(duplicateSpectraPerChargeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(50, 50, 50)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, directTagPanelLayout.createSequentialGroup()
                                .addComponent(ticCutoffLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(ticCutoffTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(directTagPanelLayout.createSequentialGroup()
                                    .addComponent(numberOfIntensityClassesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(numberOfIntensityClassesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(directTagPanelLayout.createSequentialGroup()
                                    .addComponent(maxPeakCountLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(maxPeakCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(directTagPanelLayout.createSequentialGroup()
                                    .addComponent(outputSuffixLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(outputSuffixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, directTagPanelLayout.createSequentialGroup()
                        .addComponent(isotopeToleranceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(isotopeToleranceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(precursorAdjustmentStepLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(precursorAdjustmentStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(directTagPanelLayout.createSequentialGroup()
                        .addComponent(deisotopingModeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(deisptopingModeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50)
                        .addComponent(complementToleranceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(complementToleranceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(50, 50, 50)
                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(directTagPanelLayout.createSequentialGroup()
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(minPrecursorAdjustmentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(maxPrecursorAdjustmentLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(minPrecursorAdjustmentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(maxPrecursorAdjustmentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, directTagPanelLayout.createSequentialGroup()
                        .addComponent(complementScoreWeightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(complementScoreWeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, directTagPanelLayout.createSequentialGroup()
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(mzFidelityScoreWeightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(intensityScoreWeightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(intensityScoreWeightTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                            .addComponent(mzFidelityScoreWeightTextField))))
                .addGap(10, 10, 10))
        );

        directTagPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {complementScoreWeightLabel, complementToleranceLabel, deisotopingModeLabel, duplicateSpectraLabel, intensityScoreWeightLabel, isotopeToleranceLabel, maxPeakCountLabel, maxPrecursorAdjustmentLabel, minPrecursorAdjustmentLabel, mzFidelityScoreWeightLabel, numVariableModsLabel, numberOfChargeStatesLabel, numberOfIntensityClassesLabel, outputSuffixLabel, precursorAdjustmentStepLabel, tagLengthLabel, ticCutoffLabel});

        directTagPanelLayout.setVerticalGroup(
            directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(directTagPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(directTagPanelLayout.createSequentialGroup()
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(minPrecursorAdjustmentLabel)
                            .addComponent(minPrecursorAdjustmentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(maxPrecursorAdjustmentLabel)
                            .addComponent(maxPrecursorAdjustmentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(intensityScoreWeightLabel)
                            .addComponent(intensityScoreWeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(mzFidelityScoreWeightLabel)
                            .addComponent(mzFidelityScoreWeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(complementScoreWeightLabel)
                            .addComponent(complementScoreWeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(directTagPanelLayout.createSequentialGroup()
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(numberOfIntensityClassesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(numberOfIntensityClassesLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(outputSuffixLabel)
                                    .addComponent(outputSuffixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(maxPeakCountLabel)
                                    .addComponent(maxPeakCountTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(ticCutoffTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(ticCutoffLabel)))
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(tagLengthLabel)
                                    .addComponent(tagLengthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(numVariableModsLabel)
                                    .addComponent(numVariableModsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(numberOfChargeStatesLabel)
                                    .addComponent(numberOfChargeStatesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(duplicateSpectraLabel)
                                    .addComponent(duplicateSpectraPerChargeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(complementToleranceLabel)
                                    .addComponent(complementToleranceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(isotopeToleranceLabel)
                                    .addComponent(isotopeToleranceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(precursorAdjustmentStepLabel)
                                    .addComponent(precursorAdjustmentStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(directTagPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(directTagPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(deisotopingModeLabel)
                                    .addComponent(deisptopingModeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        generalPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("General Settings"));
        generalPanel.setOpaque(false);

        fragmentMassToleranceLabel.setText("Fragment Mass Tolerance (Da)");
        fragmentMassToleranceLabel.setToolTipText("Fragment mass tolerance - max 0.75 Da");

        fragmentMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(0.5d, 0.0d, 0.75d, 0.1d));
        fragmentMassToleranceSpinner.setToolTipText("Fragment mass tolerance - max 0.75 Da");

        precursorMassToleranceLabel.setText("Precursor Mass Tolerance (Da)");
        precursorMassToleranceLabel.setToolTipText("Precursor mass tolerance - max 5.0 Da");

        precursorMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 5.0d, 0.1d));
        precursorMassToleranceSpinner.setToolTipText("Precursor mass tolerance - max 5.0 Da");

        numberOfSolutionsLabel.setText("#Solutions (max. 20)");

        numberOfSolutionsSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, 2000, 1));

        numberOfThreadsLabel.setText("#Threads/Cores");

        numberOfThreadsSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));

        adjustPrecursorLabel.setText("Use Spectrum Precursor m/z");

        adjustPrecursorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        useSpectrumChargeStageLabel.setText("Use Spectrum Charge State");

        useSpectrumChargeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addComponent(fragmentMassToleranceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addComponent(precursorMassToleranceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(50, 50, 50)
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, generalPanelLayout.createSequentialGroup()
                        .addComponent(numberOfThreadsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(numberOfThreadsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50)
                        .addComponent(useSpectrumChargeStageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(useSpectrumChargeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, generalPanelLayout.createSequentialGroup()
                        .addComponent(numberOfSolutionsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50)
                        .addComponent(adjustPrecursorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(adjustPrecursorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        generalPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {numberOfSolutionsSpinner, numberOfThreadsSpinner, precursorMassToleranceSpinner});

        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fragmentMassToleranceLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorMassToleranceLabel)))
                    .addGroup(generalPanelLayout.createSequentialGroup()
                        .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(adjustPrecursorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(adjustPrecursorLabel))
                            .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(numberOfSolutionsLabel)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(useSpectrumChargeStageLabel)
                                .addComponent(useSpectrumChargeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(numberOfThreadsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(numberOfThreadsLabel)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(directTagPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(generalPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(modificationsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(openDialogHelpJButton)
                        .addGap(863, 863, 863)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(generalPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(directTagPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modificationsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(openDialogHelpJButton)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Save the settings an close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        boolean valid = validateParametersInput(true);

        if (valid) {

            // store the number of threads
            deNovoGUI.getDeNovoSequencingHandler().setNThreads((Integer) numberOfThreadsSpinner.getValue());

            SearchParameters tempSearchParameters = getSearchParametersFromGUI();

            if (!deNovoGUI.getSearchParameters().equals(tempSearchParameters)) {

                int value = JOptionPane.showConfirmDialog(this, "The settings have changed."
                        + "\nDo you want to save the changes?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);

                if (value == JOptionPane.YES_OPTION) {

                    boolean userSelectFile = false;

                    // see if the user wants to overwrite the current settings file
                    if (tempSearchParameters.getParametersFile() != null) {
                        value = JOptionPane.showConfirmDialog(this, "Overwrite current settings file?", "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);

                        if (value == JOptionPane.NO_OPTION) {
                            userSelectFile = true;
                        } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                            return;
                        }

                    } else {
                        // no params file > have the user select a file
                        userSelectFile = true;
                    }

                    boolean fileSaved = true;
                    
                    if (userSelectFile) {
                        fileSaved = saveAsPressed();
                        tempSearchParameters = getSearchParametersFromGUI(); // see if the settings have changed
                    }

                    if (fileSaved && tempSearchParameters.getParametersFile() != null) {

                        try {
                            tempSearchParameters.setParametersFile(deNovoGUI.getSearchParameters().getParametersFile());
                            SearchParameters.saveIdentificationParameters(tempSearchParameters, deNovoGUI.getSearchParameters().getParametersFile());
                            deNovoGUI.setSearchParameters(tempSearchParameters);
                            dispose();
                        } catch (ClassNotFoundException e) {
                            JOptionPane.showMessageDialog(this, "An error occurred when saving the settings:\n"
                                    + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(this, "An error occurred when saving the settings:\n"
                                    + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    }
                } else if (value == JOptionPane.NO_OPTION) {
                    dispose(); // reject the changes
                }
            } else {
                dispose(); // no changes
            }
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Close the dialog without saving.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Add a fixed modification.
     *
     * @param evt
     */
    private void addFixedModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFixedModificationActionPerformed

        // @TODO: check if fixed terminal ptms are added and provide a warning that these will be considered variable?
        int nSelected = fixedModsTable.getRowCount();
        int nNew = modificationsTable.getSelectedRows().length;
        String[] fixedModifications = new String[nSelected + nNew];
        int cpt = 0;

        for (int i = 0; i < nSelected; i++) {
            fixedModifications[cpt] = (String) fixedModsTable.getValueAt(i, 1);
            cpt++;
        }

        for (int selectedRow : modificationsTable.getSelectedRows()) {
            String name = (String) modificationsTable.getValueAt(selectedRow, 1);
            boolean found = false;
            for (int i = 0; i < fixedModsTable.getModel().getRowCount(); i++) {
                if (((String) fixedModsTable.getValueAt(i, 1)).equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fixedModifications[cpt] = name;
                cpt++;
                if (!deNovoGUI.getModificationUse().contains(name)) {
                    deNovoGUI.getModificationUse().add(name);
                }
            }
        }

        DefaultTableModel fixedModel = (DefaultTableModel) fixedModsTable.getModel();
        fixedModel.getDataVector().removeAllElements();

        for (String fixedMod : fixedModifications) {
            ((DefaultTableModel) fixedModsTable.getModel()).addRow(new Object[]{searchParameters.getModificationProfile().getColor(fixedMod), fixedMod, ptmFactory.getPTM(fixedMod).getMass()});
        }

        ((DefaultTableModel) fixedModsTable.getModel()).fireTableDataChanged();
        fixedModsTable.repaint();

        fixedModificationsLabel.setText("Fixed Modifications (" + fixedModifications.length + ")");
        updateModificationList();
    }//GEN-LAST:event_addFixedModificationActionPerformed

    /**
     * Remove a fixed modification.
     *
     * @param evt
     */
    private void removeFixedModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFixedModificationActionPerformed
        int nSelected = fixedModsTable.getRowCount();
        int nToRemove = fixedModsTable.getSelectedRows().length;
        String[] fixedModifications = new String[nSelected - nToRemove];
        int cpt = 0;

        for (int i = 0; i < fixedModsTable.getRowCount(); i++) {
            boolean found = false;
            for (int selectedRow : fixedModsTable.getSelectedRows()) {
                if (((String) fixedModsTable.getValueAt(i, 1)).equals((String) fixedModsTable.getValueAt(selectedRow, 1))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fixedModifications[cpt] = (String) fixedModsTable.getValueAt(i, 1);
                cpt++;
            }
        }

        DefaultTableModel fixedModel = (DefaultTableModel) fixedModsTable.getModel();
        fixedModel.getDataVector().removeAllElements();

        for (String fixedMod : fixedModifications) {
            ((DefaultTableModel) fixedModsTable.getModel()).addRow(new Object[]{searchParameters.getModificationProfile().getColor(fixedMod), fixedMod, ptmFactory.getPTM(fixedMod).getMass()});
        }
        ((DefaultTableModel) fixedModsTable.getModel()).fireTableDataChanged();
        fixedModsTable.repaint();

        fixedModificationsLabel.setText("Fixed Modifications (" + fixedModifications.length + ")");
        updateModificationList();
    }//GEN-LAST:event_removeFixedModificationActionPerformed

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void fixedModsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fixedModsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_fixedModsTableMouseExited

    /**
     * Change the color of a fixed PTM.
     *
     * @param evt
     */
    private void fixedModsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fixedModsTableMouseReleased
        int row = fixedModsTable.rowAtPoint(evt.getPoint());
        int column = fixedModsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == fixedModsTable.getColumn(" ").getModelIndex()) {
                Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) fixedModsTable.getValueAt(row, column));

                if (newColor != null) {
                    searchParameters.getModificationProfile().setColor((String) fixedModsTable.getValueAt(row, 1), newColor);
                    fixedModsTable.setValueAt(newColor, row, 0);
                    ((DefaultTableModel) fixedModsTable.getModel()).fireTableDataChanged();
                    fixedModsTable.repaint();
                }
            }
        }

        enableAddRemoveButtons();
    }//GEN-LAST:event_fixedModsTableMouseReleased

    /**
     * Changes the cursor to a hand cursor if over the color column.
     *
     * @param evt
     */
    private void fixedModsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fixedModsTableMouseMoved
        int row = fixedModsTable.rowAtPoint(evt.getPoint());
        int column = fixedModsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == fixedModsTable.getColumn(" ").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_fixedModsTableMouseMoved

    /**
     * Add a variable modification.
     *
     * @param evt
     */
    private void addVariableModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addVariableModificationActionPerformed
        int nSelected = variableModsTable.getRowCount();
        int nNew = modificationsTable.getSelectedRows().length;
        String[] variableModifications = new String[nSelected + nNew];
        int cpt = 0;

        for (int i = 0; i < nSelected; i++) {
            variableModifications[cpt] = (String) variableModsTable.getValueAt(i, 1);
            cpt++;
        }

        for (int selectedRow : modificationsTable.getSelectedRows()) {
            String name = (String) modificationsTable.getValueAt(selectedRow, 1);
            boolean found = false;
            for (int i = 0; i < variableModsTable.getRowCount(); i++) {
                if (((String) variableModsTable.getValueAt(i, 1)).equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                variableModifications[cpt] = name;
                cpt++;
                if (!deNovoGUI.getModificationUse().contains(name)) {
                    deNovoGUI.getModificationUse().add(name);
                }
            }
        }

        DefaultTableModel variableModel = (DefaultTableModel) variableModsTable.getModel();
        variableModel.getDataVector().removeAllElements();

        for (String variabledMod : variableModifications) {
            ((DefaultTableModel) variableModsTable.getModel()).addRow(new Object[]{searchParameters.getModificationProfile().getColor(variabledMod), variabledMod, ptmFactory.getPTM(variabledMod).getMass()});
        }
        ((DefaultTableModel) variableModsTable.getModel()).fireTableDataChanged();
        variableModsTable.repaint();

        variableModificationsLabel.setText("Variable Modifications (" + variableModifications.length + ")");

        if (variableModifications.length > 6) {
            JOptionPane.showMessageDialog(this,
                    "It is not recommended to use more than 6 variable modifications at the same time.", "Warning", JOptionPane.WARNING_MESSAGE);
        }

        updateModificationList();
    }//GEN-LAST:event_addVariableModificationActionPerformed

    /**
     * Remove a variable modification.
     *
     * @param evt
     */
    private void removeVariableModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeVariableModificationActionPerformed
        int nSelected = variableModsTable.getRowCount();
        int nToRemove = variableModsTable.getSelectedRows().length;
        String[] variableModifications = new String[nSelected - nToRemove];
        int cpt = 0;

        for (int i = 0; i < variableModsTable.getRowCount(); i++) {
            boolean found = false;
            for (int selectedRow : variableModsTable.getSelectedRows()) {
                if (((String) variableModsTable.getValueAt(i, 1)).equals((String) variableModsTable.getValueAt(selectedRow, 1))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                variableModifications[cpt] = (String) variableModsTable.getValueAt(i, 1);
                cpt++;
            }
        }

        DefaultTableModel variableModel = (DefaultTableModel) variableModsTable.getModel();
        variableModel.getDataVector().removeAllElements();

        for (String variabledMod : variableModifications) {
            ((DefaultTableModel) variableModsTable.getModel()).addRow(new Object[]{searchParameters.getModificationProfile().getColor(variabledMod), variabledMod, ptmFactory.getPTM(variabledMod).getMass()});
        }
        ((DefaultTableModel) variableModsTable.getModel()).fireTableDataChanged();
        variableModsTable.repaint();

        variableModificationsLabel.setText("Variable Modifications (" + variableModifications.length + ")");
        updateModificationList();
    }//GEN-LAST:event_removeVariableModificationActionPerformed

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void variableModsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_variableModsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_variableModsTableMouseExited

    /**
     * Change the color of a fixed PTM.
     *
     * @param evt
     */
    private void variableModsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_variableModsTableMouseReleased
        int row = variableModsTable.rowAtPoint(evt.getPoint());
        int column = variableModsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == variableModsTable.getColumn(" ").getModelIndex()) {
                Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) variableModsTable.getValueAt(row, column));

                if (newColor != null) {
                    searchParameters.getModificationProfile().setColor((String) variableModsTable.getValueAt(row, 1), newColor);
                    variableModsTable.setValueAt(newColor, row, 0);
                    ((DefaultTableModel) variableModsTable.getModel()).fireTableDataChanged();
                    variableModsTable.repaint();
                }
            }
        }

        enableAddRemoveButtons();
    }//GEN-LAST:event_variableModsTableMouseReleased

    /**
     * Changes the cursor to a hand cursor if over the color column.
     *
     * @param evt
     */
    private void variableModsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_variableModsTableMouseMoved
        int row = variableModsTable.rowAtPoint(evt.getPoint());
        int column = variableModsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == variableModsTable.getColumn(" ").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_variableModsTableMouseMoved

    /**
     * Make sure that the fixed and variable modification panels have equal
     * size.
     *
     * @param evt
     */
    private void modificationTypesSplitPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_modificationTypesSplitPaneComponentResized
        modificationTypesSplitPane.setDividerLocation(0.5);
    }//GEN-LAST:event_modificationTypesSplitPaneComponentResized

    /**
     * Update the modification lists.
     *
     * @param evt
     */
    private void modificationsListComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modificationsListComboActionPerformed
        updateModificationList();
    }//GEN-LAST:event_modificationsListComboActionPerformed

    /**
     * Opens a file chooser where the color for the PTM can be changed.
     *
     * @param evt
     */
    private void modificationsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationsTableMouseReleased
        int row = modificationsTable.rowAtPoint(evt.getPoint());
        int column = modificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == modificationsTable.getColumn(" ").getModelIndex()) {
                Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) modificationsTable.getValueAt(row, column));

                if (newColor != null) {
                    ptmFactory.setColor((String) modificationsTable.getValueAt(row, 1), newColor);
                    modificationsTable.setValueAt(newColor, row, 0);
                    ((DefaultTableModel) modificationsTable.getModel()).fireTableDataChanged();
                    modificationsTable.repaint();
                }
            } else if (modificationsListCombo.getSelectedIndex() == 1
                    && column == modificationsTable.getColumn("  ").getModelIndex()
                    && modificationsTable.getValueAt(row, column) != null) {

                boolean selected = (Boolean) modificationsTable.getValueAt(row, column);
                String ptmName = (String) modificationsTable.getValueAt(row, 1);

                // change if the ptm is considered as default
                if (modificationsListCombo.getSelectedIndex() == 0) {
                    // remove from default ptm set
                    deNovoGUI.getModificationUse().remove(ptmName);
                } else {
                    if (selected) {
                        // add to default ptm set
                        if (!deNovoGUI.getModificationUse().contains(ptmName)) {
                            deNovoGUI.getModificationUse().add(ptmName);
                        }
                    } else {
                        // remove from default ptm set
                        deNovoGUI.getModificationUse().remove(ptmName);
                    }
                }

                updateModificationList();

                if (row < modificationsTable.getRowCount()) {
                    modificationsTable.setRowSelectionInterval(row, row);
                } else if (row - 1 < modificationsTable.getRowCount() && row >= 0) {
                    {
                        modificationsTable.setRowSelectionInterval(row - 1, row - 1);
                    }
                }
            }

            enableAddRemoveButtons();
        }
    }//GEN-LAST:event_modificationsTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void modificationsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modificationsTableMouseExited

    /**
     * Changes the cursor to a hand cursor if over the color column.
     *
     * @param evt
     */
    private void modificationsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationsTableMouseMoved
        int row = modificationsTable.rowAtPoint(evt.getPoint());
        int column = modificationsTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == modificationsTable.getColumn(" ").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_modificationsTableMouseMoved

    /**
     * Open the modifications pop up menu.
     *
     * @param evt
     */
    private void openModificationSettingsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openModificationSettingsJButtonActionPerformed
        new ModificationsDialog(deNovoGUI, this, true);
    }//GEN-LAST:event_openModificationSettingsJButtonActionPerformed

    /**
     * Change the cursor into a hand cursor.
     *
     * @param evt
     */
    private void openModificationSettingsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openModificationSettingsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openModificationSettingsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void openModificationSettingsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openModificationSettingsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openModificationSettingsJButtonMouseExited

    /**
     * Change the cursor into a hand cursor.
     *
     * @param evt
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void openDialogHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, getClass().getResource("/html/DeNovoSettings.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                "Search Settings Help", 500, 10);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void tagLengthTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tagLengthTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_tagLengthTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void numVariableModsTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_numVariableModsTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_numVariableModsTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void numberOfChargeStatesTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_numberOfChargeStatesTextFieldKeyPressed
        validateParametersInput(false);
    }//GEN-LAST:event_numberOfChargeStatesTextFieldKeyPressed

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void deisptopingModeTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_deisptopingModeTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_deisptopingModeTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void isotopeToleranceTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_isotopeToleranceTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_isotopeToleranceTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void numberOfIntensityClassesTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_numberOfIntensityClassesTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_numberOfIntensityClassesTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void outputSuffixTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_outputSuffixTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_outputSuffixTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void maxPeakCountTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_maxPeakCountTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_maxPeakCountTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void ticCutoffTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ticCutoffTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_ticCutoffTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void complementToleranceTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_complementToleranceTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_complementToleranceTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void precursorAdjustmentStepTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorAdjustmentStepTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_precursorAdjustmentStepTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void minPrecursorAdjustmentTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_minPrecursorAdjustmentTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_minPrecursorAdjustmentTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void maxPrecursorAdjustmentTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_maxPrecursorAdjustmentTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_maxPrecursorAdjustmentTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void intensityScoreWeightTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_intensityScoreWeightTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_intensityScoreWeightTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void mzFidelityScoreWeightTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mzFidelityScoreWeightTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_mzFidelityScoreWeightTextFieldKeyReleased

    /**
     * Validate the input parameters.
     *
     * @param evt
     */
    private void complementScoreWeightTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_complementScoreWeightTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_complementScoreWeightTextFieldKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFixedModification;
    private javax.swing.JButton addVariableModification;
    private javax.swing.JComboBox adjustPrecursorCmb;
    private javax.swing.JLabel adjustPrecursorLabel;
    private javax.swing.JPanel availableModsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel complementScoreWeightLabel;
    private javax.swing.JTextField complementScoreWeightTextField;
    private javax.swing.JLabel complementToleranceLabel;
    private javax.swing.JTextField complementToleranceTextField;
    private javax.swing.JLabel deisotopingModeLabel;
    private javax.swing.JTextField deisptopingModeTextField;
    private javax.swing.JPanel directTagPanel;
    private javax.swing.JLabel duplicateSpectraLabel;
    private javax.swing.JComboBox duplicateSpectraPerChargeCmb;
    private javax.swing.JLabel fixedModificationsLabel;
    private javax.swing.JScrollPane fixedModsJScrollPane;
    private javax.swing.JTable fixedModsTable;
    private javax.swing.JLabel fragmentMassToleranceLabel;
    private javax.swing.JSpinner fragmentMassToleranceSpinner;
    private javax.swing.JPanel generalPanel;
    private javax.swing.JLabel intensityScoreWeightLabel;
    private javax.swing.JTextField intensityScoreWeightTextField;
    private javax.swing.JLabel isotopeToleranceLabel;
    private javax.swing.JTextField isotopeToleranceTextField;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JLabel maxPeakCountLabel;
    private javax.swing.JTextField maxPeakCountTextField;
    private javax.swing.JLabel maxPrecursorAdjustmentLabel;
    private javax.swing.JTextField maxPrecursorAdjustmentTextField;
    private javax.swing.JLabel minPrecursorAdjustmentLabel;
    private javax.swing.JTextField minPrecursorAdjustmentTextField;
    private javax.swing.JSplitPane modificationTypesSplitPane;
    private javax.swing.JScrollPane modificationsJScrollPane;
    private javax.swing.JComboBox modificationsListCombo;
    private javax.swing.JPanel modificationsPanel1;
    private javax.swing.JTable modificationsTable;
    private javax.swing.JLabel mzFidelityScoreWeightLabel;
    private javax.swing.JTextField mzFidelityScoreWeightTextField;
    private javax.swing.JLabel numVariableModsLabel;
    private javax.swing.JTextField numVariableModsTextField;
    private javax.swing.JLabel numberOfChargeStatesLabel;
    private javax.swing.JTextField numberOfChargeStatesTextField;
    private javax.swing.JLabel numberOfIntensityClassesLabel;
    private javax.swing.JTextField numberOfIntensityClassesTextField;
    private javax.swing.JLabel numberOfSolutionsLabel;
    private javax.swing.JSpinner numberOfSolutionsSpinner;
    private javax.swing.JLabel numberOfThreadsLabel;
    private javax.swing.JSpinner numberOfThreadsSpinner;
    private javax.swing.JButton okButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JButton openModificationSettingsJButton;
    private javax.swing.JLabel outputSuffixLabel;
    private javax.swing.JTextField outputSuffixTextField;
    private javax.swing.JLabel precursorAdjustmentStepLabel;
    private javax.swing.JTextField precursorAdjustmentStepTextField;
    private javax.swing.JLabel precursorMassToleranceLabel;
    private javax.swing.JSpinner precursorMassToleranceSpinner;
    private javax.swing.JButton removeFixedModification;
    private javax.swing.JButton removeVariableModification;
    private javax.swing.JLabel tagLengthLabel;
    private javax.swing.JTextField tagLengthTextField;
    private javax.swing.JLabel ticCutoffLabel;
    private javax.swing.JTextField ticCutoffTextField;
    private javax.swing.JComboBox useSpectrumChargeCmb;
    private javax.swing.JLabel useSpectrumChargeStageLabel;
    private javax.swing.JLabel variableModificationsLabel;
    private javax.swing.JScrollPane variableModsJScrollPane;
    private javax.swing.JTable variableModsTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the search parameters as set in the GUI.
     *
     * @return the search parameters as set in the GUI
     */
    public SearchParameters getSearchParametersFromGUI() {

        SearchParameters tempSearchParameters = new SearchParameters();
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()));

        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), new OmssaParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), new XtandemParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), new MsgfParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), new PepnovoParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), new DirecTagParameters());
        }

        PepnovoParameters pepNovoParameters = (PepnovoParameters) tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex());
        DirecTagParameters direcTagParameters = (DirecTagParameters) tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex());

        Enzyme enzyme = enzymeFactory.getEnzyme("Trypsin"); // only trypsin is supported by pepnovo anyway...
        tempSearchParameters.setEnzyme(enzyme);

        // general parameters
        double fragmentIonTolerance = (Double) fragmentMassToleranceSpinner.getValue();
        tempSearchParameters.setFragmentIonAccuracy(fragmentIonTolerance);
        double precursorIonTolerance = (Double) precursorMassToleranceSpinner.getValue();
        tempSearchParameters.setPrecursorAccuracyDalton(precursorIonTolerance);
        int maxHitListLength = (Integer) numberOfSolutionsSpinner.getValue();
        pepNovoParameters.setHitListLength(maxHitListLength);

        pepNovoParameters.setCorrectPrecursorMass(adjustPrecursorCmb.getSelectedIndex() == 1);
        direcTagParameters.setAdjustPrecursorMass(adjustPrecursorCmb.getSelectedIndex() == 1);

        pepNovoParameters.setEstimateCharge(useSpectrumChargeCmb.getSelectedIndex() == 1);
        direcTagParameters.setUseChargeStateFromMS(useSpectrumChargeCmb.getSelectedIndex() == 0);

        pepNovoParameters.setDiscardLowQualitySpectra(true); // note: hardcoded to true (mainly because if not this would be the only pepnovo parameter left...)

        // DirecTag parameters
        direcTagParameters.setMaxTagCount(maxHitListLength);
        direcTagParameters.setTagLength(Integer.parseInt(tagLengthTextField.getText()));
        direcTagParameters.setMaxDynamicMods(Integer.parseInt(numVariableModsTextField.getText()));
        direcTagParameters.setNumChargeStates(Integer.parseInt(numberOfChargeStatesTextField.getText()));
        direcTagParameters.setDuplicateSpectra(duplicateSpectraPerChargeCmb.getSelectedIndex() == 0);
        direcTagParameters.setDeisotopingMode(Integer.parseInt(deisptopingModeTextField.getText()));
        direcTagParameters.setIsotopeMzTolerance(Double.parseDouble(isotopeToleranceTextField.getText()));
        direcTagParameters.setNumIntensityClasses(Integer.parseInt(numberOfIntensityClassesTextField.getText()));
        direcTagParameters.setOutputSuffix(outputSuffixTextField.getText());
        direcTagParameters.setMaxPeakCount(Integer.parseInt(maxPeakCountTextField.getText()));
        direcTagParameters.setTicCutoffPercentage(Double.parseDouble(ticCutoffTextField.getText()));
        direcTagParameters.setComplementMzTolerance(Double.parseDouble(complementToleranceTextField.getText()));
        direcTagParameters.setPrecursorAdjustmentStep(Double.parseDouble(precursorAdjustmentStepTextField.getText()));
        direcTagParameters.setMinPrecursorAdjustment(Double.parseDouble(minPrecursorAdjustmentTextField.getText()));
        direcTagParameters.setMaxPrecursorAdjustment(Double.parseDouble(maxPrecursorAdjustmentTextField.getText()));
        direcTagParameters.setIntensityScoreWeight(Double.parseDouble(intensityScoreWeightTextField.getText()));
        direcTagParameters.setMzFidelityScoreWeight(Double.parseDouble(mzFidelityScoreWeightTextField.getText()));
        direcTagParameters.setComplementScoreWeight(Double.parseDouble(complementScoreWeightTextField.getText()));

        // set the modifications
        ModificationProfile modificationProfile = new ModificationProfile();
        for (int i = 0; i < fixedModsTable.getRowCount(); i++) {
            String modName = (String) fixedModsTable.getValueAt(i, 1);
            modificationProfile.addFixedModification(ptmFactory.getPTM(modName));
            modificationProfile.setColor(modName, (Color) fixedModsTable.getValueAt(i, 0));
        }

        for (int i = 0; i < variableModsTable.getRowCount(); i++) {
            String modName = (String) variableModsTable.getValueAt(i, 1);
            modificationProfile.addVariableModification(ptmFactory.getPTM(modName));
            modificationProfile.setColor(modName, (Color) variableModsTable.getValueAt(i, 0));
        }
        tempSearchParameters.setModificationProfile(modificationProfile);

        // Set omssa indexes
        ptmFactory.setSearchedOMSSAIndexes(tempSearchParameters.getModificationProfile());

        if (searchParameters.getParametersFile() != null && searchParameters.getParametersFile().exists()) {
            tempSearchParameters.setParametersFile(searchParameters.getParametersFile());
        }

        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), pepNovoParameters);
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), direcTagParameters);

        return tempSearchParameters;
    }

    /**
     * Inspects the parameters validity.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateParametersInput(boolean showMessage) {

        boolean valid = true;

        valid = GuiUtilities.validateIntegerInput(this, tagLengthLabel, tagLengthTextField, "tag length", "Tag Length Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, numVariableModsLabel, numVariableModsTextField, "number of variable modifications", "Variable Modifications Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, numberOfChargeStatesLabel, numberOfChargeStatesTextField, "number of charge states", "Charge States Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, deisotopingModeLabel, deisptopingModeTextField, "deisotoping mode", "Deisotoping Mode Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, isotopeToleranceLabel, isotopeToleranceTextField, "isotope tolerance", "Isotope Tolerance Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, numberOfIntensityClassesLabel, numberOfIntensityClassesTextField, "number of intensity classes", "Intensity Classes Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, maxPeakCountLabel, maxPeakCountTextField, "maximum peak count", "Max Peak Count Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, ticCutoffLabel, ticCutoffTextField, "TIC cutoff", "TIC Cutoff Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, complementToleranceLabel, complementToleranceTextField, "complement tolerance", "Complement Tolerance Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, precursorAdjustmentStepLabel, precursorAdjustmentStepTextField, "precursor adjustment step", "Precursor Adjustment Step Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, minPrecursorAdjustmentLabel, minPrecursorAdjustmentTextField, "minimum precursor adjustment", "Minimum Precursor Adjustment Error", false, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, maxPrecursorAdjustmentLabel, maxPrecursorAdjustmentTextField, "maximum precursor adjustment", "Maximum Precursor Adjustment Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, intensityScoreWeightLabel, intensityScoreWeightTextField, "intensity score weight", "Intensity Score Waight Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, mzFidelityScoreWeightLabel, mzFidelityScoreWeightTextField, "mz fidelity score weight", "MZ Fidelity Score Weight Error", true, showMessage, valid);
        valid = GuiUtilities.validateDoubleInput(this, complementScoreWeightLabel, complementScoreWeightTextField, "complement score weight", "Complement Score Weight Error", true, showMessage, valid);

        okButton.setEnabled(valid);
        return valid;
    }

    /**
     * This method is called when the user clicks the 'Save' button.
     * 
     * @return true of the file was saved
     */
    public boolean savePressed() {
        if (parametersFile == null) {
            return saveAsPressed();
        } else if (validateParametersInput(true)) {
            try {
                searchParameters = getSearchParametersFromGUI();
                SearchParameters.saveIdentificationParameters(searchParameters, parametersFile);
                deNovoGUI.setSearchParameters(searchParameters);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, new String[]{"An error occurred while writing: " + parametersFile.getName(), e.getMessage()}, "Error Saving File", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * This method is called when the user clicks the 'Save As' button.
     * 
     * @return true of the file was saved
     */
    public boolean saveAsPressed() {

        if (validateParametersInput(true)) {

            // First check whether a file has already been selected.
            // If so, start from that file's parent.
            File startLocation = new File(deNovoGUI.getLastSelectedFolder());

            if (searchParameters.getParametersFile() != null) {
                startLocation = searchParameters.getParametersFile();
            }

            boolean complete = false;

            while (!complete) {
                JFileChooser fc = new JFileChooser(startLocation);
                FileFilter filter = new FileFilter() {
                    @Override
                    public boolean accept(File myFile) {
                        return myFile.getName().toLowerCase().endsWith(".parameters") || myFile.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "DeNovoGUI settings file (.parameters)";
                    }
                };
                fc.setFileFilter(filter);
                int result = fc.showSaveDialog(this);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selected = fc.getSelectedFile();
                    deNovoGUI.setLastSelectedFolder(selected.getAbsolutePath());
                    // Make sure the file is appended with '.parameters'
                    if (!selected.getName().toLowerCase().endsWith(".parameters")) {
                        selected = new File(selected.getParentFile(), selected.getName() + ".parameters");
                        parametersFile = selected;
                    } else {
                        selected = new File(selected.getParentFile(), selected.getName());
                        parametersFile = selected;
                    }
                    complete = true;
                    if (selected.exists()) {
                        int choice = JOptionPane.showConfirmDialog(this,
                                new String[]{"The file " + selected.getName() + " already exists.", "Overwrite?"},
                                "File Already Exists", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.NO_OPTION) {
                            complete = false;
                        }
                    }
                } else {
                    return complete;
                }
            }

            boolean saved = savePressed();
            searchParameters.setParametersFile(parametersFile);
            return saved;
        } else {
            return false;
        }
    }

    /**
     * Updates the modification list (right).
     */
    private void updateModificationList() {
        ArrayList<String> allModificationsList = new ArrayList<String>();
        if (modificationsListCombo.getSelectedIndex() == 0) {
            for (String name : deNovoGUI.getModificationUse()) {
                if (deNovoGUI.getModificationUse().contains(name)) {
                    allModificationsList.add(name);
                }
            }
        } else {
            allModificationsList = ptmFactory.getPTMs();
        }

        int nFixed = fixedModsTable.getRowCount();
        int nVariable = variableModsTable.getRowCount();
        ArrayList<String> allModifications = new ArrayList<String>();

        for (String name : allModificationsList) {
            boolean found = false;
            for (int j = 0; j < nFixed; j++) {
                if (((String) fixedModsTable.getValueAt(j, 1)).equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (int j = 0; j < nVariable; j++) {
                    if (((String) variableModsTable.getValueAt(j, 1)).equals(name)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                allModifications.add(name);
            }
        }

        String[] allModificationsAsArray = new String[allModifications.size()];

        for (int i = 0; i < allModifications.size(); i++) {
            allModificationsAsArray[i] = allModifications.get(i);
        }

        Arrays.sort(allModificationsAsArray);

        if (modificationsListCombo.getSelectedIndex() == 0) {
            modificationsTable.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{},
                    new String[]{
                        " ", "Name", "Mass"
                    }
            ) {
                Class[] types = new Class[]{
                    java.lang.Object.class, java.lang.String.class, java.lang.Double.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false, false
                };

                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            });
        } else {
            modificationsTable.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{},
                    new String[]{
                        " ", "Name", "Mass", "  "
                    }
            ) {
                Class[] types = new Class[]{
                    java.lang.Object.class, java.lang.String.class, java.lang.Double.class, java.lang.Boolean.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false, false, true
                };

                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            });
        }

        for (String mod : allModificationsAsArray) {
            ((DefaultTableModel) modificationsTable.getModel()).addRow(new Object[]{ptmFactory.getColor(mod), mod, ptmFactory.getPTM(mod).getMass(), deNovoGUI.getModificationUse().contains(mod)});
        }
        ((DefaultTableModel) modificationsTable.getModel()).fireTableDataChanged();
        modificationsTable.repaint();

        // get the min and max values for the mass sparklines
        double maxMass = Double.MIN_VALUE;
        double minMass = Double.MAX_VALUE;

        for (String ptm : ptmFactory.getPTMs()) {
            if (ptmFactory.getPTM(ptm).getMass() > maxMass) {
                maxMass = ptmFactory.getPTM(ptm).getMass();
            }
            if (ptmFactory.getPTM(ptm).getMass() < minMass) {
                minMass = ptmFactory.getPTM(ptm).getMass();
            }
        }

        setAllModificationTableProperties();

        modificationsTable.getColumn("Mass").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, minMass, maxMass));
        ((JSparklinesBarChartTableCellRenderer) modificationsTable.getColumn("Mass").getCellRenderer()).showNumberAndChart(true, 50);
        fixedModsTable.getColumn("Mass").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, minMass, maxMass));
        ((JSparklinesBarChartTableCellRenderer) fixedModsTable.getColumn("Mass").getCellRenderer()).showNumberAndChart(true, 50);
        variableModsTable.getColumn("Mass").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, minMass, maxMass));
        ((JSparklinesBarChartTableCellRenderer) variableModsTable.getColumn("Mass").getCellRenderer()).showNumberAndChart(true, 50);

        if (modificationsTable.getRowCount() > 0) {
            modificationsTable.setRowSelectionInterval(0, 0);
        }

        // enable/disable the add/remove ptm buttons
        enableAddRemoveButtons();
    }

    /**
     * Enable/disable the add/remove PTM buttons.
     */
    private void enableAddRemoveButtons() {
        removeVariableModification.setEnabled(variableModsTable.getSelectedRow() != -1);
        addVariableModification.setEnabled(modificationsTable.getSelectedRow() != -1);
        removeFixedModification.setEnabled(fixedModsTable.getSelectedRow() != -1);
        addFixedModification.setEnabled(modificationsTable.getSelectedRow() != -1);
    }

    @Override
    public void updateModifications() {
        updateModificationList();
    }
}
