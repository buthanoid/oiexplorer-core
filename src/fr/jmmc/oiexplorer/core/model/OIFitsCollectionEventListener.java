/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.oiexplorer.core.model.event.GenericEvent;
import fr.jmmc.oiexplorer.core.model.event.GenericEventListener;
import fr.jmmc.oiexplorer.core.model.event.OIFitsCollectionEventType;

/**
 * This interface define the methods to be implemented by OIFitsCollectionEvent listener implementations
 * @author bourgesl
 */
public interface OIFitsCollectionEventListener extends GenericEventListener<GenericEvent<OIFitsCollectionEventType>, OIFitsCollectionEventType> {
}