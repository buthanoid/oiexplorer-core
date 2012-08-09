/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.oiexplorer.core.model.OIFitsCollection;


/**
 * Base class for OIFits collection events consumed by OIFitsCollectionListener
 */
public class OIFitsCollectionEvent {

    /** event type */
    private final OIFitsCollectionEventType type;
    /** OIFits collection related to this event */
    private final OIFitsCollection oiFitsCollection;

    /**
     * Public constructor dealing with an observation
     * @param type event type
     * @param oiFitsCollection OIFits collection related to this event
     */
    public OIFitsCollectionEvent(final OIFitsCollectionEventType type, final OIFitsCollection oiFitsCollection) {
        this.type = type;
        this.oiFitsCollection = oiFitsCollection;
    }

    /**
     * Return the event type
     * @return event type
     */
    public final OIFitsCollectionEventType getType() {
        return type;
    }

    /**
     * Return the OIFits collection related to this event
     * @return  OIFits collection related to this event or null if undefined
     */
    public final OIFitsCollection getOIFitsCollection() {
        return oiFitsCollection;
    }

    /**
     * Return a string representation "OIFitsCollectionEvent{type=...}"
     * @return "OIFitsCollectionEvent{type=...}"
     */
    @Override
    public String toString() {
        return "OIFitsCollectionEvent{type=" + getType() + "}";
    }
}
