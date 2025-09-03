package de.tomalbrc.filament.util;

import de.tomalbrc.filament.behaviour.BehaviourConfigMap;
import de.tomalbrc.filament.data.DecorationData;
import de.tomalbrc.filament.data.properties.BlockStateMappedProperty;
import de.tomalbrc.filament.data.properties.DecorationProperties;
import de.tomalbrc.filament.data.resource.BlockResource;
import de.tomalbrc.filament.data.resource.ItemResource;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Map;
import java.util.Set;
import java.util.List;

public class DecorationDataBuilder {
    private final ResourceLocation id;
    private Item vanillaItem;
    private Map<String, String> translations;
    private Component displayName;
    private ItemResource itemResource;
    private BlockResource blockResource;
    private ResourceLocation itemModel;
    private BehaviourConfigMap behaviourConfig;
    private DataComponentMap components;
    private ResourceLocation itemGroup;
    private Set<ResourceLocation> itemTags;
    private Set<ResourceLocation> blockTags;
    private DecorationProperties properties;
    private List<DecorationData.BlockConfig> blocks;
    private BlockStateMappedProperty<BlockState> block;
    private Vector2f size;
    private Boolean itemFrame;

    public DecorationDataBuilder(@NotNull ResourceLocation id) {
        this.id = id;
    }

    public DecorationDataBuilder vanillaItem(@Nullable Item vanillaItem) {
        this.vanillaItem = vanillaItem;
        return this;
    }

    public DecorationDataBuilder translations(@Nullable Map<String, String> translations) {
        this.translations = translations;
        return this;
    }

    public DecorationDataBuilder displayName(@Nullable Component displayName) {
        this.displayName = displayName;
        return this;
    }

    public DecorationDataBuilder itemResource(@Nullable ItemResource itemResource) {
        this.itemResource = itemResource;
        return this;
    }

    public DecorationDataBuilder blockResource(@Nullable BlockResource blockResource) {
        this.blockResource = blockResource;
        return this;
    }

    public DecorationDataBuilder itemModel(@Nullable ResourceLocation itemModel) {
        this.itemModel = itemModel;
        return this;
    }

    public DecorationDataBuilder behaviourConfig(@Nullable BehaviourConfigMap behaviourConfig) {
        this.behaviourConfig = behaviourConfig;
        return this;
    }

    public DecorationDataBuilder components(@Nullable DataComponentMap components) {
        this.components = components;
        return this;
    }

    public DecorationDataBuilder itemGroup(@Nullable ResourceLocation itemGroup) {
        this.itemGroup = itemGroup;
        return this;
    }

    public DecorationDataBuilder itemTags(@Nullable Set<ResourceLocation> itemTags) {
        this.itemTags = itemTags;
        return this;
    }

    public DecorationDataBuilder blockTags(@Nullable Set<ResourceLocation> blockTags) {
        this.blockTags = blockTags;
        return this;
    }

    public DecorationDataBuilder properties(@Nullable DecorationProperties properties) {
        this.properties = properties;
        return this;
    }

    public DecorationDataBuilder blocks(@Nullable List<DecorationData.BlockConfig> blocks) {
        this.blocks = blocks;
        return this;
    }

    public DecorationDataBuilder block(@Nullable BlockStateMappedProperty<BlockState> block) {
        this.block = block;
        return this;
    }

    public DecorationDataBuilder size(@Nullable Vector2f size) {
        this.size = size;
        return this;
    }

    public DecorationDataBuilder itemFrame(@Nullable Boolean itemFrame) {
        this.itemFrame = itemFrame;
        return this;
    }

    public DecorationData build() {
        return new DecorationData(
                id,
                vanillaItem,
                translations,
                displayName,
                itemResource,
                blockResource,
                itemModel,
                behaviourConfig,
                components,
                itemGroup,
                itemTags,
                blockTags,
                properties,
                blocks,
                block,
                size,
                itemFrame
        );
    }
}
