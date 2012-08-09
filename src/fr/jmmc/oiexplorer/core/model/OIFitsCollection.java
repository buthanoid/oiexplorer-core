/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.oitools.model.Analyzer;
import fr.jmmc.oitools.model.OIData;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OITable;
import fr.jmmc.oitools.model.OITarget;
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
public class OIFitsCollection {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsCollection.class);
    /* members */
    /** OIFits file collection ordered by insertion order */
    private Map<String, OIFitsFile> oiFitsCollection = new LinkedHashMap<String, OIFitsFile>();
    /** cached OIFits structure per target */
    private Map<String, OIFitsFile> oiFitsPerTarget = new HashMap<String, OIFitsFile>();

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

        oiFitsPerTarget.clear();

        for (OIFitsFile oiFitsFile : oiFitsCollection.values()) {

            for (Map.Entry<String, List<OIData>> entry : oiFitsFile.getOiDataPerTarget().entrySet()) {

                final String name = entry.getKey();

                // TODO: Cross Match on target RA/DEC because names ...

                OIFitsFile oiFitsTarget = oiFitsPerTarget.get(name);
                if (oiFitsTarget == null) {
                    oiFitsTarget = new OIFitsFile();
                    oiFitsPerTarget.put(name, oiFitsTarget);
                }

                for (OIData data : entry.getValue()) {
                    oiFitsTarget.addOiTable(data);
                }
            }

            logger.warn("analyzeCollection: ");
            for (Map.Entry<String, OIFitsFile> entry : oiFitsPerTarget.entrySet()) {
                logger.warn("Target: {} : {}", entry.getKey(), Arrays.toString(entry.getValue().getOiTables()));
            }
        }
    }
}
