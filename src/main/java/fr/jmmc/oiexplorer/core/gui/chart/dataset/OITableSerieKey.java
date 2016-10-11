/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.gui.chart.dataset;

import fr.jmmc.jmcs.util.NumberUtils;
import fr.jmmc.oitools.model.OIData;

/**
 *
 * @author bourgesl
 */
public final class OITableSerieKey implements java.io.Serializable, Comparable<OITableSerieKey> {

    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;

    /* member */
    /* generated table index (ensure key uniqueness) */
    private final int tableIndex;
    /* origin of the data */
    private final OIData oiData;
    /* StaIndex index */
    private final int staIdxIndex;
    /* waveLength index (0..n) */
    private final int waveLengthIndex;

    public OITableSerieKey(final int tableIndex, final OIData oiData, final int staIdxIndex, final int waveLengthIndex) {
        this.tableIndex = tableIndex;
        this.oiData = oiData;
        this.staIdxIndex = staIdxIndex;
        this.waveLengthIndex = waveLengthIndex;
    }

    @Override
    public int compareTo(final OITableSerieKey o) {
        int res = NumberUtils.compare(tableIndex, o.getTableIndex());
        if (res == 0) {
            res = NumberUtils.compare(staIdxIndex, o.getStaIdxIndex());
            if (res == 0) {
                res = NumberUtils.compare(waveLengthIndex, o.getWaveLengthIndex());
            }
        }
        return res;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.tableIndex;
        hash = 31 * hash + this.staIdxIndex;
        hash = 67 * hash + this.waveLengthIndex;
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OITableSerieKey other = (OITableSerieKey) obj;
        if (this.tableIndex != other.getTableIndex()) {
            return false;
        }
        if (this.staIdxIndex != other.getStaIdxIndex()) {
            return false;
        }
        if (this.waveLengthIndex != other.getWaveLengthIndex()) {
            return false;
        }
        return true;
    }

    public int getTableIndex() {
        return tableIndex;
    }

    public OIData getOiData() {
        return oiData;
    }

    public int getStaIdxIndex() {
        return staIdxIndex;
    }

    public int getWaveLengthIndex() {
        return waveLengthIndex;
    }

    @Override
    public String toString() {
        return "#" + tableIndex + " B" + staIdxIndex + " W" + waveLengthIndex 
                + " oidata: " + oiData 
                + " staNames: " + getStaName()
                + " wavelength: " + getWaveLength();
    }
    
    public String getStaName() {
                // anyway (color mapping or check sta index):
        final short[][] distinctStaIndexes = oiData.getDistinctStaIndexes();

        final short[] currentStaIndex = distinctStaIndexes[staIdxIndex];
        
        return oiData.getStaNames(currentStaIndex); // cached
    }

    
    public double getWaveLength() {
        // suppose nWaves != 0:
        final double[] effWaves = oiData.getOiWavelength().getEffWaveAsDouble();
        
        return effWaves[waveLengthIndex];
    }
    
}
