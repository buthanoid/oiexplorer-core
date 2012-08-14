/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.oitools.model.Analyzer;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage data collection and provide utility methods.
 */
public final class OIFitsCollection {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsCollection.class);
    /* members */
    /** OIFits file collection ordered by insertion order */
    private Map<String, OIFitsFile> oiFitsCollection = new LinkedHashMap<String, OIFitsFile>();
    /** cached OIFitsFile structure per TargetUID */
    private Map<TargetUID, OIFitsFile> oiFitsPerTarget = new HashMap<TargetUID, OIFitsFile>();

    /**
     * Protected constructor
     */
    protected OIFitsCollection() {
        super();
    }

    /**
     * Clear the OIFits file collection
     */
    public void clear() {
        oiFitsCollection.clear();
    }

    public boolean isEmpty() {
        return oiFitsCollection.isEmpty();
    }

    public List<OIFitsFile> getOIFitsFiles() {
        return new ArrayList<OIFitsFile>(oiFitsCollection.values());
    }

    protected void addOIFitsFile(final OIFitsFile oifitsFile) {
        if (oifitsFile != null) {
            final String key = getKey(oifitsFile);

            final OIFitsFile previous = getOIFitsFile(key);

            if (previous != null) {
                logger.warn("TODO: handle overwriting OIFitsFile : {}", key);
                removeOIFitsFile(previous);
            }

            oiFitsCollection.put(key, oifitsFile);

            final Analyzer analyzer = new Analyzer();
            analyzer.visit(oifitsFile);

            logger.warn("addOIFitsFile: " + oifitsFile);

            // update collection analysis:
            analyzeCollection();
        }
    }

    public OIFitsFile getOIFitsFile(final String absoluteFilePath) {
        if (absoluteFilePath != null) {
            return oiFitsCollection.get(absoluteFilePath);
        }
        return null;
    }

    protected OIFitsFile removeOIFitsFile(final OIFitsFile oifitsFile) {
        if (oifitsFile != null) {
            final String key = getKey(oifitsFile);
            final OIFitsFile previous = oiFitsCollection.remove(key);

            if (previous != null) {
                // update collection analysis:
                analyzeCollection();
            }
        }
        return null;
    }

    private String getKey(final OIFitsFile oifitsFile) {
        if (oifitsFile.getAbsoluteFilePath() == null) {
            // TODO: remove asap
            throw new IllegalStateException("Undefined OIFitsFile.absoluteFilePath !");
        }
        return oifitsFile.getAbsoluteFilePath();
    }

    @Override
    public String toString() {
        return "OIFitsCollection[" + Integer.toHexString(System.identityHashCode(this)) + "]" + this.oiFitsCollection.keySet();
    }

    /* --- data analysis --- */
    private void analyzeCollection() {
        // clear OIFits structure per TargetUID:
        oiFitsPerTarget.clear();

        for (OIFitsFile oiFitsFile : oiFitsCollection.values()) {

            for (Map.Entry<String, List<OIData>> entry : oiFitsFile.getOiDataPerTarget().entrySet()) {

                final TargetUID target = new TargetUID(entry.getKey());

                // TODO: Cross Match on target RA/DEC because names ...

                OIFitsFile oiFitsTarget = oiFitsPerTarget.get(target);
                if (oiFitsTarget == null) {
                    oiFitsTarget = new OIFitsFile();

                    oiFitsPerTarget.put(target, oiFitsTarget);
                }

                for (OIData data : entry.getValue()) {
                    oiFitsTarget.addOiTable(data);
                }
            }

            logger.warn("analyzeCollection:");
            for (Map.Entry<TargetUID, OIFitsFile> entry : oiFitsPerTarget.entrySet()) {
                logger.warn("{} : {}", entry.getKey(), Arrays.toString(entry.getValue().getOiTables()));
            }
        }
    }

    /** 
     * Return the OIFitsFile structure per target found in loaded files.
     */
    public Map<TargetUID, OIFitsFile> getOiFitsPerTarget() {
        return oiFitsPerTarget;
    }

    /**
     * Return the OIFitsFile structure corresponding to the given target (name) or null if missing
     * @param target targetUID
     * @return list of OIData tables corresponding to the given target (name) or null if missing
     */
    public OIFitsFile getOiDataList(final TargetUID target) {
        return getOiFitsPerTarget().get(target);
    }
}
