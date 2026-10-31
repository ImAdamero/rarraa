package pl.easybot.bot;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction.Axis;

class Wander {
   private static final double LEASH = 20.0;
   private final Look look = new Look();
   private boolean walking;
   private int timer;
   private float walkYaw;
   private float drift;
   private int lookTimer;
   private int glance;
   private int stuck;
   private int jumpHold;
   private boolean run;
   private boolean hop;
   private int strafe;
   private int strafeDir;
   private boolean anchored;
   private double homeX;
   private double homeZ;

   void reset() {
      this.walking = false;
      this.timer = 20;
      this.anchored = false;
      this.stuck = 0;
      this.jumpHold = 0;
      this.look.stop();
   }

   void interrupt(Body b) {
      this.walking = false;
      this.timer = 20 + rnd().nextInt(40);
      this.look.stop();
      release(b);
   }

   static void release(Body b) {
      b.forward = 0.0F;
      b.sideways = 0.0F;
      b.jumping = false;
      b.sprinting = false;
   }

   private static ThreadLocalRandom rnd() {
      return ThreadLocalRandom.current();
   }

   void tick(Body b, World w) {
      ThreadLocalRandom r = rnd();
      if (!this.anchored) {
         this.homeX = b.x;
         this.homeZ = b.z;
         this.anchored = true;
      }

      if (b.flying) {
         this.walking = false;
         release(b);
      }

      if (!this.walking) {
         release(b);
         if (--this.lookTimer <= 0) {
            boolean flick = r.nextInt(3) == 0;
            float yaw = b.yaw + (r.nextBoolean() ? 1 : -1) * (flick ? 80.0F + r.nextFloat() * 100.0F : 10.0F + r.nextFloat() * 50.0F);
            this.look.aim(b, yaw, MathHelper.clamp(b.pitch + (r.nextFloat() * 2.0F - 1.0F) * (flick ? 35 : 20), -45.0F, 40.0F));
            this.lookTimer = flick ? r.nextInt(12, 35) : r.nextInt(6, 22);
         }

         this.look.tick(b);
         if (--this.timer <= 0 && !b.flying && !this.look.busy()) {
            this.start(b, w);
         }
      } else {
         this.walkYaw = this.walkYaw + this.drift;
         this.drift = MathHelper.clamp(this.drift + (r.nextFloat() * 2.0F - 1.0F) * 0.7F, -3.5F, 3.5F);
         if (this.glance > 0) {
            if (--this.glance == 0) {
               this.look.aim(b, this.walkYaw + (r.nextFloat() * 2.0F - 1.0F) * 4.0F, 4.0F + r.nextFloat() * 16.0F);
            }
         } else if (--this.lookTimer <= 0) {
            if (r.nextInt(4) == 0) {
               this.look.aim(b, this.walkYaw + (r.nextBoolean() ? 1 : -1) * (30.0F + r.nextFloat() * 45.0F), r.nextFloat() * 36.0F - 18.0F);
               this.glance = r.nextInt(5, 13);
            } else {
               this.look.aim(b, this.walkYaw + (r.nextFloat() * 2.0F - 1.0F) * 6.0F, 4.0F + r.nextFloat() * 18.0F);
            }

            this.lookTimer = r.nextInt(10, 30);
         } else if (Math.abs(MathHelper.wrapDegrees(this.walkYaw - this.look.targetYaw())) > 12.0F && !this.look.busy()) {
            this.look.aim(b, this.walkYaw + (r.nextFloat() * 2.0F - 1.0F) * 3.0F, b.pitch);
         }

         this.look.tick(b);
         float off = Math.abs(MathHelper.wrapDegrees(this.walkYaw - b.yaw));
         b.forward = off < 80.0F ? 1.0F : 0.0F;
         if (this.strafe > 0) {
            this.strafe--;
            b.sideways = this.strafeDir;
         } else {
            b.sideways = 0.0F;
            if (r.nextInt(70) == 0) {
               this.strafe = r.nextInt(4, 14);
               this.strafeDir = r.nextBoolean() ? 1 : -1;
            }
         }

         if (b.forward > 0.0F && danger(w, b, moveYaw(b), fast(b))) {
            if (!this.turnAway(b, w)) {
               this.pause(b);
               return;
            }

            b.forward = 0.0F;
            b.sideways = 0.0F;
            b.sprinting = false;
         }

         boolean canSprint = b.food > 6 && !b.inWater() && !b.horizontalCollision && b.forward > 0.0F && off < 30.0F;
         if (!canSprint) {
            b.sprinting = false;
         } else if (this.run && b.onGround) {
            b.sprinting = true;
         }

         if (b.inWater()) {
            b.jumping = true;
         } else if (!b.horizontalCollision || !b.onGround || !(b.forward > 0.0F)) {
            this.stuck = Math.max(0, this.stuck - 1);
         } else if (jumpable(w, b, b.yaw)) {
            this.jumpHold = 4;
         } else if (++this.stuck > 10) {
            this.stuck = 0;
            if (!this.turnAway(b, w)) {
               this.pause(b);
               return;
            }
         }

         if (this.jumpHold > 0) {
            this.jumpHold--;
            b.jumping = true;
         } else if (!b.inWater()) {
            b.jumping = this.hop && b.sprinting && off < 20.0F && !danger(w, b, moveYaw(b), true);
            if (!this.hop && b.sprinting && b.onGround && r.nextInt(90) == 0) {
               this.jumpHold = 2;
            }
         }

         if (--this.timer <= 0) {
            this.pause(b);
         }
      }
   }

