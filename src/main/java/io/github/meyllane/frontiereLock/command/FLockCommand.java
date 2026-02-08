package io.github.meyllane.frontiereLock.command;

import dev.jorel.commandapi.*;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.meyllane.frontiereLock.SFLock;
import io.github.meyllane.frontiereLock.datastruct.LockedBlock;
import io.github.meyllane.frontiereLock.utils.PluginHeader;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FLockCommand {
    public static final NamespacedKey lockID = new NamespacedKey(SFLock.getPlugin(SFLock.class), "lockid");
    public static final NamespacedKey lockKeyUUID = new NamespacedKey(SFLock.getPlugin(SFLock.class), "lockkeyuuid");
    public static void registerCommand() {

        List<Argument<?>> allLocks = new ArrayList<>();
        allLocks.add(new StringArgument("lockID").replaceSuggestions(
                ArgumentSuggestions.strings(info -> SFLock.lockMap.getLockedBlockIDs())
        ));

        List<Argument<?>> keyTextures = new ArrayList<>();
        keyTextures.add(new StringArgument("keyTexture").replaceSuggestions(
                ArgumentSuggestions.strings(
                        "card_red", "card_green", "card_white", "card_blue", "clef_fer"
                )
        ));

        // /sflock lock

        CommandAPICommand lockBlock = new CommandAPICommand("lock")
                .withPermission("sflock.lock")
                .withArguments(new GreedyStringArgument("lockName"))
                .executesPlayer(FLockCommand::lockBlock);

        // /sflock key generate/invalidate
        CommandAPICommand generateKey = new CommandAPICommand("generate")
                .withArguments(keyTextures)
                .withOptionalArguments(allLocks)
                .withPermission("sflock.generatekey")
                .executesPlayer(FLockCommand::generateKey);

        // /sflock key invalidate

        CommandAPICommand key = new CommandAPICommand("key")
                .withSubcommand(generateKey);

        // /sflock
        new CommandAPICommand("sflock")
                .withSubcommand(lockBlock)
                .withSubcommand(key)
                .register();
    }

    private static void lockBlock(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        String lockName = args.getByClass("lockName", String.class);

        Block block = sender.getTargetBlockExact(10);

        if (block == null || block.getType().isAir()) {
            throw CommandAPIPaper.failWithAdventureComponent(
                    PluginHeader.getPluginErrorMessage("Ce block ne peut pas être verrouillé.")
            );
        }

        if (!LockedBlock.isAllowedType(block)) throw CommandAPIPaper.failWithAdventureComponent(
                PluginHeader.getPluginErrorMessage("Ce block ne peut pas être verrouillé.")
        );

        LockedBlock lockedBlock = LockedBlock.getLockedBlock(
                LockedBlock.getLocationsToCheck(block)
        );

        System.out.println(LockedBlock.getLocationsToCheck(block));

        if (lockedBlock != null) {
            throw CommandAPIPaper.failWithAdventureComponent(PluginHeader.getPluginErrorMessage(
                    "Ce block a déjà un verrou. (ID: " + lockedBlock.getID() + ")"
            ));
        }

        String id = SFLock.lockMap.getNewMaxID();

        LockedBlock locked = new LockedBlock(
                id,
                block.getLocation(),
                lockName
        );

        SFLock.config.set("locks." + id, locked);

        SFLock.lockMap.add(locked.getLoc(), locked);

        sender.sendMessage(PluginHeader.getPluginSuccessMessage("Le verrou a bien été appliqué sur le block ciblé (ID : " + id + ")"));

        SFLock.getPlugin(SFLock.class).getServer().getScheduler().runTaskAsynchronously(SFLock.getPlugin(SFLock.class), runner -> {
            try {
                SFLock.config.save(SFLock.configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void generateKey(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        LockedBlock lockedBlock;
        String keyTexture = args.getByClassOrDefault("keyTexture", String.class, "card_blue");

        String id = args.getByClassOrDefault("lockID", String.class, "");
        lockedBlock = SFLock.lockMap.getLockedBlockByID(id);

        if (lockedBlock == null) {
            throw CommandAPIPaper.failWithAdventureComponent(PluginHeader.getPluginErrorMessage(
                    "Le verrou demandé n'existe pas."
            ));
        }

        ItemStack key = new ItemStack(Material.RABBIT_FOOT);

        key.lore(lockedBlock.getKeyLore());
        key.editMeta(itemMeta -> itemMeta.customName(lockedBlock.getKeyName()));

        key.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData()
                        .addString(keyTexture)
                        .build()
        );

        key.editPersistentDataContainer(pdc -> {
            pdc.set(FLockCommand.lockID, PersistentDataType.STRING, lockedBlock.getID());
            pdc.set(FLockCommand.lockKeyUUID, PersistentDataType.STRING, lockedBlock.getKeyUUID().toString());
        });

        sender.give(List.of(key), false);

        sender.sendMessage(PluginHeader.getPluginSuccessMessage(
                "Vous avez généré une clé pour le verrou " + lockedBlock.getID()
        ));
    }
}
