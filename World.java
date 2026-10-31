package pl.easybot.bot;

import net.minecraft.world.chunk.PalettesFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Box;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.block.entity.SignText;
import net.minecraft.util.math.BlockPos.Mutable;
import org.jetbrains.annotations.Nullable;

class World implements BlockView {
   private final Map<Long, ChunkSection[]> chunks = new HashMap<>();
   private final Map<Long, Long> arrived = new HashMap<>();
   private final Map<BlockPos, World.Sign> signs = new HashMap<>();
   private final DynamicRegistryManager registries;
   private final Registry<Biome> biomes;
   private final PalettesFactory palettes;
   final int bottom;
   final int height;

   World(DynamicRegistryManager registries, int bottom, int height) {
      this.registries = registries;
      this.biomes = registries.getOrThrow(RegistryKeys.BIOME);
      this.palettes = PalettesFactory.fromRegistryManager(registries);
      this.bottom = bottom;
      this.height = height;
   }

   void load(int cx, int cz, PacketByteBuf data, long tick) {
      ChunkSection[] sections = new ChunkSection[this.height >> 4];

      for (int i = 0; i < sections.length; i++) {
         sections[i] = new ChunkSection(this.palettes);
         sections[i].readDataPacket(data);
      }

      long key = ChunkPos.toLong(cx, cz);
      this.chunks.put(key, sections);
      this.arrived.put(key, tick);
      this.signs.keySet().removeIf(p -> p.getX() >> 4 == cx && p.getZ() >> 4 == cz);
   }

   void unload(int cx, int cz) {
      long key = ChunkPos.toLong(cx, cz);
      this.chunks.remove(key);
      this.arrived.remove(key);
      this.signs.keySet().removeIf(p -> p.getX() >> 4 == cx && p.getZ() >> 4 == cz);
   }

   boolean loaded(int bx, int bz) {
      return this.chunks.containsKey(ChunkPos.toLong(bx >> 4, bz >> 4));
   }

   long arrivedAt(int bx, int bz) {
      return this.arrived.getOrDefault(ChunkPos.toLong(bx >> 4, bz >> 4), Long.MAX_VALUE);
   }

   void set(BlockPos pos, BlockState state) {
      ChunkSection[] sections = this.chunks.get(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
      int i = pos.getY() - this.bottom >> 4;
      if (sections != null && i >= 0 && i < sections.length) {
         sections[i].setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state, false);
         if (!(state.getBlock() instanceof AbstractSignBlock)) {
            this.signs.remove(pos);
         }
      }
   }

   void blockEntity(BlockPos pos, BlockEntityType<?> type, @Nullable NbtCompound nbt) {
      if (type == BlockEntityType.field_11911 || type == BlockEntityType.field_40330) {
         if (nbt != null && !nbt.isEmpty()) {
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, this.registries);
            SignText front = nbt.contains("front_text")
               ? SignText.CODEC.parse(ops, nbt.get("front_text")).result().orElse(new SignText())
               : new SignText();
            SignText back = nbt.contains("back_text")
               ? SignText.CODEC.parse(ops, nbt.get("back_text")).result().orElse(new SignText())
               : new SignText();
            this.signs.put(pos.toImmutable(), new World.Sign(front, back));
         }
      }
   }

   @Nullable
   World.Sign sign(BlockPos pos) {
      return this.signs.get(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      ChunkSection[] sections = this.chunks.get(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
      int i = pos.getY() - this.bottom >> 4;
      return sections != null && i >= 0 && i < sections.length
         ? sections[i].getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15)
         : Blocks.field_10124.getDefaultState();
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return null;
   }

   public int getHeight() {
      return this.height;
   }

   public int getBottomY() {
      return this.bottom;
   }

   List<VoxelShape> collisions(Box box) {
      List<VoxelShape> list = new ArrayList<>();
      int x0 = MathHelper.floor(box.minX - 1.0E-7) - 1;
      int x1 = MathHelper.floor(box.maxX + 1.0E-7) + 1;
      int y0 = MathHelper.floor(box.minY - 1.0E-7) - 1;
      int y1 = MathHelper.floor(box.maxY + 1.0E-7) + 1;
      int z0 = MathHelper.floor(box.minZ - 1.0E-7) - 1;
      int z1 = MathHelper.floor(box.maxZ + 1.0E-7) + 1;
      Mutable pos = new Mutable();

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            for (int y = y0; y <= y1; y++) {
               int edges = (x != x0 && x != x1 ? 0 : 1) + (y != y0 && y != y1 ? 0 : 1) + (z != z0 && z != z1 ? 0 : 1);
               if (edges != 3) {
                  BlockState state = this.getBlockState(pos.set(x, y, z));
                  if (!state.isAir() && (edges != 1 || state.exceedsCube()) && (edges != 2 || state.isOf(Blocks.field_10008))) {
                     VoxelShape shape = state.getCollisionShape(this, pos);
                     if (!shape.isEmpty()) {
                        VoxelShape moved = shape.offset(x, y, z);
                        if (moved.getBoundingBox().intersects(box)) {
                           list.add(moved);
                        }
                     }
                  }
               }
            }
         }
      }

      return list;
   }

   static BlockState state(int id) {
      BlockState s = (BlockState)Block.STATE_IDS.get(id);
      return s == null ? Blocks.field_10124.getDefaultState() : s;
   }

   record Sign(SignText front, SignText back) {
   }
}
