package io.github.jumperonjava.imaginebook.image;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface BookDrawable {
    Image.ImageSize getSize();

    void render(GuiGraphicsExtractor context);
}
