package iv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Pipes-and-filters: chuỗi Function, apply tuần tự.
 */
public class Pipeline<T> {
    private final List<Function<T, T>> stages = new ArrayList<Function<T, T>>();

    public Pipeline<T> add(Function<T, T> stage) {
        stages.add(stage);
        return this;
    }

    public T run(T input) {
        T cur = input;
        for (Function<T, T> s : stages) {
            cur = s.apply(cur);
        }
        return cur;
    }
}
