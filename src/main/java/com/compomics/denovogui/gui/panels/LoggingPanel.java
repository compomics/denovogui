package com.compomics.denovogui.gui.panels;

import java.awt.Point;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;

/**
 *
 * @author Thilo Muth
 */
public class LoggingPanel extends javax.swing.JPanel {

    private static final int MAXLINES = 1000;

    /**
     * Creates a new LoggingPanel.
     */
    public LoggingPanel() {
        initComponents();
    }

    public void append(String str) {
        // append string
        loggingPanelTextArea.append(str + System.getProperty("line.separator"));

        // check line count
        int lines = loggingPanelTextArea.getLineCount();
        if (lines > MAXLINES) {
            try {
                loggingPanelTextArea.getDocument().remove(0, lines - MAXLINES);
            } catch (BadLocationException exception) {
                exception.printStackTrace();
            }
        }

        // scroll down
        Point point = new Point(0, loggingPanelTextArea.getSize().height);
        JViewport port = loggingPanelScrollPane.getViewport();
        port.setViewPosition(point);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        loggingPanelScrollPane = new javax.swing.JScrollPane();
        loggingPanelTextArea = new javax.swing.JTextArea();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Logging"));
        setOpaque(false);

        loggingPanelTextArea.setColumns(20);
        loggingPanelTextArea.setRows(5);
        loggingPanelScrollPane.setViewportView(loggingPanelTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loggingPanelScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 629, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loggingPanelScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane loggingPanelScrollPane;
    private javax.swing.JTextArea loggingPanelTextArea;
    // End of variables declaration//GEN-END:variables
}
