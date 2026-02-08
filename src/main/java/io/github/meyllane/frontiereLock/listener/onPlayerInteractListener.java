package io.github.meyllane.frontiereLock.listener;

import io.github.meyllane.frontiereLock.command.FLockCommand;
import io.github.meyllane.frontiereLock.datastruct.LockedBlock;
import io.github.meyllane.frontiereLock.utils.PluginHeader;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class onPlayerInteractListener implements Listener {
    @EventHandler
    public void onPlayerBlockUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction().isLeftClick() || block == null) return;

        Player player = event.getPlayer();

        if (!LockedBlock.isAllowedType(block)) return;

        LockedBlock lockedBlock = LockedBlock.getLockedBlock(
                LockedBlock.getLocationsToCheck(block)
        );

        if (lockedBlock == null) return;

        //Look for a valid card
        boolean hasKey = false;
        ArrayList<@Nullable ItemStack> contents = Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i <= contents.size() - 1; i++) {
            ItemStack stack = contents.get(i);
            if (stack == null) continue;
            if (stack.isEmpty() && stack.getType() != Material.RABBIT_FOOT) continue;

            extractFromBundle(stack, contents);
            extractFromShulker(stack, contents);

            PersistentDataContainerView pdc = stack.getPersistentDataContainer();

            String foundKeyID = pdc.getOrDefault(FLockCommand.lockID, PersistentDataType.STRING, "");
            String foundKeyUUID = pdc.getOrDefault(FLockCommand.lockKeyUUID, PersistentDataType.STRING, "");

            if (foundKeyID.isEmpty() || foundKeyUUID.isEmpty()) continue;

            if (lockedBlock.getID().equals(foundKeyID) && lockedBlock.getKeyUUID().equals(UUID.fromString(foundKeyUUID))) {
                hasKey = true;
                break;
            }
        }

        if (!hasKey &&
                (player.isOp() || player.hasPermission("sflock.bypass"))) {
            player.sendMessage(PluginHeader.getPluginInfoMessage("Vous avez outrepassé le verrou " + lockedBlock.getID() + "."));
            return;
        }

        if (!hasKey) {
            event.setCancelled(true);
            player.sendMessage(PluginHeader.getPluginErrorMessage(
                    "Ce block est verrouillé. (" + lockedBlock.getID() + ")"
            ));
        }
    }

    private static void extractFromShulker(ItemStack stack, ArrayList<@Nullable ItemStack> contents) {
        if (stack.getType() == Material.SHULKER_BOX) {
            BlockStateMeta im = (BlockStateMeta) stack.getItemMeta();
            ShulkerBox box = (ShulkerBox) im.getBlockState();
            List<ItemStack> boxContent = Arrays.stream(box.getInventory().getContents())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));
            contents.addAll(boxContent);
        }
    }

    private static void extractFromBundle(ItemStack stack, ArrayList<@Nullable ItemStack> contents) {
        if (stack.hasData(DataComponentTypes.BUNDLE_CONTENTS)) {
            BundleContents bundle = stack.getData(DataComponentTypes.BUNDLE_CONTENTS);
            contents.addAll(
                    new ArrayList<>(bundle.contents())
            );
        }
    }
}
