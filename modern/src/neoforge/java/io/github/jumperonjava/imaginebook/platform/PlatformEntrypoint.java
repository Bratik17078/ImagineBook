package io.github.jumperonjava.imaginebook.platform;

import io.github.jumperonjava.imaginebook.Imaginebook;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Imaginebook.MOD_ID, dist = Dist.CLIENT)
public final class PlatformEntrypoint {
    public PlatformEntrypoint() { Imaginebook.init(); }
}
