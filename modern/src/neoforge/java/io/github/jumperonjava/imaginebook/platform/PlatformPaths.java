package io.github.jumperonjava.imaginebook.platform;

import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PlatformPaths {
    private PlatformPaths() {}
    public static Path gameDirectory() { return FMLPaths.GAMEDIR.get(); }
    public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
}
