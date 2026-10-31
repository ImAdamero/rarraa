package pl.easybot.bot;

import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.block.LadderBlock;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;

class Body {
   double x;
   double y;
   double z;
   double vx;
   double vy;
   double vz;
   float yaw;
   float pitch;
   boolean onGround;
   boolean horizontalCollision;
   boolean flying;
   float flySpeed = 0.05F;
   float forward;
   float sideways;
   boolean jumping;
   boolean sprinting;
   int food = 20;
   double speed = 0.1;
   double jumpStrength = 0.42;
   double stepHeight = 0.6;
   double gravity = 0.08;
   private int jumpCooldown;
   private double waterHeight;
   private double lavaHeight;
   private boolean inWater;
   private boolean inLava;

   Box box() {
      return new Box(this.x - 0.3, this.y, this.z - 0.3, this.x + 0.3, this.y + 1.8, this.z + 0.3);
   }

   void push(double ox, double oz) {
      double d = this.x - ox;
      double e = this.z - oz;
      double f = MathHelper.absMax(d, e);
      if (!(f < 0.01F)) {
         f = Math.sqrt(f);
         d /= f;
         e /= f;
         double g = Math.min(1.0, 1.0 / f);
         this.vx += d * g * 0.05F;
         this.vz += e * g * 0.05F;
      }
   }

   boolean climbing(World world) {
      BlockPos pos = BlockPos.ofFloored(this.x, this.y, this.z);
      BlockState s = world.getBlockState(pos);
      if (s.isIn(BlockTags.field_22414)
         || s.getBlock() instanceof LadderBlock
         || s.getBlock() instanceof VineBlock
         || s.getBlock() instanceof ScaffoldingBlock) {
         return true;
      } else if (s.getBlock() instanceof TrapdoorBlock && (Boolean)s.get(TrapdoorBlock.OPEN)) {
         BlockState below = world.getBlockState(pos.method_10074());
         return below.isOf(Blocks.field_9983) && below.get(LadderBlock.FACING) == s.get(TrapdoorBlock.FACING);
      } else {
         return false;
      }
   }

   void tick(World world) {
      if (this.jumpCooldown > 0) {
         this.jumpCooldown--;
      }

      if (Math.abs(this.vx) < 0.003) {
         this.vx = 0.0;
      }

      if (Math.abs(this.vy) < 0.003) {
         this.vy = 0.0;
      }

      if (Math.abs(this.vz) < 0.003) {
         this.vz = 0.0;
      }

      this.fluids(world);
      if (this.jumping && !this.flying) {
         double g = this.inLava ? this.lavaHeight : this.waterHeight;
         boolean swim = this.inWater && g > 0.0;
         double h = 0.4;
         if (!swim || this.onGround && !(g > h)) {
            if (!this.inLava || this.onGround && !(g > h)) {
               if ((this.onGround || swim && g <= h) && this.jumpCooldown == 0) {
                  this.jump(world);
                  this.jumpCooldown = 10;
               }
            } else {
               this.vy += 0.04F;
            }
         } else {
            this.vy += 0.04F;
         }
      } else {
         this.jumpCooldown = 0;
      }

      if (this.flying && this.jumping) {
         this.vy = this.vy + this.flySpeed * 3.0F;
      }

      Vec3d input = new Vec3d(this.sideways * 0.98F, 0.0, this.forward * 0.98F);
      double startVy = this.vy;
      if ((this.inWater || this.inLava) && !this.flying) {
         this.travelFluid(world, input);
      } else {
         this.travelAir(world, input);
      }

      if (this.flying) {
         this.vy = startVy * 0.6;
      }
   }

