/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public final class EventNotifier<K extends GenericEvent<V>, V> {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(EventNotifier.class);
    /** flag to log a stack trace in method register/unregister to debug registration */
    private static final boolean DEBUG_LISTENER = false;
    /** flag to log a stack trace in method fireEvent() to debug events */
    private static final boolean DEBUG_FIRE_EVENT = false;
    /** EventNotifierController singleton */
    private static final EventNotifierController globalController = new EventNotifierController();
    /* members */
    /** event listeners */
    private final CopyOnWriteArrayList<GenericEventListener<K, V>> listeners = new CopyOnWriteArrayList<GenericEventListener<K, V>>();
    /** queued events delivered asap by EDT (ordered by insertion order) */
    private final Map<K, K> eventQueue = new LinkedHashMap<K, K>(8);

    /** 
     * Public Constructor
     */
    public EventNotifier() {
        super();
    }

    /**
     * Register the given event listener
     * @param listener event listener
     */
    public void register(final GenericEventListener<K, V> listener) {
        if (DEBUG_LISTENER) {
            logger.warn("REGISTER {}", getObjectInfo(listener), new Throwable());
        }
        this.listeners.addIfAbsent(listener);
    }

    /**
     * Unregister the given event listener
     * @param listener event listener
     */
    public void unregister(final GenericEventListener<K, V> listener) {
        if (DEBUG_LISTENER) {
            logger.warn("UNREGISTER {}", getObjectInfo(listener), new Throwable());
        }
        this.listeners.remove(listener);
    }

    /**
     * Return true if there is at least one registered event listener
     * @return true if there is at least one registered event listener 
     */
    public boolean hasListeners() {
        return !this.listeners.isEmpty();
    }

    /**
     * Send an event to the registered listeners.
     * Note : any new listener registered during the processing of this event, will not be called
     * @param event event to fire
     * @throws IllegalStateException if this method is not called by Swing EDT
     */
    public void fireEvent(final K event) throws IllegalStateException {
        fireEvent(event, true);
    }

    /**
     * Send an event to the registered listeners.
     * Note : any new listener registered during the processing of this event, will not be called
     * @param event event to fire
     * @param useQueue true to queue events (ASYNC); false to fire event (SYNC)
     * @throws IllegalStateException if this method is not called by Swing EDT
     */
    private void fireEvent(final K event, final boolean useQueue) throws IllegalStateException {
        // ensure events are fired by Swing EDT:
        if (!SwingUtils.isEDT()) {
            throw new IllegalStateException("invalid thread : use EDT", new Throwable());
        }

        if (this.listeners.isEmpty()) {
            return;
        }

        // queue this event to avoid concurrency issues and repeated event notifications (thanks to SET):
        if (useQueue) {
            if (DEBUG_FIRE_EVENT) {
                logger.warn("QUEUE EVENT {}", event, new Throwable());
            }
            // queue this event:

            // TRICKY: use event as key (hashcode / equals on type / source) 
            // but use value to have up to date arguments:
            this.eventQueue.put(event, event);

            // register this notifier in EDT:
            globalController.queueEventNotifier(this);

            return;
        }

        // Fire events:
        if (DEBUG_FIRE_EVENT) {
            logger.warn("START FIRE {}", event, new Throwable());
        }

        logger.debug("fireEvent: {}", event);

        final long start = System.nanoTime();

        for (final GenericEventListener<K, V> listener : this.listeners) {
            // do not fire event to the listener if it is also the source of this event:
            if (event.getSource() != listener) {
                if (DEBUG_FIRE_EVENT) {
                    logger.warn("  FIRE {} TO {}", event, getObjectInfo(listener));
                }
                listener.onProcess(event);
            }
        }

        // TODO: detect listener changes ??

        if (logger.isDebugEnabled()) {
            logger.debug("fireEvent: duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }

        if (DEBUG_FIRE_EVENT) {
            logger.warn("END FIRE {}", event);
        }
    }

    /**
     * Fire queued events
     */
    private void fireQueuedEvents() {

        // Copy the queued event set and clear it (available):
        if (DEBUG_FIRE_EVENT) {
            logger.warn("START FIRE QUEUED EVENTS {}", eventQueue);
        }

        // use only values (up to date event arguments):
        final List<K> events = new ArrayList<K>(eventQueue.values());

        eventQueue.clear();

        for (K event : events) {
            fireEvent(event, false);
        }

        if (DEBUG_FIRE_EVENT) {
            logger.warn("END FIRE QUEUED EVENTS");
        }
    }

    /**
     * Return a string representation "<class name>#<hashCode>"
     * @return "<class name>#<hashCode>"
     */
    @Override
    public String toString() {
        return getObjectInfo(this);
    }

    /**
     * Return the string representation "<class name>#<hashCode>"
     * @param o any object
     * @return "<class name>#<hashCode>"
     */
    public static String getObjectInfo(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.getClass().getName() + '#' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * This class represents the event notifier controller i.e. avoids concurrent notifications
     */
    private final static class EventNotifierController implements Runnable {

        /** maximum number of fire queued notifiers */
        private final static int MAX_LOOP = 100;
        /* members */
        /** flag indicating that this task is registered in EDT */
        private boolean registeredEDT = false;
        /** queued event notifiers */
        private final Set<EventNotifier<?, ?>> queuedNotifiers = new LinkedHashSet<EventNotifier<?, ?>>();
        /** callback list */
        private final List<Runnable> callbacks = new ArrayList<Runnable>();

        /**
         * Private constructor
         */
        EventNotifierController() {
            super();
        }

        void queueEventNotifier(final EventNotifier<?, ?> eventNotifier) {
            queuedNotifiers.add(eventNotifier);

            if (!registeredEDT) {
                registeredEDT = true;

                if (DEBUG_FIRE_EVENT) {
                    logger.warn("REGISTER CONTROLLER IN EDT (invokeLater)");
                }

                // fireQueuedNotifiers by next EDT event (ASYNC):
                SwingUtils.invokeLaterEDT(this);
            }
        }

        public void addCallback(final Runnable callback) {
            callbacks.add(callback);
        }

        @Override
        public void run() {
            if (DEBUG_FIRE_EVENT) {
                logger.warn("CONTROLLER EXECUTED BY EDT (invokeLater)");
            }
            try {
                // fire queued events:
                fireQueuedNotifiers();

                runCallbacks();

            } finally {
                // at the end: to avoid too much use of invokeLater !
                registeredEDT = false;
            }
        }

        /**
         * Fire queued notifiers if possible
         */
        private void fireQueuedNotifiers() {

            if (queuedNotifiers.isEmpty()) {
                return;
            }
            if (DEBUG_FIRE_EVENT) {
                logger.warn("START FIRE QUEUED NOTIFIERS");
            }

            int loop = 0;
            do {

                if (DEBUG_FIRE_EVENT) {
                    logger.warn("LOOP {} - FIRE QUEUED NOTIFIERS {}", loop, queuedNotifiers);
                }

                // Copy the queued event notifiers and clear it (available):
                final List<EventNotifier<?, ?>> eventNotifiers = new ArrayList<EventNotifier<?, ?>>(queuedNotifiers);

                queuedNotifiers.clear();

                // TODO: process only 1 notifier at a time (loop) to maximize event merges of next events:

                for (EventNotifier<?, ?> notifier : eventNotifiers) {
                    notifier.fireQueuedEvents();
                }

                loop++;

                if (loop > MAX_LOOP) {
                    throw new IllegalStateException("maximum number of fireQueuedEvents() execution reached !");
                }

                if (DEBUG_FIRE_EVENT) {
                    logger.warn("LOOP {} - END FIRE QUEUED NOTIFIERS", loop);
                }

            } while (!queuedNotifiers.isEmpty());

            if (DEBUG_FIRE_EVENT) {
                logger.warn("END FIRE QUEUED NOTIFIERS");
            }
        }

        void runCallbacks() {
            if (callbacks.isEmpty()) {
                return;
            }

            if (DEBUG_FIRE_EVENT) {
                logger.warn("START RUN CALLBACKS");
            }

            for (Iterator<Runnable> it = callbacks.iterator(); it.hasNext();) {
                final Runnable callback = it.next();
                it.remove();

                if (DEBUG_FIRE_EVENT) {
                    logger.warn("RUN CALLBACK {}", getObjectInfo(callback));
                }

                callback.run();
            }

            if (DEBUG_FIRE_EVENT) {
                logger.warn("END RUN CALLBACKS");
            }
        }
    }

    public static void addCallback(final Runnable callback) {
        globalController.addCallback(callback);
    }
}
