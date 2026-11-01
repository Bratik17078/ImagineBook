package io.github.jumperonjava.imaginebook.mixin;

import io.github.jumperonjava.imaginebook.ImageSerializer;
import io.github.jumperonjava.imaginebook.Imaginebook;
import io.github.jumperonjava.imaginebook.image.ImageData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow private int currentPage;
    @Unique private final List<List<ImageData>> imaginebook$pages = new ArrayList<>();

    protected BookScreenMixin(Component title) { super(title); }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/inventory/BookViewScreen$BookAccess;Z)V", at = @At("TAIL"))
    private void imaginebook$construct(BookViewScreen.BookAccess access, boolean sound, CallbackInfo ci) {
        imaginebook$parse(access);
    }

    @Inject(method = "setBookAccess", at = @At("TAIL"))
    private void imaginebook$setAccess(BookViewScreen.BookAccess access, CallbackInfo ci) {
        imaginebook$parse(access);
    }

    @Unique
    private void imaginebook$parse(BookViewScreen.BookAccess access) {
        imaginebook$pages.clear();
        for (Component text : access.pages()) {
            String page = text.getString();
            List<ImageData> images = new ArrayList<>();
            if (page.length() == Imaginebook.LENGTH) {
                try {
                    String[] lines = page.split("\n");
                    images.addAll(ImageSerializer.deserializeImageMetadata(Base64.getDecoder().decode(lines[lines.length - 1])));
                } catch (RuntimeException e) {
                    Imaginebook.LOGGER.warn("Invalid legacy book image metadata", e);
                }
            }
            images.addAll(ImageSerializer.parseSafeModeImages(page));
            imaginebook$pages.add(images);
        }
    }

    // Vanilla draws text and controls above these images, after extracting the background.
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void imaginebook$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (currentPage < 0 || currentPage >= imaginebook$pages.size()) return;
        for (ImageData image : imaginebook$pages.get(currentPage).reversed()) {
            image.renderImage(graphics, (width - 192) / 2, 2);
        }
    }
}
