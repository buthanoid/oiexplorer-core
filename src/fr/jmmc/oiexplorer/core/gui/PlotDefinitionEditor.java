/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.event.GenericEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.event.PlotDefinitionEvent;
import fr.jmmc.oiexplorer.core.model.event.PlotEvent;
import fr.jmmc.oiexplorer.core.model.event.SubsetDefinitionEvent;
import fr.jmmc.oiexplorer.core.model.oi.Plot;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import fr.jmmc.oiexplorer.core.model.plot.Axis;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oitools.meta.ColumnMeta;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
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
 * After being created and inserted in a GUI, it becomes editor after being linked to a plotDefinition through setPlotDefId().
 * It can also be editor for the plotDefinition of a particular Plot using setPlotId(). In the Plot case, 
 * the subset is also watched to find available columns to plot.
 * 
 * @author mella
 */
public class PlotDefinitionEditor extends javax.swing.JPanel implements ActionListener,
        OIFitsCollectionEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(PlotDefinitionEditor.class);

    /* members */
    /** OIFitsCollectionManager singleton */
    private OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** plot identifier (may leave null if we do not edit the plot def of a plot)*/
    private String plotId = null;
    /** plot (read-only reference) */
    private Plot plot = null;
    /** subset identifier (may leave null if we do not edit the plot def of a plot)*/
    private String subsetId = null;
    /** subset definition (read-only reference) */
    private SubsetDefinition subsetDefinition = null;
    /** plot definition identifier */
    private String plotDefId = null;
    /** Main plot definition */
    private PlotDefinition plotDefinition = null;
    /* Swing components */
    /** Store all choices available to plot on x axis given to current data to plot */
    private final List<String> xAxisChoices = new LinkedList<String>();
    /** Store all choices available to plot on y axes given to current data to plot */
    private final List<String> yAxisChoices = new LinkedList<String>();
    /** List of comboboxes associated to y axes */
    private final List<JComboBox> yComboBoxes = new LinkedList<JComboBox>();
    /** last selected value on x axis */
    private String lastXComboBoxValue = null;
    /** list of last selected value on y axes */
    private List<String> lastYComboBoxesValues = new LinkedList<String>();
    /** Common listener for y comboboxes */
    private ActionListener ycomboActionListener;
    /** Flag to declare that component is in initing */
    private boolean initStep;

    /** Creates new form PlotDefinitionEditor */
    public PlotDefinitionEditor() {
        // TODO maybe move it in setPlotId, setPlotId to register to te proper eventnotifiers instead of all
        ocm.getPlotDefinitionEventNotifier().register(this);
        ocm.getSubsetDefinitionEventNotifier().register(this);
        ocm.getPlotEventNotifier().register(this);

        initComponents();
        postInit();
    }

    /**
     * This method is useful to set the models and specific features of initialized swing components :
     */
    private void postInit() {
        // Comboboxes
        xAxisComboBox.setModel(new GenericListModel<String>(xAxisChoices, true));

        // Prepare a common listener to group handling in yAxisComboBoxActionPerformed()
        ycomboActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                yAxisComboBoxActionPerformed(e);
            }
        };
    }

    /** 
     * Init the plotDefinition with user adjusted widgets. 
     */
    private void updateCustomPlotDefinition() {
        // handle xAxis
        final Axis xAxis = new Axis();
        xAxis.setName(getxAxis());
        getPlotDefinition().setXAxis(xAxis);

        // handle yAxes
        final List<Axis> yAxes = getPlotDefinition().getYAxes();
        yAxes.clear();
        for (String yname : getyAxes()) {
            final Axis yAxis = new Axis();
            yAxis.setName(yname);
            yAxes.add(yAxis);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Setting custom plot definition x: {}, y : {}", getxAxis(), getyAxes());
        }
    }

    /**
     * Fill axes comboboxes with all distinct columns present in the available
     * tables.
     */
    private void refreshForm() {
        logger.warn("refreshForm : plotDefId={}, subsetId={}", plotDefId, subsetId);
        logger.warn("refreshForm : plotDef={}", getPlotDefinition());

        try {
            // Leave programatic changes on widgets ignored to prevent model changes 
            initStep = false;

            // Clear all content
            xAxisChoices.clear();
            yAxisChoices.clear();

            // remove list yAxis
            JComboBox[] yCombos = yComboBoxes.toArray(new JComboBox[]{});
            for (JComboBox yCombo : yCombos) {
                delYCombo(yCombo);
            }

            // At present time on plotDef is required to work : if it is null then return and leave in reset state
            if (getPlotDefinition() == null) {
                return;
            }

            // Add column present in associated subset if any
            // TODO generate one synthetic OiFitsSubset to give all available choices
            if (subsetId != null && getOiFitsSubset() != null) {
                // Get whole available columns
                final Set<String> columns = getDistinctColumns(getOiFitsSubset());

                xAxisChoices.addAll(columns);
                yAxisChoices.addAll(columns);
            }
            // Add choices present in the associated plotDef
            final String currentX = getPlotDefinition().getXAxis().getName();
            if (!xAxisChoices.contains(currentX)) {
                xAxisChoices.add(currentX);
            }

            for (Axis y : getPlotDefinition().getYAxes()) {
                final String currentY = y.getName();
                if (!yAxisChoices.contains(currentY)) {
                    yAxisChoices.add(currentY);
                }
            }

            logger.warn("refreshForm : xAxisChoices {}, yAxisChoices {}", xAxisChoices, yAxisChoices);

            if (!xAxisChoices.isEmpty()) {
                // Use label of associated plotdefinition if any, else try old value and finally use first by default
                if (getPlotDefinition().getXAxis() != null && getPlotDefinition().getXAxis().getName() != null) {
                    xAxisComboBox.setSelectedItem(getPlotDefinition().getXAxis().getName());
                } else if (lastXComboBoxValue != null && xAxisChoices.contains(lastXComboBoxValue)) {
                    xAxisComboBox.setSelectedItem(lastXComboBoxValue);
                } else {
                    xAxisComboBox.setSelectedIndex(0);
                }
            }

            if (!yAxisChoices.isEmpty()) {
                if (getPlotDefinition().getYAxes().isEmpty()) {
                    logger.warn("refreshForm : no yaxes to copy");
                    for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
                        if (lastYComboBoxesValues.size() > i && yAxisChoices.contains(lastYComboBoxesValues.get(i))) {
                            yComboBoxes.get(i).setSelectedItem(lastYComboBoxesValues.get(i));
                        } else {
                            yComboBoxes.get(i).setSelectedIndex(0);
                        }
                    }
                } else {
                    // fill with associated plotdefinition            
                    logger.warn("refreshForm : yaxes to add : {}", getPlotDefinition().getYAxes());
                    for (Axis yAxis : getPlotDefinition().getYAxes()) {
                        addYCombo(yAxis.getName());
                    }
                }
            }
        } finally {
            initStep = true;
        }
    }

    /**
     * Fill Y axes comboboxes with all distinct columns present in the available
     * tables compatibles with the selected x axis.
     */
    private void fillyDataSelectors() {
        // Clear all content       
        yAxisChoices.clear();

        if (getOiFitsSubset() != null) {
            // Get whole available columns compatible with selected x
            final Set<String> columns = getDistinctColumns(getOiFitsSubset(), getxAxis());

            yAxisChoices.addAll(columns);
        }

        if (!yAxisChoices.isEmpty()) {
            for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
                if (lastYComboBoxesValues.size() > i && yAxisChoices.contains(lastYComboBoxesValues.get(i))) {
                    yComboBoxes.get(i).setSelectedItem(lastYComboBoxesValues.get(i));
                } else {
                    yComboBoxes.get(i).setSelectedIndex(0);
                }
            }
        }
    }

    private String getxAxis() {
        lastXComboBoxValue = (String) xAxisComboBox.getSelectedItem();
        return lastXComboBoxValue;
    }

    private List<String> getyAxes() {
        lastYComboBoxesValues.clear();

        for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
            lastYComboBoxesValues.add((String) yComboBoxes.get(i).getSelectedItem());
        }
        return lastYComboBoxesValues;
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

        yLabel = new javax.swing.JLabel();
        xLabel = new javax.swing.JLabel();
        xAxisComboBox = new javax.swing.JComboBox();
        addYAxisButton = new javax.swing.JButton();
        delYAxisButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        yComboBoxesPanel = new javax.swing.JPanel();
        plotDefinitionName = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        yLabel.setText("yAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        add(yLabel, gridBagConstraints);

        xLabel.setText("xAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        add(xLabel, gridBagConstraints);

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

        yComboBoxesPanel.setLayout(new javax.swing.BoxLayout(yComboBoxesPanel, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(yComboBoxesPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(jScrollPane1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        add(plotDefinitionName, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void xAxisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xAxisComboBoxActionPerformed
        // TODO Reduce (Update) y combo list with compatible selection
        // fillyDataSelectors();
        actionPerformed(null);
    }//GEN-LAST:event_xAxisComboBoxActionPerformed

    private void addYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addYAxisButtonActionPerformed
        addYCombo(null);
    }//GEN-LAST:event_addYAxisButtonActionPerformed

    private void delYAxisButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delYAxisButtonActionPerformed
        if (yComboBoxes.size() > 1) {
            JComboBox ycombo = yComboBoxes.get(yComboBoxes.size() - 1);
            // TODO replace by removal of the last yCombobox which one has lost the foxus
            delYCombo(ycombo);
        }
    }//GEN-LAST:event_delYAxisButtonActionPerformed

    private void yAxisComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
        actionPerformed(null);
    }

    /** Create a new combobox and update GUI.
     * @param selectedValue string to be selected (null clears selection)
     */
    private void addYCombo(final String selectedValue) {
        final JComboBox ycombo = new JComboBox(new GenericListModel<String>(yAxisChoices, true));

        yComboBoxes.add(ycombo);
        yComboBoxesPanel.add(ycombo);
        ycombo.addActionListener(ycomboActionListener);

        // select entry if any
        ycombo.setSelectedItem(selectedValue);

        revalidate();
    }

    /** Synchronize management for the addition of a given combo and update GUI. 
     * @param ycombo ComboBox to remove
     */
    private void delYCombo(final JComboBox ycombo) {
        yComboBoxes.remove(ycombo);
        ycombo.removeActionListener(ycomboActionListener);
        yComboBoxesPanel.remove(ycombo);
        revalidate();
        actionPerformed(null);
    }

    //TODO rename
    public void actionPerformed(ActionEvent e) {
        if (getPlotDefinition() != null && initStep) {
            updateCustomPlotDefinition();
            logger.warn("Using custom plot {}", getPlotDefinition());
            ocm.updatePlotDefinition(this, getPlotDefinition());
        }
    }

    /* --- OIFitsCollectionEventListener implementation --- */
    /**
     * Return the optional subject id i.e. related object id that this listener accepts
     * @see GenericEvent#subjectId
     * @param type event type
     * @return subject id i.e. related object id (null allowed)
     */
    public String getSubjectId(final OIFitsCollectionEventType type) {
        switch (type) {
            case PLOT_CHANGED:
                return (this.plotId != null) ? this.plotId : "TO_IGNORE";
            case SUBSET_CHANGED:
                return (this.subsetId != null) ? this.subsetId : "TO_IGNORE";
            default:
        }
        return null;
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final GenericEvent<OIFitsCollectionEventType> event) {
        logger.warn("Received event to process {}", event);

        switch (event.getType()) {
            case PLOT_DEFINITION_CHANGED:
                setPlotDefId(((PlotDefinitionEvent) event).getPlotDefinition().getName());
                break;
            case SUBSET_CHANGED:
                setSubsetId(((SubsetDefinitionEvent) event).getSubsetDefinition().getName());
                break;
            case PLOT_CHANGED:
                setPlotId(((PlotEvent) event).getPlot().getName());
                break;
            default:
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addYAxisButton;
    private javax.swing.JButton delYAxisButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel plotDefinitionName;
    private javax.swing.JComboBox xAxisComboBox;
    private javax.swing.JLabel xLabel;
    private javax.swing.JPanel yComboBoxesPanel;
    private javax.swing.JLabel yLabel;
    // End of variables declaration//GEN-END:variables

    private OIFitsFile getOiFitsSubset() {
        if (getSubsetDefinition() == null) {
            return null;
        }
        return getSubsetDefinition().getOIFitsSubset();
    }

    /**
     * Return the plot definition given its plot name
     * @return Plot object
     */
    private Plot getPlot() {
        if (this.plot == null) {
            // get reference:
            this.plot = ocm.getPlotRef(this.plotId);
        }
        return this.plot;
    }

    /**
     * Define the plot identifier and reset plot
     * @param plotId plot identifier or null to reset state
     */
    public void setPlotId(final String plotId) {
        logger.warn("Requested to look for plot {}", plotId);

        this.plotId = plotId;
        // force reset:
        this.plot = null;
        
        if(plotId != null){
        // define id for data to find columns into
        setSubsetId(getPlot().getSubsetDefinition().getName());
        // define id of associated plotDefinition (that calls refreshForm)
        setPlotDefId(getPlot().getPlotDefinition().getName());
        }else{
            setSubsetId(null);
            setPlotDefId(null);
        }
    }

    /**
     * Return the subset definition given its subset name
     * @return subsetDefinition subset definition
     */
    private SubsetDefinition getSubsetDefinition() {
        if (this.subsetDefinition == null) {
            // get reference:
            this.subsetDefinition = ocm.getSubsetDefinitionRef(this.subsetId);
        }
        return this.subsetDefinition;
    }

    /**
     * Define the subset identifier and reset subset
     * @param subsetId subset identifier
     */
    private void setSubsetId(final String subsetId) {
        logger.warn("Requested to look for subset {}", subsetId);

        this.subsetId = subsetId;
        // force reset:
        this.subsetDefinition = null;

    }

    private PlotDefinition getPlotDefinition() {
        if (this.plotDefinition == null && plotDefId != null) {
            this.plotDefinition = ocm.getPlotDefinition(plotDefId);
        }
        return this.plotDefinition;
    }

    /**
     * Define the plot definition identifier and reset plot definition
     * @param plotDefId plot definition identifier
     */
    public void setPlotDefId(final String plotDefId) {
        logger.warn("Requested to look for plot definition {}", plotDefId);
        this.plotDefId = plotDefId;
        // force reset:
        this.plotDefinition = null;

        refreshForm();
    }
}