/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.oiexplorer.core.model.PlotDefinition;
import fr.jmmc.oiexplorer.core.model.PlotDefinitionFactory;
import fr.jmmc.oiexplorer.core.model.TargetUID;
import fr.jmmc.oitools.meta.ColumnMeta;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Panel allow to select data to plot and (optionnaly in the future) plots them just below.
 * @author mella
 */
public class PlotPanelEditor extends javax.swing.JPanel implements ActionListener {

    /* members */
    /** Logger */
    final private Logger logger = LoggerFactory.getLogger(PlotPanelEditor.class);
    /** Associated plot definition */
    PlotDefinition plotDefinition;
    private final List<String> xAxisChoices = new LinkedList<String>();
    private final GenericListModel<String> xAxisListModel = new GenericListModel<String>(xAxisChoices, true);
    private final List<String> yAxisChoices = new LinkedList<String>();
    private final GenericListModel<String> yAxisListModel = new GenericListModel<String>(yAxisChoices, true);
    private final List<JComboBox<String>> yComboBoxes = new LinkedList<JComboBox<String>>();
    private final List<String> plotTypeChoices = new LinkedList<String>();
    private final GenericListModel<String> plotTypeListModel = new GenericListModel<String>(plotTypeChoices, true);
    private TargetUID target = null;
    private OIFitsFile oiFitsFile = null;
    private final String customLabel = "Custom...";
    private Vis2Panel plotPanel;

    /** Creates new form PlotPanelEditor */
    // TODO add one new constructor not to always add a plotPanel at the bottom
    public PlotPanelEditor() {
        initComponents();
        
        // Vis2Panel
        plotPanel = new Vis2Panel();
        setPlotPanel(plotPanel);

        // Comboboxes
        xAxisComboBox.setModel(xAxisListModel);
        plotTypeChoices.addAll(PlotDefinitionFactory.getInstance().getDefaultList());
        plotTypeChoices.add(customLabel);
        plotTypeComboBox.setModel(plotTypeListModel);
        plotTypeComboBox.setSelectedIndex(0);
        plotTypeComboBox.addActionListener(this);


        // init default state of widgets that also initialize the plotDefinition
        actionPerformed(null);
        // fill first y axis combobox
        addYAxisButtonActionPerformed(null);
    }

    public void setPlotPanel(Vis2Panel plotPanel) {
        plotAreaPanel.removeAll();
        plotAreaPanel.add(plotPanel);
        plotPanel.setPlotDefinitionEditor(this);
        this.plotPanel = plotPanel;
    }
    
    public Vis2Panel getPlotPanel() {
        return this.plotPanel;
    }

    /**
     * Update combo and widget according to the content of given OIFits data.
     * and request the plotPanel to be updated if present.
     * @param target target id 
     * @param oiFitsFile OIFits structure
     */
    public void updateOIFits(final TargetUID target, final OIFitsFile oiFitsFile) {
        // if inputs are valid
        if (target != null && oiFitsFile != null) {
            logger.warn("updateDataToPlot() requested for selection on '{}' target ", target);

            /* store references of data to plot */
            this.target = target;
            this.oiFitsFile = oiFitsFile;

            /* fill combobox for available columns */
            fillDataSelectors();
        }
        // initialize plotDef and update plotPanel
        actionPerformed(null);
    }

    /** get the associated PlotDefinition */
    public PlotDefinition getPlotDefinition() {
        return plotDefinition;
    }
    
    /** Initialize the plotDefinition with user adjusted widgets. */
    private void defineCustomPlotDefinition() {
        if(plotDefinition==null){
            plotDefinition = new PlotDefinition();
        }
        plotDefinition.setxAxis(getxAxis());
        plotDefinition.setyAxes(getyAxes());
        logger.warn("New Plot Definition is : {}", plotDefinition);
    }

    /**
     * Fill axes comboboxes with all distinct columns present in the available
     * tables.
     */
    private void fillDataSelectors() {
        // Get whole available columns
        final Set<String> columns = getDistinctColumns(oiFitsFile);

        // Clear all content
        xAxisChoices.clear();
        yAxisChoices.clear();

        xAxisChoices.addAll(columns);
        yAxisChoices.addAll(columns);

        // TODO restore previously selected
        xAxisComboBox.setSelectedIndex(0);
        for (int i = 0; i < yComboBoxes.size(); i++) {
            yComboBoxes.get(i).setSelectedIndex(0);
        }
    }

    /**
     * Fill Y axes comboboxes with all distinct columns present in the available
     * tables compatibles with the selected x axis.
     */
    private void fillyDataSelectors() {
        // Get whole available columns compatible with selected x
        final Set<String> columns = getDistinctColumns(oiFitsFile, getxAxis());

        // Clear all content       
        yAxisChoices.clear();

        yAxisChoices.addAll(columns);

        // TODO restore previously selected
        for (int i = 0; i < yComboBoxes.size(); i++) {
            yComboBoxes.get(i).setSelectedIndex(0);
        }
    }

    private String getxAxis() {
        return (String) xAxisComboBox.getSelectedItem();
    }

    private List<String> getyAxes() {
        List<String> l = new LinkedList<String>();
        for (int i = 0; i < yComboBoxes.size(); i++) {
            l.add((String)yComboBoxes.get(i).getSelectedItem());
        }
        return l;
    }

