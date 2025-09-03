package de.tomalbrc.filament;

import net.fabricmc.api.ModInitializer;

public class FilamentNexo implements ModInitializer {
    @Override
    public void onInitialize() {
        NexoImporter.importAll();
    }
}
