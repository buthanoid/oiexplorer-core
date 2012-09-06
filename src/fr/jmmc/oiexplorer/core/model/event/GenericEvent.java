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
    /** event source (sender) */
    private final Object source;
    /** related object id (null allowed) */
    private final String objectId;

    /**
     * Public constructor without related object id
     * @param source event source
     * @param type event type
     */
    public GenericEvent(final Object source, final V type) {
        this(source, type, null);
    }

    /**
     * Public constructor
     * @param source event source
     * @param type event type
     * @param objectId optional related object id
     */
    public GenericEvent(final Object source, final V type, final String objectId) {
        if (source == null) {
            throw new IllegalArgumentException("undefined source argument for " + getClass().getSimpleName());
        }
        if (type == null) {
            throw new IllegalArgumentException("undefined type argument for " + getClass().getSimpleName());
        }
        this.type = type;
        this.source = source;
        this.objectId = objectId;
    }

    /**
     * Return the event type
     * @return event type
     */
    public final V getType() {
        return type;
    }

    /**
     * Return the event source
     * @return event source
     */
    public final Object getSource() {
        return source;
    }

    /**
     * Return the related object id (null allowed)
     * @return related object id (null allowed)
     */
    public final String getObjectId() {
        return objectId;
    }

    /* GenericEvent implements hashCode and equals because events can be postponed ie merged: 
     * only last event of the "same" kind is fired */
    @Override
    public final int hashCode() {
        int hash = 7;
        /*        hash = 97 * hash + this.source.hashCode(); */
        hash = 97 * hash + this.type.hashCode();
        hash = 97 * hash + ((this.objectId != null) ? this.type.hashCode() : 0);
        return hash;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericEvent<?> other = (GenericEvent<?>) obj;
        /*        
         if (this.source != other.getSource() && !this.source.equals(other.getSource())) {
         return false;
         }
         */
        if (this.type != other.getType() && !this.type.equals(other.getType())) {
            return false;
        }
        if ((this.objectId == null) ? (other.getObjectId() != null) : !this.objectId.equals(other.getObjectId())) {
            return false;
        }
        return true;
    }

    /**
     * Return a string representation "<class name>{type=...}"
     * @return "<class name>{type=...}"
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{source= " + EventNotifier.getObjectInfo(source)
                + " - type= " + this.type
                + ((this.objectId != null) ? " - objectId= " + this.objectId : "") + '}';
    }
}
