/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

/**
 * This enumeration defines all types of OIFitsManager events
 */
public enum OIFitsCollectionEventType {

  /** OIFits collection changed */
  CHANGED,
  /** subset changed */
  SUBSET_CHANGED,
  /** ask listeners to save their swing state into the OIFits collection  */
  DO_UPDATE,
  /** refresh swing state as the OIFits collection changed */
  REFRESH
}
