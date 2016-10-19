/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.selection;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OITarget;

/**
 *
 * @author bourgesl
 */
public final class DataPointer {

    public final static int UNDEFINED = -1;

    /* member */
    /** data table */
    private final OIData oiData;
    /* row index in the data table */
    private final int row;
    /* column index in the data table (wavelength) */
    private final int col;

    public DataPointer(final OIData oiData, final int row, final int col) {
        this.oiData = oiData;
        this.row = row;
        this.col = col;
    }

    public OIData getOiData() {
        return oiData;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.oiData != null ? this.oiData.hashCode() : 0);
        hash = 97 * hash + this.row;
        hash = 97 * hash + this.col;
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataPointer other = (DataPointer) obj;
        if (this.row != other.getRow()) {
            return false;
        }
        if (this.col != other.getCol()) {
            return false;
        }
        if (this.oiData != other.getOiData() && (this.oiData == null || !this.oiData.equals(other.getOiData()))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DataPointer{oidata: " + oiData
                + " staIndexName: " + getStaIndexName()
                + " staConfName: " + getStaConfName()
                + " ArrName: " + getArrName()
                + " InsName: " + getInsName()
                + " FileName: " + getOIFitsFileName() + '}';
    }

    private final boolean checkRow() {
        return (row >= 0 && row < oiData.getNbRows());
    }

    private final boolean checkCol() {
        return (col >= 0 && col < oiData.getNWave());
    }

    public String getStaIndexName() {
        final short[] staIndexes = checkRow() ? oiData.getStaIndex()[row] : null;
        return oiData.getStaNames(staIndexes);
    }

    public String getStaConfName() {
        final short[] staIndexes = checkRow() ? oiData.getStaConf()[row] : null;
        return oiData.getStaNames(staIndexes);
    }

    public float getWaveLength() {
        if (checkCol()) {
            return oiData.getOiWavelength().getEffWave()[col];
        }
        return Float.NaN;
    }

    public String getArrName() {
        return oiData.getArrName();
    }

    public String getInsName() {
        return oiData.getInsName();
    }

    public String getOIFitsFileName() {
        return oiData.getOIFitsFile().getName();
    }

// Fast access to computed values:    
    public String getTarget() {
        if (checkRow()) {
            final short targetId = oiData.getTargetId()[row];

            final OITarget oiTarget = oiData.getOiTarget();

            if (oiTarget != null) {
                final Integer rowTarget = oiTarget.getRowIndex(Short.valueOf(targetId)); // requires previously OIFits Analyzer call()
                if (rowTarget != null) {
                    return oiTarget.getTarget()[rowTarget.intValue()];
                }
            }

        }
        return "";
    }

    public double getSpatialFreq() {
        if (checkRow() && checkCol()) {
            return oiData.getSpatialFreq()[row][col];
        }
        return Double.NaN;
    }

    public double getRadius() {
        if (checkRow()) {
            return oiData.getRadius()[row];
        }
        return Double.NaN;
    }

    public double getPosAngle() {
        if (checkRow()) {
            return oiData.getPosAngle()[row];
        }
        return Double.NaN;
    }

    public double getHourAngle() {
        if (checkRow()) {
            return oiData.getHourAngle()[row];
        }
        return Double.NaN;
    }

    public boolean isSameCol(final DataPointer other) {
        return (oiData == other.getOiData()) && (col == other.getCol());
    }
}
