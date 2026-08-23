package iv;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Pub/Sub in-process (Observer). Subscribe theo kiểu event, publish fan-out.
 */
public class EventBus {
    private final Map<Class<?>, List<Consumer<Object>>> subs =
            new ConcurrentHashMap<Class<?>, List<Consumer<Object>>>();

    public <T> void subscribe(Class<T> type, final Consumer<T> handler) {
        List<Consumer<Object>> list = subs.get(type);
        if (list == null) {
            list = new CopyOnWriteArrayList<Consumer<Object>>();
            List<Consumer<Object>> prev = subs.putIfAbsent(type, list);
            if (prev != null) {
                list = prev;
            }
        }
        list.add(new Consumer<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public void accept(Object o) {
                handler.accept((T) o);
            }
        });
    }

    public void publish(Object event) {
        List<Consumer<Object>> list = subs.get(event.getClass());
        if (list == null) {
            return;
        }
        for (Consumer<Object> c : list) {
            c.accept(event);
        }
    }
}
