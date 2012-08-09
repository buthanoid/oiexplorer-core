/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionListener;
import fr.jmmc.oitools.model.OIFitsChecker;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.nom.tam.fits.FitsException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the oifits files collection.
 * @author mella, bourgesl
 */
public class OIFitsManager {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsManager.class);
    /** flag to log a stack trace in method fireEvent() to debug events */
    private final static boolean DEBUG_FIRE_EVENT = false;
    /** Singleton pattern */
    private final static OIFitsManager instance = new OIFitsManager();
    /* members */
    /** OIFits collection */
    private OIFitsCollection oiFitsCollection = null;
    /** OIFits collection listeners */
    private final CopyOnWriteArrayList<OIFitsCollectionListener> listeners = new CopyOnWriteArrayList<OIFitsCollectionListener>();

    /**
     * Return the Manager singleton
     * @return singleton instance
     */
    public static OIFitsManager getInstance() {
        return instance;
    }

    /** 
     * Prevent instanciation of singleton.
     * Manager instance should be obtained using getInstance().
     */
    private OIFitsManager() {
        super();
        reset();
    }

    /**
     * Load the given OI Fits File with the given checker component
     * and add it to the OIFits collection
     * @param fileLocation absolute File Path or remote URL
     * @param checker checker component
     * @throws MalformedURLException invalid url format
     * @throws FitsException if the fits can not be opened
     * @throws IOException IO failure
     */
    public void loadOIFitsFile(final String fileLocation, final OIFitsChecker checker) throws MalformedURLException, IOException, FitsException {
        //@todo test if file has already been loaded before going further ??        

        // The file must be one oidata file (next line automatically unzip gz files)
        final OIFitsFile oifitsFile = OIFitsLoader.loadOIFits(checker, fileLocation);

        addOIFitsFile(oifitsFile);

        logger.info("file loaded : '{}'", oifitsFile.getAbsoluteFilePath());
    }

    // save ...
    // merge ...
    /* --- OIFits file collection handling ------------------------------------- */
    /**
     * Reset the OIFits file collection
     */
    public void reset() {
        oiFitsCollection = new OIFitsCollection();
        fireOIFitsCollectionChanged();
    }

    public void addOIFitsFile(final OIFitsFile oifitsFile) {
        if (oifitsFile != null) {
            oiFitsCollection.addOIFitsFile(oifitsFile);

            fireOIFitsCollectionChanged();
        }
    }

    public OIFitsFile removeOIFitsFile(final OIFitsFile oifitsFile) {
        final OIFitsFile previous = oiFitsCollection.removeOIFitsFile(oifitsFile);
        if (previous != null) {
            fireOIFitsCollectionChanged();
        }
        return previous;
    }

    public OIFitsCollection getOIFitsCollection() {
        return oiFitsCollection;
    }
    // --- EVENTS ----------------------------------------------------------------

    /**
     * Register the given OIFits collection listener
     * @param listener OIFits collection listener
     */
    public void register(final OIFitsCollectionListener listener) {
        this.listeners.addIfAbsent(listener);
    }

    /**
     * Unregister the given OIFits collection listener
     * @param listener OIFits collection listener
     */
    public void unregister(final OIFitsCollectionListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * This fires an OIFits collection changed event to all registered listeners.
     * @param oiFitsFile added OIFits file
     */
    private void fireOIFitsCollectionChanged() {
        if (logger.isDebugEnabled()) {
            logger.debug("fireOIFitsCollectionChanged: {}", this.oiFitsCollection);
        }

        fireEvent(new OIFitsCollectionEvent(OIFitsCollectionEventType.CHANGED, this.oiFitsCollection));
    }

    /**
     * Send an event to the registered listeners.
     * Note : any new listener registered during the processing of this event, will not be called
     * @param event event
     */
    private void fireEvent(final OIFitsCollectionEvent event) {
        // ensure events are fired by Swing EDT :
        if (!SwingUtils.isEDT()) {
            logger.warn("invalid thread : use EDT", new Throwable());
        }
        if (DEBUG_FIRE_EVENT) {
            logger.warn("FIRE {}", event, new Throwable());
        }

        logger.debug("fireEvent: {}", event);

        final long start = System.nanoTime();

        for (final OIFitsCollectionListener listener : this.listeners) {
            listener.onProcess(event);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("fireEvent: duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }
    }
}
