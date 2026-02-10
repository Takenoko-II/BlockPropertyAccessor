package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.subnokoii78.gpcore.files.ResourceAccess;
import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class BlockPropertyAccessor extends JavaPlugin {
    private static final String DATAPACK_ROOT_DIRECTORY_PATH = "BlockPropertyAccessor" + '-' + Bukkit.getMinecraftVersion();

    private static final String DATAPACK_NAMESPACE = "block_property_accessor";

    private static final String ALL_BLOCKS_TAG_NAME = "all";

    private static final String BLOCK_ID_FINDER_FUNCTION_NAME = "get_block_id";

    private static final String BLOCK_STATES_COLLECTOR_FUNCTION_NAME = "get_block_states";

    private static final String STORAGE_PATH_OUT = "out";

    private static final String STORAGE_PATH_IDENTIFIER = STORAGE_PATH_OUT + '.' + "id";

    private static final String STORAGE_PATH_PROPERTIES = STORAGE_PATH_OUT + '.' + "properties";

    private static final int SEPARATOR = 34;

    private static final String DATAPACK_NAMESPACE_DIRECTORY_PATH = DATAPACK_ROOT_DIRECTORY_PATH + "/data/" + DATAPACK_NAMESPACE;

    private static final String DATAPACK_BLOCK_TAGS_DIRECTORY_PATH = DATAPACK_NAMESPACE_DIRECTORY_PATH + "/tags/block";

    private static final String DATAPACK_FUNCTION_TAGS_DIRECTORY_PATH = DATAPACK_NAMESPACE_DIRECTORY_PATH + "/tags/function";

    private static final String DATAPACK_FUNCTION_DIRECTORY_PATH = DATAPACK_NAMESPACE_DIRECTORY_PATH + "/function";

    @Override
    public void onLoad() {
        getComponentLogger().info("BlockPropertyAccessor をロードしています");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info("BlockPropertyAccessor が起動しました");
        create();
        getComponentLogger().info("データパック {} が生成されました", DATAPACK_ROOT_DIRECTORY_PATH);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getComponentLogger().info(Component.text("BlockPropertyAccessor が停止しました"));
    }

    private void allBlocksTagJson(JSONFile file) {
        final JSONArray values = new JSONArray();

        final Registry<BlockType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);

        for (final BlockType blockType : registry) {
            values.add(blockType.getKey().toString());
        }

        final JSONObject root = new JSONObject();
        root.set("replace", false);
        root.set("values", values);

        if (!file.exists()) file.create();
        file.write(root);
    }

    private void statesCollectorMcfunction(File file) {
        final List<String> lines = new ArrayList<>();
        lines.add(String.format(
            "data modify storage %s: %s set value {}",
            DATAPACK_NAMESPACE,
            STORAGE_PATH_PROPERTIES
        ));

        for (final Field field : BlockStateProperties.class.getDeclaredFields()) {
            final Object obj;
            try {
                obj = field.get(null);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if (!(obj instanceof Property<?> property)) {
                continue;
            }

            property.getAllValues().forEach(value -> lines.add(String.format(
                "execute if block ~ ~ ~ #%s:%s[%s=%s] run data modify storage %s: %s.%s set value %s",
                DATAPACK_NAMESPACE,
                ALL_BLOCKS_TAG_NAME,
                property.getName(),
                value.value(),
                DATAPACK_NAMESPACE,
                STORAGE_PATH_PROPERTIES,
                property.getName(),
                value.value()
            )));
        }

        try {
            if (!file.exists()) Files.createFile(file.toPath());
            Files.write(file.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void identifierFinderMcfunctions(File directory) {
        if (!directory.isDirectory()) {
            throw new RuntimeException("not a directory");
        }

        final Registry<BlockType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK);

        int i = 1;
        JSONArray array = new JSONArray();
        List<String> lines = new ArrayList<>();

        for (final BlockType blockType : registry) {
            array.add(blockType.getKey().toString());
            lines.add(String.format(
                "execute if block ~ ~ ~ %s run data modify storage %s: %s set value %s",
                blockType.getKey(),
                DATAPACK_NAMESPACE,
                STORAGE_PATH_IDENTIFIER,
                blockType.getKey()
            ));

            if (i >= SEPARATOR) {
                final JSONObject tag = new JSONObject();
                tag.set("replace", false);
                tag.set("values", array);

                final JSONFile tagFile = new JSONFile(DATAPACK_BLOCK_TAGS_DIRECTORY_PATH + '/' + i + ".json");
                if (!tagFile.exists()) tagFile.create();
                tagFile.write(tag);

                final Path functionPath = Path.of(DATAPACK_FUNCTION_DIRECTORY_PATH + '/' + i + ".mcfunction");
                try {
                    if (!Files.exists(functionPath)) Files.createFile(functionPath);
                    Files.write(functionPath, lines, StandardOpenOption.TRUNCATE_EXISTING);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

                array = new JSONArray();
                lines = new ArrayList<>();
            }

            i++;
        }

        final Path path = Path.of(DATAPACK_FUNCTION_DIRECTORY_PATH + '/' + BLOCK_ID_FINDER_FUNCTION_NAME);
        final List<String> idFuncLines = new ArrayList<>();

        idFuncLines.add(String.format(
            "data modify storage %s: %s set value minecraft:air",
            DATAPACK_NAMESPACE,
            STORAGE_PATH_IDENTIFIER
        ));

        for (int j = 1; j < i; j++) {
            idFuncLines.add(String.format(
                "execute if block ~ ~ ~ #%s:%d run function %s:%s",
                DATAPACK_NAMESPACE,
                j,
                DATAPACK_NAMESPACE,
                j
            ));
        }

        try {
            if (!Files.exists(path)) Files.createFile(path);
            Files.write(path, idFuncLines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void create() {
        final ResourceAccess resourceAccess = new ResourceAccess(DATAPACK_ROOT_DIRECTORY_PATH);
        resourceAccess.copy(Path.of(DATAPACK_ROOT_DIRECTORY_PATH));

        final JSONFile tagJson = new JSONFile(Path.of(
            DATAPACK_BLOCK_TAGS_DIRECTORY_PATH + '/' + ALL_BLOCKS_TAG_NAME + ".json")
        );
        allBlocksTagJson(tagJson);

        identifierFinderMcfunctions(new File(DATAPACK_FUNCTION_DIRECTORY_PATH));
        statesCollectorMcfunction(new File(DATAPACK_FUNCTION_DIRECTORY_PATH + '/' + BLOCK_STATES_COLLECTOR_FUNCTION_NAME + ".mcfunction"));

        final JSONFile fTagJson = new JSONFile(Path.of(
            DATAPACK_FUNCTION_TAGS_DIRECTORY_PATH + '/' + ".json"
        ));
        final JSONObject obj = new JSONObject();
        final JSONArray values = new JSONArray();
        values.add(DATAPACK_NAMESPACE + ':' + BLOCK_ID_FINDER_FUNCTION_NAME);
        values.add(DATAPACK_NAMESPACE + ':' + BLOCK_STATES_COLLECTOR_FUNCTION_NAME);
        obj.set("replace", false);
        obj.set("values", values);
        if (!fTagJson.exists()) fTagJson.create();
        fTagJson.write(obj);
    }
}
