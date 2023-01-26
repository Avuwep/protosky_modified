package protosky.stuctures;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FeatureHelper extends net.minecraft.world.gen.chunk.ChunkGenerator {


    public FeatureHelper(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return null;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 0;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return null;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 0;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return null;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        ChunkPos chunkPos = chunk.getPos();
        if (!SharedConstants.isOutsideGenerationArea(chunkPos)) {
            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos, world.getBottomSectionCoord());
            BlockPos blockPos = chunkSectionPos.getMinPos();
            Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            Map<Integer, List<Structure>> map = (Map)registry.stream().collect(Collectors.groupingBy((structureType) -> {
                return structureType.getFeatureGenerationStep().ordinal();
            }));
            List<PlacedFeatureIndexer.IndexedFeatures> list = (List)this.indexedFeaturesListSupplier.get();
            ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
            long l = chunkRandom.setPopulationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
            Set<RegistryEntry<Biome>> set = new ObjectArraySet();
            ChunkPos.stream(chunkSectionPos.toChunkPos(), 1).forEach((chunkPosx) -> {
                Chunk chunk1 = world.getChunk(chunkPosx.x, chunkPosx.z);
                ChunkSection[] var4 = chunk1.getSectionArray();
                int var5 = var4.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    ChunkSection chunkSection = var4[var6];
                    ReadableContainer var10000 = chunkSection.getBiomeContainer();
                    Objects.requireNonNull(set);
                    var10000.forEachValue(set::add);
                }

            });
            set.retainAll(this.biomeSource.getBiomes());
            int i = list.size();

            try {
                Registry<PlacedFeature> registry2 = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);
                int j = Math.max(GenerationStep.Feature.values().length, i);

                for(int k = 0; k < j; ++k) {
                    int m = 0;
                    CrashReportSection var10000;
                    Iterator var20;
                    if (structureAccessor.shouldGenerateStructures()) {
                        List<Structure> list2 = (List)map.getOrDefault(k, Collections.emptyList());

                        for(var20 = list2.iterator(); var20.hasNext(); ++m) {
                            Structure structure = (Structure)var20.next();
                            chunkRandom.setDecoratorSeed(l, m, k);
                            Supplier<String> supplier = () -> {
                                Optional var10000 = registry.getKey(structure).map(Object::toString);
                                Objects.requireNonNull(structure);
                                return (String)var10000.orElseGet(structure::toString);
                            };

                            try {
                                world.setCurrentlyGeneratingStructureName(supplier);
                                structureAccessor.getStructureStarts(chunkSectionPos, structure).forEach((start) -> {
                                    start.place(world, structureAccessor, this, chunkRandom, getBlockBoxForChunk(chunk), chunkPos);
                                });
                            } catch (Exception var29) {
                                CrashReport crashReport = CrashReport.create(var29, "Feature placement");
                                var10000 = crashReport.addElement("Feature");
                                Objects.requireNonNull(supplier);
                                var10000.add("Description", supplier::get);
                                throw new CrashException(crashReport);
                            }
                        }
                    }

                    if (k < i) {
                        IntSet intSet = new IntArraySet();
                        var20 = set.iterator();

                        while(var20.hasNext()) {
                            RegistryEntry<Biome> registryEntry = (RegistryEntry)var20.next();
                            List<RegistryEntryList<PlacedFeature>> list3 = ((GenerationSettings)this.generationSettingsGetter.apply(registryEntry)).getFeatures();
                            if (k < list3.size()) {
                                RegistryEntryList<PlacedFeature> registryEntryList = (RegistryEntryList)list3.get(k);
                                PlacedFeatureIndexer.IndexedFeatures indexedFeatures = (PlacedFeatureIndexer.IndexedFeatures)list.get(k);
                                registryEntryList.stream().map(RegistryEntry::value).forEach((placedFeaturex) -> {
                                    intSet.add(indexedFeatures.indexMapping().applyAsInt(placedFeaturex));
                                });
                            }
                        }

                        int n = intSet.size();
                        int[] is = intSet.toIntArray();
                        Arrays.sort(is);
                        PlacedFeatureIndexer.IndexedFeatures indexedFeatures2 = (PlacedFeatureIndexer.IndexedFeatures)list.get(k);

                        for(int o = 0; o < n; ++o) {
                            int p = is[o];
                            PlacedFeature placedFeature = (PlacedFeature)indexedFeatures2.features().get(p);
                            Supplier<String> supplier2 = () -> {
                                Optional var10000 = registry2.getKey(placedFeature).map(Object::toString);
                                Objects.requireNonNull(placedFeature);
                                return (String)var10000.orElseGet(placedFeature::toString);
                            };
                            chunkRandom.setDecoratorSeed(l, p, k);

                            try {
                                world.setCurrentlyGeneratingStructureName(supplier2);
                                placedFeature.generate(world, this, chunkRandom, blockPos);
                            } catch (Exception var30) {
                                CrashReport crashReport2 = CrashReport.create(var30, "Feature placement");
                                var10000 = crashReport2.addElement("Feature");
                                Objects.requireNonNull(supplier2);
                                var10000.add("Description", supplier2::get);
                                throw new CrashException(crashReport2);
                            }
                        }
                    }
                }

                world.setCurrentlyGeneratingStructureName((Supplier)null);
            } catch (Exception var31) {
                CrashReport crashReport3 = CrashReport.create(var31, "Biome decoration");
                crashReport3.addElement("Generation").add("CenterX", chunkPos.x).add("CenterZ", chunkPos.z).add("Seed", l);
                throw new CrashException(crashReport3);
            }
        }
    }
}
