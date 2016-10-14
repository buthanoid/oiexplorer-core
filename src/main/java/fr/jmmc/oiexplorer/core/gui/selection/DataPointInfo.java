/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.selection;

/**
 *
 * @author bourgesl
 */
public final class DataPointInfo extends DataPoint {

    private final DataPointer ptr;

    public DataPointInfo(final double x, final double y, final DataPointer ptr) {
        super(x, y);
        this.ptr = ptr;
    }

    public DataPointer getDataPointer() {
        return ptr;
    }

    @Override
    public String toString() {
        return super.toString() + "{ptr = " + ptr + '}';
    }
}
