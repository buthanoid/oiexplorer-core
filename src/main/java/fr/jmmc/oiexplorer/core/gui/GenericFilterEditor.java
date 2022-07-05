/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package fr.jmmc.oiexplorer.core.gui;

import fr.jmmc.jmcs.gui.component.Disposable;
import fr.jmmc.oiexplorer.core.model.oi.DataType;
import fr.jmmc.oiexplorer.core.model.oi.GenericFilter;
import fr.jmmc.oiexplorer.core.model.plot.Range;
import static fr.jmmc.oitools.OIFitsConstants.COLUMN_EFF_WAVE;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 */
public class GenericFilterEditor extends javax.swing.JPanel implements Disposable {

    /**
     * default serial UID for Serializable interface
     */
    private static final long serialVersionUID = 1;

    /**
     * Logger
     */
    //private static final Logger logger = LoggerFactory.getLogger(GenericFilterEditor.class);

    private GenericFilter genericFilter;

    private final List<RangeEditor> rangeEditors;

    private final RangeEditorChangeListener rangeEditorChangeListener;

    /**
     * Creates new form GenericFilterEditor
     */
    public GenericFilterEditor() {
        initComponents();
        rangeEditors = new ArrayList<>();
        rangeEditorChangeListener = new RangeEditorChangeListener();
    }

    public final void setGenericFilter(GenericFilter genericFilter) {
        this.genericFilter = genericFilter;

        final boolean modified = forceSupportedGenericFilter();

        jCheckBoxEnabled.setSelected(genericFilter.isEnabled());

        final String columnName = genericFilter.getColumnName();
        jLabelColumnName.setText(columnName);

        rangeEditors.forEach(RangeEditor::dispose);
        rangeEditors.clear();
        jPanelRangeEditors.removeAll();
        for (Range range : genericFilter.getAcceptedRanges()) {
            RangeEditor rangeEditor = new RangeEditor();
            rangeEditor.addChangeListener(rangeEditorChangeListener);
            rangeEditor.setAlias(columnName);
            rangeEditor.setRange(range);
            rangeEditor.updateRangeEditor(range, true);
            rangeEditor.updateRangeList(null);
            rangeEditor.setEnabled(genericFilter.isEnabled());
            rangeEditors.add(rangeEditor);
            jPanelRangeEditors.add(rangeEditor);
        }

        revalidate();

        if (modified) {
            fireStateChanged();
        }
    }

    public GenericFilter getGenericFilter() {
        return genericFilter;
    }

    private boolean forceSupportedGenericFilter() {
        boolean modified = false;

        if (!COLUMN_EFF_WAVE.equals(genericFilter.getColumnName())) {
            genericFilter.setColumnName(COLUMN_EFF_WAVE);
            modified = true;
        }

        if (!DataType.NUMERIC.equals(genericFilter.getDataType())) {
            genericFilter.setDataType(DataType.NUMERIC);
            modified = true;
        }
        
        if (genericFilter.getAcceptedRanges().isEmpty()) {
            final Range range = new Range();
            range.setMin(0);
            range.setMax(10);
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
        if (genericFilter != null) {

            final boolean enabled = jCheckBoxEnabled.isSelected();

            genericFilter.setEnabled(enabled);

            for (RangeEditor rangeEditor : rangeEditors) {
                rangeEditor.setEnabled(enabled);
            }

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

    private class RangeEditorChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent ce) {
            GenericFilterEditor.this.fireStateChanged();
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
        jLabelColumnName = new javax.swing.JLabel();
        jPanelRangeEditors = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        jCheckBoxEnabled.setSelected(true);
        jCheckBoxEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnabledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jCheckBoxEnabled, gridBagConstraints);

        jLabelColumnName.setText("COLUMN_NAME");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(jLabelColumnName, gridBagConstraints);

        jPanelRangeEditors.setLayout(new javax.swing.BoxLayout(jPanelRangeEditors, javax.swing.BoxLayout.PAGE_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        add(jPanelRangeEditors, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnabledActionPerformed
        handlerEnabled();
    }//GEN-LAST:event_jCheckBoxEnabledActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBoxEnabled;
    private javax.swing.JLabel jLabelColumnName;
    private javax.swing.JPanel jPanelRangeEditors;
    // End of variables declaration//GEN-END:variables

}
