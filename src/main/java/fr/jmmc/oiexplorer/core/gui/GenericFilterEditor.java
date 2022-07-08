/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.oiexplorer.core.function.Converter;
import fr.jmmc.oiexplorer.core.function.ConverterFactory;
import fr.jmmc.oiexplorer.core.model.oi.DataType;
import fr.jmmc.oiexplorer.core.model.oi.GenericFilter;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import static fr.jmmc.oitools.OIFitsConstants.COLUMN_EFF_WAVE;
import static fr.jmmc.oitools.OIFitsConstants.COLUMN_MJD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* */
public class GenericFilterEditor extends javax.swing.JPanel implements Disposable, ChangeListener {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(GenericFilterEditor.class);

    private static final String[] supportedColumnNames = {COLUMN_EFF_WAVE, COLUMN_MJD};

    private static final ConverterFactory CONVERTER_FACTORY = ConverterFactory.getInstance();

    /** Map giving a list of predefined ranges for a column name. Some column name don't have predefined ranges and
     * receive a null value. TODO: move this to RangeEditor ?
     */
    private static final Map<String, Map<String, double[]>> predefinedRangesByColumnName;
    static {
        predefinedRangesByColumnName = new HashMap<>(1);
        predefinedRangesByColumnName.put(COLUMN_EFF_WAVE, RangeEditor.EFF_WAVE_PREDEFINED_RANGES);
    }

    private GenericFilter genericFilter;

    private final List<RangeEditor> rangeEditors;

    /**
     * updatingGUI true disables the handlers and the listeners on the widgets. useful when updating the widgets
     */
    private boolean updatingGUI;

    /** Creates new form GenericFilterEditor */
    public GenericFilterEditor() {
        initComponents();
        jComboBoxColumnName.setModel(new DefaultComboBoxModel<>(supportedColumnNames));
        rangeEditors = new ArrayList<>();
        updatingGUI = false;
    }

    public final void setGenericFilter(GenericFilter genericFilter) {

        this.genericFilter = genericFilter;

        final boolean modified = forceSupportedGenericFilter();

        try {
            updatingGUI = true;

            jCheckBoxEnabled.setSelected(genericFilter.isEnabled());

            final String columnName = genericFilter.getColumnName();
            jComboBoxColumnName.setSelectedItem(columnName);

            final Converter converter = CONVERTER_FACTORY.getDefault(CONVERTER_FACTORY.getDefaultByColumn(columnName));

            rangeEditors.forEach(RangeEditor::dispose);
            rangeEditors.clear();
            jPanelRangeEditors.removeAll();
            for (Range range : genericFilter.getAcceptedRanges()) {
                RangeEditor rangeEditor = new RangeEditor();
                rangeEditor.addChangeListener(this);
                rangeEditor.setAlias(columnName);

                // we create a new Range so it can have a different unit
                Range microMeterRange = (Range) range.clone();
                if (converter != null) {
                    try {
                        microMeterRange.setMin(converter.evaluate(microMeterRange.getMin()));
                        microMeterRange.setMax(converter.evaluate(microMeterRange.getMax()));
                    }
                    catch (IllegalArgumentException e) {
                        logger.error(e.getMessage());
                    }
                }
                rangeEditor.setRange(microMeterRange);
                rangeEditor.updateRangeEditor(microMeterRange, true);

                rangeEditor.updateRangeList(predefinedRangesByColumnName.get(columnName));

                rangeEditor.setEnabled(genericFilter.isEnabled());
                rangeEditors.add(rangeEditor);
                jPanelRangeEditors.add(rangeEditor);
            }

            revalidate();
        }
        finally {
            updatingGUI = false;
        }

        if (modified) {
            fireStateChanged();
        }
    }

    public GenericFilter getGenericFilter() {
        return genericFilter;
    }

    // force values we currently support
    // TODO: to be updated when we support other values
    private boolean forceSupportedGenericFilter() {
        boolean modified = false;

        boolean columnNameIsSupported = false;
        for (String supportedColumnName : supportedColumnNames) {
            if (supportedColumnName.equals(genericFilter.getColumnName())) {
                columnNameIsSupported = true;
                break;
            }
        }
        if (!columnNameIsSupported) {
            genericFilter.setColumnName(COLUMN_EFF_WAVE);
            modified = true;
        }

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

    private void handlerColumnName() {
        if (!updatingGUI && genericFilter != null) {

            final String columnName = (String) jComboBoxColumnName.getSelectedItem();
            genericFilter.setColumnName(columnName);

            final Map<String, double[]> predefinedRanges = predefinedRangesByColumnName.get(columnName);
            for (RangeEditor rangeEditor : rangeEditors) {
                rangeEditor.updateRangeList(predefinedRanges);
            }

            // TODO set dataType

            fireStateChanged();
        }
    }

    @Override
    public void dispose() {
        genericFilter = null;
        for (ChangeListener listener : getChangeListeners()) {
            removeChangeListener(listener);
        }
        rangeEditors.forEach(RangeEditor::dispose);
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        if (!updatingGUI) {

            // some rangeEditor has changed. we need to update the corresponding range in genericFilter
            // and we need to convert the unit

            final RangeEditor rangeEditorChanged = (RangeEditor) ce.getSource();

            final Converter invConverter = CONVERTER_FACTORY.getInverseDefault(
                    CONVERTER_FACTORY.getDefaultByColumn(genericFilter.getColumnName()));

            int index = 0;
            for (RangeEditor rangeEditor : rangeEditors) {
                if (rangeEditor.equals(rangeEditorChanged)) {

                    Range rangeToModify = genericFilter.getAcceptedRanges().get(index);

                    rangeToModify.copy(rangeEditorChanged.getRange());

                    if (invConverter != null) {
                        try {
                            rangeToModify.setMin(invConverter.evaluate(rangeToModify.getMin()));
                            rangeToModify.setMax(invConverter.evaluate(rangeToModify.getMax()));
                        }
                        catch (IllegalArgumentException e) {
                            logger.error(e.getMessage());
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
    protected void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
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
        jComboBoxColumnName = new javax.swing.JComboBox<>();

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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.7;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jPanelRangeEditors, gridBagConstraints);

        jComboBoxColumnName.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "COLUMN_NAME" }));
        jComboBoxColumnName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxColumnNameActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jComboBoxColumnName, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        handlerEnabled();
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed

    private void jComboBoxColumnNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxColumnNameActionPerformed
        handlerColumnName();
    }//GEN-LAST:event_jComboBoxColumnNameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JComboBox<String> jComboBoxColumnName;
    private javax.swing.JPanel jPanelRangeEditors;
    // End of variables declaration//GEN-END:variables

}
