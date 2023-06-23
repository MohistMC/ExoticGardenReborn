package com.mohistmc.exoticgarden;

import com.mohistmc.exoticgarden.items.BonemealableItem;
import com.mohistmc.exoticgarden.items.Crook;
import com.mohistmc.exoticgarden.items.CustomFood;
import com.mohistmc.exoticgarden.items.ExoticGardenFruit;
import com.mohistmc.exoticgarden.items.FoodRegistry;
import com.mohistmc.exoticgarden.items.GrassSeeds;
import com.mohistmc.exoticgarden.items.Kitchen;
import com.mohistmc.exoticgarden.items.MagicalEssence;
import com.mohistmc.exoticgarden.listeners.AndroidListener;
import com.mohistmc.exoticgarden.listeners.PlantsListener;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.Juice;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

public class ExoticGarden extends JavaPlugin implements SlimefunAddon {

    public static ExoticGarden instance;

    private final File schematicsFolder = new File(getDataFolder(), "schematics");

    private final List<Berry> berries = new ArrayList<>();
    private final List<Tree> trees = new ArrayList<>();
    private final Map<String, ItemStack> items = new HashMap<>();
    private final Set<String> treeFruits = new HashSet<>();

    protected Config cfg;

    private NestedItemGroup nestedItemGroup;
    private ItemGroup mainItemGroup;
    private ItemGroup miscItemGroup;
    private ItemGroup foodItemGroup;
    private ItemGroup drinksItemGroup;
    private ItemGroup magicalItemGroup;
    private Kitchen kitchen;
    public static TranslateHelper I18N;

    @Override
    public void onEnable() {
        PaperLib.suggestPaper(this);

        String lang = getConfig().getString("lang", "xx_XX");
        String l = lang.split("_")[0];
        String c = lang.split("_")[1];
        I18N = new TranslateHelper(this.getClass(), new Locale(l, c));
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }

        instance = this;
        cfg = new Config(this);

        registerItems();

