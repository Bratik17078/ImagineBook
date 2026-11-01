package io.github.jumperonjava.imaginebook.image;

import io.github.jumperonjava.imaginebook.util.VersionFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class Image implements BookDrawable {
    private final Identifier identifier;
    private final ImageSize size;

    @Override
    public ImageSize getSize() {
        return size;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Image(Identifier identifier, ImageSize size) {
        this.identifier = identifier;
        this.size = size;
    }
    public Image(Identifier identifier) {
        this.identifier = identifier;

        var texture = Minecraft.getInstance().getTextureManager().getTexture(identifier);

        if (texture instanceof DynamicTexture imageBackedTexture){
            size = new ImageSize(imageBackedTexture.getPixels().getWidth(), imageBackedTexture.getPixels().getHeight());
        }
        else {
            size = new ImageSize(64,64);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context) {
        VersionFunctions.drawTexture(context, this.getIdentifier(),
                0,
                0,
                (float) 0,
                (float) 0,
                getSize().getWidth(),
                getSize().getHeight(),
                getSize().getWidth(),
                getSize().getHeight()
        );
    }

    public static class ImageSize {
        private final int width;
        private final int height;

        public ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        private int min() {
            return Math.min(width, height);
        }

        public int getWidth() {
            return width*100/min();
        }

        public int getHeight() {
            return height*100/min();
        }
    }
}
