package me.hsgamer.extrastorage.data.island;

import com.google.common.base.Preconditions;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

public class IslandItem implements Item {
    private final IslandStorage storage;
    private final String key;
    private final io.github.projectunified.uniitem.api.Item item;

    public IslandItem(IslandStorage storage, String key) {
        this.storage = storage;
        this.key = key;
        this.item = ItemUtil.getItem(key);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean isLoaded() {
        return item.isValid();
    }

    @Override
    public ItemUtil.ItemType getType() {
        return ItemUtil.getItemType(item);
    }

    @Override
    public ItemStack getItem() {
        ItemStack itemStack = item.bukkitItem();
        Preconditions.checkNotNull(itemStack);
        return itemStack;
    }

    @Override
    public boolean isFiltered() {
        return storage.entry.getValue().items.get(key).filtered;
    }

    @Override
    public void setFiltered(boolean status) {
        storage.entry.setValue(u -> u.withItemModifiedIfFound(key, i -> i.withFiltered(status)));
    }

    @Override
    public long getQuantity() {
        return storage.entry.getValue().items.get(key).quantity;
    }

    @Override
    public void add(long quantity) {
        storage.entry.setValue(u -> u.withItemModifiedIfFound(key, i -> i.withQuantity(i.quantity + quantity)));
    }

    @Override
    public void subtract(long quantity) {
        storage.entry.setValue(u -> u.withItemModifiedIfFound(key, i -> i.withQuantity(i.quantity - quantity)));
    }

    @Override
    public void set(long quantity) {
        storage.entry.setValue(u -> u.withItemModifiedIfFound(key, i -> i.withQuantity(quantity)));
    }
}
