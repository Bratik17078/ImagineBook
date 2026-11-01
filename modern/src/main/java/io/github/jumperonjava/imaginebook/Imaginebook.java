package io.github.jumperonjava.imaginebook;

import io.github.jumperonjava.imaginebook.resolvers.AsyncImageDownloader;
import io.github.jumperonjava.imaginebook.resolvers.Resolver;
import io.github.jumperonjava.imaginebook.resolvers.TextureResolver;
import io.github.jumperonjava.imaginebook.resolvers.UrlResolver;

import io.github.jumperonjava.imaginebook.platform.PlatformPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Imaginebook {
    public static final String MOD_ID = "imaginebook";
    public static Logger LOGGER = LoggerFactory.getLogger("ImagineBook");
    public static final String TEST_BALLER = "https://i.kym-cdn.com/photos/images/original/002/461/188/20d.png";

    public static void init() {
        createImagineBookFolder();
        io.github.jumperonjava.imaginebook.util.ImageRenderPipeline.init();

    }

    public static int LENGTH = 1023;
    public static boolean cancelledFinalize = false;

    public static String fixImgurLink(String link) {
        if (link.startsWith("https://imgur.com/")) {
            String imageId = link.substring("https://imgur.com/".length());
            return "https://i.imgur.com/" + imageId + ".png";
        }
        return link;
    }

    public static Path getCachePath(){

        return PlatformPaths.gameDirectory().resolve("imaginebook");

    }
    public static Path getConfigFile(){

        return PlatformPaths.configDirectory().resolve("imaginebook.json");

    }

    private static Map<String,Resolver> resolvers = new HashMap<>(Map.of(
            "http",UrlResolver.INSTANCE,
            "https",UrlResolver.INSTANCE_S,
            "res", TextureResolver.INSTANCE
    ));
    public static void addResolver(String id, Resolver resolver) {
        resolvers.put(id, resolver);
    }
    public static Resolver getResolver(String type) {
        if(resolvers.containsKey(type))
            return resolvers.get(type);
        return path -> AsyncImageDownloader.ERROR_IMAGE;
    }

    public static boolean resolverExists(String resolver) {
        return resolvers.containsKey(resolver);
    }

    public void cleanFiles(){

    }

    public static void createImagineBookFolder() {
        Path imagineBookPath = getCachePath();

        try {
            if (!Files.exists(imagineBookPath)) {
                Files.createDirectories(imagineBookPath);
                LOGGER.info("imaginebook folder created at: " + imagineBookPath.toString());
            } else {
                //LOGGER.info("imaginebook folder already exists at: " + imagineBookPath.toString());
            }
        } catch (IOException e) {
            LOGGER.info("Failed to create imaginebook folder: " + e.getMessage());
        }
    }

}
