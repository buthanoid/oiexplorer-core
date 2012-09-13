/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEvent;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventListener;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManagerEventType;
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
public final class PlotDefinitionEditor extends javax.swing.JPanel implements ActionListener, OIFitsCollectionManagerEventListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    private final static Logger logger = LoggerFactory.getLogger(PlotDefinitionEditor.class);

    /* members */
    /** OIFitsCollectionManager singleton */
    private final OIFitsCollectionManager ocm = OIFitsCollectionManager.getInstance();
    /** OPTIONAL plot identifier */
    private String plotId = null;
    /** plot definition identifier */
    private String plotDefId = null;
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
        ocm.getPlotDefinitionChangedEventNotifier().register(this);
        ocm.getPlotChangedEventNotifier().register(this);

        initComponents();
        postInit();
    }

    /**
     * Free any ressource or reference to this instance :
     * remove this instance from OIFitsCollectionManager event notifiers
     */
    @Override
    public void dispose() {
        if (logger.isDebugEnabled()) {
            logger.debug("dispose: {}", ObjectUtils.getObjectInfo(this));
        }

        ocm.unbind(this);
    }

    /**
     * This method is useful to set the models and specific features of initialized swing components :
     */
    private void postInit() {
        // Comboboxes
        // TODO: fix that code: invalid as modifying the internal list is forbidden !
        xAxisComboBox.setModel(new GenericListModel<String>(xAxisChoices, true));

        // Prepare a common listener to group handling in yAxisComboBoxActionPerformed()
        ycomboActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                yAxisComboBoxActionPerformed(e);
            }
        };
    }

    /**
     * Fill axes comboboxes with all distinct columns present in the available
     * tables.
     * @param plotDef plot definition to use
     * @param oiFitsSubset OIFits structure coming from plot's subset definition
     */
    private void refreshForm(final PlotDefinition plotDef, final OIFitsFile oiFitsSubset) {
        logger.debug("refreshForm : plotDefId = {} - plotDef {}", plotDefId, plotDef);

        try {
            // Leave programatic changes on widgets ignored to prevent model changes 
            initStep = false;

            // Clear all content
            // TODO: fix that code: invalid as modifying the internal list is forbidden !

            xAxisChoices.clear();
            yAxisChoices.clear();

            // remove list yAxis
            JComboBox[] yCombos = yComboBoxes.toArray(new JComboBox[]{});
            for (JComboBox yCombo : yCombos) {
                delYCombo(yCombo);
            }

            // At present time on plotDef is required to work : if it is null then return and leave in reset state
            if (plotDef == null) {
                return;
            }

            // Add column present in associated subset if any
            // TODO generate one synthetic OiFitsSubset to give all available choices
            if (oiFitsSubset != null) {
                // Get whole available columns
                final Set<String> columns = getDistinctColumns(oiFitsSubset);

                xAxisChoices.addAll(columns);
                yAxisChoices.addAll(columns);
            }
            // Add choices present in the associated plotDef
            final String currentX = plotDef.getXAxis().getName();
            if (!xAxisChoices.contains(currentX)) {
                xAxisChoices.add(currentX);
            }

            for (Axis y : plotDef.getYAxes()) {
                final String currentY = y.getName();
                if (!yAxisChoices.contains(currentY)) {
                    yAxisChoices.add(currentY);
                }
            }

            logger.debug("refreshForm : xAxisChoices {}, yAxisChoices {}", xAxisChoices, yAxisChoices);

            if (!xAxisChoices.isEmpty()) {
                // Use label of associated plotdefinition if any, else try old value and finally use first by default
                if (plotDef.getXAxis() != null && plotDef.getXAxis().getName() != null) {
                    xAxisComboBox.setSelectedItem(plotDef.getXAxis().getName());
                } else if (lastXComboBoxValue != null && xAxisChoices.contains(lastXComboBoxValue)) {
                    xAxisComboBox.setSelectedItem(lastXComboBoxValue);
                } else {
                    xAxisComboBox.setSelectedIndex(0);
                }
            }

            if (!yAxisChoices.isEmpty()) {
                if (plotDef.getYAxes().isEmpty()) {
                    logger.debug("refreshForm : no yaxes to copy");
                    for (int i = 0, len = yComboBoxes.size(); i < len; i++) {
                        if (lastYComboBoxesValues.size() > i && yAxisChoices.contains(lastYComboBoxesValues.get(i))) {
                            yComboBoxes.get(i).setSelectedItem(lastYComboBoxesValues.get(i));
                        } else {
                            yComboBoxes.get(i).setSelectedIndex(0);
                        }
                    }
                } else {
                    // fill with associated plotdefinition            
                    logger.debug("refreshForm : yaxes to add : {}", plotDef.getYAxes());
                    for (Axis yAxis : plotDef.getYAxes()) {
                        addYCombo(yAxis.getName());
                    }
                }
            }
        } finally {
            initStep = true;
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
                logger.debug("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiVis()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.debug("Can't use data from '{}' table with column '{}'", oiTable, columnName);
            }
        }
        for (OITable oiTable : oiFitsFile.getOiT3()) {
            ColumnMeta meta = oiTable.getColumnMeta(columnName);
            if (meta != null) {
                oiTable.getNumericalColumnsNames(columns);
            } else {
                logger.debug("Can't use data from '{}' table with column '{}'", oiTable, columnName);
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
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(yLabel, gridBagConstraints);

        xLabel.setText("xAxis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
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
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
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
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
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
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(delYAxisButton, gridBagConstraints);

        yComboBoxesPanel.setLayout(new javax.swing.BoxLayout(yComboBoxesPanel, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane1.setViewportView(yComboBoxesPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
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
        if (initStep) {
            // get copy:
            final PlotDefinition plotDefCopy = getPlotDefinition();

            if (plotDefCopy != null) {
                // handle xAxis
                final Axis xAxis = new Axis();
                xAxis.setName(getxAxis());
                plotDefCopy.setXAxis(xAxis);

                // handle yAxes
                final List<Axis> yAxes = plotDefCopy.getYAxes();
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
            logger.debug("update plotDef {}", plotDefCopy);

            ocm.updatePlotDefinition(this, plotDefCopy);
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

    /**
     * Define the plot identifier and reset plot
     * @param plotId plot identifier or null to reset state
     */
    public void setPlotId(final String plotId) {
        logger.debug("setPlotId {}", plotId);

        final String prevPlotId = this.plotId;

        _setPlotId(plotId);

        if (plotId != null && !ObjectUtils.areEquals(prevPlotId, plotId)) {
            logger.debug("firePlotChanged {}", plotId);

            // bind(plotId) ?
            // fire PlotChanged event to initialize correctly the widget:
            ocm.firePlotChanged(null, plotId, this); // null forces different source
        }
    }

    /**
     * Define the plot identifier and reset plot
     * @param plotId plot identifier or null to reset state
     */
    private void _setPlotId(final String plotId) {
        logger.debug("_setPlotId {}", plotId);

        this.plotId = plotId;

        // reset case:
        if (plotId == null) {
            // reset plotDefId:
            if (this.plotDefId != null) {
                _setPlotDefId(null);
            }

            // TODO: how to fire reset event ie DELETE(id)
            refreshForm(null, null);
        }
    }

    /**
     * Return a new copy of the PlotDefinition given its identifier (to update it)
     * @return copy of the PlotDefinition or null if not found
     */
    private PlotDefinition getPlotDefinition() {
        if (plotDefId != null) {
            return ocm.getPlotDefinition(plotDefId);
        }
        return null;
    }

    /**
     * Define the plot definition identifier and reset plot definition
     * @param plotDefId plot definition identifier
     */
    public void setPlotDefId(final String plotDefId) {
        logger.debug("setPlotDefId {}", plotDefId);

        final String prevPlotDefId = this.plotDefId;

        _setPlotDefId(plotDefId);

        // reset plotId:
        if (this.plotId != null) {
            _setPlotId(null);
        }

        // reset case:
        if (plotDefId == null) {
            // reset plotId:
            if (this.plotId != null) {
                _setPlotId(null);
            }

            // TODO: how to fire reset event ie DELETE(id)
            refreshForm(null, null);
        }

        if (plotDefId != null && !ObjectUtils.areEquals(prevPlotDefId, plotDefId)) {
            logger.debug("firePlotDefinitionChanged {}", plotDefId);

            // bind(plotDefId) ?
            // fire PlotDefinitionChanged event to initialize correctly the widget:
            ocm.firePlotDefinitionChanged(null, plotDefId, this); // null forces different source
        }
    }

    /**
     * Define the plot definition identifier and reset plot definition
     * @param plotDefId plot definition identifier
     */
    private void _setPlotDefId(final String plotDefId) {
        logger.debug("_setPlotDefId {}", plotDefId);

        this.plotDefId = plotDefId;

        // do not change plotId
    }

    /*
     * OIFitsCollectionManagerEventListener implementation 
     */
    /**
     * Return the optional subject id i.e. related object id that this listener accepts
     * @param type event type
     * @return subject id (null means accept any event) or DISCARDED_SUBJECT_ID to discard event
     */
    public String getSubjectId(final OIFitsCollectionManagerEventType type) {
        switch (type) {
            case PLOT_DEFINITION_CHANGED:
                if (this.plotDefId != null) {
                    return this.plotDefId;
                }
                break;
            case PLOT_CHANGED:
                if (this.plotId != null) {
                    return this.plotId;
                }
                break;
            default:
        }
        return DISCARDED_SUBJECT_ID;
    }

    /**
     * Handle the given OIFits collection event
     * @param event OIFits collection event
     */
    @Override
    public void onProcess(final OIFitsCollectionManagerEvent event) {
        logger.debug("onProcess {}", event);

        switch (event.getType()) {
            case PLOT_DEFINITION_CHANGED:
                // define id of associated plotDefinition
                _setPlotDefId(event.getPlotDefinition().getName());

                refreshForm(event.getPlotDefinition(), null);
                break;
            case PLOT_CHANGED:
                final PlotDefinition plotDef = event.getPlot().getPlotDefinition();

                // define id of associated plotDefinition
                _setPlotDefId(plotDef.getName());

                refreshForm(plotDef, event.getPlot().getSubsetDefinition().getOIFitsSubset());
                break;
            default:
                logger.debug("onProcess {} - done", event);
        }
    }
}
