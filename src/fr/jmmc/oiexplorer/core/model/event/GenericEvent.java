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
    /** optional destination listener (null means all) */
    private GenericEventListener<GenericEvent<V>, V> destination;
    /** subject id i.e. related object id (null allowed) */
    private final String subjectId;

    /**
     * Public constructor
     * @param source event source
     * @param type event type
     * @param destination optional destination listener (null means all)
     * @param objectId optional related object id
     */
    public GenericEvent(final Object source, final V type,
                        final GenericEventListener<GenericEvent<V>, V> destination, final String objectId) {
        if (source == null) {
            throw new IllegalArgumentException("undefined source argument for " + getClass().getSimpleName());
        }
        if (type == null) {
            throw new IllegalArgumentException("undefined type argument for " + getClass().getSimpleName());
        }
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.subjectId = objectId;
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
     * Return the optional destination listener (null means all)
     * @return optional destination listener (null means all)
     */
    public final GenericEventListener<GenericEvent<V>, V> getDestination() {
        return destination;
    }

    /**
     * PROTECTED: Define the optional destination listener (null means all)
     * @param destination optional destination listener (null means all)
     */
    final void setDestination(final GenericEventListener<GenericEvent<V>, V> destination) {
        this.destination = destination;
    }

    /**
     * Return the subject id i.e. related object id
     * @return subject id i.e. related object id
     */
    public final String getSubjectId() {
        return subjectId;
    }

    /* GenericEvent implements hashCode and equals because events can be postponed ie merged: 
     * only last event of the "same" kind is fired */
    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.type.hashCode();
        hash = 97 * hash + ((this.subjectId != null) ? this.type.hashCode() : 0);
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
        if (this.type != other.getType() && !this.type.equals(other.getType())) {
            return false;
        }
        if ((this.subjectId == null) ? (other.getSubjectId() != null) : !this.subjectId.equals(other.getSubjectId())) {
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
                + ((this.destination != null) ? " - destination= " + EventNotifier.getObjectInfo(this.destination) : "")
                + ((this.subjectId != null) ? " - subjectId= " + this.subjectId : "") + '}';
    }
}
