/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

/**
 * This class represents an unique target identifier among one OIFitsCollection
 * @author bourgesl
 */
public final class TargetUID {

    /* member */
    /** target name */
    private final String target;

    /** TODO: mapping of targetId per OIFitsFile */
    /**
     * Constructor
     * @param target target name
     */
    public TargetUID(final String target) {
        this.target = target;
    }

    /**
     * Return the target name
     * @return target name
     */
    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "TargetUID[" + target + ']';
    }

    /**
     * Return the hashCode() of the target name 
     * @return hashCode() of the target name 
     */
    @Override
    public int hashCode() {
        return (this.target != null ? this.target.hashCode() : 0);
    }

    /**
     * Returns true only if:
     * - obj is a TargetUID instance and target name are equals
     * - obj is a String instance and target name are equals
     * @param obj other object to compare with
     * @return true if target name are equals
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (TargetUID.class == obj.getClass()) {
            final TargetUID other = (TargetUID) obj;
            if ((this.target == null) ? (other.target != null) : !this.target.equals(other.getTarget())) {
                return false;
            }
        }
        if (String.class == obj.getClass()) {
            final String other = (String) obj;
            if ((this.target == null) ? (other != null) : !this.target.equals(other)) {
                return false;
            }
        }
        return true;
    }
}
