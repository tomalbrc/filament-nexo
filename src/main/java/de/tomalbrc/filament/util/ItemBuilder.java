package de.tomalbrc.filament.util;

import de.tomalbrc.filament.behaviour.BehaviourConfigMap;
import de.tomalbrc.filament.data.ItemData;
import de.tomalbrc.filament.data.properties.ItemProperties;
import de.tomalbrc.filament.data.resource.ItemResource;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class ItemBuilder {
    private ResourceLocation id;
    private Item vanillaItem;
    private Map<String, String> translations;
    private Component displayName;
    private ItemResource itemResource;
    private ResourceLocation itemModel;
    private BehaviourConfigMap behaviourConfig;
    private ItemProperties properties;
    private DataComponentMap components;
    private ResourceLocation itemGroup;
    private Set<ResourceLocation> itemTags;

    public ItemBuilder(@NotNull ResourceLocation id) {
        this.id = id;
    }

    public ItemBuilder vanillaItem(@Nullable Item vanillaItem) {
        this.vanillaItem = vanillaItem;
        return this;
    }

    public ItemBuilder translations(@Nullable Map<String, String> translations) {
        this.translations = translations;
        return this;
    }

    public ItemBuilder displayName(@Nullable Component displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemBuilder itemResource(@Nullable ItemResource itemResource) {
        this.itemResource = itemResource;
        return this;
    }

    public ItemBuilder itemModel(@Nullable ResourceLocation itemModel) {
        this.itemModel = itemModel;
        return this;
    }

    public ItemBuilder behaviourConfig(@Nullable BehaviourConfigMap behaviourConfig) {
        this.behaviourConfig = behaviourConfig;
        return this;
    }

    public ItemBuilder properties(@Nullable ItemProperties properties) {
        this.properties = properties;
        return this;
    }

    public ItemBuilder components(@Nullable DataComponentMap components) {
        this.components = components;
        return this;
    }

    public ItemBuilder itemGroup(@Nullable ResourceLocation itemGroup) {
        this.itemGroup = itemGroup;
        return this;
    }

    public ItemBuilder itemTags(@Nullable Set<ResourceLocation> itemTags) {
        this.itemTags = itemTags;
        return this;
    }

    public ItemData build() {
        return new ItemData(
                id,
                vanillaItem,
                translations,
                displayName,
                itemResource,
                itemModel,
                behaviourConfig,
                properties,
                components,
                itemGroup,
                itemTags
        );
    }
}