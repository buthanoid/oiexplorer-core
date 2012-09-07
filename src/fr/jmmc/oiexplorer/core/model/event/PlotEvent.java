/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.oiexplorer.core.model.OIFitsCollectionEventListener;
import fr.jmmc.oiexplorer.core.model.oi.Plot;

/**
 * Base class for plot events consumed by OIFitsCollectionListener
 */
public final class PlotEvent extends GenericEvent<OIFitsCollectionEventType> {

    /** plot related to this event */
    private final Plot plot;

    /**
     * Public constructor dealing with a plot
     * @param source event source
     * @param type event type
     * @param plot plot related to this event
     */
    public PlotEvent(final Object source, final OIFitsCollectionEventType type, final Plot plot) {
        this(source, type, null, plot);
    }

    /**
     * Public constructor dealing with a plot
     * @param source event source
     * @param type event type
     * @param destination optional destination listener (null means all)
     * @param plot plot related to this event
     */
    public PlotEvent(final Object source, final OIFitsCollectionEventType type,
                     final OIFitsCollectionEventListener destination, final Plot plot) {
        super(source, type, destination, (plot != null) ? plot.getName() : null);
        this.plot = plot;
    }

    /**
     * Return the plot related to this event
     * @return plot related to this event or null if undefined
     */
    public Plot getPlot() {
        return plot;
    }
}
