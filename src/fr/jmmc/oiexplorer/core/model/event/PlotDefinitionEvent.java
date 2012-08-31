/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.oiexplorer.core.model.plot.PlotDefinition;

/**
 * Base class for plot definition events consumed by OIFitsCollectionListener
 */
public final class PlotDefinitionEvent extends GenericEvent<OIFitsCollectionEventType> {

    /** plot definition related to this event */
    private final PlotDefinition plotDefinition;

    /**
     * Public constructor dealing with a plot definition
     * @param type event type
     * @param plotDefinition plot definition related to this event
     */
    public PlotDefinitionEvent(final OIFitsCollectionEventType type, final PlotDefinition plotDefinition) {
        super(type);
        this.plotDefinition = plotDefinition;
    }

    /**
     * Return the plot definition related to this event
     * @return plot definition related to this event or null if undefined
     */
    public final PlotDefinition getPlotDefinition() {
        return plotDefinition;
    }
}
