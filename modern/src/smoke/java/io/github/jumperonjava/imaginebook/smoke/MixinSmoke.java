package io.github.jumperonjava.imaginebook.smoke;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class MixinSmoke implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            for (String name : new String[] {
                    "net.minecraft.client.gui.Gui",
                    "net.minecraft.client.gui.screens.inventory.BookViewScreen",
                    "net.minecraft.client.gui.screens.inventory.BookEditScreen",
                    "net.minecraft.client.gui.components.MultiLineEditBox",
                    "net.minecraft.client.gui.components.MultilineTextField",
                    "net.minecraft.client.renderer.RenderPipelines"}) {
                Class.forName(name, false, loader);
                System.out.println("IMAGINEBOOK_MIXIN_OK " + name);
            }
            var field = Class.forName("io.github.jumperonjava.imaginebook.util.ImageRenderPipeline", true, loader).getDeclaredField("PIPELINE");
            field.setAccessible(true);
            Object pipeline = field.get(null);
            if ((boolean) pipeline.getClass().getMethod("isCull").invoke(pipeline)) {
                throw new AssertionError("Mirrored images must render without back-face culling");
            }
            System.out.println("IMAGINEBOOK_SMOKE_PASSED");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
