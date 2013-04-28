package com.compomics.denovogui.gui;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.preferences.ModificationProfile;
import java.awt.Color;
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
public class SettingsDialog extends javax.swing.JDialog {

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

        if (setVisible) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setVisible(true);
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
        enzymeComboBox.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));
        modelComboBox.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));

        fixedModsJScrollPane.getViewport().setOpaque(false);
        variableModsJScrollPane.getViewport().setOpaque(false);
        modificationsJScrollPane.getViewport().setOpaque(false);

        fixedModsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());
        variableModsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());
        modificationsTable.getColumn(" ").setCellRenderer(new JSparklinesColorTableCellRenderer());

        fixedModsTable.getColumn(" ").setMaxWidth(35);
        fixedModsTable.getColumn(" ").setMinWidth(35);
        variableModsTable.getColumn(" ").setMaxWidth(35);
        variableModsTable.getColumn(" ").setMinWidth(35);
        modificationsTable.getColumn(" ").setMaxWidth(35);
        modificationsTable.getColumn(" ").setMinWidth(35);
        modificationsTable.getColumn("  ").setMaxWidth(30);
        modificationsTable.getColumn("  ").setMinWidth(30);

        fixedModsTable.getColumn("Mass").setMaxWidth(100);
        fixedModsTable.getColumn("Mass").setMinWidth(100);
        variableModsTable.getColumn("Mass").setMaxWidth(100);
        variableModsTable.getColumn("Mass").setMinWidth(100);
        modificationsTable.getColumn("Mass").setMaxWidth(100);
        modificationsTable.getColumn("Mass").setMinWidth(100);

        modificationTableToolTips = new ArrayList<String>();
        modificationTableToolTips.add(null);
        modificationTableToolTips.add("Modification Name");
        modificationTableToolTips.add("Modification Mass");
        modificationTableToolTips.add("Default Modification");

        loadModificationsInGUI();

        // make sure that the scroll panes are see-through
        modificationsJScrollPane.getViewport().setOpaque(false);

        modificationsTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Loads the modifications.
     */
    private void loadModificationsInGUI() {
        updateModificationList();
    }

    /**
     * Insert the search parameters into the GUI.
     */
    private void insertData() {

        if (searchParameters.getParametersFile() != null) {
            settingsFileJTextField.setText(searchParameters.getParametersFile().getAbsolutePath());
            parametersFile = searchParameters.getParametersFile();
        }

        if (searchParameters.getEnzyme() == null || searchParameters.getEnzyme().getName().equalsIgnoreCase("Trypsin")) {
            enzymeComboBox.setSelectedItem("TRYPSIN");
        } else if (searchParameters.getEnzyme().getName().equalsIgnoreCase("No enzyme")) {
            enzymeComboBox.setSelectedItem("NON_SPECIFIC");
        } else {
            // enzyme not supported...
            // @TODO: support more enzymes??
        }

        modelComboBox.setSelectedItem("CID_IT_TRYP"); // @TODO: support other models??

        fragmentMassToleranceSpinner.setValue(searchParameters.getFragmentIonAccuracy());
        precursorMassToleranceSpinner.setValue(searchParameters.getPrecursorAccuracy()); // @TODO: type? Da or ppm?

        numberOfSolutionsSpinner.setValue(searchParameters.getHitListLength());

        spectrumChargeCheckBox.setSelected(!searchParameters.isEstimateCharge());
        spectrumPrecursorCheckBox.setSelected(!searchParameters.isCorrectPrecursorMass());
        filterLowQualityCheckBox.setSelected(searchParameters.getDiscardLowQualitySpectra());

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
        searchSettingsPanel = new javax.swing.JPanel();
        enzymeLabel = new javax.swing.JLabel();
        modelComboBox = new javax.swing.JComboBox();
        modelLabel = new javax.swing.JLabel();
        enzymeComboBox = new javax.swing.JComboBox();
        fragmentMassToleranceLabel = new javax.swing.JLabel();
        fragmentMassToleranceSpinner = new javax.swing.JSpinner();
        precursorMassToleranceLabel = new javax.swing.JLabel();
        precursorMassToleranceSpinner = new javax.swing.JSpinner();
        numberOfSolutionsLabel = new javax.swing.JLabel();
        numberOfSolutionsSpinner = new javax.swing.JSpinner();
        spectrumChargeCheckBox = new javax.swing.JCheckBox();
        spectrumPrecursorCheckBox = new javax.swing.JCheckBox();
        filterLowQualityCheckBox = new javax.swing.JCheckBox();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        configurationFilePanelSettings = new javax.swing.JPanel();
        configurationFileLbl2 = new javax.swing.JLabel();
        browseConfigurationButton2 = new javax.swing.JButton();
        settingsFileJTextField = new javax.swing.JTextField();
        saveConfiguration = new javax.swing.JButton();
        saveConfigurationAs = new javax.swing.JButton();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Search Settings");
        setMinimumSize(new java.awt.Dimension(800, 600));

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        searchSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Settings"));
        searchSettingsPanel.setOpaque(false);

        enzymeLabel.setText("Enzyme");

        modelComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "CID_IT_TRYP" }));

        modelLabel.setText("Model");

        enzymeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TRYPSIN", "NON_SPECIFIC" }));

        fragmentMassToleranceLabel.setText("Fragment Mass Tolerance");

        fragmentMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(0.5d, 0.0d, 2.0d, 0.1d));

        precursorMassToleranceLabel.setText("Precursor Mass Tolerance");

        precursorMassToleranceSpinner.setModel(new javax.swing.SpinnerNumberModel(1.0d, 0.0d, 2.0d, 0.01d));

        numberOfSolutionsLabel.setText("No. Solutions (max. 20)");

        numberOfSolutionsSpinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, 20, 1));

        spectrumChargeCheckBox.setText("Use Spectrum Charge (No Correction)");
        spectrumChargeCheckBox.setIconTextGap(15);
        spectrumChargeCheckBox.setOpaque(false);

        spectrumPrecursorCheckBox.setText("Use Spectrum Precursor m/z (No Correction)");
        spectrumPrecursorCheckBox.setIconTextGap(15);
        spectrumPrecursorCheckBox.setOpaque(false);

        filterLowQualityCheckBox.setText("Filter Low Quality Spectra");
        filterLowQualityCheckBox.setIconTextGap(15);
        filterLowQualityCheckBox.setOpaque(false);

        javax.swing.GroupLayout searchSettingsPanelLayout = new javax.swing.GroupLayout(searchSettingsPanel);
        searchSettingsPanel.setLayout(searchSettingsPanelLayout);
        searchSettingsPanelLayout.setHorizontalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(precursorMassToleranceLabel)
                            .addComponent(fragmentMassToleranceLabel)
                            .addComponent(enzymeLabel))
                        .addGap(18, 18, 18))
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addComponent(numberOfSolutionsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(28, 28, 28)))
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(enzymeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(81, 81, 81)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spectrumChargeCheckBox)
                            .addComponent(spectrumPrecursorCheckBox)
                            .addComponent(filterLowQualityCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                        .addComponent(modelLabel)
                        .addGap(31, 31, 31)
                        .addComponent(modelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        searchSettingsPanelLayout.setVerticalGroup(
            searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enzymeLabel)
                    .addComponent(modelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(modelLabel)
                    .addComponent(enzymeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fragmentMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fragmentMassToleranceLabel)
                    .addComponent(spectrumChargeCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(precursorMassToleranceSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(precursorMassToleranceLabel)
                    .addComponent(spectrumPrecursorCheckBox))
                .addGap(3, 3, 3)
                .addGroup(searchSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberOfSolutionsSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(numberOfSolutionsLabel)
                    .addComponent(filterLowQualityCheckBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        configurationFilePanelSettings.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));
        configurationFilePanelSettings.setOpaque(false);

        configurationFileLbl2.setText("Settings File");

        browseConfigurationButton2.setText("Load");
        browseConfigurationButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseConfigurationButton2ActionPerformed(evt);
            }
        });

        settingsFileJTextField.setEditable(false);

        saveConfiguration.setText("Save");
        saveConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveConfigurationActionPerformed(evt);
            }
        });

        saveConfigurationAs.setText("Save As");
        saveConfigurationAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveConfigurationAsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout configurationFilePanelSettingsLayout = new javax.swing.GroupLayout(configurationFilePanelSettings);
        configurationFilePanelSettings.setLayout(configurationFilePanelSettingsLayout);
        configurationFilePanelSettingsLayout.setHorizontalGroup(
            configurationFilePanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configurationFilePanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(configurationFileLbl2)
                .addGap(18, 18, 18)
                .addComponent(settingsFileJTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseConfigurationButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveConfiguration)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveConfigurationAs, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        configurationFilePanelSettingsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseConfigurationButton2, saveConfiguration, saveConfigurationAs});

        configurationFilePanelSettingsLayout.setVerticalGroup(
            configurationFilePanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(configurationFilePanelSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(configurationFilePanelSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(configurationFileLbl2)
                    .addComponent(settingsFileJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(saveConfigurationAs)
                    .addComponent(saveConfiguration)
                    .addComponent(browseConfigurationButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fixedModsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                fixedModsTableMouseReleased(evt);
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
                        .addComponent(fixedModificationsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
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
                        .addContainerGap(61, Short.MAX_VALUE))
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
            public void mouseExited(java.awt.event.MouseEvent evt) {
                variableModsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                variableModsTableMouseReleased(evt);
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
                .addComponent(variableModsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
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
                        .addContainerGap(62, Short.MAX_VALUE))
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

        javax.swing.GroupLayout availableModsPanelLayout = new javax.swing.GroupLayout(availableModsPanel);
        availableModsPanel.setLayout(availableModsPanelLayout);
        availableModsPanelLayout.setHorizontalGroup(
            availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(modificationsListCombo, 0, 340, Short.MAX_VALUE)
            .addComponent(modificationsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        availableModsPanelLayout.setVerticalGroup(
            availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(availableModsPanelLayout.createSequentialGroup()
                .addComponent(modificationsListCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addComponent(availableModsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        modificationsPanel1Layout.setVerticalGroup(
            modificationsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanel1Layout.createSequentialGroup()
                .addGroup(modificationsPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modificationTypesSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                    .addComponent(availableModsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(configurationFilePanelSettings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(modificationsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(configurationFilePanelSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modificationsPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
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

        SearchParameters tempSearchParameters = getSearchParametersFromGUI();

        if (settingsFileJTextField.getText().length() == 0) {
            tempSearchParameters.setParametersFile(null);
            deNovoGUI.setSearchParameters(tempSearchParameters);
            dispose();
        } else {
            if (!deNovoGUI.getSearchParameters().equals(tempSearchParameters)) {

                int value = JOptionPane.showConfirmDialog(this, "The search parameters have been changed."
                        + "\nDo you want to save the changes?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);

                if (value == JOptionPane.YES_OPTION) {
                    try {
                        tempSearchParameters.setParametersFile(deNovoGUI.getSearchParameters().getParametersFile());
                        SearchParameters.saveIdentificationParameters(tempSearchParameters, deNovoGUI.getSearchParameters().getParametersFile());
                        deNovoGUI.setSearchParameters(tempSearchParameters);
                        dispose();
                    } catch (ClassNotFoundException e) {
                        JOptionPane.showMessageDialog(this, "An error occurred when saving the search parameter:\n"
                                + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "An error occurred when saving the search parameter:\n"
                                + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
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
     * Load search parameters from file.
     *
     * @param evt
     */
    private void browseConfigurationButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseConfigurationButton2ActionPerformed
        // First check whether a file has already been selected.
        // If so, start from that file's parent.
        File startLocation = new File(deNovoGUI.getLastSelectedFolder());
        if (settingsFileJTextField.getText() != null && new File(settingsFileJTextField.getText()).exists()) {
            File temp = new File(settingsFileJTextField.getText());
            startLocation = temp.getParentFile();
        }
        JFileChooser fc = new JFileChooser(startLocation);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".properties")
                        || myFile.getName().toLowerCase().endsWith(".parameters")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "DeNovoGUI search parameters";
            }
        };
        fc.setFileFilter(filter);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            deNovoGUI.setLastSelectedFolder(file.getAbsolutePath());
            try {
                searchParameters = SearchParameters.getIdentificationParameters(file);
                deNovoGUI.loadModifications(searchParameters);
                insertData();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occured while reading " + file + ". Please verify the search paramters.", "File Error", JOptionPane.ERROR_MESSAGE);
            }
            parametersFile = file;
            settingsFileJTextField.setText(parametersFile.getAbsolutePath());
            searchParameters = getSearchParametersFromGUI();
            searchParameters.setParametersFile(parametersFile);
            deNovoGUI.setSearchParameters(searchParameters);
            validateParametersInput(true);
        }
    }//GEN-LAST:event_browseConfigurationButton2ActionPerformed

    /**
     * Save the search parameters.
     *
     * @param evt
     */
    private void saveConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveConfigurationActionPerformed
        savePressed();
    }//GEN-LAST:event_saveConfigurationActionPerformed

    /**
     * Save the search parameters.
     *
     * @param evt
     */
    private void saveConfigurationAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveConfigurationAsActionPerformed
        saveAsPressed();
    }//GEN-LAST:event_saveConfigurationAsActionPerformed

    /**
     * Add a fixed modification.
     * 
     * @param evt 
     */
    private void addFixedModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFixedModificationActionPerformed
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
                    "It is not recommended to use more than 6 variable modifications in the same search.", "Warning", JOptionPane.WARNING_MESSAGE);
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
            } else if (column == modificationsTable.getColumn("  ").getModelIndex() && modificationsTable.getValueAt(row, column) != null) {

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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFixedModification;
    private javax.swing.JButton addVariableModification;
    private javax.swing.JPanel availableModsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton browseConfigurationButton2;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel configurationFileLbl2;
    private javax.swing.JPanel configurationFilePanelSettings;
    private javax.swing.JComboBox enzymeComboBox;
    private javax.swing.JLabel enzymeLabel;
    private javax.swing.JCheckBox filterLowQualityCheckBox;
    private javax.swing.JLabel fixedModificationsLabel;
    private javax.swing.JScrollPane fixedModsJScrollPane;
    private javax.swing.JTable fixedModsTable;
    private javax.swing.JLabel fragmentMassToleranceLabel;
    private javax.swing.JSpinner fragmentMassToleranceSpinner;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JComboBox modelComboBox;
    private javax.swing.JLabel modelLabel;
    private javax.swing.JSplitPane modificationTypesSplitPane;
    private javax.swing.JScrollPane modificationsJScrollPane;
    private javax.swing.JComboBox modificationsListCombo;
    private javax.swing.JPanel modificationsPanel1;
    private javax.swing.JTable modificationsTable;
    private javax.swing.JLabel numberOfSolutionsLabel;
    private javax.swing.JSpinner numberOfSolutionsSpinner;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel precursorMassToleranceLabel;
    private javax.swing.JSpinner precursorMassToleranceSpinner;
    private javax.swing.JButton removeFixedModification;
    private javax.swing.JButton removeVariableModification;
    private javax.swing.JButton saveConfiguration;
    private javax.swing.JButton saveConfigurationAs;
    private javax.swing.JPanel searchSettingsPanel;
    private javax.swing.JTextField settingsFileJTextField;
    private javax.swing.JCheckBox spectrumChargeCheckBox;
    private javax.swing.JCheckBox spectrumPrecursorCheckBox;
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

        // @TODO: implement other enzymes
        Enzyme enzyme;
        if (enzymeComboBox.getSelectedIndex() == 0) {
            enzyme = enzymeFactory.getEnzyme("Trypsin");
        } else {
            enzyme = enzymeFactory.getEnzyme("No enzyme");
        }
        tempSearchParameters.setEnzyme(enzyme);

        // @TODO: implement other models
        String fragmentationModel = (String) modelComboBox.getSelectedItem();
        tempSearchParameters.setFragmentationModel(fragmentationModel);
        double fragmentIonTolerance = (Double) fragmentMassToleranceSpinner.getValue();
        tempSearchParameters.setFragmentIonAccuracy(fragmentIonTolerance);
        double precursorIonTolerance = (Double) precursorMassToleranceSpinner.getValue();
        tempSearchParameters.setPrecursorAccuracy(precursorIonTolerance);
        int maxHitListLength = (Integer) numberOfSolutionsSpinner.getValue();
        tempSearchParameters.setHitListLength(maxHitListLength);
        boolean estimateCharge = !spectrumChargeCheckBox.isSelected();
        tempSearchParameters.setEstimateCharge(estimateCharge);
        boolean estimatePrecursorMass = !spectrumPrecursorCheckBox.isSelected();
        tempSearchParameters.correctPrecursorMass(estimatePrecursorMass);
        boolean filterLowQualitySpectra = filterLowQualityCheckBox.isSelected();
        tempSearchParameters.setDiscardLowQualitySpectra(filterLowQualitySpectra);

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

        // @TODO: do we need any sort of validation here??

        okButton.setEnabled(valid);

        return valid;
    }

    /**
     * This method is called when the user clicks the 'Save' button.
     */
    public void savePressed() {
        if (parametersFile == null) {
            saveAsPressed();
        } else if (validateParametersInput(true)) {
            try {
                searchParameters = getSearchParametersFromGUI();
                SearchParameters.saveIdentificationParameters(searchParameters, parametersFile);
                deNovoGUI.setSearchParameters(searchParameters);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, new String[]{"An error occurred while witing: " + parametersFile.getName(), e.getMessage()}, "Error Saving File", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * This method is called when the user clicks the 'Save As' button.
     */
    public void saveAsPressed() {

        if (validateParametersInput(true)) {

            // First check whether a file has already been selected.
            // If so, start from that file's parent.
            File startLocation = new File(deNovoGUI.getLastSelectedFolder());

            if (settingsFileJTextField.getText() != null && !settingsFileJTextField.getText().trim().equals("")) {
                File temp = new File(settingsFileJTextField.getText());
                startLocation = temp.getParentFile();
            }

            boolean complete = false;

            while (!complete) {
                JFileChooser fc = new JFileChooser(startLocation);
                FileFilter filter = new FileFilter() {
                    @Override
                    public boolean accept(File myFile) {

                        return myFile.getName().toLowerCase().endsWith(".parameters")
                                || myFile.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "DeNovoGUI search parameters";
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
                    return;
                }
            }

            savePressed();
            settingsFileJTextField.setText(parametersFile.getPath());
            searchParameters.setParametersFile(parametersFile);
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

        DefaultTableModel modsModel = (DefaultTableModel) modificationsTable.getModel();
        modsModel.getDataVector().removeAllElements();

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

        modificationsTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
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
}
