package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.files.ResourceAccess;
import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.JSONPath;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    private static @Nullable Plugin plugin;

    @Override
    public void onLoad() {
        plugin = this;
        getComponentLogger().info("BlockPropertyAccessor をロードしています");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info("BlockPropertyAccessor が起動しました");

        final ResourceAccess resourceAccess = new ResourceAccess(BLOCK_PROPERTY_ACCESSOR);
        resourceAccess.copy(ROOT_DIRECTORY, handle -> {
            if (handle.getTo().endsWith("MARKER")) {
                handle.ignore();
            }
            else if (handle.getTo().endsWith("pack.mcmeta")) {
                handle.postProcess(p -> {
                    getComponentLogger().info("pack.mcmeta を作成しています");

                    final JSONFile packMcmeta = new JSONFile(p);
                    final JSONObject jsonObject = packMcmeta.readAsObject();

                    final PackFormat packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
                    final JSONArray version = JSONArray.valueOf(List.of(packFormat.major(), packFormat.minor()));

                    jsonObject.set(JSONPath.of("pack.min_format"), version);
                    jsonObject.set(JSONPath.of("pack.max_format"), version);

                    packMcmeta.write(jsonObject);

                    getComponentLogger().info("pack.mcmeta をロードしました: バージョン " + version.asList());
                });
            }
        });

        final BlockBinarySearchLayerizer layerizer = new BlockBinarySearchLayerizer(
            Registry.BLOCK.stream().toList(),
            '0',
            '1'
        );
        layerizer.layerize();

        getComponentLogger().info("データパックを .zip に圧縮しています");

        final ZipCompressor compressor = new ZipCompressor(ROOT_DIRECTORY);
        compressor.compress(FINAL_OUTPUT);

        getComponentLogger().info("不要なファイルを除去します");

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

    public static Plugin getPlugin() {
        if (plugin == null) {
            throw new RuntimeException();
        }

        return plugin;
    }
}
