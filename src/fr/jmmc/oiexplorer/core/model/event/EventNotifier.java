/*******************************************************************************
 * JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
 ******************************************************************************/
package fr.jmmc.oiexplorer.core.model.event;

import fr.jmmc.jmcs.gui.util.SwingUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
public final class EventNotifier<K extends GenericEvent<V>, V> implements Comparable<EventNotifier<?, ?>> {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(EventNotifier.class);
    /** flag to log a stack trace in method register/unregister to debug registration */
    private static final boolean DEBUG_LISTENER = true;
    /** flag to log information useful to debug events */
    private static final boolean DEBUG_FIRE_EVENT = true;
    /** flag to log also a stack trace to debug events */
    private static final boolean DEBUG_STACK = false;
    /** flag to detect new registered listener(s) while fireEvent runs to fire events to them also */
    private static final boolean FIRE_NEW_REGISTERED_LISTENER = false;
    /** EventNotifierController singleton */
    private static final EventNotifierController globalController = new EventNotifierController();
    /* members */
    /** event notifier's priority (compare to other event notifiers): lower values means higher priority */
    private final int priority;
    /** flag to disable listener notification if it is the source of the event */
    private final boolean skipSourceListener;
    /** event listeners using WeakReferences to avoid memory leaks */
    /* may detect widgets waiting for events on LOST subject objects ?? */
    private final CopyOnWriteArrayList<WeakReference<GenericEventListener<K, V>>> listeners = new CopyOnWriteArrayList<WeakReference<GenericEventListener<K, V>>>();
    /** queued events delivered asap by EDT (ordered by insertion order) */
    private final Map<K, K> eventQueue = new LinkedHashMap<K, K>(8);

    /** 
     * Public Constructor
     * @param priority event notifier's priority
     */
    public EventNotifier(final int priority) {
        this(priority, true);
    }

    /** 
     * Public Constructor
     * @param skipSourceListener flag to disable listener notification if it is the source of the event
     */
    public EventNotifier(final boolean skipSourceListener) {
        this(0, skipSourceListener);
    }

    /** 
     * Public Constructor
     * @param priority event notifier's priority
     * @param skipSourceListener flag to disable listener notification if it is the source of the event
     */
    public EventNotifier(final int priority, final boolean skipSourceListener) {
        this.priority = priority;
        this.skipSourceListener = skipSourceListener;
    }

    /**
     * Return the event notifier's priority
     * @return event notifier's priority
     */
    private int getPriority() {
        return priority;
    }

    /**
     * Compare this event notifier with another
     * @param other another event notifier
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     */
    public int compareTo(final EventNotifier<?, ?> other) {
        final int otherPriority = other.getPriority();
        return (priority < otherPriority) ? -1 : ((priority == otherPriority) ? 0 : 1);
    }

    /**
     * Register the given event listener
     * @param listener event listener
     */
    public void register(final GenericEventListener<K, V> listener) {
        if (DEBUG_LISTENER) {
            logger.warn("REGISTER {}", getObjectInfo(listener), (DEBUG_STACK) ? new Throwable() : null);
        }
        this.listeners.addIfAbsent(new WeakReference<GenericEventListener<K, V>>(listener));
    }

    /**
     * Unregister the given event listener
     * @param listener event listener
     */
    public void unregister(final GenericEventListener<K, V> listener) {
        if (DEBUG_LISTENER) {
            logger.warn("UNREGISTER {}", getObjectInfo(listener), (DEBUG_STACK) ? new Throwable() : null);
        }

        WeakReference<GenericEventListener<K, V>> ref;
        GenericEventListener<K, V> l;

        for (int i = 0, size = this.listeners.size(); i < size; i++) {
            ref = this.listeners.get(i);
            l = ref.get();

            if (l == null || l == listener) {
                // remove empty reference (GC):
                this.listeners.remove(i);
                size--;
                i--;
            }
        }
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
            throw new IllegalStateException("invalid thread : use EDT", (DEBUG_STACK) ? new Throwable() : null);
        }
        // queue this event to avoid concurrency issues and repeated event notifications (thanks to SET):
        if (useQueue) {
            if (DEBUG_FIRE_EVENT) {
                logger.warn("QUEUE EVENT {}", event, (DEBUG_STACK) ? new Throwable() : null);
            }
            // queue this event:

            // TRICKY: use event as key (hashcode / equals on type / subjectId) 
            // but use value to have up to date arguments:
            final K prevEvent = this.eventQueue.put(event, event);

            // fix destination on last event (all vs destination listener):
            if (event.getDestination() != null && prevEvent != null && prevEvent.getDestination() == null) {
                event.setDestination(null);
            }

            // register this notifier in EDT:
            globalController.queueEventNotifier(this);

            return;
        }

