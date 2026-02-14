package com.gmail.takenokoii78.blockpropertyaccessor;

import com.gmail.takenokoii78.json.JSONFile;
import com.gmail.takenokoii78.json.values.JSONArray;
import com.gmail.takenokoii78.json.values.JSONObject;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@NullMarked
public class BlockBinarySearchLayerizer {
    public static final String IDENTIFIER = "id";

    public static final String PROPERTIES = "properties";

    private final char ZERO;

    private final char ONE;

    private final List<BlockType> list;

    private int c;

    public BlockBinarySearchLayerizer(List<BlockType> list, char zero, char one) {
        this.list = new ArrayList<>(list);
        this.ZERO = zero;
        this.ONE = one;
    }

    public void layerize() {
        BlockPropertyAccessor.getPlugin().getComponentLogger().info("二分探索を階層構造化しています");

        layerize(list, BlockPropertyAccessor.FUNCTION_DIRECTORY);

        BlockPropertyAccessor.getPlugin().getComponentLogger().info("階層構造の生成が完了しました");

        BlockPropertyAccessor.getPlugin().getComponentLogger().info("エントリポイントのコマンドを調整しています");

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

        BlockPropertyAccessor.getPlugin().getComponentLogger().info("エントリポイントの編集が完了しました");

        BlockPropertyAccessor.getPlugin().getComponentLogger().info("処理されたブロックタイプ数: {}", c);
    }

    private @Nullable List<BlockType> layerize(List<BlockType> list, Path directory) {
        try {
            Files.createDirectories(directory);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (list.size() <= 2) {
            final List<BlockType> values = new ArrayList<>();
            if (!list.isEmpty()) values.add(list.removeFirst());
            if (!list.isEmpty()) values.add(list.removeFirst());

            finalBranchFunction(directory, values);

            for (final BlockType value : values) {
                BlockPropertyAccessor.getPlugin().getComponentLogger().info("ブロック {} に関連する処理を作成しました", value.getKey());
                c++;
            }

            return values;
        }
        else {
            final int s = list.size() / 2;
            final List<BlockType> a = list.subList(0, s);
            final List<BlockType> b = list.subList(s, list.size());
            final List<BlockType> values0 = layerize(new ArrayList<>(a), directory.resolve(String.valueOf(ZERO)));
            final List<BlockType> values1 = layerize(new ArrayList<>(b), directory.resolve(String.valueOf(ONE)));

            final Path relative = BlockPropertyAccessor.FUNCTION_DIRECTORY.relativize(directory);
            final Path tagDirectory = BlockPropertyAccessor.TAGS_BLOCK_DIRECTORY.resolve(relative);

            try {
                Files.createDirectories(tagDirectory);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (values0 == null) {
                tagBlockTags(tagDirectory.resolve(ZERO + ".json"), relative.resolve(String.valueOf(ZERO)));
            }
            else {
                tagBlocks(tagDirectory.resolve(ZERO + ".json"), values0);
            }

            if (values1 == null) {
                tagBlockTags(tagDirectory.resolve(ONE + ".json"), relative.resolve(String.valueOf(ONE)));
            }
            else {
                tagBlocks(tagDirectory.resolve(ONE + ".json"), values1);
            }

            final Path functionPath = directory.resolve(".mcfunction");
            try {
                Files.createFile(functionPath);
                Files.write(functionPath, List.of(
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/"),
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/") + '/'
                    ),
                    String.format(
                        "execute if block ~ ~ ~ #%s:%s run function %s:%s",
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/"),
                        BlockPropertyAccessor.NAMESPACE,
                        relative.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/") + '/'
                    )
                ));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
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
            property.getAllValues().forEach(value -> finalLines.add(String.format(
                "execute if block ~ ~ ~ %s[%s=%s] run data modify storage %s: %s.%s set value %s",
                key,
                property.getName(),
                getPropertyValueName(value),
                BlockPropertyAccessor.NAMESPACE,
                PROPERTIES,
                property.getName(),
                getPropertyValueName(value)
            )));
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
        final String $0 = '#' + BlockPropertyAccessor.NAMESPACE + ':' + directory.resolve(String.valueOf(ZERO)).toString().replaceAll("\\\\", "/");
        final String $1 = '#' + BlockPropertyAccessor.NAMESPACE + ':' + directory.resolve(String.valueOf(ONE)).toString().replaceAll("\\\\", "/");
        array.add(JSONObject.valueOf(Map.of("id", $0, "required", false)));
        array.add(JSONObject.valueOf(Map.of("id", $1, "required", false)));
        object.set("values", array);

        final JSONFile file = new JSONFile(path);
        if (!file.exists()) file.create();
        file.write(object);
    }
}
