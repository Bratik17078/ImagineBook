package io.github.jumperonjava.imaginebook.platform;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public final class PlatformPaths {
    private PlatformPaths() {}
    public static Path gameDirectory() { return FabricLoader.getInstance().getGameDir(); }
    public static Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
}
