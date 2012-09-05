/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a base class for all generated classes in model.oi (Optical Interferometry Data Model)
 * @author bourgesl
 */
public class OIBase implements Cloneable {

    /** Class logger for model classes */
    protected static final Logger logger = LoggerFactory.getLogger(OIBase.class.getName());

    /**
     * Public Constructor
     */
    public OIBase() {
        super();
    }

    /**
     * Perform a deep-copy of the given other instance into this instance
     * 
     * Note: to be overriden in child class to perform deep-copy of class fields
     * @see OIBase#clone() 
     * 
     * @param other other instance
     */
    public void copy(final OIBase other) {
        // do nothing by default
    }

    /**
     * Return a "deep-copy" of this instance using the clone + copy(OIBase) method
     * @see OIBase#copy(fr.jmmc.oiexplorer.core.model.OIBase) 
     * 
     * @return "deep-copy" of this instance
     */
    @Override
    public final Object clone() {
        try {
            final OIBase other = (OIBase) super.clone();
            other.copy(this);
            return other;
        } catch (CloneNotSupportedException cnse) {
            logger.error("clone failure:", cnse);
        }
        return null;
    }

    /**
     * Return a deep "copy" of the list of objects (recursive call to clone() on each object instance)
     * @param <K> OIBase child class
     * @param list list of objects to clone
     * @return deep "copy" of the list
     */
    @SuppressWarnings("unchecked")
    public static <K extends OIBase> List<K> deepCopyList(final List<K> list) {
        if (list != null) {
            final List<K> newList = new ArrayList<K>(list.size());
            for (K o : list) {
                newList.add((K) o.clone());
            }
            return newList;
        }
        return null;
    }

    /**
     * Return a simple "copy" of the list of objects without cloning each object instance
     * @param <K> OIBase child class
     * @param list list of objects to clone
     * @return deep "copy" of the list
     */
    @SuppressWarnings("unchecked")
    public static <K> List<K> copyList(final List<K> list) {
        if (list != null) {
            final List<K> newList = new ArrayList<K>(list.size());
            for (K o : list) {
                newList.add(o);
            }
            return newList;
        }
        return null;
    }

    /**
     * Utility method for <code>equals()</code> methods.
     *
     * @param o1 one object
     * @param o2 another object
     *
     * @return <code>true</code> if they're both <code>null</code> or both equal
     */
    public static boolean areEquals(final Object o1, final Object o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }
}
