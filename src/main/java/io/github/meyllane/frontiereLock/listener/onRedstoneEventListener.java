package io.github.meyllane.frontiereLock.listener;

import io.github.meyllane.frontiereLock.datastruct.LockedBlock;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class onRedstoneEventListener implements Listener {
    @EventHandler
    public void onRedStoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (!LockedBlock.isAllowedType(block)) return;

        LockedBlock lockedBlock = LockedBlock.getLockedBlock(LockedBlock.getLocationsToCheck(block));

        if (lockedBlock == null) return;

        event.setNewCurrent(0);
    }
}
