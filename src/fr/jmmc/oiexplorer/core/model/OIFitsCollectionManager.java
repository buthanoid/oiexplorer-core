/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.oiexplorer.core.model.event.EventNotifier;
import fr.jmmc.oiexplorer.core.model.event.GenericEventListener;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.oi.OIDataFile;
import fr.jmmc.oiexplorer.core.model.oi.OiDataCollection;
import fr.jmmc.oitools.model.OIFitsChecker;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.nom.tam.fits.FitsException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the oifits files collection.
 * @author mella, bourgesl
 */
public final class OIFitsCollectionManager {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(OIFitsCollectionManager.class);
    /** Singleton pattern */
    private final static OIFitsCollectionManager instance = new OIFitsCollectionManager();
    /* members */
    /** OIFits collection */
    private OIFitsCollection oiFitsCollection = null;
    /** Container of loaded data and user plot definitions */
    private OiDataCollection userCollection = null;
    /* event dispatchers */
    /** OIFitsCollectionEvent notifier */
    private final EventNotifier<OIFitsCollectionEvent, OIFitsCollectionEventType> oiFitsCollectionEventNotifier;

    /**
     * Return the Manager singleton
     * @return singleton instance
     */
    public static OIFitsCollectionManager getInstance() {
        return instance;
    }

    /** 
     * Prevent instanciation of singleton.
     * Manager instance should be obtained using getInstance().
     */
    private OIFitsCollectionManager() {
        super();
        this.oiFitsCollectionEventNotifier = new EventNotifier<OIFitsCollectionEvent, OIFitsCollectionEventType>() {
            /**
             * Disable repeating notifications ...
             */
            @Override
            public void setNotify(final boolean notify) {
                super.setNotify(notify);
                if (notify) {
                    // TODO: record event type to fire ...
                    fireOIFitsCollectionChanged();
                }
            }
        };

        reset(false);
    }

    /* --- OIFits file collection handling ------------------------------------- */
    /**
     * Load the given OI Fits Files with the given checker component
     * and add it to the OIFits collection
     * @param files files to load
     * @param checker checker component
     * @throws IOException if a fits file can not be loaded
     */
    public void loadOIFitsFiles(final File[] files, final OIFitsChecker checker) throws IOException {
        this.oiFitsCollectionEventNotifier.setNotify(false);
        try {
            for (File file : files) {
                loadOIFitsFile(file.getAbsolutePath(), checker);
            }
        } finally {
            this.oiFitsCollectionEventNotifier.setNotify(true);
        }
    }

    /**
     * Load OIDataCollection files (TODO: plot def to be handled)
     * @param collection OiDataCollection to look for
     * @param checker to report validation information
     * @throws IOException if a fits file can not be loaded
     */
    public void loadOIDataCollection(final OiDataCollection collection, final OIFitsChecker checker) throws IOException {
        this.oiFitsCollectionEventNotifier.setNotify(false);
        try {
            reset();

            for (OIDataFile oidataFile : collection.getFiles()) {
                loadOIFitsFile(oidataFile.getFile(), checker);
            }
        } finally {
            this.oiFitsCollectionEventNotifier.setNotify(true);
        }
        userCollection.getFiles().addAll(collection.getFiles());

        // TODO what about user plot definitions
    }

    /**
     * Load the given OI Fits File with the given checker component
     * and add it to the OIFits collection
     * @param fileLocation absolute File Path or remote URL
     * @param checker checker component
     * @throws IOException if a fits file can not be loaded
     */
    private void loadOIFitsFile(final String fileLocation, final OIFitsChecker checker) throws IOException {
        //@todo test if file has already been loaded before going further ??        

        StatusBar.show("loading file: " + fileLocation);

        try {
            // The file must be one oidata file (next line automatically unzip gz files)
            final OIFitsFile oifitsFile = OIFitsLoader.loadOIFits(checker, fileLocation);

            addOIFitsFile(oifitsFile);

            logger.info("file loaded : '{}'", oifitsFile.getAbsoluteFilePath());

        } catch (MalformedURLException mue) {
            throw new IOException("Could not load the file : " + fileLocation, mue);
        } catch (IOException ioe) {
            throw new IOException("Could not load the file : " + fileLocation, ioe);
        } catch (FitsException fe) {
            throw new IOException("Could not load the file : " + fileLocation, fe);
        }
    }

    // save ...
    // merge ...
    /* --- OIFits file collection handling ------------------------------------- */
    /**
     * Reset the OIFits file collection
     */
    public void reset() {
        reset(true);
    }

    /**
     * Reset the OIFits file collection
     * @param notify true means event notification enabled; false means event notificationdisabled
     */
    private void reset(final boolean notify) {
        oiFitsCollection = new OIFitsCollection();
        userCollection = new OiDataCollection();

        if (notify) {
            fireOIFitsCollectionChanged();
        }
    }

    public OIFitsCollection getOIFitsCollection() {
        return oiFitsCollection;
    }

    /** This method can be used to export current file list */
    public OiDataCollection getUserCollection() {
        return userCollection;
    }

    public void addOIFitsFile(final OIFitsFile oifitsFile) {
        if (oifitsFile != null) {
            oiFitsCollection.addOIFitsFile(oifitsFile);

            // Add new OIDataFile in collection 
            final OIDataFile dataFile = new OIDataFile();
            dataFile.setFile(oifitsFile.getAbsoluteFilePath());

            userCollection.getFiles().add(dataFile);

            fireOIFitsCollectionChanged();
        }
    }

    public OIFitsFile removeOIFitsFile(final OIFitsFile oifitsFile) {
        final OIFitsFile previous = oiFitsCollection.removeOIFitsFile(oifitsFile);

        if (previous != null) {
            // Remove OiDataFile from user collection
            final String filePath = oifitsFile.getAbsoluteFilePath();
            for (final Iterator<OIDataFile> it = userCollection.getFiles().iterator(); it.hasNext();) {
                final OIDataFile dataFile = it.next();
                if (filePath.equals(dataFile.getFile())) {
                    it.remove();
                }
            }

            fireOIFitsCollectionChanged();
        }

        return previous;
    }
    // --- EVENTS ----------------------------------------------------------------

    /**
     * Return the OIFitsCollectionEvent notifier
     * @return OIFitsCollectionEvent notifier
     */
    public EventNotifier<OIFitsCollectionEvent, OIFitsCollectionEventType> getOiFitsCollectionEventNotifier() {
        return oiFitsCollectionEventNotifier;
    }

    /**
     * This fires an OIFits collection changed event to all registered listeners.
     */
    private void fireOIFitsCollectionChanged() {
        if (!oiFitsCollectionEventNotifier.isNotify()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("fireOIFitsCollectionChanged: {}", this.oiFitsCollection);
        }

        this.oiFitsCollectionEventNotifier.fireEvent(new OIFitsCollectionEvent(OIFitsCollectionEventType.CHANGED, this.oiFitsCollection));
    }

    /**
     * This interface define the methods to be implemented by OIFits collection listener implementations
     * @author bourgesl
     */
    public interface OIFitsCollectionListener extends GenericEventListener<OIFitsCollectionEvent, OIFitsCollectionEventType> {
    }
}
