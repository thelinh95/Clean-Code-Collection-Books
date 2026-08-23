package iv;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * FSM đơn hàng: CREATED → PAID → SHIPPED → DELIVERED; CANCEL từ CREATED/PAID.
 */
public class OrderStateMachine {
    public enum State {
        CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    public enum Event {
        PAY, SHIP, DELIVER, CANCEL
    }

    private static final Map<State, Map<Event, State>> TRANSITIONS;

    static {
        Map<State, Map<Event, State>> t = new EnumMap<State, Map<Event, State>>(State.class);
        put(t, State.CREATED, Event.PAY, State.PAID);
        put(t, State.CREATED, Event.CANCEL, State.CANCELLED);
        put(t, State.PAID, Event.SHIP, State.SHIPPED);
        put(t, State.PAID, Event.CANCEL, State.CANCELLED);
        put(t, State.SHIPPED, Event.DELIVER, State.DELIVERED);
        TRANSITIONS = Collections.unmodifiableMap(t);
    }

    private State state = State.CREATED;

    public synchronized State apply(Event event) {
        Map<Event, State> row = TRANSITIONS.get(state);
        if (row == null || !row.containsKey(event)) {
            throw new IllegalStateException(state + " + " + event);
        }
        state = row.get(event);
        return state;
    }

    public synchronized State state() {
        return state;
    }

    public static Set<Event> allowed(State from) {
        Map<Event, State> row = TRANSITIONS.get(from);
        return row == null ? EnumSet.noneOf(Event.class) : EnumSet.copyOf(row.keySet());
    }

    private static void put(Map<State, Map<Event, State>> t, State from, Event e, State to) {
        Map<Event, State> row = t.get(from);
        if (row == null) {
            row = new EnumMap<Event, State>(Event.class);
            t.put(from, row);
        }
        row.put(e, to);
    }
}
