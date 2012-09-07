/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for OIFits collection events consumed by OIFitsCollectionListener
 * @param <V> event type class
 */
public class GenericEvent<V> {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(GenericEvent.class);

    /* members */
    /** event type */
    private final V type;
    /** event source (sender) */
    private final Object source;
    /** optional destination listeners (null means all) */
    private Set<GenericEventListener<GenericEvent<V>, V>> destinations = null;
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
        this.subjectId = objectId;

        addDestination(destination);
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
     * PROTECTED: Return the optional destination listeners (null means all)
     * @return optional destination listeners (null means all)
     */
    final Set<GenericEventListener<GenericEvent<V>, V>> getDestinations() {
        return destinations;
    }

    /**
     * PROTECTED: Define the optional destination listeners (null means all)
     * @param destinations  optional destination listeners (null means all)
     */
    final void setDestinations(final Set<GenericEventListener<GenericEvent<V>, V>> destinations) {
        this.destinations = destinations;
    }

    /**
     * PROTECTED: Add the destination listener (null means all)
     * @param destination optional destination listeners (null means all)
     */
    final void addDestination(final GenericEventListener<GenericEvent<V>, V> destination) {
        if (destination == null) {
            // Note: if there was specific destination(s), eraze them i.e. send to all:
            this.destinations = null;
        } else {
            if (this.destinations == null) {
                this.destinations = new LinkedHashSet<GenericEventListener<GenericEvent<V>, V>>(4); // small
            }
            // ensure listener unicity:
            this.destinations.add(destination);
        }
    }

    /**
     * PROTECTED: Merge the given destination listeners with its destination listeners (null means all)
     * @param otherDestinations optional destination listeners (null means all)
     */
    final void mergeDestinations(final Set<GenericEventListener<GenericEvent<V>, V>> otherDestinations) {
        if (this.destinations != null || otherDestinations != null) {
            logger.warn("mergeDestinations: current vs other: {} vs {}", EventNotifier.getObjectInfo(destinations), EventNotifier.getObjectInfo(otherDestinations));
        }
        // means all:
        if (this.destinations != null) {
            logger.warn("mergeDestinations: current destinations: {}", EventNotifier.getObjectInfo(destinations));
            if (otherDestinations == null) {
                // means all:
                addDestination(null);
            } else {
                for (GenericEventListener<GenericEvent<V>, V> destination : otherDestinations) {
                    addDestination(destination);
                }
            }
            logger.warn("mergeDestinations: final destinations: {}", EventNotifier.getObjectInfo(destinations));
        }
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
                + ((this.destinations != null) ? " - destination= " + EventNotifier.getObjectInfo(this.destinations) : "")
                + ((this.subjectId != null) ? " - subjectId= " + this.subjectId : "") + '}';
    }
}
