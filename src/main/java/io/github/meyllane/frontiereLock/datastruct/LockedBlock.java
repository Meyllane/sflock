package io.github.meyllane.frontiereLock.datastruct;

import io.github.meyllane.frontiereLock.SFLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LockedBlock implements ConfigurationSerializable {
    private final String ID;
    private Location loc;
    private String name;
    private final UUID keyUUID;

    public LockedBlock(String ID, Location loc, String name) {
        this.loc = loc;
        this.ID = ID;
        this.name = name;
        this.keyUUID = UUID.randomUUID();
    }

    public LockedBlock(String ID, Location loc, String name, String keyUUID) {
        this.loc = loc;
        this.ID = ID;
        this.name = name;
        this.keyUUID = UUID.fromString(keyUUID);
    }

    public String getID() {
        return ID;
    }

    public Location getLoc() {
        return loc;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }

    public UUID getKeyUUID() {
        return keyUUID;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("ID", this.ID);
        data.put("name", this.name);
        data.put("loc", this.loc);
        data.put("keyUUID", this.keyUUID.toString());

        return data;
    }
    //TODO: Check this cleanly

    public static LockedBlock deserialize(Map<String, Object> args) {
        return new LockedBlock(
                (String) args.get("ID"),
                (Location) args.get("loc"),
                (String) args.get("name"),
                (String) args.get("keyUUID")
        );
    }

    public static List<Location> getLocationsToCheck(Block block) {
        List<Location> locations = new ArrayList<>();
        Location baseLocation = block.getLocation().clone();

        BlockData data = block.getBlockData();

        if (data instanceof Door door && door.getHalf() == Bisected.Half.TOP) {
            baseLocation.setY(baseLocation.getBlockY() - 1);
            locations.add(baseLocation);
        }
        else if (data instanceof Chest chest) {
            locations.add(baseLocation);

            if (chest.getType() == Chest.Type.LEFT) {
                locations.add(
                        block.getRelative(rotateRight(chest.getFacing())).getLocation()
                );
            }
            else if (chest.getType() == Chest.Type.RIGHT) {
                locations.add(
                        block.getRelative(rotateLeft(chest.getFacing())).getLocation()
                );
            }
        } else {
            locations.add(block.getLocation());
        }

        return locations;
    }

    public static LockedBlock getLockedBlock(List<Location> locations) {
        for (Location loc : locations) {
            if (SFLock.lockMap.exists(loc)) return SFLock.lockMap.getLockedBlock(loc);
        }

        return null;
    }

    public static BlockFace rotateRight(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST  -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST  -> BlockFace.NORTH;
            default    -> face;
        };
    }

    public static BlockFace rotateLeft(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST  -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST  -> BlockFace.NORTH;
            default    -> face;
        };
    }

    public static boolean isAllowedType(Block block) {
        return SFLock.allowedTypes.stream()
                .anyMatch(c -> c.isInstance(block.getBlockData()));
    }

    public Component getKeyName() {
        return Component.text("Carte magnétique " + this.getID())
                .color(TextColor.fromHexString("#68A9E8"));
    }

    public List<Component> getKeyLore() {
        List<Component> keyLore = new ArrayList<>();
        String infoColor = "#9E9E9E";
        keyLore.add(
                Component.text("Carte d'accès au verrou " + this.getID() + ".")
                        .color(TextColor.fromHexString("#ED9761"))
        );

        keyLore.add(
                Component.text("Nom du verrou: " + this.getName())
                        .color(TextColor.fromHexString(infoColor))
        );

        keyLore.add(
                Component.text("X: " + this.getLoc().getBlockX())
                        .color(TextColor.fromHexString(infoColor))
        );

        keyLore.add(
                Component.text("Y: " + this.getLoc().getBlockY())
                        .color(TextColor.fromHexString(infoColor))
        );

        keyLore.add(
                Component.text("Z: " + this.getLoc().getBlockZ())
                        .color(TextColor.fromHexString(infoColor))
        );

        keyLore.add(
                Component.text("World: " + this.getLoc().getWorld().getName())
                        .color(TextColor.fromHexString(infoColor))
        );

        return keyLore;
    }
}