        // check listeners just before firing events:
        if (this.listeners.isEmpty()) {
            logger.warn("FIRE {} - NO LISTENER", event, (DEBUG_STACK) ? new Throwable() : null);
            return;
        }

        // Fire events:
        if (DEBUG_FIRE_EVENT) {
            logger.warn("START FIRE {}", event, (DEBUG_STACK) ? new Throwable() : null);
        }

        logger.debug("fireEvent: {}", event);

        // optional event source:
        final Object source = event.getSource();
        final GenericEventListener<GenericEvent<V>, V> destination = event.getDestination();
        final String subjectId = event.getSubjectId();
        String listenerSubjectId;

        final long start = System.nanoTime();

        // used listeners:
        final Set<GenericEventListener<K, V>> firedListeners = new HashSet<GenericEventListener<K, V>>();
        boolean done;

        WeakReference<GenericEventListener<K, V>> ref;
        GenericEventListener<K, V> listener;

        do {
            // multiple pass until all listener fired:
            for (int i = 0, size = this.listeners.size(); i < size; i++) {
                ref = this.listeners.get(i);
                listener = ref.get();

                if (listener == null) {
                    // remove empty reference (GC):
                    this.listeners.remove(i);
                    size--;
                    i--;
                } else if (!firedListeners.contains(listener)) {

                    // check destination:
                    if (destination == null || destination == listener) {
                        firedListeners.add(listener);

                        // do not fire event to the listener if it is also the source of this event:
                        if ((!skipSourceListener) || (listener != source)) {
                            if (DEBUG_FIRE_EVENT) {
                                logger.warn("  FIRE {} TO {}", event, getObjectInfo(listener));
                            }

                            if (subjectId == null) {
                                listener.onProcess(event);
                            } else {
                                listenerSubjectId = listener.getSubjectId(event.getType());

                                // check if the listener is accepting this subject id or any (null):
                                if ((listenerSubjectId == null) || (subjectId.equals(listenerSubjectId))) {
                                    listener.onProcess(event);

                                } else if (DEBUG_FIRE_EVENT) {
                                    logger.warn("Skip Listener {} because subjectId does not match: {} != {}",
                                            new Object[]{getObjectInfo(listener), event.getSubjectId(), listenerSubjectId});
                                }
                            }
                        } else if (DEBUG_FIRE_EVENT) {
                            logger.warn("Skip Listener {} because it is the source of this event: {}", getObjectInfo(listener), event);
                        }
                    } else if (DEBUG_FIRE_EVENT) {
                        logger.warn("Skip Listener {} because it is not the destination of this event: {}", getObjectInfo(listener), event);
                    }
                }
            }

            // check if new listener(s) registered meanwhile:
            done = true;

            if (FIRE_NEW_REGISTERED_LISTENER) {
                if (destination != null) {
                    for (int i = 0, size = this.listeners.size(); i < size; i++) {
                        ref = this.listeners.get(i);
                        listener = ref.get();
                        if (listener == null) {
                            // remove empty reference (GC):
                            this.listeners.remove(i);
                            size--;
                            i--;
                        } else if (!firedListeners.contains(listener)) {
                            // there is still one listener not fired:
                            done = false;
                            break;
                        }
                    }
                } else if (DEBUG_FIRE_EVENT) {
                    logger.warn("Skip Fire new listener as the event has a destination: {}", event);
                }
            }
        } while (!done);

