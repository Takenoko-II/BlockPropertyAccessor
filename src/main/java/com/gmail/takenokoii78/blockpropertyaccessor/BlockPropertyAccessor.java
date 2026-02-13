package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.files.ResourceAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@NullMarked
public final class BlockPropertyAccessor extends JavaPlugin {
    public static final String BLOCK_PROPERTY_ACCESSOR = "BlockPropertyAccessor";

    public static final String NAMESPACE = "block_property_accessor";

    public static final Path ROOT_DIRECTORY = Path.of(BLOCK_PROPERTY_ACCESSOR + '-' + Bukkit.getMinecraftVersion());

    public static final Path DATA_DIRECTORY = ROOT_DIRECTORY.resolve("data");

    public static final Path NAMESPACE_DIRECTORY = DATA_DIRECTORY.resolve(NAMESPACE);

    public static final Path FUNCTION_DIRECTORY = NAMESPACE_DIRECTORY.resolve("function");

    public static final Path TAGS_BLOCK_DIRECTORY = NAMESPACE_DIRECTORY.resolve("tags/block");

    public static final Path FINAL_OUTPUT = Path.of(ROOT_DIRECTORY + ".zip");

    @Override
    public void onLoad() {
        getComponentLogger().info("BlockPropertyAccessor をロードしています");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info("BlockPropertyAccessor が起動しました");

        final ResourceAccess resourceAccess = new ResourceAccess(BLOCK_PROPERTY_ACCESSOR);
        resourceAccess.copy(ROOT_DIRECTORY);

        final BlockBinarySearchLayerizer layerizer = new BlockBinarySearchLayerizer(
            Registry.BLOCK.stream().toList(),
            '0',
            '1'
        );
        layerizer.layerize();

        final ZipCompressor compressor = new ZipCompressor(ROOT_DIRECTORY);
        compressor.compress(FINAL_OUTPUT);
        try (final Stream<Path> stream = Files.walk(ROOT_DIRECTORY).sorted(Comparator.reverseOrder())) {
            stream.forEach(path -> {
                try {
                    Files.delete(path);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        getComponentLogger().info(
            Component.text("データパック {} が生成されました").color(NamedTextColor.GREEN),
            FINAL_OUTPUT
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getComponentLogger().info(Component.text("BlockPropertyAccessor が停止しました"));
    }
}
