/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

/**
 * This interface define the methods to be implemented by OIFits collection listener implementations
 * @author bourgesl
 */
public interface OIFitsCollectionListener {

  /**
   * Handle the given OIFits collection event
   * @param event OIFits collection event
   */
  public void onProcess(final OIFitsCollectionEvent event);
}