        new AndroidListener(this);
        new PlantsListener(this);
    }

    private void registerItems() {
        nestedItemGroup = new NestedItemGroup(new NamespacedKey(this, "parent_category"), new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode("847d73a91b52393f2c27e453fb89ab3d784054d414e390d58abd22512edd2b")), I18N.get("tab.all")));
        mainItemGroup = new SubItemGroup(new NamespacedKey(this, "plants_and_fruits"), nestedItemGroup, new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode("a5a5c4a0a16dabc9b1ec72fc83e23ac15d0197de61b138babca7c8a29c820")), I18N.get("tab.plants_and_fruits")));
        miscItemGroup = new SubItemGroup(new NamespacedKey(this, "misc"), nestedItemGroup, new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode("606be2df2122344bda479feece365ee0e9d5da276afa0e8ce8d848f373dd131")), I18N.get("tab.extras_and_tools")));
        foodItemGroup = new SubItemGroup(new NamespacedKey(this, "food"), nestedItemGroup, new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode("a14216d10714082bbe3f412423e6b19232352f4d64f9aca3913cb46318d3ed")), I18N.get("tab.food")));
        drinksItemGroup = new SubItemGroup(new NamespacedKey(this, "drinks"), nestedItemGroup, new CustomItemStack(PlayerHead.getItemStack(PlayerSkin.fromHashCode("2a8f1f70e85825607d28edce1a2ad4506e732b4a5345a5ea6e807c4b313e88")), I18N.get("tab.drinks")));
        magicalItemGroup = new SubItemGroup(new NamespacedKey(this, "magical_crops"), nestedItemGroup, new CustomItemStack(Material.BLAZE_POWDER, I18N.get("tab.magic_plants")));

        kitchen = new Kitchen(this, miscItemGroup);
        kitchen.register(this);
        Research kitchenResearch = new Research(new NamespacedKey(this, "kitchen"), 600, "Kitchen", 30);
        kitchenResearch.addItems(kitchen);
        kitchenResearch.register();

        // @formatter:off
        SlimefunItemStack iceCube = new SlimefunItemStack("ICE_CUBE", "9340bef2c2c33d113bac4e6a1a84d5ffcecbbfab6b32fa7a7f76195442bd1a2", I18N.get("item.ice_cube"));
        new SlimefunItem(miscItemGroup, iceCube, RecipeType.GRIND_STONE, new ItemStack[] {new ItemStack(Material.ICE), null, null, null, null, null, null, null, null}, new SlimefunItemStack(iceCube, 4))
        .register(this);

        registerBerry("Grape", I18N.get("berry.grape"), ChatColor.RED, Color.RED, PlantType.BUSH, "6ee97649bd999955413fcbf0b269c91be4342b10d0755bad7a17e95fcefdab0");
        registerBerry("Blueberry", I18N.get("berry.blueberry"), ChatColor.BLUE, Color.BLUE, PlantType.BUSH, "a5a5c4a0a16dabc9b1ec72fc83e23ac15d0197de61b138babca7c8a29c820");
        registerBerry("Elderberry", I18N.get("berry.elderberry"), ChatColor.RED, Color.FUCHSIA, PlantType.BUSH, "1e4883a1e22c324e753151e2ac424c74f1cc646eec8ea0db3420f1dd1d8b");
        registerBerry("Raspberry", I18N.get("berry.raspberry"), ChatColor.LIGHT_PURPLE, Color.FUCHSIA, PlantType.BUSH, "8262c445bc2dd1c5bbc8b93f2482f9fdbef48a7245e1bdb361d4a568190d9b5");
        registerBerry("Blackberry", I18N.get("berry.blackberry"), ChatColor.DARK_GRAY, Color.GRAY, PlantType.BUSH, "2769f8b78c42e272a669d6e6d19ba8651b710ab76f6b46d909d6a3d482754");
        registerBerry("Cranberry", I18N.get("berry.cranberry"), ChatColor.RED, Color.FUCHSIA, PlantType.BUSH, "d5fe6c718fba719ff622237ed9ea6827d093effab814be2192e9643e3e3d7");
        registerBerry("Cowberry", I18N.get("berry.cowberry"), ChatColor.RED, Color.FUCHSIA, PlantType.BUSH, "a04e54bf255ab0b1c498ca3a0ceae5c7c45f18623a5a02f78a7912701a3249");
        registerBerry("Strawberry", I18N.get("berry.strawberry"), ChatColor.DARK_RED, Color.FUCHSIA, PlantType.FRUIT, "cbc826aaafb8dbf67881e68944414f13985064a3f8f044d8edfb4443e76ba");

        registerPlant("Tomato", I18N.get("plant.tomato"), ChatColor.DARK_RED, PlantType.FRUIT, "99172226d276070dc21b75ba25cc2aa5649da5cac745ba977695b59aebd");
        registerPlant("Lettuce", I18N.get("plant.lettuce"), ChatColor.DARK_GREEN, PlantType.FRUIT, "477dd842c975d8fb03b1add66db8377a18ba987052161f22591e6a4ede7f5");
        registerPlant("Tea Leaf", I18N.get("plant.tea_leaf"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "1514c8b461247ab17fe3606e6e2f4d363dccae9ed5bedd012b498d7ae8eb3");
        registerPlant("Cabbage", I18N.get("plant.cabbage"), ChatColor.DARK_GREEN, PlantType.FRUIT, "fcd6d67320c9131be85a164cd7c5fcf288f28c2816547db30a3187416bdc45b");
        registerPlant("Sweet Potato",I18N.get("plant.sweet_potato"), ChatColor.GOLD, PlantType.FRUIT, "3ff48578b6684e179944ab1bc75fec75f8fd592dfb456f6def76577101a66");
        registerPlant("Mustard Seed", I18N.get("plant.mustard_seed"), ChatColor.YELLOW, PlantType.FRUIT, "ed53a42495fa27fb925699bc3e5f2953cc2dc31d027d14fcf7b8c24b467121f");
        registerPlant("Curry Leaf", I18N.get("plant.curry_leaf"), ChatColor.DARK_GREEN, PlantType.DOUBLE_PLANT, "32af7fa8bdf3252f69863b204559d23bfc2b93d41437103437ab1935f323a31f");
        registerPlant("Onion", I18N.get("plant.onion"), ChatColor.RED, PlantType.FRUIT, "6ce036e327cb9d4d8fef36897a89624b5d9b18f705384ce0d7ed1e1fc7f56");
        registerPlant("Garlic", I18N.get("plant.garlic"), ChatColor.RESET, PlantType.FRUIT, "3052d9c11848ebcc9f8340332577bf1d22b643c34c6aa91fe4c16d5a73f6d8");
        registerPlant("Cilantro", I18N.get("plant.cilantro"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "16149196f3a8d6d6f24e51b27e4cb71c6bab663449daffb7aa211bbe577242");
        registerPlant("Black Pepper", I18N.get("plant.black_pepper"), ChatColor.DARK_GRAY, PlantType.DOUBLE_PLANT, "2342b9bf9f1f6295842b0efb591697b14451f803a165ae58d0dcebd98eacc");
        registerPlant("Green Durian", I18N.get("plant.green_durian"), ChatColor.GREEN, PlantType.FRUIT, "aaa139ecc894c4e455825e313b542e2068601f2f31ab26d30cf276d51345bf3b");
        registerPlant("Durian", I18N.get("plant.durian"), ChatColor.GOLD, PlantType.FRUIT, "44ba890fa8d8684c5119cf1b4b9d5460f5eff392e26ce68b3434e52d18fc666");
        registerPlant("Honeydew Melon", I18N.get("plant.honeydew_melon"), ChatColor.DARK_GREEN, PlantType.FRUIT, "fb14cba0f42a2d138ed243b3bff99cb1ea8cbdcd94fb5fb1e3a307f8e21ab1c");
        registerPlant("Demon Melon", I18N.get("plant.demon_melon"), ChatColor.DARK_GRAY, PlantType.FRUIT, "24c66af64948fd84493dacd1a9dc40736a30931707d838948949bd8e9488d575");
        registerPlant("Papaya", I18N.get("plant.papaya"), ChatColor.YELLOW, PlantType.FRUIT, "631233362962e34f70de66c26ee6fcd2bbd5bc345c744f2dc42a73d779e0647e");

        registerPlant("Leek", I18N.get("plant.leek"), ChatColor.GREEN, PlantType.FRUIT, "c2dd5433db4fddebc4a77166735699400cb18d43672ab31326a83f0b7c2586cc");
        registerPlant("Ginger", I18N.get("plant.ginger"), ChatColor.YELLOW, PlantType.FRUIT, "693c3512fc5885fccbb25d2daf7fdcfae82641ed7e5e3597cddf73e41159f24");
        registerPlant("Paddy", I18N.get("plant.paddy"), ChatColor.GOLD, PlantType.FRUIT, "3b3c84e4bdaf5cc5f85632ac928d059fc2f1ff0cc9e5998f1fe8b227881ada85");
        registerPlant("Ginseng Baby", I18N.get("plant.ginseng_baby"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "36aae6717f49917e043080241264b43a8f387b2df3f61f8f70c2836cd7c3d95c");
        registerPlant("Aubergine", I18N.get("plant.aubergine"), ChatColor.BLUE, PlantType.DOUBLE_PLANT, "8825536a44f1861633484753835e5873ed5667ec5b60ef41757a16a768aa76");
        registerPlant("Radish", I18N.get("plant.radish"), ChatColor.RED, PlantType.FRUIT, "c60339f116115c5d8466f9ce17607410fdafc288ed313850712c78b66b93c0ce");
        registerPlant("White Radish", I18N.get("plant.white_radish"), ChatColor.RESET, PlantType.FRUIT, "374f5302e94be7a27c8ba654d97a658716ca7dbefc8e11484ff683a4164f2d");
        registerPlant("Kohlrabi", I18N.get("plant.kohlrabi"), ChatColor.GREEN, PlantType.FRUIT, "2969d3149333e1e658f5da69dc6131a87fa6817cda2ba6387d5f5f31e0ef73");
        registerPlant("Red Cabbage", I18N.get("plant.red_cabbage"), ChatColor.RED, PlantType.FRUIT, "95c27e9e07446825fa7ecbac1925109e2c16253564a4628202d894492d2c36f8");
        registerPlant("Tree Mushroom", I18N.get("plant.tree_mushroom"), ChatColor.RESET, PlantType.FRUIT, "80f886503d25fadcbea9ee7779890257de0e3e94a4caf7a67c688631cf2b669");
        registerPlant("Olive", I18N.get("plant.olive"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "92bc8fd736d64a83bda5c161625b49de5c13494fb2f1b2c8ebbfca199651ff");
        registerPlant("Passionfruit", I18N.get("plant.passionfruit"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "61609954bdf7d4715e15af2d28c718e91f25ca39fcb8343951bf14706e9966");
        registerPlant("Tumbleweed", I18N.get("plant.tumbleweed"), ChatColor.YELLOW, PlantType.FRUIT, "c2ef3ad5f653a72d936f0c255ced1b0d03688d8c489fcf044eb55d16bc11c8b8");
        registerPlant("Japanese Pumpkin", I18N.get("plant.japanese_pumpkin"), ChatColor.GREEN, PlantType.FRUIT, "5a625495ea6891673014fb65b63e4d817d5bf80d1fae8d5811b1b1179f1f0e4b");
        registerPlant("Blue Pumpkin", I18N.get("plant.blue_pumpkin"), ChatColor.BLUE, PlantType.FRUIT, "dd3384c4d34a8f986e26802ba3587a2aab1f4d2346dd8eb318ce8b7bd194cad2");
        registerPlant("Persimmon", I18N.get("plant.persimmon"), ChatColor.RED, PlantType.DOUBLE_PLANT, "2562a9e019b07f3b60b24f46eb29349d1d6d2695b6dc619ed6cfcaeaf21c0f2b");
        registerPlant("Rainbow Fruits", I18N.get("plant.rainbow_fruits"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "6221fac3c17d189d9c5eced6ff23caa0f73e35b7452d918acb8b7900d14b8950");
        registerPlant("Fig", I18N.get("plant.fig"), ChatColor.DARK_GRAY, PlantType.DOUBLE_PLANT, "90b0537c0c0e8928bb7c85a425ece777494d508e55de59f8e8f462eecbc07835");

        registerPlant("Wine Fruit", I18N.get("plant.wine_fruit"), ChatColor.DARK_GREEN, PlantType.DOUBLE_PLANT, "c4c05dd5d7a92889d8d22d4df0f1a1fe2bee3eddf192f78fc44e02e14dbf629");
        registerPlant("Yummy Fruit", I18N.get("plant.yummy_fruit"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "8cdcf38a8438ed3a547f8d5b47e0801559c595f0e26c45656a76b5bf8a56f");

        registerPlant("Peanut", I18N.get("plant.peanut"), ChatColor.GOLD, PlantType.FRUIT, "608043c5788050ce7ee54edddd48239bce491a9949d1410ad79e165436153ea4");
        registerPlant("Hazelnut", I18N.get("plant.hazelnut"), ChatColor.GOLD, PlantType.FRUIT, "89e521885f3a20f6769b484f069a41d1105b285829cc78f7b6df79c5916e0b10");
        registerPlant("Walnut", I18N.get("plant.walnut"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "9b878a91ee4278d16ef15175ed8e2861541de797475cf4a4732915876c6e9a");
        registerPlant("Almond", I18N.get("plant.almond"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "89ce6a02c3d45fb6d5a8648ee430ac4e39e3e2a7503749f2369437d4deeb93bf");
        registerPlant("Pistachio", I18N.get("plant.pistachio"), ChatColor.GOLD, PlantType.FRUIT, "52a90a34d8740818b0bab2a687ebd2bfd956e08949d930d6ace666f470b3d9c8");
        registerPlant("Gooseberry", I18N.get("plant.gooseberry"), ChatColor.RED, PlantType.FRUIT, "7e57cc56fb21d50af4890a59a18cf919bea1c2b13171e104d32ae67eda49aa16");
        registerPlant("Cauliflower", I18N.get("plant.cauliflower"), ChatColor.RESET, PlantType.FRUIT, "14a6dedd99bb9af3f1b2f338d509a926606cddfdc351e018aad1c07015ad566d");
        registerPlant("Cotton", I18N.get("plant.cotton"), ChatColor.RESET, PlantType.FRUIT, "d1392c68be8dc9eb62b3161b8062c294c4cb7f662330fbec2d31488bff605d90");

        registerPlant("Tequila", I18N.get("plant.tequila"), ChatColor.RESET, PlantType.FRUIT, "3525db972cefca7d71976c1287fc7da3e1951323563dc342a6c4e0f702e8ffb");
        registerPlant("Peashooter", I18N.get("plant.peashooter"), ChatColor.GREEN, PlantType.DOUBLE_PLANT, "dbcbcf932296090ac687db4074ca9e4c9980ce5ed21e96564035a7f52dcc678b");
        registerPlant("Sunflower", I18N.get("plant.sunflower"), ChatColor.YELLOW, PlantType.DOUBLE_PLANT, "49392a2bfa1c4a795bad101797cd54077910c55c1fa8ae55b679e95d2c6e860f");
        registerPlant("Chomper", I18N.get("plant.chomper"), ChatColor.BLUE, PlantType.DOUBLE_PLANT, "798e90575e7d9a0f49587ffd784e2861357e2be83b7c591da3d1bc2d9c482d32");

        registerPlant("Corn", I18N.get("plant.corn"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "9bd3802e5fac03afab742b0f3cca41bcd4723bee911d23be29cffd5b965f1");
        registerPlant("Red Corn", I18N.get("plant.red_corn"), ChatColor.RED, PlantType.DOUBLE_PLANT, "b920b5226b625bc0649c447dda0e268f1c486bd536c220e22992a328c5c27ac6");
        registerPlant("Blue Corn", I18N.get("plant.blue_corn"), ChatColor.BLUE, PlantType.DOUBLE_PLANT, "fd541581b0d24b1b5ab1dad4f51e383d03b9b0bcb4cf86f1345145468efd1c5a");
        registerPlant("Pineapple", I18N.get("plant.pineapple"), ChatColor.GOLD, PlantType.DOUBLE_PLANT, "d7eddd82e575dfd5b7579d89dcd2350c991f0483a7647cffd3d2c587f21");

        registerPlant("Red Bell Peper", "红甜椒", ChatColor.RED, PlantType.DOUBLE_PLANT, "65f7810414a2cee2bc1de12ecef7a4c89fc9b38e9d0414a90991241a5863705f");
        registerPlant("Jalapeno Chili", "墨西哥辣椒", ChatColor.GREEN, PlantType.DOUBLE_PLANT, "5c8e453e84f663f2f6f4af8ed58e65a47aa8c5bffc2a4f67fad318a523b7a75c");
        registerPlant("Chipotle Chili", "熏辣椒", ChatColor.RED, PlantType.DOUBLE_PLANT, "a1406d5e25189fc57e10ee5e97ecb24143b47c1190047f21b63169f2fe6dad7a");
        registerPlant("Habanero Chili", "哈瓦那辣椒", ChatColor.GOLD, PlantType.DOUBLE_PLANT, "1243cc88ef2ff200a512dc898f0c10349eb509ebe360d60f90e5c8630f8ede74");
        registerPlant("Carolina Reaper Chili", "卡罗莱纳死神辣椒", ChatColor.DARK_RED, PlantType.DOUBLE_PLANT, "1bc39557facf985c4f6592d055155102b464f2a4651dbbbeb835b90ed57a98f3");

        registerPlant("Lychee", "荔枝", ChatColor.RED, PlantType.DOUBLE_PLANT, "7b18a885844c9f1dfe8d2db18b3992e3022b68acc9d19f5fe9747208c202df7");
        registerPlant("Banana", "香蕉", ChatColor.YELLOW, PlantType.DOUBLE_PLANT, "20aaa1425d2b99383697d57193f27d872442bcb995508f42d19de4af1f8612");
        registerPlant("Kiwi", "猕猴桃", ChatColor.GREEN, PlantType.DOUBLE_PLANT, "4cc18ec4649f07d5a38a583d9271fd83a6f37318758e46ea87fc2b2d1afc2d9");
        registerPlant("Avocado", "鳄梨", ChatColor.DARK_GRAY, PlantType.DOUBLE_PLANT, "5bd752b141daea14b6b7f8793364538d85517136433893274069b1a90889f1cb");

        registerTree("Oak Apple", "橡树苹果", "cbb311f3ba1c07c3d1147cd210d81fe11fd8ae9e3db212a0fa748946c3633", "&c", Color.FUCHSIA, "橡树苹果汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Coconut", "椰子", "6d27ded57b94cf715b048ef517ab3f85bef5a7be69f14b1573e14e7e42e2e8", "&6", Color.MAROON, "椰奶", false, Material.SAND);
        registerTree("Cherry", "樱桃", "c520766b87d2463c34173ffcd578b0e67d163d37a2d7c2e77915cd91144d40d1", "&c", Color.FUCHSIA, "樱桃汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Pomegranate", "石榴", "cbb311f3ba1c07c3d1147cd210d81fe11fd8ae9e3db212a0fa748946c3633", "&4", Color.RED, "石榴汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Lemon", "柠檬", "957fd56ca15978779324df519354b6639a8d9bc1192c7c3de925a329baef6c", "&e", Color.YELLOW, "柠檬汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Plum", "梅子", "69d664319ff381b4ee69a697715b7642b32d54d726c87f6440bf017a4bcd7", "&5", Color.RED, "酸梅汤", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Lime", "酸橙", "5a5153479d9f146a5ee3c9e218f5e7e84c4fa375e4f86d31772ba71f6468", "&a", Color.LIME, "酸橙汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Orange", "橙子", "65b1db547d1b7956d4511accb1533e21756d7cbc38eb64355a2626412212", "&6", Color.ORANGE, "橙汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Peach", "桃子", "d3ba41fe82757871e8cbec9ded9acbfd19930d93341cf8139d1dfbfaa3ec2a5", "&5", Color.RED, "桃汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Pear", "梨子", "2de28df844961a8eca8efb79ebb4ae10b834c64a66815e8b645aeff75889664b", "&a", Color.LIME, "梨汁", true, Material.DIRT, Material.GRASS_BLOCK);
        registerTree("Dragon Fruit", "火龙果", "847d73a91b52393f2c27e453fb89ab3d784054d414e390d58abd22512edd2b", "&d", Color.FUCHSIA, "火龙果汁", true, Material.DIRT, Material.GRASS_BLOCK);

        FoodRegistry.register(this, miscItemGroup, drinksItemGroup, foodItemGroup);

        registerMagicalPlant("Dirt", "泥土", new ItemStack(Material.DIRT, 2), "1ab43b8c3d34f125e5a3f8b92cd43dfd14c62402c33298461d4d4d7ce2d3aea",
        new ItemStack[] {null, new ItemStack(Material.DIRT), null, new ItemStack(Material.DIRT), new ItemStack(Material.WHEAT_SEEDS), new ItemStack(Material.DIRT), null, new ItemStack(Material.DIRT), null});

        registerMagicalPlant("Coal", "煤炭", new ItemStack(Material.COAL, 2), "7788f5ddaf52c5842287b9427a74dac8f0919eb2fdb1b51365ab25eb392c47",
        new ItemStack[] {null, new ItemStack(Material.COAL_ORE), null, new ItemStack(Material.COAL_ORE), new ItemStack(Material.WHEAT_SEEDS), new ItemStack(Material.COAL_ORE), null, new ItemStack(Material.COAL_ORE), null});

        registerMagicalPlant("Iron", "铁锭", new ItemStack(Material.IRON_INGOT), "db97bdf92b61926e39f5cddf12f8f7132929dee541771e0b592c8b82c9ad52d",
        new ItemStack[] {null, new ItemStack(Material.IRON_BLOCK), null, new ItemStack(Material.IRON_BLOCK), getItem("COAL_PLANT"), new ItemStack(Material.IRON_BLOCK), null, new ItemStack(Material.IRON_BLOCK), null});

        registerMagicalPlant("Gold", "金", SlimefunItems.GOLD_4K, "e4df892293a9236f73f48f9efe979fe07dbd91f7b5d239e4acfd394f6eca",
        new ItemStack[] {null, SlimefunItems.GOLD_16K, null, SlimefunItems.GOLD_16K, getItem("IRON_PLANT"), SlimefunItems.GOLD_16K, null, SlimefunItems.GOLD_16K, null});

        registerMagicalPlant("Copper", "铜", new CustomItemStack(SlimefunItems.COPPER_DUST, 8), "d4fc72f3d5ee66279a45ac9c63ac98969306227c3f4862e9c7c2a4583c097b8a",
        new ItemStack[] {null, SlimefunItems.COPPER_DUST, null, SlimefunItems.COPPER_DUST, getItem("GOLD_PLANT"), SlimefunItems.COPPER_DUST, null, SlimefunItems.COPPER_DUST, null});

        registerMagicalPlant("Aluminum", "铝", new CustomItemStack(SlimefunItems.ALUMINUM_DUST, 8), "f4455341eaff3cf8fe6e46bdfed8f501b461fb6f6d2fe536be7d2bd90d2088aa",
        new ItemStack[] {null, SlimefunItems.ALUMINUM_DUST, null, SlimefunItems.ALUMINUM_DUST, getItem("IRON_PLANT"), SlimefunItems.ALUMINUM_DUST, null, SlimefunItems.ALUMINUM_DUST, null});

        registerMagicalPlant("Tin", "锡", new CustomItemStack(SlimefunItems.TIN_DUST, 8), "6efb43ba2fe6959180ee7307f3f054715a34c0a07079ab73712547ffd753dedd",
        new ItemStack[] {null, SlimefunItems.TIN_DUST, null, SlimefunItems.TIN_DUST, getItem("IRON_PLANT"), SlimefunItems.TIN_DUST, null, SlimefunItems.TIN_DUST, null});

        registerMagicalPlant("Silver", "银", new CustomItemStack(SlimefunItems.SILVER_DUST, 8), "1dd968b1851aa7160d1cd9db7516a8e1bf7b7405e5245c5338aa895fe585f26c",
        new ItemStack[] {null, SlimefunItems.SILVER_DUST, null, SlimefunItems.SILVER_DUST, getItem("IRON_PLANT"), SlimefunItems.SILVER_DUST, null, SlimefunItems.SILVER_DUST, null});

        registerMagicalPlant("Lead", "铅", new CustomItemStack(SlimefunItems.LEAD_DUST, 8), "93c3c418039c4b28b0da75a6d9b22712c7015432d4f4226d6cc0a77d54b64178",
        new ItemStack[] {null, SlimefunItems.LEAD_DUST, null, SlimefunItems.LEAD_DUST, getItem("IRON_PLANT"), SlimefunItems.LEAD_DUST, null, SlimefunItems.LEAD_DUST, null});

        registerMagicalPlant("Redstone", "红石", new ItemStack(Material.REDSTONE, 8), "e8deee5866ab199eda1bdd7707bdb9edd693444f1e3bd336bd2c767151cf2",
        new ItemStack[] {null, new ItemStack(Material.REDSTONE_BLOCK), null, new ItemStack(Material.REDSTONE_BLOCK), getItem("GOLD_PLANT"), new ItemStack(Material.REDSTONE_BLOCK), null, new ItemStack(Material.REDSTONE_BLOCK), null});

        registerMagicalPlant("Lapis", "青金石", new ItemStack(Material.LAPIS_LAZULI, 16), "2aa0d0fea1afaee334cab4d29d869652f5563c635253c0cbed797ed3cf57de0",
        new ItemStack[] {null, new ItemStack(Material.LAPIS_ORE), null, new ItemStack(Material.LAPIS_ORE), getItem("REDSTONE_PLANT"), new ItemStack(Material.LAPIS_ORE), null, new ItemStack(Material.LAPIS_ORE), null});

        registerMagicalPlant("Ender", "末影珍珠", new ItemStack(Material.ENDER_PEARL, 4), "4e35aade81292e6ff4cd33dc0ea6a1326d04597c0e529def4182b1d1548cfe1",
        new ItemStack[] {null, new ItemStack(Material.ENDER_PEARL), null, new ItemStack(Material.ENDER_PEARL), getItem("LAPIS_PLANT"), new ItemStack(Material.ENDER_PEARL), null, new ItemStack(Material.ENDER_PEARL), null});

        registerMagicalPlant("Quartz", "石英", new ItemStack(Material.QUARTZ, 8), "26de58d583c103c1cd34824380c8a477e898fde2eb9a74e71f1a985053b96",
        new ItemStack[] {null, new ItemStack(Material.NETHER_QUARTZ_ORE), null, new ItemStack(Material.NETHER_QUARTZ_ORE), getItem("ENDER_PLANT"), new ItemStack(Material.NETHER_QUARTZ_ORE), null, new ItemStack(Material.NETHER_QUARTZ_ORE), null});

        registerMagicalPlant("Diamond", "钻石", new ItemStack(Material.DIAMOND), "f88cd6dd50359c7d5898c7c7e3e260bfcd3dcb1493a89b9e88e9cbecbfe45949",
        new ItemStack[] {null, new ItemStack(Material.DIAMOND), null, new ItemStack(Material.DIAMOND), getItem("QUARTZ_PLANT"), new ItemStack(Material.DIAMOND), null, new ItemStack(Material.DIAMOND), null});

        registerMagicalPlant("Emerald", "绿宝石", new ItemStack(Material.EMERALD), "4fc495d1e6eb54a386068c6cb121c5875e031b7f61d7236d5f24b77db7da7f",
        new ItemStack[] {null, new ItemStack(Material.EMERALD), null, new ItemStack(Material.EMERALD), getItem("DIAMOND_PLANT"), new ItemStack(Material.EMERALD), null, new ItemStack(Material.EMERALD), null});

        if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_16)) {
            registerMagicalPlant("Netherite", "下界合金", new ItemStack(Material.NETHERITE_INGOT), "27957f895d7bc53423a35aac59d584b41cc30e040269c955e451fe680a1cc049",
            new ItemStack[] {null, new ItemStack(Material.NETHERITE_BLOCK), null, new ItemStack(Material.NETHERITE_BLOCK), getItem("EMERALD_PLANT"), new ItemStack(Material.NETHERITE_BLOCK), null, new ItemStack(Material.NETHERITE_BLOCK), null});
        }

        registerMagicalPlant("Glowstone", "萤石", new ItemStack(Material.GLOWSTONE_DUST, 8), "65d7bed8df714cea063e457ba5e87931141de293dd1d9b9146b0f5ab383866",
        new ItemStack[] { null, new ItemStack(Material.GLOWSTONE), null, new ItemStack(Material.GLOWSTONE), getItem("REDSTONE_PLANT"), new ItemStack(Material.GLOWSTONE), null, new ItemStack(Material.GLOWSTONE), null });

        registerMagicalPlant("Obsidian", "黑曜石", new ItemStack(Material.OBSIDIAN, 2), "7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5",
        new ItemStack[] {null, new ItemStack(Material.OBSIDIAN), null, new ItemStack(Material.OBSIDIAN), getItem("LAPIS_PLANT"), new ItemStack(Material.OBSIDIAN), null, new ItemStack(Material.OBSIDIAN), null});

        registerMagicalPlant("Slime", "粘液球", new ItemStack(Material.SLIME_BALL, 8), "90e65e6e5113a5187dad46dfad3d3bf85e8ef807f82aac228a59c4a95d6f6a",
        new ItemStack[] {null, new ItemStack(Material.SLIME_BALL), null, new ItemStack(Material.SLIME_BALL), getItem("ENDER_PLANT"), new ItemStack(Material.SLIME_BALL), null, new ItemStack(Material.SLIME_BALL), null});

        new Crook(miscItemGroup, new SlimefunItemStack("CROOK", new CustomItemStack(Material.WOODEN_HOE, "&r拐棍", "", "&7+ 树苗掉率提升 &b25%")), RecipeType.ENHANCED_CRAFTING_TABLE,
        new ItemStack[] {new ItemStack(Material.STICK), new ItemStack(Material.STICK), null, null, new ItemStack(Material.STICK), null, null, new ItemStack(Material.STICK), null})
        .register(this);

        SlimefunItemStack grassSeeds = new SlimefunItemStack("GRASS_SEEDS", Material.PUMPKIN_SEEDS, "&r草籽", "", "&7&o可以种在泥土上");
        new GrassSeeds(mainItemGroup, grassSeeds, ExoticGardenRecipeTypes.BREAKING_GRASS, new ItemStack[] {null, null, null, null, new ItemStack(Material.GRASS), null, null, null, null})
        .register(this);
        // @formatter:on

        items.put("WHEAT_SEEDS", new ItemStack(Material.WHEAT_SEEDS));
        items.put("PUMPKIN_SEEDS", new ItemStack(Material.PUMPKIN_SEEDS));
        items.put("MELON_SEEDS", new ItemStack(Material.MELON_SEEDS));

        for (Material sapling : Tag.SAPLINGS.getValues()) {
            items.put(sapling.name(), new ItemStack(sapling));
        }

        items.put("GRASS_SEEDS", grassSeeds);

        Iterator<String> iterator = items.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            cfg.setDefaultValue("grass-drops." + key, true);

            if (!cfg.getBoolean("grass-drops." + key)) {
                iterator.remove();
            }
        }

        cfg.save();

        for (Tree tree : ExoticGarden.getTrees()) {
            treeFruits.add(tree.getFruitID());
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    private void registerTree(String rawName, String name, String texture, String color, Color pcolor, String juice, boolean pie, Material... soil) {
        String id = rawName.toUpperCase(Locale.ROOT).replace(' ', '_');
        Tree tree = new Tree(id, texture, soil);
        trees.add(tree);

        SlimefunItemStack sapling = new SlimefunItemStack(id + "_SAPLING", Material.OAK_SAPLING, color + name + " 树苗");

        items.put(id + "_SAPLING", sapling);

        new BonemealableItem(mainItemGroup, sapling, ExoticGardenRecipeTypes.BREAKING_GRASS, new ItemStack[] { null, null, null, null, new ItemStack(Material.GRASS), null, null, null, null }).register(this);

        new ExoticGardenFruit(mainItemGroup, new SlimefunItemStack(id, texture, color + name), ExoticGardenRecipeTypes.HARVEST_TREE, true, new ItemStack[] { null, null, null, null, getItem(id + "_SAPLING"), null, null, null, null }).register(this);

        if (pcolor != null) {
            new Juice(drinksItemGroup, new SlimefunItemStack(juice.toUpperCase().replace(" ", "_"), new CustomPotion(color + juice, pcolor, new PotionEffect(PotionEffectType.SATURATION, 6, 0), "", "&7&o恢复 &b&o" + "3.0" + " &7&o点饥饿值")), RecipeType.JUICER, new ItemStack[] { getItem(id), null, null, null, null, null, null, null, null }).register(this);
        }

        if (pie) {
            new CustomFood(foodItemGroup, new SlimefunItemStack(id + "_PIE", "3418c6b0a29fc1fe791c89774d828ff63d2a9fa6c83373ef3aa47bf3eb79", color + name + " 派", "", "&7&o恢复 &b&o" + "6.5" + " &7&o点饥饿值"), new ItemStack[] { getItem(id), new ItemStack(Material.EGG), new ItemStack(Material.SUGAR), new ItemStack(Material.MILK_BUCKET), SlimefunItems.WHEAT_FLOUR, null, null, null, null }, 13).register(this);
        }

        if (!new File(schematicsFolder, id + "_TREE.schematic").exists()) {
            saveSchematic(id + "_TREE");
        }
    }

    private void saveSchematic(@Nonnull String id) {
        try (InputStream input = getClass().getResourceAsStream("/schematics/" + id + ".schematic")) {
            try (FileOutputStream output = new FileOutputStream(new File(schematicsFolder, id + ".schematic"))) {
                byte[] buffer = new byte[1024];
                int len;

                while ((len = input != null ? input.read(buffer) : 0) > 0) {
                    output.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, e, () -> "Failed to load file: \"" + id + ".schematic\"");
        }
    }

    public void registerBerry(String rawName, String name, ChatColor color, Color potionColor, PlantType type, String texture) {
        String upperCase = rawName.toUpperCase(Locale.ROOT);
        Berry berry = new Berry(upperCase, type, texture);
        berries.add(berry);

        SlimefunItemStack sfi = new SlimefunItemStack(upperCase + "_BUSH", Material.OAK_SAPLING, color + name + "灌木丛");

        items.put(upperCase + "_BUSH", sfi);

        new BonemealableItem(mainItemGroup, sfi, ExoticGardenRecipeTypes.BREAKING_GRASS, new ItemStack[] { null, null, null, null, new ItemStack(Material.GRASS), null, null, null, null }).register(this);

        new ExoticGardenFruit(mainItemGroup, new SlimefunItemStack(upperCase, texture, color + name), ExoticGardenRecipeTypes.HARVEST_BUSH, true, new ItemStack[] { null, null, null, null, getItem(upperCase + "_BUSH"), null, null, null, null }).register(this);

        new Juice(drinksItemGroup, new SlimefunItemStack(upperCase + "_JUICE", new CustomPotion(color + name + "果汁", potionColor, new PotionEffect(PotionEffectType.SATURATION, 6, 0), "", "&7&o恢复 &b&o" + "3.0" + " &7&o点饥饿值")), RecipeType.JUICER, new ItemStack[] { getItem(upperCase), null, null, null, null, null, null, null, null }).register(this);

        new Juice(drinksItemGroup, new SlimefunItemStack(upperCase + "_SMOOTHIE", new CustomPotion(color + name + "冰沙", potionColor, new PotionEffect(PotionEffectType.SATURATION, 10, 0), "", "&7&o恢复 &b&o" + "5.0" + " &7&o点饥饿值")), RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] { getItem(upperCase + "_JUICE"), getItem("ICE_CUBE"), null, null, null, null, null, null, null }).register(this);

        new CustomFood(foodItemGroup, new SlimefunItemStack(upperCase + "_JELLY_SANDWICH", "8c8a939093ab1cde6677faf7481f311e5f17f63d58825f0e0c174631fb0439", color + name + "果酱三明治", "", "&7&o恢复 &b&o" + "8.0" + " &7&o点饥饿值"), new ItemStack[] { null, new ItemStack(Material.BREAD), null, null, getItem(upperCase + "_JUICE"), null, null, new ItemStack(Material.BREAD), null }, 16).register(this);

        new CustomFood(foodItemGroup, new SlimefunItemStack(upperCase + "_PIE", "3418c6b0a29fc1fe791c89774d828ff63d2a9fa6c83373ef3aa47bf3eb79", color + name + "派", "", "&7&o恢复 &b&o" + "6.5" + " &7&o点饥饿值"), new ItemStack[] { getItem(upperCase), new ItemStack(Material.EGG), new ItemStack(Material.SUGAR), new ItemStack(Material.MILK_BUCKET), SlimefunItems.WHEAT_FLOUR, null, null, null, null }, 13).register(this);
    }

    @Nullable
    private static ItemStack getItem(@Nonnull String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        return item != null ? item.getItem() : null;
    }

    public void registerPlant(String rawName, String name, ChatColor color, PlantType type, String texture) {
        String upperCase = rawName.toUpperCase(Locale.ROOT);
        String enumStyle = upperCase.replace(' ', '_');

        if(type == PlantType.DOUBLE_PLANT){
            type = PlantType.BUSH;
        }

        Berry berry = new Berry(enumStyle, type, texture);
        berries.add(berry);

        SlimefunItemStack bush = new SlimefunItemStack(enumStyle + "_BUSH", Material.OAK_SAPLING, color + name + "植物");
        items.put(upperCase + "_BUSH", bush);

        new BonemealableItem(mainItemGroup, bush, ExoticGardenRecipeTypes.BREAKING_GRASS, new ItemStack[] { null, null, null, null, new ItemStack(Material.GRASS), null, null, null, null })
            .register(this);

        new ExoticGardenFruit(mainItemGroup, new SlimefunItemStack(enumStyle, texture, color + name), ExoticGardenRecipeTypes.HARVEST_BUSH, true, new ItemStack[] { null, null, null, null, getItem(enumStyle + "_BUSH"), null, null, null, null }).register(this);
    }

    private void registerMagicalPlant(String rawName, String name, ItemStack item, String texture, ItemStack[] recipe) {
        String upperCase = rawName.toUpperCase(Locale.ROOT);
        String enumStyle = upperCase.replace(' ', '_');

        SlimefunItemStack essence = new SlimefunItemStack(enumStyle + "_ESSENCE", Material.BLAZE_POWDER, "&r魔法精华", "", "&7" + name);

        Berry berry = new Berry(essence, upperCase + "_ESSENCE", PlantType.ORE_PLANT, texture);
        berries.add(berry);

        new BonemealableItem(magicalItemGroup, new SlimefunItemStack(enumStyle + "_PLANT", Material.OAK_SAPLING, "&f" + name + "植物"), RecipeType.ENHANCED_CRAFTING_TABLE, recipe)
            .register(this);

        MagicalEssence magicalEssence = new MagicalEssence(magicalItemGroup, essence);

        magicalEssence.setRecipeOutput(item.clone());
        magicalEssence.register(this);
    }

    @Nullable
    public static ItemStack harvestPlant(@Nonnull Block block) {
        SlimefunItem item = BlockStorage.check(block);

        if (item == null) {
            return null;
        }

        for (Berry berry : getBerries()) {
            if (item.getId().equalsIgnoreCase(berry.getID())) {
                switch (berry.getType()) {
                    case ORE_PLANT, DOUBLE_PLANT -> {
                        Block plant = block;
                        if (Tag.LEAVES.isTagged(block.getType())) {
                            block = block.getRelative(BlockFace.UP);
                        } else {
                            plant = block.getRelative(BlockFace.SELF);
                        }
                        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, Material.OAK_LEAVES);
                        block.setType(Material.AIR, false);

                        plant.setType(Material.OAK_SAPLING, false);
                        BlockStorage.clearBlockInfo(block.getRelative(BlockFace.UP).getLocation(), false);
                        BlockStorage.clearBlockInfo(plant.getLocation(), false);
                        BlockStorage.store(plant, getItem(berry.toBush()));
                        return berry.getItem().clone();
                    }
                    default -> {
                        block.setType(Material.OAK_SAPLING);
                        BlockStorage.clearBlockInfo(block.getLocation(), false);
                        BlockStorage.store(block, getItem(berry.toBush()));
                        return berry.getItem().clone();
                    }
                }
            }
        }

        return null;
    }

    public void harvestFruit(Block fruit) {
        Location loc = fruit.getLocation();
        SlimefunItem check = BlockStorage.check(loc);

        if (check == null) {
            return;
        }

        if (treeFruits.contains(check.getId())) {
            BlockStorage.clearBlockInfo(loc);
            ItemStack fruits = check.getItem().clone();
            fruit.getWorld().playEffect(loc, Effect.STEP_SOUND, Material.OAK_LEAVES);
            fruit.getWorld().dropItemNaturally(loc, fruits);
            fruit.setType(Material.AIR, false);
        }
    }

    public static ExoticGarden getInstance() {
        return instance;
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    public static Kitchen getKitchen() {
        return instance.kitchen;
    }

    public static List<Tree> getTrees() {
        return instance.trees;
    }

    public static List<Berry> getBerries() {
        return instance.berries;
    }

    public static Map<String, ItemStack> getGrassDrops() {
        return instance.items;
    }

    public Config getCfg() {
        return cfg;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/TheBusyBiscuit/ExoticGarden/issues";
    }

}
