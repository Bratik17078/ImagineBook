package io.github.jumperonjava.imaginebook.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelineAccessor {
    @Accessor("GUI_TEXTURED_SNIPPET")
    static RenderPipeline.Snippet imaginebook$texturedSnippet() { throw new AssertionError(); }

    @Invoker("register")
    static RenderPipeline imaginebook$register(RenderPipeline pipeline) { throw new AssertionError(); }
}
