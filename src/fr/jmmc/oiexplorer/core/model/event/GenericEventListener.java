/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

/**
 * This interface define the onProcess method to handle generic event
 * @param <K> event class
 * @param <V> event type class
 * @author bourgesl
 */
public interface GenericEventListener<K extends GenericEvent<V>, V> {

    /**
     * Return the optional subject id i.e. related object id that this listener accepts
     * @see GenericEvent#subjectId
     * @param type event type
     * @return subject id i.e. related object id (null allowed)
     */
    public String getSubjectId(final V type);

    /**
     * Handle the given generic event
     * @param event generic event
     */
    public void onProcess(final K event);
}