    /** Return the set of distinct columns available in the table of given OIFitsFile.
     * @param oiFitsFile oifitsFile to search data into
     * @return a Set of Strings with every distinct column names
     */
    private Set<String> getDistinctColumns(final OIFitsFile oiFitsFile) {
        final Set<String> columns = new LinkedHashSet<String>();


        // Add every column of every tables for given target into combomodel sets
        // TODO optimization could be operated walking only on the first element
        for (OITable oiTable : oiFitsFile.getOiVis2()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        for (OITable oiTable : oiFitsFile.getOiVis()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        for (OITable oiTable : oiFitsFile.getOiT3()) {
            oiTable.getNumericalColumnsNames(columns);
        }
        return columns;
    }

    /** Return the set of distinct columns from the tables of given 
     * OIFitsFile and compatible with given column.
     * @param oiFitsFile oifitsFile to search data into
     * @return a Set of Strings with every distinct column names
     */
    private Set<String> getDistinctColumns(final OIFitsFile oiFitsFile, String columnName) {
        final Set<String> columns = new LinkedHashSet<String>();

        // TODO see previous getDistinctColumns() for perf note
        // Add every column of every tables for given target into combomodel sets
        for (OITable oiTable : oiFitsFile.getOiVis2()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiVis()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiT3()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.warn("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        return columns;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        plotTypeLabel = new javax.swing.JLabel();
        plotTypeComboBox = new javax.swing.JComboBox();
        yLabel = new javax.swing.JLabel();
        xLabel = new javax.swing.JLabel();
        xAxisComboBox = new javax.swing.JComboBox();
        addYAxisButton = new javax.swing.JButton();
        delYAxisButton = new javax.swing.JButton();
        plotAreaPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        yComboBoxesPanel = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        plotTypeLabel.setText("Plot type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(plotTypeLabel, gridBagConstraints);

        plotTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(plotTypeComboBox, gridBagConstraints);

        yLabel.setText("yAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        add(yLabel, gridBagConstraints);

        xLabel.setText("xAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        add(xLabel, gridBagConstraints);

        xAxisComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        xAxisComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xAxisComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        add(xAxisComboBox, gridBagConstraints);

        addYAxisButton.setText("+");
        addYAxisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addYAxisButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        add(addYAxisButton, gridBagConstraints);

        delYAxisButton.setText("-");
        delYAxisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delYAxisButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        add(delYAxisButton, gridBagConstraints);

        plotAreaPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(plotAreaPanel, gridBagConstraints);

        yComboBoxesPanel.setBorder(null);
        yComboBoxesPanel.setLayout(new javax.swing.BoxLayout(yComboBoxesPanel, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(yComboBoxesPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(jScrollPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void xAxisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xAxisComboBoxActionPerformed
        // Reduce (Update) y combo list with compatible selection
        fillyDataSelectors();
        actionPerformed(null);
    }//GEN-LAST:event_xAxisComboBoxActionPerformed

    private void addYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addYAxisButtonActionPerformed
        final JComboBox<String> ycombo = new JComboBox<String>();

        final GenericListModel<String> yListModel = new GenericListModel<String>(yAxisChoices, true);
        ycombo.setModel(yListModel);

        yComboBoxes.add(ycombo);
        yComboBoxesPanel.add(ycombo);
        ycombo.addActionListener(this);  
        
        revalidate();        
    }//GEN-LAST:event_addYAxisButtonActionPerformed

    
    private void delYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delYAxisButtonActionPerformed
        if (yComboBoxes.size() > 1) {
            // TODO replace by removal of the last yCombobox which one has lost the foxus
            final JComboBox<String> ycombo = yComboBoxes.remove(yComboBoxes.size() - 1);
            ycombo.removeActionListener(this);
            yComboBoxesPanel.remove(ycombo);
            revalidate();        
        }
    }//GEN-LAST:event_delYAxisButtonActionPerformed

    public void actionPerformed(ActionEvent e) {

        final String selectedType = (String) plotTypeComboBox.getSelectedItem();
        final boolean useCustom = (selectedType == customLabel);

        /* TODO move following lines */
        xAxisComboBox.setEnabled(useCustom);
        for (int i = 0; i < yComboBoxes.size(); i++) {
            yComboBoxes.get(i).setEnabled(useCustom);
        }
        xLabel.setEnabled(useCustom);
        yLabel.setEnabled(useCustom);

        if (target == null || oiFitsFile == null) {
            return;
        }

        if (useCustom) {
            defineCustomPlotDefinition();
        } else {
            plotDefinition = PlotDefinitionFactory.getInstance().getDefault(selectedType);
        }

        if (plotPanel != null) {
            plotPanel.plot(target, oiFitsFile);
        }
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addYAxisButton;
    private javax.swing.JButton delYAxisButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel plotAreaPanel;
    private javax.swing.JComboBox plotTypeComboBox;
    private javax.swing.JLabel plotTypeLabel;
    private javax.swing.JComboBox xAxisComboBox;
    private javax.swing.JLabel xLabel;
    private javax.swing.JPanel yComboBoxesPanel;
    private javax.swing.JLabel yLabel;
    // End of variables declaration//GEN-END:variables
    
}
