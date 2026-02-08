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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        CommandAPICommand generateLock = new CommandAPICommand("generate")
                .withPermission("sflock.generatelock")
                .withArguments(new GreedyStringArgument("lockName"))
                .executesPlayer(FLockCommand::generateLock);

        CommandAPICommand removeLock = new CommandAPICommand("remove")
                .withPermission("sflock.removelock")
                .withArguments(allLocks)
                .executesPlayer(FLockCommand::removeLock);

        CommandAPICommand viewLock = new CommandAPICommand("view")
                .withPermission("sflock.view")
                .withArguments(allLocks)
                .executesPlayer(FLockCommand::viewLock);

        CommandAPICommand updateLockLocation = new CommandAPICommand("location")
                .withPermission("sflock.update.location")
                .withArguments(allLocks)
                .executesPlayer(FLockCommand::updateLockLocation);

        CommandAPICommand updateLockName = new CommandAPICommand("name")
                .withPermission("sflock.update.name")
                .withArguments(allLocks)
                .withArguments(new GreedyStringArgument("newName"))
                .executesPlayer(FLockCommand::updateLockName);

        CommandAPICommand updateLock = new CommandAPICommand("update")
                .withSubcommand(updateLockLocation)
                .withSubcommand(updateLockName);

        CommandAPICommand lock = new CommandAPICommand("lock")
                .withSubcommand(generateLock)
                .withSubcommand(removeLock)
                .withSubcommand(viewLock)
                .withSubcommand(updateLock);

        // /sflock key generate/invalidate
        CommandAPICommand generateKey = new CommandAPICommand("generate")
                .withArguments(keyTextures)
                .withArguments(allLocks)
                .withPermission("sflock.generatekey")
                .executesPlayer(FLockCommand::generateKey);

        // /sflock key invalidate

        CommandAPICommand invalidateKey = new CommandAPICommand("invalidate")
                .withPermission("sflock.invalidatekey")
                .withArguments(allLocks)
                .executesPlayer(FLockCommand::invalidateKey);

        CommandAPICommand key = new CommandAPICommand("key")
                .withSubcommand(generateKey)
                .withSubcommand(invalidateKey);

        // /sflock
        new CommandAPICommand("sflock")
                .withSubcommand(lock)
                .withSubcommand(key)
                .register();
    }

    private static void generateLock(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
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

        SFLock.saveChanges();
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

    private static void invalidateKey(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        LockedBlock lockedBlock = getLockedBlock(args);

        lockedBlock.setKeyUUID(UUID.randomUUID());

        SFLock.saveChanges();

        sender.sendMessage(PluginHeader.getPluginSuccessMessage(
                "Vous avez invalidé les clés du verrou " + lockedBlock.getID() + "."
        ));
    }

    private static void removeLock(Player sender, CommandArguments args) throws WrapperCommandSyntaxException  {
        LockedBlock lockedBlock = getLockedBlock(args);

        SFLock.lockMap.remove(lockedBlock.getLoc());
        SFLock.config.set("locks." + lockedBlock.getID(), null);

        SFLock.saveChanges();

        sender.sendMessage(PluginHeader.getPluginSuccessMessage(
                "Le verrou " + lockedBlock.getID() + " a bien été supprimé."
        ));
    }

    private static void viewLock(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        LockedBlock lockedBlock = getLockedBlock(args);

        sender.sendMessage(PluginHeader.getPluginHeader().append(
                lockedBlock.getInfoComponent()
        ));
    }

    private static void updateLockLocation(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        //Get LockedBlock
        LockedBlock lockedBlock = getLockedBlock(args);

        //Get Block looked at

        Block targetBlock = sender.getTargetBlockExact(10);

        if (targetBlock == null || !LockedBlock.isAllowedType(targetBlock)) {
            throw CommandAPIPaper.failWithAdventureComponent(PluginHeader.getPluginErrorMessage(
               "Block cible invalide."
            ));
        }

        //Remove the LockedBlock from the registry
        SFLock.lockMap.remove(lockedBlock.getLoc());

        //Change LockedBlock coords
        lockedBlock.setLoc(targetBlock.getLocation());

        //Add it back to the registry
        SFLock.lockMap.add(lockedBlock.getLoc(), lockedBlock);

        SFLock.saveChanges();

        sender.sendMessage(PluginHeader.getPluginSuccessMessage(
                "La position du verrou a bien été mise à jour."
        ));
    }

    private static void updateLockName(Player sender, CommandArguments args) throws WrapperCommandSyntaxException {
        LockedBlock lockedBlock = getLockedBlock(args);

        String newName = args.getByClassOrDefault("newName", String.class, "");

        if (newName.isBlank()) {
            throw CommandAPIPaper.failWithAdventureComponent(PluginHeader.getPluginErrorMessage(
                    "Nouveau nom invalide"
            ));
        }

        lockedBlock.setName(newName);

        SFLock.saveChanges();

        sender.sendMessage(PluginHeader.getPluginSuccessMessage(
                "Le nom du verrou a bien été changé."
        ));
    }

    private static @NotNull LockedBlock getLockedBlock(CommandArguments args) throws WrapperCommandSyntaxException {
        //Get LockedBlock
        String id = args.getByClassOrDefault("lockID", String.class, "");

        LockedBlock lockedBlock = SFLock.lockMap.getLockedBlockByID(id);

        if (lockedBlock == null) {
            throw CommandAPIPaper.failWithAdventureComponent(PluginHeader.getPluginErrorMessage(
                    "Le verrou demandé n'existe pas"
            ));
        }
        return lockedBlock;
    }
}
