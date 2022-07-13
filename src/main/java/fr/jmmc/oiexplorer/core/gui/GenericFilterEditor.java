/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.oiexplorer.core.function.Converter;
import fr.jmmc.oiexplorer.core.function.ConverterFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* */
public final class GenericFilterEditor extends javax.swing.JPanel implements Disposable, ChangeListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(GenericFilterEditor.class);

    private static final ConverterFactory CONVERTER_FACTORY = ConverterFactory.getInstance();

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

    /**
     * updatingGUI true disables the handlers and the listeners on the widgets. useful when updating the widgets
     */
    private boolean updatingGUI;

    /** Creates new form GenericFilterEditor */
    public GenericFilterEditor() {
        initComponents();
        rangeEditors = new ArrayList<>();
        updatingGUI = false;
    }

    @Override
    public void dispose() {
        genericFilter = null;
        for (ChangeListener listener : getChangeListeners()) {
            removeChangeListener(listener);
        }
        rangeEditors.forEach(RangeEditor::dispose);
    }

    public boolean setGenericFilter(final GenericFilter genericFilter) {
        this.genericFilter = genericFilter;

        final boolean modified = forceSupportedGenericFilter();

        try {
            updatingGUI = true;

            rangeEditors.forEach(RangeEditor::dispose);
            rangeEditors.clear();
            jPanelRangeEditors.removeAll();

            final String columnName = genericFilter.getColumnName();

            jCheckBoxEnabled.setSelected(genericFilter.isEnabled());
            jCheckBoxEnabled.setText(columnName);

            final Converter converter = CONVERTER_FACTORY.getDefault(CONVERTER_FACTORY.getDefaultByColumn(columnName));

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
                jPanelRangeEditors.add(rangeEditor);
            }

            revalidate();
        } finally {
            updatingGUI = false;
        }

        if (modified) {
            fireStateChanged();
        }
        return modified;
    }

    public GenericFilter getGenericFilter() {
        return genericFilter;
    }

    // force values we currently support
    // TODO: to be updated when we support other values
    private boolean forceSupportedGenericFilter() {
        boolean modified = false;

        if (!DataType.NUMERIC.equals(genericFilter.getDataType())) {
            genericFilter.setDataType(DataType.NUMERIC);
            modified = true;
        }

        if (genericFilter.getAcceptedRanges().isEmpty()) {
            final Range range = new Range();
            range.setMin(Double.NaN);
            range.setMax(Double.NaN);
            genericFilter.getAcceptedRanges().add(range);
            modified = true;
        } else if (genericFilter.getAcceptedRanges().size() > 1) {
            Range firstRange = genericFilter.getAcceptedRanges().remove(0);
            genericFilter.getAcceptedRanges().clear();
            genericFilter.getAcceptedRanges().add(firstRange);
            modified = true;
        }

        if (!genericFilter.getAcceptedValues().isEmpty()) {
            genericFilter.getAcceptedValues().clear();
            modified = true;
        }

        if (modified) {
            genericFilter.setEnabled(false);
        }

        return modified;
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

    @Override
    public void stateChanged(ChangeEvent ce) {
        if (!updatingGUI) {

            // some rangeEditor has changed. we need to update the corresponding range in genericFilter
            // and we need to convert the unit
            final RangeEditor rangeEditorChanged = (RangeEditor) ce.getSource();

            final Converter converter = CONVERTER_FACTORY.getDefault(CONVERTER_FACTORY.getDefaultByColumn(genericFilter.getColumnName()));

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

        jCheckBoxEnabled = new javax.swing.JCheckBox();
        jPanelRangeEditors = new javax.swing.JPanel();

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

        jPanelRangeEditors.setLayout(new javax.swing.BoxLayout(jPanelRangeEditors, javax.swing.BoxLayout.PAGE_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        add(jPanelRangeEditors, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        handlerEnabled();
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JPanel jPanelRangeEditors;
    // End of variables declaration//GEN-END:variables
}