        if (logger.isDebugEnabled()) {
            logger.debug("fireEvent: duration = {} ms.", 1e-6d * (System.nanoTime() - start));
        }

        if (DEBUG_FIRE_EVENT) {
            logger.warn("END FIRE {} : duration = {} ms.", event, 1e-6d * (System.nanoTime() - start));
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
     * Return the string representation "<simple class name>#<hashCode>"
     * @param o any object
     * @return "<class name>#<hashCode>"
     */
    public static String getObjectInfo(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * Return the string representation "<full class name>#<hashCode>"
     * @param o any object
     * @return "<class name>#<hashCode>"
     */
    public static String getFullObjectInfo(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * This class represents the event notifier controller i.e. avoids concurrent notifications
     */
    private final static class EventNotifierController implements Runnable {

        /* members */
        /** flag indicating that this task is registered in EDT */
        private boolean registeredEDT = false;
        /** queued event notifiers (ordered by insertion order) */
        private final Set<EventNotifier<?, ?>> queuedNotifiers = new LinkedHashSet<EventNotifier<?, ?>>();
        /** callback list */
        private final List<Runnable> callbacks = new ArrayList<Runnable>();
        /** temporary storage for queuedNotifiers */
        private final List<EventNotifier<?, ?>> queuedNotifiersCopy = new ArrayList<EventNotifier<?, ?>>();

        /**
         * Private constructor
         */
        EventNotifierController() {
            super();
        }

        void queueEventNotifier(final EventNotifier<?, ?> eventNotifier) {
            queuedNotifiers.add(eventNotifier);
            registerEDT();
        }

        private void registerEDT() {
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
            boolean done = true;
            try {
                // fire queued events:
                done = fireQueuedNotifiers();

            } finally {
                // at the end: to avoid too much use of invokeLater !
                registeredEDT = false;
            }
            if (!done) {
                // register EDT
                registerEDT();
            } else {
                // run callbacks once all queued notifiers / events are fired:
                runCallbacks();
            }
        }

        /**
         * Fire queued notifiers if possible using EDT (may use interlacing with standard Swing EDT)
         * @return true if queuedNotifiers is empty i.e. done
         */
        private boolean fireQueuedNotifiers() {

            if (queuedNotifiers.isEmpty()) {
                return true;
            }
            if (DEBUG_FIRE_EVENT) {
                logger.warn("START FIRE {} QUEUED NOTIFIERS", queuedNotifiers.size());
            }

            // process only 1 notifier at a time (loop) to maximize event merges of next events:
            queuedNotifiersCopy.clear();
            queuedNotifiersCopy.addAll(queuedNotifiers);

            if (queuedNotifiersCopy.size() > 1) {
                Collections.sort(queuedNotifiersCopy);
            }

            if (DEBUG_FIRE_EVENT) {
                logger.warn("SORTED QUEUED NOTIFIERS {}", queuedNotifiersCopy);
            }

            // Get first (highest priority):
            final EventNotifier<?, ?> eventNotifier = queuedNotifiersCopy.get(0);
            queuedNotifiersCopy.clear();

            // Remove this event notifier in queued notifiers:
            queuedNotifiers.remove(eventNotifier);

            if (DEBUG_FIRE_EVENT) {
                logger.warn("FIRE QUEUED NOTIFIER {}", eventNotifier);
            }

            eventNotifier.fireQueuedEvents();

            if (DEBUG_FIRE_EVENT) {
                logger.warn("END FIRE QUEUED NOTIFIER", eventNotifier);
            }

            final boolean done = queuedNotifiers.isEmpty();
            if (DEBUG_FIRE_EVENT) {
                logger.warn("END FIRE QUEUED NOTIFIERS : {}", done);
            }

            return done;
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
                    logger.warn("RUN CALLBACK {}", getFullObjectInfo(callback));
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
