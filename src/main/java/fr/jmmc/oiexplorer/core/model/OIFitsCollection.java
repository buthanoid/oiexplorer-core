/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.util.ObjectUtils;
import fr.jmmc.oiexplorer.core.model.oi.TargetUID;
import fr.jmmc.oitools.meta.OIFitsStandard;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manage data collection and provide utility methods.
 */
public final class OIFitsCollection extends fr.jmmc.oitools.model.OIFitsCollection {

    /* members */
    /** fake OIFitsFile structure per TargetUID */
    private final Map<TargetUID, OIFitsFile> oiFitsPerTarget = new HashMap<TargetUID, OIFitsFile>();

    /**
     * Protected constructor
     */
    protected OIFitsCollection() {
        super();
    }

    /**
     * Clear the cached meta-data
     */
    @Override
    public void clearCache() {
        super.clearCache();
        // clear OIFits structure per TargetUID:
        oiFitsPerTarget.clear();
    }

    /**
     * toString() implementation using string builder
     * 
     * @param sb string builder to append to
     * @param full true to get complete information; false to get main information (shorter)
     */
    @Override
    public void toString(final StringBuilder sb, final boolean full) {
        super.toString(sb, full);

        if (full) {
            sb.append('{');
            if (this.oiFitsPerTarget != null) {
                sb.append("oiFitsPerTarget=");
                ObjectUtils.toString(sb, full, this.oiFitsPerTarget);
            }
            sb.append('}');
        }
    }

    /* --- data analysis --- */
    /**
     * Analyze the complete OIFits collection to provide OIFits structure per unique target (name)
     */
    @Override
    public void analyzeCollection() {
        super.analyzeCollection();

        for (OIFitsFile oiFitsFile : getSortedOIFitsFiles()) {

            // Note: per OIFits, multiple target ids may have the same target name !
            // take care !
            for (Map.Entry<String, List<OIData>> entry : oiFitsFile.getOiDataPerTarget().entrySet()) {

                final TargetUID target = new TargetUID(entry.getKey());

                // TODO: Cross Match on target RA/DEC because names ...
                OIFitsFile oiFitsTarget = oiFitsPerTarget.get(target);
                if (oiFitsTarget == null) {
                    oiFitsTarget = new OIFitsFile(OIFitsStandard.VERSION_1);

                    oiFitsPerTarget.put(target, oiFitsTarget);
                }

                for (OIData data : entry.getValue()) {
                    oiFitsTarget.addOiTable(data);
                }
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "analyzeCollection: Targets: ");

            for (Map.Entry<TargetUID, OIFitsFile> entry : oiFitsPerTarget.entrySet()) {
                logger.log(Level.FINE, "{0} : {1}",
                        new Object[]{entry.getKey(), entry.getValue().getOiDataList()});
            }
        }
    }

    /** 
     * Return the OIFitsFile structure per target found in loaded files.
     * @return OIFitsFile structure per target
     */
    public Map<TargetUID, OIFitsFile> getOiFitsPerTarget() {
        return oiFitsPerTarget;
    }

    /**
     * Return the OIFitsFile structure corresponding to the given target (name) or null if missing
     * @param target targetUID
     * @return list of OIData tables corresponding to the given target (name) or null if missing
     */
    public OIFitsFile getOiFits(final TargetUID target) {
        // TODO: create an OIFits dynamically from filters (target...)
        return getOiFitsPerTarget().get(target);
    }
}
