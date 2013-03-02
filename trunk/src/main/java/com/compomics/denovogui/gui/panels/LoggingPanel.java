/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.denovogui.gui.panels;

import java.awt.Point;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;

/**
 *
 * @author tmuth
 */
public class LoggingPanel extends javax.swing.JPanel {
    
    private static final int MAXLINES = 1000;
    
    /**
     * Creates new form LoggingPanel
     */
    public LoggingPanel() {
        initComponents();
    }

    public void append(String str) {
        // append string
        jTextArea1.append(str + System.getProperty("line.separator"));

        // check line count
        int lines = jTextArea1.getLineCount();
        if (lines > MAXLINES) {
            try {
                jTextArea1.getDocument().remove(0, lines - MAXLINES);
            } catch (BadLocationException exception) {
                exception.printStackTrace();
            }
        }

        // scroll down
        Point point = new Point(0, jTextArea1.getSize().height);
        JViewport port = jScrollPane1.getViewport();
        port.setViewPosition(point);
    }
  

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Logging"));

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 629, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
