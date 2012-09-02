/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.oiexplorer.core.model.event.EventNotifier;
import fr.jmmc.oiexplorer.core.model.event.GenericEvent;
import fr.jmmc.oiexplorer.core.model.event.GenericEventListener;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEvent;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;
import fr.jmmc.oiexplorer.core.model.event.PlotDefinitionEvent;
import fr.jmmc.oiexplorer.core.model.event.SubsetDefinitionEvent;
import fr.jmmc.oiexplorer.core.model.oi.OIDataFile;
import fr.jmmc.oiexplorer.core.model.oi.OiDataCollection;
import fr.jmmc.oiexplorer.core.model.oi.SubsetDefinition;
import fr.jmmc.oiexplorer.core.model.oi.TableUID;
import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;
import fr.jmmc.oiexplorer.core.util.Constants;
import fr.jmmc.oitools.model.OIFitsChecker;
import fr.jmmc.oitools.model.OIFitsFile;
import fr.jmmc.oitools.model.OIFitsLoader;
import fr.jmmc.oitools.model.OITable;
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
    /** Current key */
    private final static String CURRENT = "CURRENT";
    /* members */
    /** OIFits collection */
    private OIFitsCollection oiFitsCollection = null;
    /** Container of loaded data and user plot definitions */
    private OiDataCollection userCollection = null;
    /* event dispatchers */
    /** OIFitsCollectionEvent notifier */
    private final EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> oiFitsCollectionEventNotifier;
    /** SubsetDefinitionEvent notifier */
    private final EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> subsetDefinitionEventNotifier;
    /** PlotDefinitionEvent notifier */
    private final EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> plotDefinitionEventNotifier;

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

        this.oiFitsCollectionEventNotifier = new EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType>();
        this.subsetDefinitionEventNotifier = new EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType>();
        this.plotDefinitionEventNotifier = new EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType>();

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

        for (File file : files) {
            loadOIFitsFile(file.getAbsolutePath(), checker);
        }
    }

    /**
     * Load OIDataCollection files (TODO: plot def to be handled)
     * @param oiDataCollection OiDataCollection to look for
     * @param checker to report validation information
     * @throws IOException if a fits file can not be loaded
     */
    public void loadOIDataCollection(final OiDataCollection oiDataCollection, final OIFitsChecker checker) throws IOException {

        // first reset:
        reset();

        for (OIDataFile oidataFile : oiDataCollection.getFiles()) {
            loadOIFitsFile(oidataFile.getFile(), checker);
        }

        // TODO: check missing files !

        // TODO what about user plot definitions ...
        // add them but should be check for consistency related to loaded files (errors can occur while loading):
        this.userCollection.getSubsetDefinitions().addAll(oiDataCollection.getSubsetDefinitions());
        this.userCollection.getPlotDefinitions().addAll(oiDataCollection.getPlotDefinitions());
        this.userCollection.getPlots().addAll(oiDataCollection.getPlots());
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

    // TODO: save / merge ... (elsewhere)
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

    public void addOIFitsFile(final OIFitsFile oiFitsFile) {
        if (oiFitsFile != null) {
            oiFitsCollection.addOIFitsFile(oiFitsFile);

            // Add new OIDataFile in collection 
            final OIDataFile dataFile = new OIDataFile();

            final String id = oiFitsFile.getName().replaceAll(Constants.REGEXP_INVALID_TEXT_CHARS, "_");

            // TODO: make it unique !!
            dataFile.setName(id);

            dataFile.setFile(oiFitsFile.getAbsoluteFilePath());
            // checksum !

            // store oiFitsFile reference:
            dataFile.setOIFitsFile(oiFitsFile);

            userCollection.getFiles().add(dataFile);

            fireOIFitsCollectionChanged();
        }
    }

    public OIFitsFile removeOIFitsFile(final OIFitsFile oiFitsFile) {
        final OIFitsFile previous = oiFitsCollection.removeOIFitsFile(oiFitsFile);

        if (previous != null) {
            // Remove OiDataFile from user collection
            final String filePath = oiFitsFile.getAbsoluteFilePath();
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

    /** This method can be used to export current file list */
    public OiDataCollection getUserCollection() {
        return userCollection;
    }

    /* --- file handling ------------------------------------- */
    /**
     * Return an OIDataFile given its name
     * @param name file name
     * @return OIDataFile or null if not found
     */
    public OIDataFile getOIDataFile(final String name) {
        for (OIDataFile dataFile : this.userCollection.getFiles()) {
            if (name.equals(dataFile.getName())) {
                return dataFile;
            }
        }
        return null;
    }

    /**
     * Return an OIDataFile given its related OIFitsFile
     * @param oiFitsFile OIFitsFile to find
     * @return OIDataFile or null if not found
     */
    public OIDataFile getOIDataFile(final OIFitsFile oiFitsFile) {
        for (OIDataFile dataFile : this.userCollection.getFiles()) {
            if (oiFitsFile == dataFile.getOIFitsFile()) {
                return dataFile;
            }
        }
        return null;
    }

    /* --- subset definition handling ------------------------------------- */
    /**
     * Return the current subset definition
     * @return subset definition
     */
    public SubsetDefinition getCurrentSubsetDefinition() {
        SubsetDefinition subsetDefinition = getSubsetDefinition(CURRENT);
        if (subsetDefinition == null) {
            subsetDefinition = new SubsetDefinition();
            subsetDefinition.setName(CURRENT);
            this.userCollection.getSubsetDefinitions().add(subsetDefinition);
        }
        return subsetDefinition;
    }

    /**
     * Return a subset definition by its name
     * @param name plot definition name
     * @return subset definition or null if not found
     */
    public SubsetDefinition getSubsetDefinition(final String name) {
        for (SubsetDefinition subsetDefinition : this.userCollection.getSubsetDefinitions()) {
            if (name.equals(subsetDefinition.getName())) {
                return subsetDefinition;
            }
        }
        return null;
    }

    /**
     * Update the subset definition corresponding to the same name
     * @param source event source
     * @param subsetDefinition subset definition with updated values
     */
    public void updateSubsetDefinition(final Object source, final SubsetDefinition subsetDefinition) {
        final SubsetDefinition subset = getSubsetDefinition(subsetDefinition.getName());

        if (subset != subsetDefinition) {
            // TODO: do copy
            throw new IllegalStateException("Not implemented !");
        }

        // Get OIFitsFile structure for this target:
        final OIFitsFile oiFitsSubset;

        if (this.oiFitsCollection.isEmpty()) {
            oiFitsSubset = null;
        } else {
            final OIFitsFile dataForTarget = this.oiFitsCollection.getOiDataList(subset.getTarget());

            if (dataForTarget == null) {
                oiFitsSubset = null;
            } else {
                // apply table selection:
                if (subset.getTables().isEmpty()) {
                    oiFitsSubset = dataForTarget;
                } else {
                    oiFitsSubset = new OIFitsFile();

                    for (TableUID table : subset.getTables()) {
                        final OIDataFile oiDataFile = table.getFile();
                        final OIFitsFile oiFitsFile = oiDataFile.getOIFitsFile();

                        if (oiFitsFile != null) {
                            final Integer extNb = table.getExtNb();

                            // add all tables:
                            for (OITable oiData : dataForTarget.getOiTables()) {
                                // file path comparison:
                                if (oiData.getOIFitsFile().equals(oiFitsFile)) {

                                    if (extNb == null || oiData.getExtNb() == extNb.intValue()) {
                                        oiFitsSubset.addOiTable(oiData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        logger.warn("updateSubsetDefinition: oiFitsSubset: " + oiFitsSubset);

        subset.setOIFitsSubset(oiFitsSubset);

        fireSubsetDefinitionChanged(source, subset);
    }

    /* --- plot definition handling --------- ---------------------------- */
    /**
     * Return the current plot definition
     * @return plot definition
     */
    public PlotDefinition getCurrentPlotDefinition() {
        PlotDefinition plotDefinition = getPlotDefinition(CURRENT);
        if (plotDefinition == null) {
            plotDefinition = new PlotDefinition();
            plotDefinition.setName(CURRENT);
            this.userCollection.getPlotDefinitions().add(plotDefinition);

            //TODO: copy info from default plot definition (preset) ??
        }
        return plotDefinition;
    }

    /**
     * Return a plot definition by its name
     * @param name plot definition name
     * @return plot definition or null if not found
     */
    public PlotDefinition getPlotDefinition(final String name) {
        for (PlotDefinition plotDefinition : this.userCollection.getPlotDefinitions()) {
            if (name.equals(plotDefinition.getName())) {
                return plotDefinition;
            }
        }
        return null;
    }

    /**
     * Update the plot definition corresponding to the same name
     * @param source event source
     * @param plotDefinition plot definition with updated values
     */
    public void updatePlotDefinition(final Object source, final PlotDefinition plotDefinition) {
        final PlotDefinition plotDef = getPlotDefinition(plotDefinition.getName());

        if (plotDef == null) {
            // help to support preset case
        } else if (plotDef != plotDefinition) {
            // TODO: do copy
            throw new IllegalStateException("Not yet implemented !");
        }
        firePlotDefinitionChanged(source, plotDefinition);
    }

    // --- EVENTS ----------------------------------------------------------------
    /**
     * Return the OIFitsCollectionEvent notifier
     * @return OIFitsCollectionEvent notifier
     */
    public EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> getOiFitsCollectionEventNotifier() {
        return oiFitsCollectionEventNotifier;
    }

    /**
     * This fires an OIFitsCollectionChanged event to all registered listeners ASYNCHRONOUSLY !
     */
    private void fireOIFitsCollectionChanged() {
        if (this.oiFitsCollectionEventNotifier.hasListeners()) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireOIFitsCollectionChanged: {}", this.oiFitsCollection);
            }

            this.oiFitsCollectionEventNotifier.fireEvent(new OIFitsCollectionEvent(this, OIFitsCollectionEventType.CHANGED, this.oiFitsCollection));
        }
    }

    /**
     * Return the SubsetDefinitionEvent notifier
     * @return SubsetDefinitionEvent notifier
     */
    public EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> getSubsetDefinitionEventNotifier() {
        return subsetDefinitionEventNotifier;
    }

    /**
     * This fires a SubsetDefinitionChanged event to all registered listeners ASYNCHRONOUSLY !
     * @param source event source
     * @param subsetDefinition subset definition to use
     */
    private void fireSubsetDefinitionChanged(final Object source, final SubsetDefinition subsetDefinition) {
        if (this.subsetDefinitionEventNotifier.hasListeners()) {
            if (logger.isDebugEnabled()) {
                logger.debug("fireSubsetDefinitionChanged: {}", this.oiFitsCollection);
            }

            this.subsetDefinitionEventNotifier.fireEvent(new SubsetDefinitionEvent(source, OIFitsCollectionEventType.SUBSET_CHANGED, subsetDefinition));
        }
    }

    /**
     * Return the PlotDefinitionEvent notifier
     * @return PlotDefinitionEvent notifier
     */
    public EventNotifier<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> getPlotDefinitionEventNotifier() {
        return plotDefinitionEventNotifier;
    }

    /**
     * This fires a PlotDefinitionChanged event to all registered listeners ASYNCHRONOUSLY !
     * @param source event source
     * @param plotDefinition plot definition to use
     */
    private void firePlotDefinitionChanged(final Object source, final PlotDefinition plotDefinition) {
        if (this.plotDefinitionEventNotifier.hasListeners()) {
            if (logger.isDebugEnabled()) {
                logger.debug("firePlotDefinitionChanged: {}", this.oiFitsCollection);
            }

            this.plotDefinitionEventNotifier.fireEvent(new PlotDefinitionEvent(source, OIFitsCollectionEventType.PLOT_DEFINITION_CHANGED, plotDefinition));
        }
    }

    /**
     * This interface define the methods to be implemented by OIFitsCollectionEvent listener implementations
     * @author bourgesl
     */
    public interface OIFitsCollectionEventListener extends GenericEventListener<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> {
    }
}
