/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.selection;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.model.OIData;

/**
 *
 * @author bourgesl
 */
public final class DataPointer {

    public final static int UNDEFINED = -1;

    /* member */
 /* data table */
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

}
