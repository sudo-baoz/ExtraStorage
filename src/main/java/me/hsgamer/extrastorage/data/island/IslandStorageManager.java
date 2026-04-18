package me.hsgamer.extrastorage.data.island;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.common.task.Task;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.user.ItemImpl;
import me.hsgamer.extrastorage.data.user.UserImpl;
import me.hsgamer.extrastorage.util.ItemUtil;
import me.hsgamer.hscore.database.client.sql.java.JavaSqlClient;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.simple.SimpleDataHolder;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.sql.converter.UUIDSqlValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.mysql.MySqlDataStorageSupplier;
import me.hsgamer.topper.storage.sql.sqlite.SqliteDataStorageSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages island-level storage, keyed by island UUID.
 * Each island has a single shared storage that all members can access.
 */
public final class IslandStorageManager extends SimpleDataHolder<UUID, UserImpl> {
    private final ExtraStorage instance;
    private final DataStorage<UUID, UserImpl> storage;
    private final Task autoSaveTask;

    public IslandStorageManager(ExtraStorage instance) {
        this.instance = instance;
        boolean isMySql = instance.getSetting().getDBType().equalsIgnoreCase("mysql");
        SqlDataStorageSupplier supplier = isMySql
                ? new MySqlDataStorageSupplier(instance.getSetting().getSqlDatabaseSetting(), JavaSqlClient::new)
                : new SqliteDataStorageSupplier(instance.getDataFolder(), instance.getSetting().getSqlDatabaseSetting(), JavaSqlClient::new);
        this.storage = supplier.getStorage(
                instance.getSetting().getDBTable() + "_island",
                new UUIDSqlValueConverter("island_uuid"),
                UserImpl.getConverter(isMySql),
                SqlDataStorageSupplier.options().setIncrementalKey("id")
        );

        try {
            this.storage.onRegister();
            this.storage.load().forEach((uuid, user) -> {
                if (user.space != 0 || user.items.values().stream().mapToLong(item -> item.quantity).sum() != 0) {
                    getOrCreateEntry(uuid).setValue(user, false);
                }
            });
        } catch (Exception e) {
            instance.getLogger().log(Level.SEVERE, "Error while loading island storage", e);
        }

        this.autoSaveTask = AsyncScheduler.get(instance).runTimer(
                () -> save(),
                instance.getSetting().getAutoUpdateTime(),
                instance.getSetting().getAutoUpdateTime(),
                TimeUnit.SECONDS
        );
    }

    private final Map<UUID, UserImpl> saveMap = new LinkedHashMap<>();

    public void save() {
        if (saveMap.isEmpty()) return;
        Map<UUID, UserImpl> toSave = new LinkedHashMap<>(saveMap);
        saveMap.clear();

        Optional<DataStorage.Modifier<UUID, UserImpl>> optionalModifier = storage.modify();
        if (!optionalModifier.isPresent()) {
            instance.getLogger().log(Level.WARNING, "Failed to get modifier for island storage");
            return;
        }
        DataStorage.Modifier<UUID, UserImpl> modifier = optionalModifier.get();

        try {
            modifier.save(toSave);
            modifier.commit();
        } catch (Exception e) {
            instance.getLogger().log(Level.SEVERE, "Error while saving island storage", e);
            modifier.rollback();
        }
    }

    @Override
    public @NotNull UserImpl getDefaultValue() {
        return UserImpl.EMPTY;
    }

    @Override
    public void onCreate(DataEntry<UUID, UserImpl> entry) {
        Map<String, ItemImpl> map = instance.getSetting().getWhitelist().stream()
                .map(ItemUtil::normalizeMaterialKey)
                .filter(key -> !key.trim().isEmpty())
                .distinct()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> ItemImpl.EMPTY.withFiltered(true)
                ));
        entry.setValue(user -> user.withItems(map).withSpace(instance.getSetting().getMaxSpace()), false);
    }

    @Override
    public void onUpdate(DataEntry<UUID, UserImpl> entry, UserImpl oldValue, UserImpl newValue) {
        synchronized (saveMap) {
            saveMap.put(entry.getKey(), newValue);
        }
    }

    /**
     * Get island storage entry by island UUID, creating if absent.
     */
    public DataEntry<UUID, UserImpl> getIslandEntry(UUID islandUUID) {
        return getOrCreateEntry(islandUUID);
    }

    /**
     * Check if an island storage entry exists (is loaded).
     */
    public boolean hasIslandEntry(UUID islandUUID) {
        return getEntryMap().containsKey(islandUUID);
    }

    public void stop() {
        autoSaveTask.cancel();
    }
}
