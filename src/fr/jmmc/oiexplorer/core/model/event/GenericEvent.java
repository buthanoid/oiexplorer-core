/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

/**
 * Base class for OIFits collection events consumed by OIFitsCollectionListener
 * @param <V> event type class
 */
public class GenericEvent<V> {

    /** event type */
    private final V type;

    /**
     * Public constructor without any argument
     * @param type event type
     */
    public GenericEvent(final V type) {
        this.type = type;
    }

    /**
     * Return the event type
     * @return event type
     */
    public final V getType() {
        return type;
    }

    /**
     * Return a string representation "<class name>{type=...}"
     * @return "<class name>{type=...}"
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{type=" + this.type + '}';
    }
}
