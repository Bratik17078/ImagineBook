package io.github.jumperonjava.imaginebook.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.jumperonjava.imaginebook.mixin.RenderPipelineAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ImageRenderPipeline {
    private static final RenderPipeline PIPELINE = RenderPipelineAccessor.imaginebook$register(
            RenderPipeline.builder(RenderPipelineAccessor.imaginebook$texturedSnippet())
                    .withLocation(Identifier.fromNamespaceAndPath("imaginebook", "pipeline/gui_textured_nocull"))
                    .withCull(false).build());

    private ImageRenderPipeline() {}

    public static void init() { /* Register before the game compiles required pipelines. */ }

    public static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                            float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(PIPELINE, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}
