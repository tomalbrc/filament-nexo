package de.tomalbrc.filament.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class RegUtil {
    public static <T> T getValue(Registry<T> registry, ResourceLocation id) {
        //? if >1.21.1 {
        /*return registry.getValue(id);
         *///?} else {
        return registry.get(id);
        //?}
    }

    public static <T> Holder.Reference<T> get(Registry<T> registry, ResourceLocation id) {
        //? if >1.21.1 {
        /*return registry.get(id).orElseThrow();
         *///?} else {
        return registry.getHolder(id).orElseThrow();
        //?}
    }
}
