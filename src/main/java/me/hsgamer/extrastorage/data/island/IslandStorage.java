package me.hsgamer.extrastorage.data.island;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.data.stub.StubItem;
import me.hsgamer.extrastorage.data.user.ItemImpl;
import me.hsgamer.extrastorage.util.Digital;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.topper.data.core.DataEntry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Island-level storage implementation.
 * Mirrors StubStorage but operates on island-level UserImpl.
 */
public class IslandStorage implements Storage {
    private static final ExtraStorage instance = ExtraStorage.getInstance();
    final DataEntry<UUID, me.hsgamer.extrastorage.data.user.UserImpl> entry;

    public IslandStorage(DataEntry<UUID, me.hsgamer.extrastorage.data.user.UserImpl> entry) {
        this.entry = entry;
    }

    @Override
    public boolean getStatus() {
        return entry.getValue().status;
    }

    @Override
    public void setStatus(boolean status) {
        entry.setValue(u -> u.withStatus(status));
    }

    @Override
    public long getSpace() {
        long space = entry.getValue().space;
        if ((instance.getSetting().getMaxSpace() == -1) || (space < 0))
            return -1;
        return Digital.getBetween(0, Long.MAX_VALUE, space);
    }

    @Override
    public void setSpace(long space) {
        entry.setValue(u -> u.withSpace(space));
    }

    @Override
    public void addSpace(long space) {
        entry.setValue(u -> u.withSpace(u.space + space));
    }

    @Override
    public long getUsedSpace() {
        long usedSpace = 0;
        for (Map.Entry<String, ItemImpl> entry : entry.getValue().items.entrySet()) {
            try {
                usedSpace = Math.addExact(usedSpace, entry.getValue().quantity);
            } catch (ArithmeticException e) {
                return Long.MAX_VALUE;
            }
        }
        return usedSpace;
    }

    @Override
    public long getFreeSpace() {
        long space = this.getSpace();
        if (space < 0) return -1;
        return (space - this.getUsedSpace());
    }

    @Override
    public boolean isMaxSpace() {
        long free = this.getFreeSpace();
        if (free == -1) return false;
        return (free < 1);
    }

    @Override
    public double getSpaceAsPercent(boolean order) {
        long space = this.getSpace();
        if (space < 0) return -1;
        double percent = (double) getUsedSpace() / space * 100;
        return Digital.getBetween(0.0, 100.0, Digital.formatDouble(order ? percent : (100.0 - percent)));
    }

    @Override
    public boolean canStore(Object key) {
        if (!entry.getValue().status) return false;
        String validKey = ItemUtil.toMaterialKey(key);
        ItemImpl item = entry.getValue().items.get(validKey);
        return (item != null) && item.filtered;
    }

    @Override
    public Map<String, Item> getFilteredItems() {
        return entry.getValue().items.entrySet()
                .stream()
                .filter(e -> e.getValue().filtered)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new StubItem(this, e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Item> getUnfilteredItems() {
        return entry.getValue().items.entrySet()
                .stream()
                .filter(e -> !e.getValue().filtered)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new StubItem(this, e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Item> getItems() {
        return entry.getValue().items.keySet()
                .stream()
                .map(k -> new AbstractMap.SimpleEntry<>(k, new StubItem(this, k)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Optional<Item> getItem(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        return entry.getValue().items.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(validKey))
                .findFirst()
                .map(e -> new StubItem(this, e.getKey()));
    }

    @Override
    public void addNewItem(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        entry.setValue(u -> u.withItemIfNotFound(validKey, ItemImpl.EMPTY.withFiltered(true).withQuantity(0)));
    }

    @Override
    public void unfilter(Object key) {
        String validKey = ItemUtil.toMaterialKey(key);
        entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> i.withFiltered(false)));
    }

    @Override
    public void add(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> i.withQuantity(i.quantity + quantity)));
    }

    @Override
    public void subtract(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> {
            long newQuantity = i.quantity - quantity;
            if (newQuantity < 0) return null;
            return i.withQuantity(newQuantity);
        }));
    }

    @Override
    public void set(Object key, long quantity) {
        String validKey = ItemUtil.toMaterialKey(key);
        entry.setValue(u -> u.withItemModifiedIfFound(validKey, i -> {
            if (quantity < 0) return null;
            return i.withQuantity(quantity);
        }));
    }

    @Override
    public void reset(Object key) {
        if (key != null) this.set(key, 0);
        else {
            entry.setValue(u -> {
                Map<String, ItemImpl> items = new HashMap<>(entry.getValue().items);
                Iterator<Map.Entry<String, ItemImpl>> iterator = items.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, ItemImpl> e = iterator.next();
                    if (e.getValue().quantity < 1 && !e.getValue().filtered) {
                        iterator.remove();
                    } else {
                        e.setValue(e.getValue().withQuantity(0));
                    }
                }
                return u.withItems(items);
            });
        }
    }
}
