package me.megamichiel.animatedmenu.util.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class MaterialSpecific {

    private static final Map<Class<?>, Class<?>> mapped = new HashMap<>();

    private final Map<Class<?>, Action> actions = new HashMap<>();

    public <IM extends ItemMeta> void add(Class<? extends IM> type, Action<? super IM> action) {
        actions.put(type, action);
    }

    public void apply(Player player, ItemMeta meta) {
        Class<?> type = mapped.computeIfAbsent(meta.getClass(), a -> {
            for (Class<?> b : actions.keySet()) {
                if (b.isAssignableFrom(a)) {
                    return b;
                }
            }
            return null;
        });
        if (type != null) {
            Action action = actions.get(type);
            if (action != null) {
                action.apply(player, meta);
            }
        }
    }

    public interface Action<IM extends ItemMeta> {
        void apply(Player player, IM meta);
    }
}