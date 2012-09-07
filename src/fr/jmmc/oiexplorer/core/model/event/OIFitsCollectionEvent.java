/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.oiexplorer.core.model.OIFitsCollection;
import fr.jmmc.oiexplorer.core.model.OIFitsCollectionEventListener;

/**
 * Base class for OIFits collection events consumed by OIFitsCollectionListener
 */
public final class OIFitsCollectionEvent extends GenericEvent<OIFitsCollectionEventType> {

    /** OIFits collection related to this event */
    private final OIFitsCollection oiFitsCollection;

    /**
     * Public constructor dealing with an OIFits collection 
     * @param source event source
     * @param type event type
     * @param oiFitsCollection OIFits collection related to this event
     */
    public OIFitsCollectionEvent(final Object source, final OIFitsCollectionEventType type, final OIFitsCollection oiFitsCollection) {
        this(source, type, null, oiFitsCollection);
    }

    /**
     * Public constructor dealing with an OIFits collection 
     * @param source event source
     * @param type event type
     * @param destination optional destination listener (null means all)
     * @param oiFitsCollection OIFits collection related to this event
     */
    public OIFitsCollectionEvent(final Object source, final OIFitsCollectionEventType type,
                                 final OIFitsCollectionEventListener destination, final OIFitsCollection oiFitsCollection) {
        super(source, type, destination, null);
        this.oiFitsCollection = oiFitsCollection;
    }

    /**
     * Return the OIFits collection related to this event
     * @return  OIFits collection related to this event or null if undefined
     */
    public OIFitsCollection getOIFitsCollection() {
        return oiFitsCollection;
    }
}
