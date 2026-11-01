package io.github.jumperonjava.imaginebook.platform;

import io.github.jumperonjava.imaginebook.Imaginebook;
import net.fabricmc.api.ClientModInitializer;

public final class PlatformEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() { Imaginebook.init(); }
}
