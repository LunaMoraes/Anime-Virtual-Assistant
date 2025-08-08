package actions;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object that carries data between actions and provides state information.
 */
public class ActionContext {
    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public void clear() {
        data.clear();
    }
}
