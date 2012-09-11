/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import fr.jmmc.oiexplorer.core.model.event.GenericEventListener;

/**
 * This interface define the methods to be implemented by OIFitsCollectionManagerEvent listener implementations
 * 
 * @author bourgesl
 */
public interface OIFitsCollectionManagerEventListener
        extends GenericEventListener<OIFitsCollectionManagerEvent, OIFitsCollectionManagerEventType, Object> {
}