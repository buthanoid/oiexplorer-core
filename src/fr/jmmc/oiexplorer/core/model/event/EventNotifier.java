/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is dedicated to event dispatching
 * @param <K> event class
 * @param <V> event type class
 * 
 * @author bourgesl
 */
public class EventNotifier<K extends GenericEvent<V>, V> {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(EventNotifier.class);
    /** flag to log a stack trace in method fireEvent() to debug events */
    private static boolean DEBUG_FIRE_EVENT = false;
    /* members */
    /** enable / disable notifications */
    private boolean notify = true;
    /** event listeners */
    private final CopyOnWriteArrayList<GenericEventListener<K, V>> listeners = new CopyOnWriteArrayList<GenericEventListener<K, V>>();

    /** 
     * Public Constructor
     */
    public EventNotifier() {
        super();
    }

    /**
     * Return true if event notifications are enabled
     * @return true if event notifications are enabled
     */
    public boolean isNotify() {
        return notify;
    }

    /**
     * Enable or disable event notifications
     * 
     * Note: can be overriden in child classes
     * 
     * @param notify true means event notification enabled; false means event notificationdisabled
     */
    public void setNotify(final boolean notify) {
        this.notify = notify;
    }

    /**
     * Register the given event listener
     * @param listener event listener
     */
    public final void register(final GenericEventListener<K, V> listener) {
        if (DEBUG_FIRE_EVENT) {
            logger.warn("REGISTER {}", listener, new Throwable());
        }
        this.listeners.addIfAbsent(listener);
    }

    /**
     * Unregister the given event listener
     * @param listener event listener
     */
    public final void unregister(final GenericEventListener<K, V> listener) {
        if (DEBUG_FIRE_EVENT) {
            logger.warn("UNREGISTER {}", listener, new Throwable());
        }
        this.listeners.remove(listener);
    }

    /**
     * Send an event to the registered listeners.
     * Note : any new listener registered during the processing of this event, will not be called
     * @param event event
     */
    public final void fireEvent(final K event) {
        // ensure events are fired by Swing EDT :
        if (!SwingUtils.isEDT()) {
            logger.warn("invalid thread : use EDT", new Throwable());
        }
        if (DEBUG_FIRE_EVENT) {
            logger.warn("FIRE {}", event, new Throwable());
        }

        logger.debug("fireEvent: {}", event);

        final long start = System.nanoTime();

        for (final GenericEventListener<K, V> listener : this.listeners) {
            listener.onProcess(event);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("fireEvent: duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }
    }

    public static boolean isDEBUG_FIRE_EVENT() {
        return DEBUG_FIRE_EVENT;
    }

    public static void setDEBUG_FIRE_EVENT(final boolean DEBUG_FIRE_EVENT) {
        EventNotifier.DEBUG_FIRE_EVENT = DEBUG_FIRE_EVENT;
    }
}
