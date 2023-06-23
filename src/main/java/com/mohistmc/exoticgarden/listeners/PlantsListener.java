package com.mohistmc.exoticgarden.listeners;

import com.mohistmc.exoticgarden.schematics.Schematic;
import com.mohistmc.exoticgarden.Berry;
import com.mohistmc.exoticgarden.ExoticGarden;
import com.mohistmc.exoticgarden.PlantType;
import com.mohistmc.exoticgarden.Tree;
import com.mohistmc.exoticgarden.items.BonemealableItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class PlantsListener implements Listener {

    private final Config cfg;
    private final ExoticGarden plugin;
    private final BlockFace[] faces = { BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST };

    public PlantsListener(ExoticGarden plugin) {
        this.plugin = plugin;
        cfg = plugin.getCfg();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onGrow(StructureGrowEvent e) {
        if (PaperLib.isPaper()) {
            if (PaperLib.isChunkGenerated(e.getLocation())) {
                growStructure(e);
            }
            else {
                PaperLib.getChunkAtAsync(e.getLocation()).thenRun(() -> growStructure(e));
            }
        }
        else {
            if (!e.getLocation().getChunk().isLoaded()) {
                e.getLocation().getChunk().load();
            }
            growStructure(e);
        }
    }

    @EventHandler
    public void onGenerate(ChunkPopulateEvent e) {
        final World world = e.getWorld();

        if (BlockStorage.getStorage(world) == null) {
            return;
        }

        if (!Slimefun.getWorldSettingsService().isWorldEnabled(world)) {
            return;
        }

        if (!cfg.getStringList("world-blacklist").contains(world.getName())) {
            Random random = ThreadLocalRandom.current();

            final int worldLimit = getWorldBorder(world);

            if (random.nextInt(100) < cfg.getInt("chances.BUSH")) {
                Berry berry = ExoticGarden.getBerries().get(random.nextInt(ExoticGarden.getBerries().size()));
                if (berry.getType().equals(PlantType.ORE_PLANT)) return;

                int chunkX = e.getChunk().getX();
                int chunkZ = e.getChunk().getZ();

                // Middle of chunk between 3-13 (to avoid loading neighbouring chunks)
                int x = chunkX * 16 + random.nextInt(10) + 3;
                int z = chunkZ * 16 + random.nextInt(10) + 3;

                if ((x < worldLimit && x > -worldLimit) && (z < worldLimit && z > -worldLimit)) {
                    if (PaperLib.isPaper()) {
                        if (PaperLib.isChunkGenerated(world, chunkX, chunkZ)) {
                            growBush(e, x, z, berry, random, true);
                        }
                        else {
                            PaperLib.getChunkAtAsync(world, chunkX, chunkZ).thenRun(() -> growBush(e, x, z, berry, random, true));
                        }
                    }
                    else {
                        growBush(e, x, z, berry, random, false);
                    }
                }
            }
            else if (random.nextInt(100) < cfg.getInt("chances.TREE")) {
                Tree tree = ExoticGarden.getTrees().get(random.nextInt(ExoticGarden.getTrees().size()));

                int chunkX = e.getChunk().getX();
                int chunkZ = e.getChunk().getZ();

                // Tree size defaults (width/length)
                int tw = 7;
                int tl = 7;

                // Get the sizes of the tree being placed
                // Value is padded +2 blocks to avoid loading neighbouring chunks for block updates
                try {
                    tw = tree.getSchematic().getWidth() + 2;
                    tl = tree.getSchematic().getLength() + 2;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // Ensure schematic fits inside the chunk
                int x = chunkX * 16 + random.nextInt(16 - tw) + (int) Math.floor(tw/2);
                int z = chunkZ * 16 + random.nextInt(16 - tl) + (int) Math.floor(tl/2);

                if ((x < worldLimit && x > -worldLimit) && (z < worldLimit && z > -worldLimit)) {
                    if (PaperLib.isPaper()) {
                        if (PaperLib.isChunkGenerated(world, chunkX, chunkZ)) {
                            pasteTree(e, x, z, tree);
                        }
                        else {
                            PaperLib.getChunkAtAsync(world, chunkX, chunkZ).thenRun(() -> pasteTree(e, x, z, tree));
                        }
                    }
                    else {
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> pasteTree(e, x, z, tree));
                    }
                }
            }
        }
    }

    private int getWorldBorder(World world) {
        return (int) world.getWorldBorder().getSize();
    }

    private void growStructure(StructureGrowEvent e) {
        SlimefunItem item = BlockStorage.check(e.getLocation().getBlock());

        if (item != null) {
            e.setCancelled(true);
            for (Tree tree : ExoticGarden.getTrees()) {
                if (item.getId().equalsIgnoreCase(tree.getSapling())) {
                    BlockStorage.clearBlockInfo(e.getLocation());
                    Schematic.pasteSchematic(e.getLocation(), tree, false);
                    return;
                }
            }

            for (Berry berry : ExoticGarden.getBerries()) {
                if (item.getId().equalsIgnoreCase(berry.toBush())) {
                    switch (berry.getType()) {
                        case BUSH ->
                                e.getLocation().getBlock().setType(Material.OAK_LEAVES, false);
                        case ORE_PLANT, DOUBLE_PLANT -> {
                            Block blockAbove = e.getLocation().getBlock().getRelative(BlockFace.UP);
                            item = BlockStorage.check(blockAbove);
                            if (item != null) return;
                            if (!Tag.SAPLINGS.isTagged(blockAbove.getType()) && !Tag.LEAVES.isTagged(blockAbove.getType())) {
                                switch (blockAbove.getType()) {
                                    case AIR, CAVE_AIR, SNOW:
                                        break;
                                    default:
                                        return;
                                }
                            }
                            BlockStorage.store(blockAbove, berry.getItem());
                            e.getLocation().getBlock().setType(Material.OAK_LEAVES, false);
                            blockAbove.setType(Material.PLAYER_HEAD, false);
                            Rotatable rotatable = (Rotatable) blockAbove.getBlockData();
                            rotatable.setRotation(faces[ThreadLocalRandom.current().nextInt(faces.length)]);
                            blockAbove.setBlockData(rotatable, false);
                            PlayerHead.setSkin(blockAbove, PlayerSkin.fromHashCode(berry.getTexture()), true);
                        }
                        default -> {
                            e.getLocation().getBlock().setType(Material.PLAYER_HEAD, false);
                            Rotatable s = (Rotatable) e.getLocation().getBlock().getBlockData();
                            s.setRotation(faces[ThreadLocalRandom.current().nextInt(faces.length)]);
                            e.getLocation().getBlock().setBlockData(s);
                            PlayerHead.setSkin(e.getLocation().getBlock(), PlayerSkin.fromHashCode(berry.getTexture()), true);
                        }
                    }

                    BlockStorage.clearBlockInfo(e.getLocation(), false);
                    BlockStorage.store(e.getLocation().getBlock(), berry.getItem());
                    e.getWorld().playEffect(e.getLocation(), Effect.STEP_SOUND, Material.OAK_LEAVES);
                    break;
                }
            }
        }
    }

    private void pasteTree(ChunkPopulateEvent e, int x, int z, Tree tree) {
        for (int y = e.getWorld().getHighestBlockYAt(x, z) + 2; y > 30; y--) {
            Block current = e.getWorld().getBlockAt(x, y, z);
            if (current.getType() != Material.WATER && current.getType() != Material.SEAGRASS && current.getType() != Material.TALL_SEAGRASS && !current.getType().isSolid() && !(current.getBlockData() instanceof Waterlogged && ((Waterlogged) current.getBlockData()).isWaterlogged()) && tree.isSoil(current.getRelative(0, -1, 0).getType()) && isFlat(current)) {
                Schematic.pasteSchematic(new Location(e.getWorld(), x, y, z), tree, false);
                break;
            }
        }
    }

    private void growBush(ChunkPopulateEvent e, int x, int z, Berry berry, Random random, boolean isPaper) {
        for (int y = e.getWorld().getHighestBlockYAt(x, z) + 2; y > 30; y--) {
            Block current = e.getWorld().getBlockAt(x, y, z);
            if (current.getType() != Material.WATER && !current.getType().isSolid() && berry.isSoil(current.getRelative(BlockFace.DOWN).getType())) {
                BlockStorage.store(current, berry.getItem());
                switch (berry.getType()) {
                case BUSH:
                    if (isPaper) {
                        current.setType(Material.OAK_LEAVES, false);
                    }
                    else {
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> current.setType(Material.OAK_LEAVES));
                    }
                    break;
                    case FRUIT, ORE_PLANT, DOUBLE_PLANT:
                    if (isPaper) {
                        current.setType(Material.PLAYER_HEAD, false);
                        Rotatable s = (Rotatable) current.getBlockData();
                        s.setRotation(faces[random.nextInt(faces.length)]);
                        current.setBlockData(s, false);
                        PlayerHead.setSkin(current, PlayerSkin.fromHashCode(berry.getTexture()), true);
                    }
                    else {
                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            current.setType(Material.PLAYER_HEAD, false);
                            Rotatable s = (Rotatable) current.getBlockData();
                            s.setRotation(faces[random.nextInt(faces.length)]);
                            current.setBlockData(s, false);
                            PlayerHead.setSkin(current, PlayerSkin.fromHashCode(berry.getTexture()), true);
                        });
                    }
                    break;
                default:
                    break;
                }
                break;
            }
        }
    }

    private boolean isFlat(Block current) {
        for (int i = -2; i < 2; i++) {
            for (int j = -2; j < 2; j++) {
                for (int k = 0; k < 6; k++) {
                    Block block = current.getRelative(i, k, j);
                    if (block.getType().isSolid()
                            || Tag.LEAVES.isTagged(block.getType())
                            || !current.getRelative(i, -1, j).getType().isSolid()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent e) {
        if (Slimefun.getProtectionManager().hasPermission(e.getPlayer(), e.getBlock().getLocation(), Interaction.BREAK_BLOCK)) {
            if (e.getBlock().getType() == Material.PLAYER_HEAD || Tag.LEAVES.isTagged(e.getBlock().getType())) {
                dropFruitFromTree(e.getBlock());
            }

            if (e.getBlock().getType() == Material.GRASS) {
                if (!ExoticGarden.getGrassDrops().keySet().isEmpty() && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    Random random = ThreadLocalRandom.current();

                    if (random.nextInt(100) < 6) {
                        ItemStack[] items = ExoticGarden.getGrassDrops().values().toArray(new ItemStack[0]);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), items[random.nextInt(items.length)]);
                    }
                }
            }
            else {
                ItemStack item = ExoticGarden.harvestPlant(e.getBlock());

                if (item != null) {
                    e.setCancelled(true);
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (!Slimefun.getWorldSettingsService().isWorldEnabled(e.getBlock().getWorld())) {
            return;
        }

        String id = BlockStorage.checkID(e.getBlock());

        if (id != null) {
            for (Berry berry : ExoticGarden.getBerries()) {
                if (id.equalsIgnoreCase(berry.getID())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        dropFruitFromTree(e.getBlock());
        ItemStack item = BlockStorage.retrieve(e.getBlock());

        if (item != null) {
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR, false);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getPlayer().isSneaking()) return;

        if (Slimefun.getProtectionManager().hasPermission(e.getPlayer(), e.getClickedBlock().getLocation(), Interaction.BREAK_BLOCK)) {
            ItemStack item = ExoticGarden.harvestPlant(e.getClickedBlock());

            if (item != null) {
                e.getClickedBlock().getWorld().playEffect(e.getClickedBlock().getLocation(), Effect.STEP_SOUND, Material.OAK_LEAVES);
                e.getClickedBlock().getWorld().dropItemNaturally(e.getClickedBlock().getLocation(), item);
            } else {
                // The block wasn't a plant, we try harvesting a fruit instead
                ExoticGarden.getInstance().harvestFruit(e.getClickedBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeAll(getAffectedBlocks(e.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeAll(getAffectedBlocks(e.blockList()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBonemealPlant(BlockFertilizeEvent e) {
        Block b = e.getBlock();
        if (b.getType() == Material.OAK_SAPLING) {
            SlimefunItem item = BlockStorage.check(b);

            if (item instanceof BonemealableItem && ((BonemealableItem) item).isBonemealDisabled()) {
                e.setCancelled(true);
                b.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, b.getLocation().clone().add(0.5, 0, 0.5), 4);
                b.getWorld().playSound(b.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            }
        }
    }

    private Set<Block> getAffectedBlocks(List<Block> blockList) {
        Set<Block> blocksToRemove = new HashSet<>();

        for (Block block : blockList) {
            ItemStack item = ExoticGarden.harvestPlant(block);

            if (item != null) {
                blocksToRemove.add(block);
                block.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        return blocksToRemove;
    }

    private void dropFruitFromTree(Block block) {
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    // inspect a cube at the reference
                    Block fruit = block.getRelative(x, y, z);
                    if (fruit.isEmpty()) continue;


                    Location loc = fruit.getLocation();
                    SlimefunItem check = BlockStorage.check(loc);
                    if (check == null) continue;
                    for (Tree tree : ExoticGarden.getTrees()) {
                        if (check.getId().equalsIgnoreCase(tree.getFruitID())) {
                            BlockStorage.clearBlockInfo(loc);
                            ItemStack fruits = check.getItem();
                            fruit.getWorld().playEffect(loc, Effect.STEP_SOUND, Material.OAK_LEAVES);
                            fruit.getWorld().dropItemNaturally(loc, fruits);
                            fruit.setType(Material.AIR, false);
                            break;
                        }
                    }
                }
            }
        }
    }

}
