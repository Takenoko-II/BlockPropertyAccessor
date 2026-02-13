package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NullMarked
public class BlockBinarySearchLayerizer {
    public static final String IDENTIFIER = "id";

    public static final String PROPERTIES = "properties";

    private final List<BlockType> list;

    public BlockBinarySearchLayerizer(List<BlockType> list) {
        this.list = list;
    }

    public void layerize() {
        layerize(list.size(), BlockPropertyAccessor.FUNCTION_DIRECTORY);

        final Path entrypoint = BlockPropertyAccessor.FUNCTION_DIRECTORY.resolve(".mcfunction");
        try {
            final List<String> lines = new ArrayList<>(Files.readAllLines(entrypoint));
            lines.addFirst(String.format(
                "data remove storage %s: %s",
                BlockPropertyAccessor.NAMESPACE,
                IDENTIFIER
            ));
            lines.addFirst(String.format(
                "data remove storage %s: %s",
                BlockPropertyAccessor.NAMESPACE,
                PROPERTIES
            ));
            Files.write(entrypoint, lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<BlockType> layerize(int size, Path directory) {
        try {
            Files.createDirectories(directory);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (size <= 2) {
            final List<BlockType> values = new ArrayList<>();
            if (!list.isEmpty()) values.add(list.removeFirst());
            if (!list.isEmpty()) values.add(list.removeFirst());

            finalBranchFunction(directory, values);

            return values;
        }
        else {
            final List<BlockType> values0 = layerize(size / 2, directory.resolve("0"));
            final List<BlockType> values1 = layerize(size / 2, directory.resolve("1"));

            final Path relative = BlockPropertyAccessor.FUNCTION_DIRECTORY.relativize(directory);
            final Path tagDirectory = BlockPropertyAccessor.TAGS_BLOCK_DIRECTORY.resolve(relative);

            try {
                Files.createDirectories(tagDirectory);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (values0.isEmpty() && values1.isEmpty()) {
                tagBlockTags(tagDirectory.resolve("0.json"), relative.resolve("0"));
                tagBlockTags(tagDirectory.resolve("1.json"), relative.resolve("1"));
            }
            else {
                tagBlocks(tagDirectory.resolve("0.json"), values0);
                tagBlocks(tagDirectory.resolve("1.json"), values1);
            }

            final Path functionPath = directory.resolve(".mcfunction");
            try {
                Files.createFile(functionPath);
                Files.write(functionPath, List.of(
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve("0").toString().replaceAll("\\\\", "/"),
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve("0").toString().replaceAll("\\\\", "/") + '/'
                    ),
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve("1").toString().replaceAll("\\\\", "/"),
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve("1").toString().replaceAll("\\\\", "/") + '/'
                    )
                ));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return List.of();
        }
    }

    private void finalBranchFunction(Path directory, List<BlockType> values) {
        final Path path = directory.resolve(".mcfunction");
        final List<String> lines = new ArrayList<>();

        for (final BlockType blockType : values) {
            final String functionName = BlockPropertyAccessor.FUNCTION_DIRECTORY
                .relativize(directory.resolve(blockType.getKey().value()))
                .toString()
                .replaceAll("\\\\", "/");

            lines.add(String.format(
                "execute if block ~ ~ ~ %s run function %s:%s",
                blockType.getKey(),
                BlockPropertyAccessor.NAMESPACE,
                functionName
            ));

            dataModifyFunction(directory, blockType);
        }

        try {
            Files.createFile(path);
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void dataModifyFunction(Path directory, BlockType blockType) {
        final NamespacedKey key = blockType.getKey();
        final Path specificBlockFunctionPath = directory.resolve(key.value() + ".mcfunction");
        final List<String> finalLines = new ArrayList<>();
        finalLines.add(String.format(
            "data modify storage %s: %s set value \"%s\"",
            BlockPropertyAccessor.NAMESPACE,
            IDENTIFIER,
            key
        ));

        final Collection<Property<?>> properties = ((CraftBlockType<?>) blockType).getHandle()
            .defaultBlockState().getProperties();

        for (final Property<?> property : properties) {
            property.getAllValues().forEach(value -> {
                finalLines.add(String.format(
                    "execute if block ~ ~ ~ %s[%s=%s] run data modify storage %s: %s.%s set value %s",
                    key,
                    property.getName(),
                    getPropertyValueName(value),
                    BlockPropertyAccessor.NAMESPACE,
                    PROPERTIES,
                    property.getName(),
                    getPropertyValueName(value)
                ));
            });
        }

        try {
            Files.createFile(specificBlockFunctionPath);
            Files.write(specificBlockFunctionPath, finalLines);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Comparable<T>> String getPropertyValueName(Property.Value<T> value) {
        return value.property().getName(value.value());
    }

    private void tagBlocks(Path path, List<BlockType> values) {
        final JSONObject object = new JSONObject();
        object.set("replace", false);
        final JSONArray array = new JSONArray();
        for (final BlockType value : values) {
            /*final JSONObject element = new JSONObject();
            element.set("required", false);
            element.set("id", value.getKey().toString());
            array.add(element);*/
            array.add(value.getKey().toString());
        }
        object.set("values", array);

        final JSONFile file = new JSONFile(path);
        if (!file.exists()) file.create();
        file.write(object);
    }

    private void tagBlockTags(Path path, Path directory) {
        final JSONObject object = new JSONObject();
        object.set("replace", false);
        final JSONArray array = new JSONArray();
        array.add('#' + BlockPropertyAccessor.NAMESPACE + ':' + directory.resolve("0").toString().replaceAll("\\\\", "/"));
        array.add('#' + BlockPropertyAccessor.NAMESPACE + ':' + directory.resolve("1").toString().replaceAll("\\\\", "/"));
        object.set("values", array);

        final JSONFile file = new JSONFile(path);
        if (!file.exists()) file.create();
        file.write(object);
    }
}
