package com.compomics.denovogui.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.AndromedaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.CometParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.DirecTagParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MsAmandaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MsgfParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.MyriMatchParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.OmssaParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PNovoParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.PepnovoParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.TideParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.XtandemParameters;
import com.compomics.util.gui.GuiUtilities;
import javax.swing.SwingConstants;

/**
 * Dialog for editing pNovo advanced settings. ¨
 *
 * @author Harald Barsnes
 */
public class PNovoSettingsDialog extends javax.swing.JDialog {

    /**
     * The DeNovoGUI main frame.
     */
    private DeNovoGUI deNovoGUI;
    /**
     * The search parameters
     */
    private SearchParameters searchParameters;

    /**
     * Creates a new PNovoSettingsDialog.
     *
     * @param deNovoGUI the parent frame
     * @param searchParameters the search parameters
     * @param modal whether the dialog is modal or not
     */
    public PNovoSettingsDialog(DeNovoGUI deNovoGUI, SearchParameters searchParameters, boolean modal) {
        super(deNovoGUI, modal);
        this.deNovoGUI = deNovoGUI;
        this.searchParameters = searchParameters;
        initComponents();
        setUpGUI();
        insertData();
        setLocationRelativeTo(deNovoGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        activationTypeCmb.setRenderer(new com.compomics.util.gui.renderers.AlignedListCellRenderer(SwingConstants.CENTER));
    }

    /**
     * Insert the settings.
     */
    private void insertData() {

        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), new OmssaParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), new XtandemParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), new MsgfParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.comet.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.comet.getIndex(), new CometParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex(), new MsAmandaParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex(), new MyriMatchParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), new PepnovoParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), new DirecTagParameters());
        }
        if (searchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex()) == null) {
            searchParameters.setIdentificationAlgorithmParameter(Advocate.pNovo.getIndex(), new PNovoParameters());
        }

        PNovoParameters pNovoParameters = (PNovoParameters) searchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex());

        minPrecursorMassTextField.setText(String.valueOf(pNovoParameters.getLowerPrecursorMass()));
        maxPrecursorMassTextField.setText(String.valueOf(pNovoParameters.getUpperPrecursorMass()));
        activationTypeCmb.setSelectedItem(pNovoParameters.getActicationType());
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
        pNovoPanel = new javax.swing.JPanel();
        activationTypeLabel = new javax.swing.JLabel();
        minPrecursorMassLabel = new javax.swing.JLabel();
        minPrecursorMassTextField = new javax.swing.JTextField();
        maxPrecursorMassLabel = new javax.swing.JLabel();
        maxPrecursorMassTextField = new javax.swing.JTextField();
        activationTypeCmb = new javax.swing.JComboBox();
        cancelButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("pNovo+ Advanced Settings");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        pNovoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("pNovo+ Settings"));
        pNovoPanel.setOpaque(false);

        activationTypeLabel.setText("Activation Type");

        minPrecursorMassLabel.setText("Min Precusor Mass (Da)");

        minPrecursorMassTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        minPrecursorMassTextField.setText("300");
        minPrecursorMassTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                minPrecursorMassTextFieldKeyReleased(evt);
            }
        });

        maxPrecursorMassLabel.setText("Max Precursor Mass (Da)");

        maxPrecursorMassTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        maxPrecursorMassTextField.setText("5000");
        maxPrecursorMassTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                maxPrecursorMassTextFieldKeyReleased(evt);
            }
        });

        activationTypeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "HCD", "CID", "ETD" }));

        javax.swing.GroupLayout pNovoPanelLayout = new javax.swing.GroupLayout(pNovoPanel);
        pNovoPanel.setLayout(pNovoPanelLayout);
        pNovoPanelLayout.setHorizontalGroup(
            pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pNovoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pNovoPanelLayout.createSequentialGroup()
                        .addComponent(activationTypeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(activationTypeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pNovoPanelLayout.createSequentialGroup()
                        .addComponent(minPrecursorMassLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(minPrecursorMassTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pNovoPanelLayout.createSequentialGroup()
                        .addComponent(maxPrecursorMassLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(maxPrecursorMassTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pNovoPanelLayout.setVerticalGroup(
            pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pNovoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(activationTypeLabel)
                    .addComponent(activationTypeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(minPrecursorMassTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(minPrecursorMassLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pNovoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(maxPrecursorMassLabel)
                    .addComponent(maxPrecursorMassTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        openDialogHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        openDialogHelpJButton.setToolTipText("Open the pNovo+ web page");
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

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(pNovoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(openDialogHelpJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addComponent(pNovoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Validate the settings.
     *
     * @param evt
     */
    private void minPrecursorMassTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_minPrecursorMassTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_minPrecursorMassTextFieldKeyReleased

    /**
     * Validate the settings.
     *
     * @param evt
     */
    private void maxPrecursorMassTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_maxPrecursorMassTextFieldKeyReleased
        validateParametersInput(false);
    }//GEN-LAST:event_maxPrecursorMassTextFieldKeyReleased

    /**
     * Save the settings and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        boolean valid = validateParametersInput(true);

        if (valid) {
            // see if there are changes to the parameters and ask the user if these are to be saved
            if (deNovoGUI.checkSearchParameters(getSearchParametersFromGUI())) {
                dispose();
            } else {
                // canceled by the user
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
     * Open the pNovo+ web page.
     *
     * @param evt
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://pfind.ict.ac.cn/software/pNovo/");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    /**
     * Change the icon into a hand cursor.
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox activationTypeCmb;
    private javax.swing.JLabel activationTypeLabel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel maxPrecursorMassLabel;
    private javax.swing.JTextField maxPrecursorMassTextField;
    private javax.swing.JLabel minPrecursorMassLabel;
    private javax.swing.JTextField minPrecursorMassTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JPanel pNovoPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Inspects the parameters validity.
     *
     * @param showMessage if true an error messages are shown to the users
     * @return a boolean indicating if the parameters are valid
     */
    public boolean validateParametersInput(boolean showMessage) {

        boolean valid = true;

        valid = GuiUtilities.validateIntegerInput(this, minPrecursorMassLabel, minPrecursorMassTextField, "minimum precursor mass", "Minimum Precursor Mass Error", true, showMessage, valid);
        valid = GuiUtilities.validateIntegerInput(this, maxPrecursorMassLabel, maxPrecursorMassTextField, "maximum precursor mass", "Maximum Precursor Mass Error", true, showMessage, valid);

        okButton.setEnabled(valid);
        return valid;
    }

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
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.comet.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.comet.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.tide.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.tide.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.andromeda.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()));
        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pNovo.getIndex(), searchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex()));

        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.omssa.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.omssa.getIndex(), new OmssaParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.xtandem.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.xtandem.getIndex(), new XtandemParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.msgf.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.msgf.getIndex(), new MsgfParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.msAmanda.getIndex(), new MsAmandaParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.myriMatch.getIndex(), new MyriMatchParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.comet.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.comet.getIndex(), new CometParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.tide.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.tide.getIndex(), new TideParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.andromeda.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.andromeda.getIndex(), new AndromedaParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pepnovo.getIndex(), new PepnovoParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.direcTag.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), new DirecTagParameters());
        }
        if (tempSearchParameters.getIdentificationAlgorithmParameter(Advocate.pNovo.getIndex()) == null) {
            tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pNovo.getIndex(), new PNovoParameters());
        }

        tempSearchParameters.setEnzyme(searchParameters.getEnzyme());
        tempSearchParameters.setParametersFile(searchParameters.getParametersFile());
        tempSearchParameters.setFragmentIonAccuracy(searchParameters.getFragmentIonAccuracy());
        tempSearchParameters.setFragmentAccuracyType(searchParameters.getFragmentAccuracyType());
        tempSearchParameters.setPrecursorAccuracy(searchParameters.getPrecursorAccuracy());
        tempSearchParameters.setPrecursorAccuracyType(searchParameters.getPrecursorAccuracyType());
        tempSearchParameters.setModificationProfile(searchParameters.getModificationProfile());

        PNovoParameters pNovoParameters = new PNovoParameters();
        pNovoParameters.setLowerPrecursorMass(Integer.parseInt(minPrecursorMassTextField.getText()));
        pNovoParameters.setUpperPrecursorMass(Integer.parseInt(maxPrecursorMassTextField.getText()));
        pNovoParameters.setActicationType((String) activationTypeCmb.getSelectedItem());

        tempSearchParameters.setIdentificationAlgorithmParameter(Advocate.pNovo.getIndex(), pNovoParameters);

        return tempSearchParameters;
    }
}
