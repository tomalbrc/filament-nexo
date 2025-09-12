package de.tomalbrc.filament;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.tomalbrc.filament.behaviour.BehaviourConfigMap;
import de.tomalbrc.filament.behaviour.Behaviours;
import de.tomalbrc.filament.behaviour.decoration.Seat;
import de.tomalbrc.filament.behaviour.item.*;
import de.tomalbrc.filament.behaviour.block.*;
import de.tomalbrc.filament.data.BlockData;
import de.tomalbrc.filament.data.DecorationData;
import de.tomalbrc.filament.data.ItemData;
import de.tomalbrc.filament.data.properties.BlockProperties;
import de.tomalbrc.filament.data.properties.BlockStateMappedProperty;
import de.tomalbrc.filament.data.properties.DecorationProperties;
import de.tomalbrc.filament.data.properties.ItemProperties;
import de.tomalbrc.filament.data.resource.BlockResource;
import de.tomalbrc.filament.data.resource.ItemResource;
import de.tomalbrc.filament.registry.BlockRegistry;
import de.tomalbrc.filament.registry.DecorationRegistry;
import de.tomalbrc.filament.registry.ItemRegistry;
import de.tomalbrc.filament.util.*;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.material.PushReaction;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NexoImporter {
    public static void importAll() {
        var root = FabricLoader.getInstance().getGameDir().resolve("nexo");
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Map<String, String> redirects = new Object2ObjectOpenHashMap<>();
        try (var stream = Files.list(root)) {
            stream.forEach(x -> importPack(x, redirects));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void importPack(Path path, Map<String, String> fileRedirects) {
        String dirName = FilenameUtils.getBaseName(path.toString());

        try (var stream = Files.walk(path.resolve("items"))) {
            stream.forEach(file -> {
                try {
                    Path filename = file.toAbsolutePath();
                    String ext = FilenameUtils.getExtension(filename.toString());
                    String baseName = FilenameUtils.getBaseName(filename.toString());
                    if (baseName.startsWith("."))
                        return;

                    if (ext.equals("yml") || ext.equals("yaml")) {
                        InputStream inputStream = new FileInputStream(file.toFile());
                        importSingleFile(dirName, inputStream, fileRedirects);
                    }
                } catch (Throwable e) {
                    Filament.LOGGER.error("Error loading nexo file", e);
                }
            });
        } catch (Throwable e) {
            Filament.LOGGER.error("Error reading nexo directory", e);
        }

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(resourcePackBuilder -> {
            Set<ResourceLocation> texturePaths = new ObjectArraySet<>();

            Path packPath = path.resolve("pack");
            try (var walk = Files.walk(packPath)) {
                walk.forEach(filepath -> {
                    if (filepath.toFile().isDirectory())
                        return;

                    try (var stream = new FileInputStream(filepath.toFile())) {
                        String relativePath = packPath.relativize(filepath).toString().replace("\\", "/");

                        // oraxen or older packs
                        if (!relativePath.startsWith("assets/")) {
                            relativePath = "assets/minecraft/" + relativePath;
                        }

                        String dir = getTextureParent(relativePath);
                        if (dir != null && relativePath.endsWith(".png")) {
                            String ns = getNamespace(relativePath);
                            if (ns != null) {
                                texturePaths.add(ResourceLocation.fromNamespaceAndPath(ns, dir));
                            }
                        }

                        resourcePackBuilder.addData(fileRedirects.getOrDefault(relativePath, relativePath), stream.readAllBytes());
                    } catch (Throwable e) {
                        Filament.LOGGER.error("Error reading nexo asset", e);
                    }
                });
            } catch (Throwable e) {
                Filament.LOGGER.error("Error reading nexo pack assets", e);
            }

            byte[] atlas = generateAtlasJson(texturePaths);
            resourcePackBuilder.addData("assets/minecraft/atlases/blocks.json", atlas);
            resourcePackBuilder.addData("assets/minecraft/atlases/particle.json", atlas);
        });
    }

    public static String getTextureParent(String fullPath) {
        String normalized = fullPath.replace("\\", "/"); // windoof
        String prefix = "assets/";
        String texturesToken = "/textures/";

        int assetsIndex = normalized.indexOf(prefix);
        if (assetsIndex == -1) return null;

        int texturesIndex = normalized.indexOf(texturesToken, assetsIndex);
        if (texturesIndex == -1) return null;

        String afterTextures = normalized.substring(texturesIndex + texturesToken.length());

        int lastSlash = afterTextures.lastIndexOf('/');
        if (lastSlash == -1) return "";

        return afterTextures.substring(0, lastSlash);
    }

    public static String getNamespace(String fullPath) {
        String normalized = fullPath.replace("\\", "/");
        String prefix = "assets/";
        String texturesToken = "/textures/";

        int assetsIndex = normalized.indexOf(prefix);
        int texturesIndex = normalized.indexOf(texturesToken, assetsIndex);

        if (assetsIndex == -1 || texturesIndex == -1) return null;

        return normalized.substring(assetsIndex + prefix.length(), texturesIndex);
    }

    private static byte[] generateAtlasJson(Collection<ResourceLocation> sourceDirs) {
        JsonObject root = new JsonObject();
        JsonArray sources = new JsonArray();

        for (ResourceLocation dir : sourceDirs) {
            JsonObject source = new JsonObject();
            source.addProperty("type", "directory");
            source.addProperty("prefix", dir.getPath() + "/");
            source.addProperty("source", dir.getPath());
            sources.add(source);
        }
        root.add("sources", sources);

        Gson gson = new GsonBuilder().create();
        return gson.toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    public static void importSingleFile(String baseName, InputStream inputStream, Map<String, String> fileRedirects) {
        Yaml yaml = new Yaml();
        Map<String, Object> elements = yaml.load(inputStream);
        for (Map.Entry<String, Object> element : elements.entrySet()) {
            processElement(element.getKey().contains(":") ? ResourceLocation.parse(element.getKey()) : ResourceLocation.fromNamespaceAndPath(baseName.toLowerCase(), element.getKey().toLowerCase()), element.getValue(), fileRedirects);
        }
    }

    private static void processElement(ResourceLocation id, Object data, Map<String, String> fileRedirects) {
        var name = getValue("displayname", data, String.class);
        if (name == null)
            name = getValue("itemname", data, String.class); // fallback for older configs (nexo for <1.20.4)

        var components = getValue("Components", data, String.class);
        DataComponentMap xmap = null;
        if (components != null) {
            try {
                var jsonData = Json.GSON.fromJson(new Gson().toJson(components), DataComponentMap.class);
                if (jsonData != null) {
                    xmap = jsonData;
                }
            } catch (Exception e) {
                Filament.LOGGER.error("Could not load components for nexo item {}", id, e);
            }
        }

        var builder = DataComponentMap.builder();
        if (xmap != null) builder.addAll(xmap);

        if (name != null)
            builder.set(DataComponents.ITEM_NAME, TextUtil.formatText(name));

        var material = getValue("material", data, String.class);
        Item vanillaItem;
        if (material != null) {
            vanillaItem = RegUtil.getValue(BuiltInRegistries.ITEM, ResourceLocation.parse(material.toLowerCase()));
        } else {
            vanillaItem = Items.LEATHER_HORSE_ARMOR;
        }

        var attr = getValue("AttributeModifiers", data, List.class);
        if (attr != null) {
            handleOraxenAttributes(attr, builder);
        }

        var mechanics = getMap("Mechanics", data);
        Map<String, Object> customBlock = null;
        Map<String, Object> furniture = null;
        Integer dur = null;
        if (mechanics != null) {
            customBlock = getMap("custom_block", mechanics);
            furniture = getMap("furniture", mechanics);
            dur = getValue("durability", mechanics, Integer.class);
        }

        if (dur != null) builder.set(DataComponents.MAX_DAMAGE, dur);

        if (customBlock != null || furniture != null) {
            if (customBlock != null) {
                addBlock(id, data, furniture, vanillaItem, name, builder);
            } else {
                addDecoration(id, data, furniture, vanillaItem, name, builder);
            }
        } else {
            addItem(id, data, vanillaItem, name, builder, fileRedirects);
        }
    }

    private static void handleOraxenAttributes(List<?> attr, DataComponentMap.Builder builder) {
        var attrBuilder = ItemAttributeModifiers.builder();
        for (Object o : attr) {
            var amount = getValue("amount", o, Number.class);
            var attribute = getValue("attribute", o, String.class);
            var operation = getValue("operation", o, Integer.class);
            var slot = EquipmentSlot.valueOf(getValue("slot", o, String.class));

            var attrId = ResourceLocation.parse(attribute.toLowerCase().replace("_", ".").replace("generic.", ""));
            attrBuilder.add(RegUtil.get(BuiltInRegistries.ATTRIBUTE, attrId), new AttributeModifier(ResourceLocation.fromNamespaceAndPath("filament", "armor"), amount.doubleValue(), Arrays.stream(AttributeModifier.Operation.values()).filter(y -> y.id() == operation).findAny().orElseThrow()), EquipmentSlotGroup.bySlot(slot));
        }
        builder.set(DataComponents.ATTRIBUTE_MODIFIERS, attrBuilder.build());
    }

    private static void addBlock(ResourceLocation id, Object data, Map<String, Object> furniture, Item vanillaItem, String name, DataComponentMap.Builder builder) {
        // load as block
        var model = getValue("model", data, String.class);

        var props = new BlockProperties();
        props.destroyTime = 2;
        props.explosionResistance = 2;
        props.solid = true;
        props.transparent = false;
        props.allowsSpawning = true;

        var lightObj = getValue("light", furniture, Integer.class);
        if (lightObj != null)
            props.lightEmission = BlockStateMappedProperty.of(lightObj);

        BlockData blockData = new BlockDataBuilder(
                id,
                new BlockResource(Map.of(
                        "default",
                        new PolymerBlockModel(ResourceLocation.parse(model), 0, 0, false, 0)
                )))
                .vanillaItem(vanillaItem)
                //? if >1.21.1 {
                .displayName(TextUtil.formatText(name))
                //?} else {
                /*.displayName(TextUtil.formatText(name))
                 *///?}
                .components(builder.build())
                .blockModelType(BlockStateMappedProperty.of(BlockModelType.FULL_BLOCK))
                .properties(props)
                .build();

        BlockRegistry.register(blockData);
    }

    private static void addDecoration(ResourceLocation id, Object data, Map<String, Object> furniture, Item vanillaItem, String name, DataComponentMap.Builder builder) {
        // load as decoration
        var pack = getMap("Pack", data);
        String model = getValue("model", pack, String.class);
        BehaviourConfigMap behaviourConfigMap = new BehaviourConfigMap();

        if (model == null)
            return;

        var props = new DecorationProperties();
        props.destroyTime = 0.5f;
        props.explosionResistance = 0.5f;
        props.solid = false;
        props.transparent = true;
        props.allowsSpawning = false;
        props.pushReaction = PushReaction.BLOCK;
        var rotObj = getValue("rotatable", furniture, Boolean.class);
        if (rotObj == Boolean.TRUE || rotObj == null) {
            //? if >1.21.1 {
            Rotating.Config config = new Rotating.Config();
            var restrictedRot = getValue("restricted_rotation", furniture, String.class);
            if (restrictedRot != null) {
                config.smooth = restrictedRot.equals("STRICT");
            } else {
                config.smooth = true;
            }
            behaviourConfigMap.put(Behaviours.ROTATING, config);
            //?} else {
            /*props.rotate = true;
            var restrictedRot = getValue("restricted_rotation", furniture, String.class);
            if (restrictedRot != null) {
                props.rotateSmooth = restrictedRot.equals("STRICT");
            } else {
                props.rotateSmooth = true;
            }
            *///?}
        }

        var lightObj = getValue("light", furniture, Integer.class);
        if (lightObj != null)
            props.lightEmission = BlockStateMappedProperty.of(lightObj);

        var placing = getMap("limited_placing", furniture);
        if (placing != null) {
            props.placement = new DecorationProperties.Placement(
                    getValue("wall", placing, Boolean.class) == Boolean.TRUE,
                    getValue("floor", placing, Boolean.class) == Boolean.TRUE,
                    getValue("roof", placing, Boolean.class) == Boolean.TRUE);
        }

        var waterloggable = getValue("waterloggable", furniture, Boolean.class) == Boolean.TRUE;
        if (waterloggable) {
            //? if >1.21.1 {
            behaviourConfigMap.put(Behaviours.WATERLOGGABLE, new Waterloggable.Config());
            //?} else {
            /*props.waterloggable = true;
             *///?}
        }

        var drop = getMap("drop", furniture);
        if (drop != null) {
            // TODO silk_touch
            // and custom drop support
            props.drops = true;
        } else {
            props.drops = false;
        }

        var type = getValue("type", furniture, String.class);
        boolean forceItemFrame = false;
        if (type != null && type.endsWith("ITEM_FRAME")) {
            props.display = ItemDisplayContext.FIXED;
            forceItemFrame = true;
            if (type.startsWith("GLOW")) {
                props.glow = true;
            }
        }

        var barrier = getValue("barrier", furniture, Boolean.class);
        List<DecorationData.BlockConfig> blocks = new ObjectArrayList<>();
        if (barrier != null && barrier) {
            blocks.add(new DecorationData.BlockConfig(new Vector3f(), new Vector3f(1)));
        }

        var barriers = getValue("barriers", furniture, List.class);
        if (barriers != null) {
            for (Object blockConf : barriers) {
                boolean origin = blockConf instanceof String s && s.equals("origin");
                if (origin) {
                    blocks.add(new DecorationData.BlockConfig(new Vector3f(), new Vector3f(1)));
                } else {
                    Integer x = getValue("x", blockConf, Integer.class);
                    Integer y = getValue("y", blockConf, Integer.class);
                    Integer z = getValue("z", blockConf, Integer.class);
                    blocks.add(new DecorationData.BlockConfig(new Vector3f(
                            x == null ? 0 : x,
                            y == null ? 0 : y,
                            z == null ? 0 : z
                    ).rotateY(Mth.PI).round(), new Vector3f(1)));
                }
            }
        }

        //? if >1.21.1 {
        Seat.Config filamentSeats = new Seat.Config();
        //?} else {
        /*Seat.SeatConfig filamentSeats = new Seat.SeatConfig();
         *///?}
        var seat = getMap("seat", furniture);
        if (seat != null) {
            var height = getValue("height", seat, Float.class);
            var yaw = getValue("yaw", seat, Float.class);

            if (yaw == null) yaw = 180f;
            else yaw -= 180;

            if (height == null) height = 0f;

            if (blocks.isEmpty()) {
                var seatConf = new Seat.SeatConfigData();
                seatConf.offset = new Vector3f();
                filamentSeats.add(seatConf);
            } else {
                for (DecorationData.BlockConfig blockConfig : blocks) {
                    var seatConf = new Seat.SeatConfigData();
                    seatConf.offset = new Vector3f(blockConfig.origin().add(0, 0.5f + height, 0, new Vector3f()));
                    seatConf.direction = yaw;
                    filamentSeats.add(seatConf);
                }
            }
        }

        if (!filamentSeats.isEmpty()) {
            behaviourConfigMap.put(Behaviours.SEAT, filamentSeats);
        }

        if (blocks.isEmpty()) {
            props.destroyTime = 0f;
        }

        DecorationData decorationData = new DecorationDataBuilder(id)
                .vanillaItem(vanillaItem)
                .displayName(TextUtil.formatText(name))
                .itemResource(defaultItemResource(ResourceLocation.parse(model)))
                .behaviourConfig(behaviourConfigMap)
                .components(builder.build())
                .properties(props)
                .blocks(blocks.isEmpty() ? null : blocks)
                .size(blocks.isEmpty() ? new Vector2f(1, 1) : null)
                .itemFrame(forceItemFrame)
                .build();

        DecorationRegistry.register(decorationData);
    }

    private static ItemResource defaultItemResource(ResourceLocation model) {
        //? if >1.21.1 {
        return new ItemResource(
                Map.of("default", model),
                null,
                null
        );
        //?} else {
        /*return ItemResource.of(Map.of("default", model), null, null);
         *///?}
    }

    private static ItemResource texturedItemResource(ResourceLocation model, String parent_model, Map<String, ResourceLocation> textures) {
        //? if >1.21.1 {
        return new ItemResource(
                Map.of("default", model),
                parent_model != null ? ResourceLocation.parse(parent_model) : null,
                !textures.isEmpty() ? Map.of("default", textures) : null
        );
        //?} else {
        /*return ItemResource.of(Map.of("default", model), parent_model != null ? ResourceLocation.parse(parent_model) : null, !textures.isEmpty() ? Map.of("default", textures) : null);
         *///?}
    }

    private static void addItem(ResourceLocation id, Object data, Item vanillaItem, String name, DataComponentMap.Builder builder, Map<String, String> fileRedirects) {
        // load as simple item
        var pack = getMap("Pack", data);
        var model = getValue("model", pack, String.class);
        if (model == null)
            return;

        var props = new ItemProperties();
        props.copyComponents = true;
        props.copyTags = true;

        BehaviourConfigMap behaviourConfigMap = new BehaviourConfigMap();

        String parent_model = getValue("parent_model", pack, String.class);

        String texture = getValue("texture", pack, String.class);
        List<String> textureList = getValue("textures", pack, List.class);
        Map<String, ResourceLocation> textures = new Object2ObjectOpenHashMap<>();
        if (texture != null || textureList != null) {
            if (parent_model == null) parent_model = "item/generated";

            if (texture != null) {
                textures.put("layer0", ResourceLocation.parse(texture));
            } else {
                for (int i = 0; i < textureList.size(); i++) {
                    textures.put("layer" + i, ResourceLocation.parse(textureList.get(i)));
                }
            }
        }

        //? if <=1.21.1 {
        /*var customArmor = getMap("#CustomArmor", pack);
        if (customArmor != null) {
            String l1 = customArmor.get("layer1").toString();
            String l2 = customArmor.get("layer2").toString();
            var src1 = "assets/minecraft/textures/" + l1 + ".png";
            var src2 = "assets/minecraft/textures/" + l2 + ".png";
            var pathParts = l1.replace("_layer_1", "").split("/");
            var path = pathParts[pathParts.length - 1];
            fileRedirects.put(src1, "assets/minecraft/textures/trims/models/armor/" + path + ".png");
            fileRedirects.put(src2, "assets/minecraft/textures/trims/models/armor/" + path + "_leggings.png");

            var conf = new Armor.Config();
            conf.trim = true;
            conf.texture = ResourceLocation.withDefaultNamespace(path);
            if (vanillaItem instanceof ArmorItem armorItem) {
                conf.slot = armorItem.getEquipmentSlot();
            }

            behaviourConfigMap.put(Behaviours.ARMOR, conf);
        }

        if (vanillaItem instanceof ElytraItem) {
            behaviourConfigMap.put(Behaviours.ELYTRA, new Elytra.Config());
        }

        var mechanics = getMap("Mechanics", data);
        if (mechanics != null && mechanics.containsKey("cosmetic") && vanillaItem instanceof Equipable equipable) {
            var conf = new Cosmetic.Config();
            conf.slot = equipable.getEquipmentSlot();
            behaviourConfigMap.put(Behaviours.COSMETIC, conf);
        }
        *///?}

        if (vanillaItem instanceof ShovelItem) {
            behaviourConfigMap.put(Behaviours.SHOVEL, new Shovel.Config());
        }

        if (vanillaItem instanceof HoeItem) {
            behaviourConfigMap.put(Behaviours.HOE, new Hoe.Config());
        }

        if (vanillaItem instanceof ShearsItem) {
            behaviourConfigMap.put(Behaviours.SHEARS, new Shears.Config());
        }

        if (vanillaItem instanceof AxeItem) {
            behaviourConfigMap.put(Behaviours.STRIPPER, new Stripper.Config());
        }

        //? if >1.21.1 {
        if (vanillaItem instanceof HoneycombItem) {
            behaviourConfigMap.put(Behaviours.WAX, new Stripper.Config());
        }
        //?}

        ItemData itemData = new ItemBuilder(id)
                .vanillaItem(vanillaItem)
                .displayName(TextUtil.formatText(name))
                .itemResource(texturedItemResource(ResourceLocation.parse(model), parent_model, textures))
                .properties(props)
                .components(builder.build())
                .build();

        ItemRegistry.register(itemData);
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Map<String, Object> getMap(String key, Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?>) {
                return (Map<String, Object>) value;
            }
        }
        return null;
    }

    public static <T> T getValue(String key, Object obj, Class<T> clazz) {
        if (obj instanceof Map<?, ?> map) {
            Object value = map.get(key);
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
        }
        return null;
    }

    //? if <=1.21.1 {
    /*private static class TextUtil {
        public static Component formatText(String text) {
            return TextParserUtils.formatText(text);
        }
    }
    *///?}
}
