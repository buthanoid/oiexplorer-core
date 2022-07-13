/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.jmcs.gui.component.GenericListModel;
import fr.jmmc.oiexplorer.core.function.Converter;
import fr.jmmc.oiexplorer.core.function.ConverterFactory;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionManager;
import fr.jmmc.oiexplorer.core.model.oi.DataType;
import fr.jmmc.oiexplorer.core.model.oi.GenericFilter;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import static fr.jmmc.oitools.OIFitsConstants.COLUMN_EFF_WAVE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* */
public final class GenericFilterEditor extends javax.swing.JPanel
        implements Disposable, ChangeListener, ListSelectionListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(GenericFilterEditor.class);

    private static final ConverterFactory CONVERTER_FACTORY = ConverterFactory.getInstance();

    /** OIFitsCollectionManager singleton reference. used for reset button. */
    private final static OIFitsCollectionManager OCM = OIFitsCollectionManager.getInstance();

    /** Map giving a list of predefined ranges for a column name. Some column name don't have predefined ranges and
     * receive a null value. TODO: move this to RangeEditor ?
     */
    private static final Map<String, Map<String, double[]>> predefinedRangesByColumnName;

    static {
        predefinedRangesByColumnName = new HashMap<>(8);
        predefinedRangesByColumnName.put(COLUMN_EFF_WAVE, RangeEditor.EFF_WAVE_PREDEFINED_RANGES);
    }

    // members:
    /* related GenericFilter */
    private GenericFilter genericFilter;
    /* associated Range editors */
    private final List<RangeEditor> rangeEditors;

    /* for DataType.STRING list of checkboxes for accepted values */
    private final List<String> checkBoxListValuesModel;

    /** converter associated to the column name of the generic filter. It allows us to make the range editor use a
     * different unit more user friendly. */
    private Converter converter;

    /**
     * updatingGUI true disables the handlers and the listeners on the widgets. useful when updating the widgets
     */
    private boolean updatingGUI;

    /** Creates new form GenericFilterEditor */
    public GenericFilterEditor() {
        initComponents();
        rangeEditors = new ArrayList<>();

        checkBoxListValuesModel = new ArrayList<>(16);
        checkBoxListValues.setModel(new GenericListModel<String>(checkBoxListValuesModel));
        checkBoxListValues.getCheckBoxListSelectionModel().addListSelectionListener(this);

        updatingGUI = false;
    }

    @Override
    public void dispose() {
        genericFilter = null;
        for (ChangeListener listener : getChangeListeners()) {
            removeChangeListener(listener);
        }
        rangeEditors.forEach(RangeEditor::dispose);
        checkBoxListValues.getCheckBoxListSelectionModel().removeListSelectionListener(this);
    }

    public void setGenericFilter(final GenericFilter genericFilter) {
        this.genericFilter = genericFilter;

        try {
            updatingGUI = true;

            rangeEditors.forEach(RangeEditor::dispose);
            rangeEditors.clear();
            checkBoxListValuesModel.clear();
            checkBoxListValues.clearCheckBoxListSelection();
            jPanelRangesOrValues.removeAll();

            final String columnName = genericFilter.getColumnName();

            jCheckBoxEnabled.setSelected(genericFilter.isEnabled());
            jCheckBoxEnabled.setText(columnName);

            if (genericFilter.getDataType() == DataType.NUMERIC) {
                converter = CONVERTER_FACTORY.getDefault(CONVERTER_FACTORY.getDefaultByColumn(columnName));

                for (Range range : genericFilter.getAcceptedRanges()) {
                    final RangeEditor rangeEditor = new RangeEditor();
                    rangeEditor.setAlias(columnName);
                    rangeEditor.addChangeListener(this);

                    // we create a new Range so it can have a different unit:
                    Range convertedRange = (Range) range.clone();
                    if (converter != null) {
                        try {
                            convertedRange.setMin(converter.evaluate(convertedRange.getMin()));
                            convertedRange.setMax(converter.evaluate(convertedRange.getMax()));
                        } catch (IllegalArgumentException iae) {
                            logger.error("conversion failed: ", iae);
                        }
                    }
                    rangeEditor.setRange(convertedRange);
                    rangeEditor.updateRange(convertedRange, true);

                    rangeEditor.updateRangeList(predefinedRangesByColumnName.get(columnName));

                    rangeEditor.setEnabled(genericFilter.isEnabled());
                    // add editor:
                    rangeEditors.add(rangeEditor);
                    jPanelRangesOrValues.add(rangeEditor);
                }
            } else if (genericFilter.getDataType() == DataType.STRING) {

                // generating check box list possible values
                final List<String> initValues = OCM.getOIFitsCollection().getDistinctValues(columnName);
                if (initValues != null) {
                    checkBoxListValuesModel.addAll(initValues);
                }
                jPanelRangesOrValues.add(jScrollPaneValues);

                // selecting values in the check box list
                for (String value : genericFilter.getAcceptedValues()) {
                    if (!checkBoxListValuesModel.contains(value)) {
                        // in case the generic filter has a selected value that does not
                        // exist in the checkboxlist alternatives, we add it
                        checkBoxListValuesModel.add(value);
                    }
                    checkBoxListValues.addCheckBoxListSelectedValue(value, false);
                }
            }

            revalidate();
        } finally {
            updatingGUI = false;
        }
    }

    public GenericFilter getGenericFilter() {
        return genericFilter;
    }

    private void handlerEnabled() {
        if (!updatingGUI && genericFilter != null) {

            final boolean enabled = jCheckBoxEnabled.isSelected();

            genericFilter.setEnabled(enabled);

            for (RangeEditor rangeEditor : rangeEditors) {
                rangeEditor.setEnabled(enabled);
            }

            fireStateChanged();
        }
    }

    private void handlerReset() {
        if (!updatingGUI && genericFilter != null) {

            final String columnName = genericFilter.getColumnName();

            if (genericFilter.getDataType() == DataType.NUMERIC) {

                final fr.jmmc.oitools.model.range.Range oitoolsRange
                        = OCM.getOIFitsCollection().getColumnRange(columnName);

                double newMin = Double.NaN, newMax = Double.NaN;
                if (oitoolsRange.isFinite()) {
                    newMin = oitoolsRange.getMin();
                    newMax = oitoolsRange.getMax();
                }

                // updating generic filter
                for (Range range : genericFilter.getAcceptedRanges()) {
                    range.setMin(newMin);
                    range.setMax(newMax);
                }

                // updating range editors with converted new min and max

                double convertedNewMin = Double.NaN, convertedNewMax = Double.NaN;
                if (converter == null) {
                    convertedNewMin = newMin;
                    convertedNewMax = newMax;
                } else {
                    try {
                        convertedNewMin = converter.evaluate(newMin);
                        convertedNewMax = converter.evaluate(newMax);
                    } catch (IllegalArgumentException iae) {
                        logger.error("conversion failed: ", iae);
                    }
                }

                for (RangeEditor rangeEditor : rangeEditors) {
                    rangeEditor.setRangeFieldValues(convertedNewMin, convertedNewMax);
                }
            } else if (genericFilter.getDataType() == DataType.STRING) {

                // update gui widget
                checkBoxListValuesModel.clear();
                final List<String> initValues = OCM.getOIFitsCollection().getDistinctValues(columnName);
                if (initValues != null) {
                    checkBoxListValuesModel.addAll(initValues);
                }
                checkBoxListValues.selectAll();

                // update generic filter
                genericFilter.getAcceptedValues().clear();
                if (initValues != null) {
                    genericFilter.getAcceptedValues().addAll(initValues);
                }

                jPanelRangesOrValues.add(jScrollPaneValues);
            }

            repaint();

            fireStateChanged();
        }
    }

    /** handler for changes in range editors.
     *
     * @param ce Event */
    @Override
    public void stateChanged(ChangeEvent ce) {
        if (!updatingGUI) {

            // some rangeEditor has changed. we need to update the corresponding range in genericFilter
            // and we need to convert the unit
            final RangeEditor rangeEditorChanged = (RangeEditor) ce.getSource();

            int index = 0;
            for (RangeEditor rangeEditor : rangeEditors) {
                if (rangeEditor == rangeEditorChanged) {

                    final Range rangeToModify = genericFilter.getAcceptedRanges().get(index);

                    rangeToModify.copy(rangeEditorChanged.getRange());

                    if (converter != null) {
                        try {
                            // invert conversion:
                            rangeToModify.setMin(converter.invert(rangeToModify.getMin()));
                            rangeToModify.setMax(converter.invert(rangeToModify.getMax()));
                        } catch (IllegalArgumentException iae) {
                            logger.error("conversion failed: ", iae);
                        }
                    }
                    break;
                }
                index++;
            }

            fireStateChanged();
        }
    }

    /** handler for changes in check box list values.
     *
     * @param lse Event
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        if (!updatingGUI && genericFilter != null) {
            final List<String> genericFilterValues = genericFilter.getAcceptedValues();
            genericFilterValues.clear();
            for (Object selected : checkBoxListValues.getCheckBoxListSelectedValues()) {
                genericFilterValues.add((String) selected);
            }
            fireStateChanged();
        }
    }

    /**
     * Listen to changes to Range.
     *
     * @param listener listener to notify when changes occur
     */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Stop listening to changes to Range.
     *
     * @param listener to stop notifying when changes occur
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * @return the list of ChangeListeners
     */
    private ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Notify listeners that changes occured to Range
     */
    void fireStateChanged() {
        final ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener changeListener : getChangeListeners()) {
            changeListener.stateChanged(event);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPaneValues = new javax.swing.JScrollPane();
        checkBoxListValues = new com.jidesoft.swing.CheckBoxList();
        jCheckBoxEnabled = new javax.swing.JCheckBox();
        jPanelRangesOrValues = new javax.swing.JPanel();
        jButtonReset = new javax.swing.JButton();

        checkBoxListValues.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "A1", "A2", "B1", "B2" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        checkBoxListValues.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        checkBoxListValues.setClickInCheckBoxOnly(false);
        checkBoxListValues.setVisibleRowCount(4);
        jScrollPaneValues.setViewportView(checkBoxListValues);

        setLayout(new java.awt.GridBagLayout());

        jCheckBoxEnabled.setSelected(true);
        jCheckBoxEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnabledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jCheckBoxEnabled, gridBagConstraints);

        jPanelRangesOrValues.setLayout(new javax.swing.BoxLayout(jPanelRangesOrValues, javax.swing.BoxLayout.PAGE_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        add(jPanelRangesOrValues, gridBagConstraints);

        jButtonReset.setText("reset");
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jButtonReset, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        handlerEnabled();
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        handlerReset();
    }//GEN-LAST:event_jButtonResetActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.jidesoft.swing.CheckBoxList checkBoxListValues;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JPanel jPanelRangesOrValues;
    private javax.swing.JScrollPane jScrollPaneValues;
    // End of variables declaration//GEN-END:variables
}
