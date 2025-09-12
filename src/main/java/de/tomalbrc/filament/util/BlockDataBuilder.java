package de.tomalbrc.filament.util;

import de.tomalbrc.filament.behaviour.BehaviourConfigMap;
import de.tomalbrc.filament.data.BlockData;
import de.tomalbrc.filament.data.properties.BlockProperties;
import de.tomalbrc.filament.data.properties.BlockStateMappedProperty;
import de.tomalbrc.filament.data.resource.BlockResource;
import de.tomalbrc.filament.data.resource.ItemResource;
import eu.pb4.polymer.blocks.api.BlockModelType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class BlockDataBuilder {
    private final ResourceLocation id;
    private final BlockResource blockResource;
    private Item vanillaItem;
    private Map<String, String> translations;
    private Component displayName;
    private ItemResource itemResource;
    private ResourceLocation itemModel;
    private BehaviourConfigMap behaviourConfig;
    private DataComponentMap components;
    private ResourceLocation itemGroup;
    private BlockStateMappedProperty<BlockModelType> blockModelType;
    private BlockProperties properties;
    private Set<ResourceLocation> itemTags;
    private Set<ResourceLocation> blockTags;

    public BlockDataBuilder(@NotNull ResourceLocation id, @NotNull BlockResource blockResource) {
        this.id = id;
        this.blockResource = blockResource;
    }

    public BlockDataBuilder vanillaItem(@Nullable Item vanillaItem) {
        this.vanillaItem = vanillaItem;
        return this;
    }

    public BlockDataBuilder translations(@Nullable Map<String, String> translations) {
        this.translations = translations;
        return this;
    }

    public BlockDataBuilder displayName(@Nullable Component displayName) {
        this.displayName = displayName;
        return this;
    }

    public BlockDataBuilder itemResource(@Nullable ItemResource itemResource) {
        this.itemResource = itemResource;
        return this;
    }

    public BlockDataBuilder itemModel(@Nullable ResourceLocation itemModel) {
        this.itemModel = itemModel;
        return this;
    }

    public BlockDataBuilder behaviourConfig(@Nullable BehaviourConfigMap behaviourConfig) {
        this.behaviourConfig = behaviourConfig;
        return this;
    }

    public BlockDataBuilder components(@Nullable DataComponentMap components) {
        this.components = components;
        return this;
    }

    public BlockDataBuilder itemGroup(@Nullable ResourceLocation itemGroup) {
        this.itemGroup = itemGroup;
        return this;
    }

    public BlockDataBuilder blockModelType(@Nullable BlockStateMappedProperty<BlockModelType> blockModelType) {
        this.blockModelType = blockModelType;
        return this;
    }

    public BlockDataBuilder properties(@Nullable BlockProperties properties) {
        this.properties = properties;
        return this;
    }

    public BlockDataBuilder itemTags(@Nullable Set<ResourceLocation> itemTags) {
        this.itemTags = itemTags;
        return this;
    }

    public BlockDataBuilder blockTags(@Nullable Set<ResourceLocation> blockTags) {
        this.blockTags = blockTags;
        return this;
    }

    public BlockData build() {
        //? if >1.21.1 {
                return new BlockData(
                id,
                vanillaItem,
                translations,
                displayName,
                itemResource,
                itemModel,
                behaviourConfig,
                components,
                itemGroup,
                blockResource,
                blockModelType,
                properties,
                itemTags,
                blockTags
        );
        //?} else {
        /*return new BlockData(
                id,
                vanillaItem,
                translations,
                blockResource,
                itemResource,
                blockModelType,
                properties,
                behaviourConfig,
                components,
                itemGroup,
                itemTags,
                blockTags
        );
        *///?}
    }
}