   private void jump(World world) {
      float f1 = world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z)).getBlock().getJumpVelocityMultiplier();
      float f2 = world.getBlockState(this.below()).getBlock().getJumpVelocityMultiplier();
      double v = this.jumpStrength * (f1 == 1.0F ? f2 : f1);
      if (!(v <= 1.0E-5)) {
         this.vy = Math.max(v, this.vy);
         if (this.sprinting) {
            float g = this.yaw * (float) (Math.PI / 180.0);
            this.vx = this.vx + -MathHelper.sin(g) * 0.2;
            this.vz = this.vz + MathHelper.cos(g) * 0.2;
         }
      }
   }

   private BlockPos below() {
      return BlockPos.ofFloored(this.x, this.y - 0.5000001, this.z);
   }

   private void travelAir(World world, Vec3d input) {
      float slip = this.onGround ? world.getBlockState(this.below()).getBlock().getSlipperiness() : 1.0F;
      float friction = slip * 0.91F;
      float moveSpeed = this.onGround
         ? (float)(this.sprinting ? this.speed * 1.3 : this.speed) * (0.21600002F / (slip * slip * slip))
         : (this.flying ? (this.sprinting ? this.flySpeed * 2.0F : this.flySpeed) : (this.sprinting ? 0.025999999F : 0.02F));
      this.accelerate(moveSpeed, input);
      boolean climb = this.climbing(world);
      if (climb) {
         this.vx = MathHelper.clamp(this.vx, -0.15F, 0.15F);
         this.vz = MathHelper.clamp(this.vz, -0.15F, 0.15F);
         this.vy = Math.max(this.vy, -0.15F);
      }

      this.move(world);
      if ((this.horizontalCollision || this.jumping) && this.climbing(world)) {
         this.vy = 0.2;
      }

      double d = this.vy;
      if (world.loaded(MathHelper.floor(this.x), MathHelper.floor(this.z))) {
         d -= this.gravity;
      } else {
         d = this.y > world.bottom ? -0.1 : 0.0;
      }

      this.vx *= friction;
      this.vy = d * 0.98F;
      this.vz *= friction;
   }

   private void travelFluid(World world, Vec3d input) {
      boolean falling = this.vy <= 0.0;
      double startY = this.y;
      if (this.inWater) {
         this.accelerate(0.02F, input);
         this.move(world);
         if (this.horizontalCollision && this.climbing(world)) {
            this.vy = 0.2;
         }

         this.vx *= 0.8F;
         this.vy *= 0.8F;
         this.vz *= 0.8F;
         this.vy = this.fluidFall(this.vy, falling);
      } else {
         this.accelerate(0.02F, input);
         this.move(world);
         if (this.lavaHeight <= 0.4) {
            this.vx *= 0.5;
            this.vy *= 0.8F;
            this.vz *= 0.5;
            this.vy = this.fluidFall(this.vy, falling);
         } else {
            this.vx *= 0.5;
            this.vy *= 0.5;
            this.vz *= 0.5;
         }

         this.vy = this.vy - this.gravity / 4.0;
      }

      if (this.horizontalCollision) {
         Box moved = this.box().offset(this.vx, this.vy + 0.6F - this.y + startY, this.vz);
         if (world.collisions(moved).isEmpty() && !fluidIn(world, moved)) {
            this.vy = 0.3F;
         }
      }
   }

   private double fluidFall(double v, boolean falling) {
      if (this.gravity == 0.0) {
         return v;
      } else {
         return falling && Math.abs(v - 0.005) >= 0.003 && Math.abs(v - this.gravity / 16.0) < 0.003 ? -0.003 : v - this.gravity / 16.0;
      }
   }

   private void accelerate(float moveSpeed, Vec3d input) {
      double len = input.lengthSquared();
      if (!(len < 1.0E-7)) {
         Vec3d v = (len > 1.0 ? input.normalize() : input).multiply(moveSpeed);
         float sin = MathHelper.sin(this.yaw * (float) (Math.PI / 180.0));
         float cos = MathHelper.cos(this.yaw * (float) (Math.PI / 180.0));
         this.vx = this.vx + (v.x * cos - v.z * sin);
         this.vz = this.vz + (v.z * cos + v.x * sin);
      }
   }

   private void move(World world) {
      double mx = this.vx;
      double my = this.vy;
      double mz = this.vz;
      BlockState inside = world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z));
      if (inside.isOf(Blocks.field_10343)) {
         mx *= 0.25;
         my *= 0.05F;
         mz *= 0.25;
         this.vx = this.vy = this.vz = 0.0;
      } else if (inside.isOf(Blocks.field_16999)) {
         mx *= 0.8F;
         my *= 0.75;
         mz *= 0.8F;
         this.vx = this.vy = this.vz = 0.0;
      }

      Vec3d moved = this.collide(world, new Vec3d(mx, my, mz));
      this.x = this.x + moved.x;
      this.y = this.y + moved.y;
      this.z = this.z + moved.z;
      boolean hx = !MathHelper.approximatelyEquals(mx, moved.x);
      boolean hz = !MathHelper.approximatelyEquals(mz, moved.z);
      this.horizontalCollision = hx || hz;
      boolean vertical = my != moved.y;
      this.onGround = vertical && my < 0.0;
      if (hx) {
         this.vx = 0.0;
      }

      if (hz) {
         this.vz = 0.0;
      }

      if (vertical) {
         this.vy = 0.0;
      }

      BlockState at = world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z));
      float m = at.getBlock().getVelocityMultiplier();
      if (!at.isOf(Blocks.field_10382) && !at.isOf(Blocks.field_10422) && m == 1.0F) {
         m = world.getBlockState(this.below()).getBlock().getVelocityMultiplier();
      }

      this.vx *= m;
      this.vz *= m;
   }

   private Vec3d collide(World world, Vec3d movement) {
      Box box = this.box();
      List<VoxelShape> shapes = world.collisions(box.stretch(movement));
      Vec3d result = movement.lengthSquared() == 0.0 ? movement : adjust(movement, box, shapes);
      boolean bx = movement.x != result.x;
      boolean by = movement.y != result.y;
      boolean bz = movement.z != result.z;
      boolean landed = by && movement.y < 0.0;
      if (this.stepHeight > 0.0 && (landed || this.onGround) && (bx || bz)) {
         Box base = landed ? box.offset(0.0, result.y, 0.0) : box;
         Box reach = base.stretch(movement.x, this.stepHeight, movement.z);
         if (!landed) {
            reach = reach.stretch(0.0, -1.0E-5F, 0.0);
         }

         List<VoxelShape> around = world.collisions(reach);
         float[] heights = stepHeights(base, around, (float)this.stepHeight, (float)result.y);

         for (float g : heights) {
            Vec3d step = adjust(new Vec3d(movement.x, g, movement.z), base, around);
            if (step.horizontalLengthSquared() > result.horizontalLengthSquared()) {
               return step.add(0.0, -(box.minY - base.minY), 0.0);
            }
         }
      }

      return result;
   }

   private static float[] stepHeights(Box box, List<VoxelShape> shapes, float max, float current) {
      FloatSet set = new FloatArraySet(4);

      for (VoxelShape shape : shapes) {
         DoubleListIterator var7 = shape.getPointPositions(Axis.field_11052).iterator();

         while (var7.hasNext()) {
            double d = (Double)var7.next();
            float g = (float)(d - box.minY);
            if (!(g < 0.0F) && g != current) {
               if (g > max) {
                  break;
               }

               set.add(g);
            }
         }
      }

      float[] a = set.toFloatArray();
      FloatArrays.unstableSort(a);
      return a;
   }

   private static Vec3d adjust(Vec3d m, Box box, List<VoxelShape> shapes) {
      double d = m.x;
      double e = m.y;
      double f = m.z;
      if (shapes.isEmpty()) {
         return m;
      }

      if (e != 0.0) {
         e = VoxelShapes.calculateMaxOffset(Axis.field_11052, box, shapes, e);
         if (e != 0.0) {
            box = box.offset(0.0, e, 0.0);
         }
      }

      boolean zFirst = Math.abs(d) < Math.abs(f);
      if (zFirst && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(Axis.field_11051, box, shapes, f);
         if (f != 0.0) {
            box = box.offset(0.0, 0.0, f);
         }
      }

      if (d != 0.0) {
         d = VoxelShapes.calculateMaxOffset(Axis.field_11048, box, shapes, d);
         if (!zFirst && d != 0.0) {
            box = box.offset(d, 0.0, 0.0);
         }
      }

      if (!zFirst && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(Axis.field_11051, box, shapes, f);
      }

      return new Vec3d(d, e, f);
   }

   private void fluids(World world) {
      Box b = this.box().contract(0.001);
      this.waterHeight = this.lavaHeight = 0.0;
      this.inWater = this.inLava = false;
      Mutable pos = new Mutable();

      for (int bx = MathHelper.floor(b.minX); bx < MathHelper.ceil(b.maxX); bx++) {
         for (int by = MathHelper.floor(b.minY); by < MathHelper.ceil(b.maxY); by++) {
            for (int bz = MathHelper.floor(b.minZ); bz < MathHelper.ceil(b.maxZ); bz++) {
               FluidState fluid = world.getFluidState(pos.set(bx, by, bz));
               if (!fluid.isEmpty()) {
                  double top = by + fluid.getHeight(world, pos);
                  if (!(top < b.minY)) {
                     if (fluid.getFluid().matchesType(Fluids.WATER)) {
                        this.inWater = true;
                        this.waterHeight = Math.max(this.waterHeight, top - b.minY);
                     } else {
                        this.inLava = true;
                        this.lavaHeight = Math.max(this.lavaHeight, top - b.minY);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean fluidIn(World world, Box b) {
      Mutable pos = new Mutable();

      for (int bx = MathHelper.floor(b.minX); bx < MathHelper.ceil(b.maxX); bx++) {
         for (int by = MathHelper.floor(b.minY); by < MathHelper.ceil(b.maxY); by++) {
            for (int bz = MathHelper.floor(b.minZ); bz < MathHelper.ceil(b.maxZ); bz++) {
               if (!world.getFluidState(pos.set(bx, by, bz)).isEmpty()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   boolean inWater() {
      return this.inWater;
   }
}
