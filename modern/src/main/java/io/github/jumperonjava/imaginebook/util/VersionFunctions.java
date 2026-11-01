package io.github.jumperonjava.imaginebook.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import java.util.*;

public class VersionFunctions {
    public static void drawTexture(GuiGraphicsExtractor context, Identifier identifier, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {

        ImageRenderPipeline.draw(context, identifier, x, y, u, v, width, height, textureWidth, textureHeight);

    }

    public static void pushMatrix(GuiGraphicsExtractor context) {

        context.pose().pushMatrix();

    }

    public static void popMatrix(GuiGraphicsExtractor context) {

        context.pose().popMatrix();

    }

    public static void translate2D(GuiGraphicsExtractor context, float x, float y) {

        context.pose().translate(x, y);

    }

    public static void scale2D(GuiGraphicsExtractor context, float x, float y) {

        context.pose().scale(x, y);

    }

    public static void rotateZ(GuiGraphicsExtractor context, float degrees) {

        context.pose().rotate((float) Math.toRadians(degrees));

    }

    // List.reversed method was added only in java 21, and it crashes mod (softly, just logs errors) when running on lower java version
    // this method reimplements that functionality
    public static <T> List<T> reversed(List<T> collection) {
        var reversedList = new LinkedList<T>();
        collection.forEach((item) -> reversedList.add(0, item));
        return new ArrayList<>(reversedList);
    }
}
