package iv;

import java.util.ArrayList;
import java.util.List;

/**
 * Các pattern hay bị bắt phác trên bảng: Observer, Strategy, Decorator, Builder, Factory.
 */
public final class ClassicPatterns {
    private ClassicPatterns() {
    }

    public interface Observer {
        void onChange(int value);
    }

    public static final class Subject {
        private final List<Observer> observers = new ArrayList<Observer>();
        private int value;

        public void add(Observer o) {
            observers.add(o);
        }

        public void set(int value) {
            this.value = value;
            for (Observer o : observers) {
                o.onChange(value);
            }
        }

        public int get() {
            return value;
        }
    }

    public interface Discount {
        int apply(int price);
    }

    public static final class PercentOff implements Discount {
        private final int percent;

        public PercentOff(int percent) {
            this.percent = percent;
        }

        @Override
        public int apply(int price) {
            return price * (100 - percent) / 100;
        }
    }

    public static final class CouponOff implements Discount {
        private final int amount;

        public CouponOff(int amount) {
            this.amount = amount;
        }

        @Override
        public int apply(int price) {
            return Math.max(0, price - amount);
        }
    }

    public static int checkout(int price, Discount discount) {
        return discount.apply(price);
    }

    public interface Text {
        String render();
    }

    public static final class Plain implements Text {
        private final String s;

        public Plain(String s) {
            this.s = s;
        }

        @Override
        public String render() {
            return s;
        }
    }

    public static final class Bold implements Text {
        private final Text inner;

        public Bold(Text inner) {
            this.inner = inner;
        }

        @Override
        public String render() {
            return "*" + inner.render() + "*";
        }
    }

    public static final class User {
        public final String name;
        public final int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public static final class UserBuilder {
        private String name = "";
        private int age;

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder age(int age) {
            this.age = age;
            return this;
        }

        public User build() {
            if (name.isEmpty()) {
                throw new IllegalStateException("name");
            }
            return new User(name, age);
        }
    }

    public interface Animal {
        String speak();
    }

    public static Animal create(String type) {
        if ("dog".equals(type)) {
            return new Animal() {
                @Override
                public String speak() {
                    return "woof";
                }
            };
        }
        if ("cat".equals(type)) {
            return new Animal() {
                @Override
                public String speak() {
                    return "meow";
                }
            };
        }
        throw new IllegalArgumentException(type);
    }
}
