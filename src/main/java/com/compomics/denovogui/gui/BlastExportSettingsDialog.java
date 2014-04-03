package com.compomics.denovogui.gui;

import javax.swing.JOptionPane;

/**
 * Dialog for setting the BLAST export settings.
 *
 * @author Harald Barsnes
 */
public class BlastExportSettingsDialog extends javax.swing.JDialog {

    /**
     * The maximum number of hits to export per spectrum.
     */
    private int numberOfHits = 5;
    /**
     * The minimum score requited for a PSM to be exported.
     */
    private double threshold = 0;
    /**
     * True of the dialog was canceled by the user.
     */
    private boolean canceled = true;

    /**
     * Creates a new BlastExportSettingsDialog.
     *
     * @param parent the parent
     * @param modal
     */
    public BlastExportSettingsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        numberHitsTextField.setText(String.valueOf(numberOfHits));
        scoreThresholdTextField.setText(String.valueOf(threshold));
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Returns the maximum number of hits to export.
     *
     * @return the maximum number of hits to export
     */
    public int getNumberOfPeptides() {
        return numberOfHits;
    }

    /**
     * Returns the minimum score requited for a PSM to be exported.
     *
     * @return the minimum score requited for a PSM to be exported
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Returns true if a greater than threshold is used.
     *
     * @return true if a greater than threshold is used.
     */
    public boolean isGreaterThenThreshold() {
        return greaterThanRadioButton.isSelected();
    }

    /**
     * Returns true of the dialog was canceled by the user.
     *
     * @return true of the dialog was canceled by the user
     */
    public boolean canceled() {
        return canceled;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup = new javax.swing.ButtonGroup();
        backgroundPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        scoreThresholdLabel = new javax.swing.JLabel();
        scoreThresholdTextField = new javax.swing.JTextField();
        numberHitsTextField = new javax.swing.JTextField();
        numberHitsLabel = new javax.swing.JLabel();
        thresholdLabel = new javax.swing.JLabel();
        greaterThanRadioButton = new javax.swing.JRadioButton();
        lessThanRadioButton = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BLAST Export Settings");
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));
        jPanel1.setOpaque(false);

        scoreThresholdLabel.setText("De Novo Score Threshold");
        scoreThresholdLabel.setToolTipText("The min score for including a spectrum in the export");

        scoreThresholdTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        numberHitsTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        numberHitsLabel.setText("#Matches per Spectrum");
        numberHitsLabel.setToolTipText("Max number of matches to export per spectrum");

        thresholdLabel.setText("Score Threshold Type");

        buttonGroup.add(greaterThanRadioButton);
        greaterThanRadioButton.setSelected(true);
        greaterThanRadioButton.setText(">=");
        greaterThanRadioButton.setIconTextGap(10);
        greaterThanRadioButton.setOpaque(false);

        buttonGroup.add(lessThanRadioButton);
        lessThanRadioButton.setText("<=");
        lessThanRadioButton.setIconTextGap(10);
        lessThanRadioButton.setOpaque(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scoreThresholdLabel)
                    .addComponent(numberHitsLabel)
                    .addComponent(thresholdLabel))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(scoreThresholdTextField, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(greaterThanRadioButton)
                        .addGap(18, 18, 18)
                        .addComponent(lessThanRadioButton))
                    .addComponent(numberHitsTextField))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(numberHitsLabel)
                    .addComponent(numberHitsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scoreThresholdLabel)
                    .addComponent(scoreThresholdTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(thresholdLabel)
                    .addComponent(greaterThanRadioButton)
                    .addComponent(lessThanRadioButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
     * Validate the input and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        boolean valid = true;

        try {
            numberOfHits = new Integer(numberHitsTextField.getText());

            if (numberOfHits < 1) {
                JOptionPane.showMessageDialog(this, "The number of hits per spectrum has to be a positive integer!", "Input Error", JOptionPane.WARNING_MESSAGE);
                valid = false;
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "The number of hits per spectrum has to be an integer!", "Input Error", JOptionPane.WARNING_MESSAGE);
            valid = false;
        }

        if (valid) {
            try {
                threshold = new Double(scoreThresholdTextField.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "The score threshold has to be a number!", "Input Error", JOptionPane.WARNING_MESSAGE);
                valid = false;
            }
        }

        // close dialog if input is valid
        if (valid) {
            canceled = false;
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Cancel the dialog.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton greaterThanRadioButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton lessThanRadioButton;
    private javax.swing.JLabel numberHitsLabel;
    private javax.swing.JTextField numberHitsTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel scoreThresholdLabel;
    private javax.swing.JTextField scoreThresholdTextField;
    private javax.swing.JLabel thresholdLabel;
    // End of variables declaration//GEN-END:variables
}