   private boolean turnAway(Body b, World w) {
      ThreadLocalRandom r = rnd();
      int side = r.nextBoolean() ? 1 : -1;

      for (int a = 35; a <= 180; a += 25) {
         for (int s = 0; s < 2; s++) {
            float yaw = this.walkYaw + a * (s == 0 ? side : -side) + (r.nextFloat() * 2.0F - 1.0F) * 8.0F;
            if (clear(w, b, yaw)) {
               this.walkYaw = yaw;
               this.drift = 0.0F;
               this.glance = 0;
               this.look.aim(b, yaw, 4.0F + r.nextFloat() * 16.0F);
               this.lookTimer = r.nextInt(12, 30);
               return true;
            }
         }
      }

      return false;
   }

   private static float moveYaw(Body b) {
      return b.sideways == 0.0F ? b.yaw : b.yaw - (float)Math.toDegrees(Math.atan2(b.sideways, Math.max(b.forward, 0.01F)));
   }

   private static boolean fast(Body b) {
      return b.sprinting || b.vx * b.vx + b.vz * b.vz > 0.04;
   }

   private void pause(Body b) {
      this.walking = false;
      release(b);
      this.timer = rnd().nextInt(10, 60);
      this.lookTimer = rnd().nextInt(3, 12);
   }

   private void start(Body b, World w) {
      ThreadLocalRandom r = rnd();
      double dist = Math.hypot(b.x - this.homeX, b.z - this.homeZ);
      float home = (float)Math.toDegrees(Math.atan2(-(this.homeX - b.x), this.homeZ - b.z));

      for (int i = 0; i < 12; i++) {
         float yaw = dist > 20.0 ? home + (r.nextFloat() * 2.0F - 1.0F) * 50.0F : b.yaw + (r.nextFloat() * 2.0F - 1.0F) * 180.0F;
         if (clear(w, b, yaw)) {
            this.walking = true;
            this.walkYaw = yaw;
            this.drift = 0.0F;
            this.glance = 0;
            this.run = r.nextInt(5) != 0;
            this.hop = this.run && r.nextInt(3) == 0;
            this.strafe = 0;
            this.look.aim(b, yaw, 4.0F + r.nextFloat() * 18.0F);
            this.lookTimer = r.nextInt(12, 30);
            this.timer = r.nextInt(40, 160);
            this.stuck = 0;
            return;
         }
      }

      this.timer = r.nextInt(10, 40);
   }

   private static boolean clear(World w, Body b, float yaw) {
      for (double d = 1.0; d <= 5.0; d++) {
         if (danger(w, b.x, b.y, b.z, yaw, d)) {
            return false;
         }
      }

      return true;
   }

   private static boolean danger(World w, Body b, float yaw, boolean fast) {
      return !danger(w, b.x, b.y, b.z, yaw, 0.9) && !danger(w, b.x, b.y, b.z, yaw, 1.6)
         ? fast && (danger(w, b.x, b.y, b.z, yaw, 2.4) || danger(w, b.x, b.y, b.z, yaw, 3.3))
         : true;
   }

   private static boolean danger(World w, double x, double y, double z, float yaw, double dist) {
      double rad = Math.toRadians(yaw);
      double px = x - Math.sin(rad) * dist;
      double pz = z + Math.cos(rad) * dist;
      int bx = MathHelper.floor(px);
      int bz = MathHelper.floor(pz);
      int by = MathHelper.floor(y + 0.01);
      if (!w.loaded(bx, bz)) {
         return true;
      }

      for (int dy = -1; dy <= 2; dy++) {
         if (bad(w.getBlockState(new BlockPos(bx, by + dy, bz)))) {
            return true;
         }
      }

      if (solid(w, new BlockPos(bx, by, bz))) {
         return false;
      }

      for (int dy = 1; dy <= 4; dy++) {
         BlockPos p = new BlockPos(bx, by - dy, bz);
         BlockState s = w.getBlockState(p);
         if (bad(s)) {
            return true;
         }

         if (solid(w, p) || s.isOf(Blocks.field_9983)) {
            return false;
         }
      }

      return true;
   }

   private static boolean bad(BlockState s) {
      return !s.getFluidState().isEmpty()
         || s.isOf(Blocks.field_10036)
         || s.isOf(Blocks.field_22089)
         || s.isOf(Blocks.field_10092)
         || s.isOf(Blocks.field_10029)
         || s.isOf(Blocks.field_17350)
         || s.isOf(Blocks.field_23860)
         || s.isOf(Blocks.field_16999)
         || s.isOf(Blocks.field_27879)
         || s.isOf(Blocks.field_10606)
         || s.isOf(Blocks.field_10343)
         || s.isOf(Blocks.field_10316)
         || s.isOf(Blocks.field_10027)
         || s.isOf(Blocks.field_10613)
         || s.isOf(Blocks.field_28048)
         || s.isIn(BlockTags.field_21952);
   }

   private static boolean solid(World w, BlockPos p) {
      VoxelShape shape = w.getBlockState(p).getCollisionShape(w, p);
      return !shape.isEmpty();
   }

   private static boolean jumpable(World w, Body b, float yaw) {
      double rad = Math.toRadians(yaw);
      double px = b.x - Math.sin(rad) * 0.8;
      double pz = b.z + Math.cos(rad) * 0.8;
      int bx = MathHelper.floor(px);
      int bz = MathHelper.floor(pz);
      int by = MathHelper.floor(b.y + 0.01);
      BlockPos feet = new BlockPos(bx, by, bz);
      VoxelShape shape = w.getBlockState(feet).getCollisionShape(w, feet);
      if (shape.isEmpty() || shape.getMax(Axis.field_11052) > 1.0) {
         return false;
      } else if (!solid(w, feet.up()) && !solid(w, feet.up(2))) {
         BlockPos head = BlockPos.ofFloored(b.x, b.y + 2.1, b.z);
         return !solid(w, head);
      } else {
         return false;
      }
   }
}
