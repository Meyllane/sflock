package io.github.meyllane.frontiereLock.datastruct;

import io.github.meyllane.frontiereLock.SFLock;
import org.bukkit.Location;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LockedBlockRegistry {
    private final HashMap<Location, LockedBlock> registry;

    public LockedBlockRegistry() {
        this.registry = new HashMap<>();
    }

    public void add(Location loc, LockedBlock lockedBlock) {
        this.registry.put(loc, lockedBlock);
    }

    public void remove(Location loc) {
        this.registry.remove(loc);
    }

    public String[] getLockedBlockIDs() {
        return this.registry.values().stream()
                .map(LockedBlock::getID)
                .toArray(String[]::new);
    }

    public String getNewMaxID() {
        int maxID = this.registry.values().stream()
                .map(lb -> Integer.parseInt(lb.getID().substring(1)))
                .max(Comparator.naturalOrder()).orElse(0);
        return "L" + (maxID + 1);
    }

    public LockedBlock getLockedBlock(Location loc) {
        return this.registry.get(loc);
    }

    public boolean exists(Location loc) {
        return this.registry.containsKey(loc);
    }

    public LockedBlock getLockedBlockByID(String id) {
        return this.registry.values().stream()
                .filter(lockedBlock -> lockedBlock.getID().equals(id))
                .findFirst()
                .orElse(null);
    }
}
