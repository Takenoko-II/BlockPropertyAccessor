# BlockPropertyAccessor

ブロックID及びブロック状態を全列挙することにより、ブロックのデータをコマンドから取得するためのデータパックを生成するPaperプラグイン
<br>またはそれによって生成されたデータパックそのもの

## 使い方 ／ Usage

### 1. データパックの生成 ／ Generation

> [!NOTE]
> 基本的に、既に生成済みのデータパックが [Releases](https://github.com/Takenoko-II/BlockPropertyAccessor/releases) にあるのでこの項は必要に応じて読み飛ばしてOK

1. 該当のバージョンの Paper を用意する
2. `plugins/` にプラグインの `.jar` を入れる
3. サーバーを起動する
4. サーバーコンソールからコマンド `/blockpropertyaccessor` を実行する
5. サーバーディレクトリ直下にデータパックの `.zip` が自動生成される！！！

### 2. データパックの導入 ／ Installation

`.zip` をワールドの `datapacks/` に入れるだけ

### 3. データパックの使い方 ／ Usage

以下のコマンドを実行すると、実行座標に存在するブロックのID・プロパティがすべてストレージ `block_property_accessor:` に格納される
```mcfunction
function block_property_accessor:
```

例えば上記のコマンドを実行した上で以下のコマンドを実行すると
```mcfunction
data get storage block_property_accessor:
```

レッドストーンランプの場合はこんな感じに出力される
```json
{
    "id": "minecraft:redstone_lamp",
    "properties": {
        "lit": false
    }
}
```

### 4. 実行負荷 ／ Load

バージョン1.21.11時点で全1166のブロックID・88のブロックプロパティに対応しているが、それらの特定は二分探索によって実装されているため、限りなく高速で軽量

#### 実行例: レッドストーンランプ

`/debug` によるプロファイリング
```txt
block_property_accessor:
    [F] block_property_accessor: size=4
        [C] data remove storage block_property_accessor: properties
            [M] ストレージ変数block_property_accessor:を変更しました
        [R = 1] data remove storage block_property_accessor: properties
        [C] data remove storage block_property_accessor: id
            [M] ストレージ変数block_property_accessor:を変更しました
        [R = 1] data remove storage block_property_accessor: id
        [C] execute if block ~ ~ ~ #block_property_accessor:0 run function block_property_accessor:0/
        [C] execute if block ~ ~ ~ #block_property_accessor:1 run function block_property_accessor:1/
            [M] 関数「block_property_accessor:1/」を実行中です
        [F] block_property_accessor:1/ size=2
            [C] execute if block ~ ~ ~ #block_property_accessor:1/0 run function block_property_accessor:1/0/
                [M] 関数「block_property_accessor:1/0/」を実行中です
            [F] block_property_accessor:1/0/ size=2
                [C] execute if block ~ ~ ~ #block_property_accessor:1/0/0 run function block_property_accessor:1/0/0/
                [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1 run function block_property_accessor:1/0/1/
                    [M] 関数「block_property_accessor:1/0/1/」を実行中です
                [F] block_property_accessor:1/0/1/ size=2
                    [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/0 run function block_property_accessor:1/0/1/0/
                    [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1 run function block_property_accessor:1/0/1/1/
                        [M] 関数「block_property_accessor:1/0/1/1/」を実行中です
                    [F] block_property_accessor:1/0/1/1/ size=2
                        [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0 run function block_property_accessor:1/0/1/1/0/
                            [M] 関数「block_property_accessor:1/0/1/1/0/」を実行中です
                        [F] block_property_accessor:1/0/1/1/0/ size=2
                            [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0 run function block_property_accessor:1/0/1/1/0/0/
                                [M] 関数「block_property_accessor:1/0/1/1/0/0/」を実行中です
                            [F] block_property_accessor:1/0/1/1/0/0/ size=2
                                [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/0 run function block_property_accessor:1/0/1/1/0/0/0/
                                [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/1 run function block_property_accessor:1/0/1/1/0/0/1/
                                    [M] 関数「block_property_accessor:1/0/1/1/0/0/1/」を実行中です
                                [F] block_property_accessor:1/0/1/1/0/0/1/ size=2
                                    [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/1/0 run function block_property_accessor:1/0/1/1/0/0/1/0/
                                        [M] 関数「block_property_accessor:1/0/1/1/0/0/1/0/」を実行中です
                                    [F] block_property_accessor:1/0/1/1/0/0/1/0/ size=2
                                        [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/1/0/0 run function block_property_accessor:1/0/1/1/0/0/1/0/0/
                                            [M] 関数「block_property_accessor:1/0/1/1/0/0/1/0/0/」を実行中です
                                        [F] block_property_accessor:1/0/1/1/0/0/1/0/0/ size=2
                                            [C] execute if block ~ ~ ~ minecraft:vault run function block_property_accessor:1/0/1/1/0/0/1/0/0/vault
                                            [C] execute if block ~ ~ ~ minecraft:redstone_lamp run function block_property_accessor:1/0/1/1/0/0/1/0/0/redstone_lamp
                                                [M] 関数「block_property_accessor:1/0/1/1/0/0/1/0/0/redstone_lamp」を実行中です
                                            [F] block_property_accessor:1/0/1/1/0/0/1/0/0/redstone_lamp size=3
                                                [C] data modify storage block_property_accessor: id set value "minecraft:redstone_lamp"
                                                    [M] ストレージ変数block_property_accessor:を変更しました
                                                [R = 1] data modify storage block_property_accessor: id set value "minecraft:redstone_lamp"
                                                [C] execute if block ~ ~ ~ minecraft:redstone_lamp[lit=true] run data modify storage block_property_accessor: properties.lit set value true
                                                [C] execute if block ~ ~ ~ minecraft:redstone_lamp[lit=false] run data modify storage block_property_accessor: properties.lit set value false
                                                    [M] ストレージ変数block_property_accessor:を変更しました
                                                [R = 1] execute if block ~ ~ ~ minecraft:redstone_lamp[lit=false] run data modify storage block_property_accessor: properties.lit set value false
                                        [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/1/0/1 run function block_property_accessor:1/0/1/1/0/0/1/0/1/
                                    [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/0/1/1 run function block_property_accessor:1/0/1/1/0/0/1/1/
                            [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/0/1 run function block_property_accessor:1/0/1/1/0/1/
                        [C] execute if block ~ ~ ~ #block_property_accessor:1/0/1/1/1 run function block_property_accessor:1/0/1/1/1/
            [C] execute if block ~ ~ ~ #block_property_accessor:1/1 run function block_property_accessor:1/1/
```

`[C]` (コマンドの実行) を数えると約25コマンドであり、一度の探索にかかる実行数は100にも満たないことがわかる

## 対応バージョン
- `1.21.11`

## ライセンス ／ License

[**MIT LICENSE**](/LICENSE)

## 連絡 ／ Contact

- Twitter(X): [@Takenoko_4096](https://x.com/Takenoko_4096)
