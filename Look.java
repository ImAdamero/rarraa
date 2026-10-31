package pl.easybot.bot;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;

class Look {
   private final double step;
   private float targetYaw;
   private float targetPitch;
   private float moveYaw;
   private float movePitch;
   private float bendYaw;
   private float bendPitch;
   private int length;
   private int done;
   private boolean active;
   private boolean correcting;
   private double carryX;
   private double carryY;
   private int tremor;

   Look() {
      double s = 0.38 + rnd().nextDouble() * 0.3;
      double f = s * 0.6 + 0.2;
      this.step = f * f * f * 8.0 * 0.15;
   }

   private static ThreadLocalRandom rnd() {
      return ThreadLocalRandom.current();
   }

   void stop() {
      this.active = false;
      this.correcting = false;
   }

   boolean busy() {
      return this.active;
   }

   float targetYaw() {
      return this.targetYaw;
   }

   void aim(Body b, float yaw, float pitch) {
      ThreadLocalRandom r = rnd();
      pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      this.targetYaw = yaw;
      this.targetPitch = pitch;
      this.correcting = false;
      float dy = MathHelper.wrapDegrees(yaw - b.yaw);
      float dp = pitch - b.pitch;
      double dist = Math.hypot(dy, dp);
      if (dist > 35.0 && r.nextInt(3) != 0) {
         float over = 1.03F + r.nextFloat() * 0.09F;
         dy *= over;
         dp *= over;
         this.correcting = true;
      }

      this.plan(dy, dp, (int)MathHelper.clamp(Math.round(1.5 + dist / 38.0 + r.nextDouble() * 2.0), 2L, 9L));
   }

   private void plan(float dy, float dp, int ticks) {
      ThreadLocalRandom r = rnd();
      this.moveYaw = dy;
      this.movePitch = dp;
      double dist = Math.hypot(dy, dp);
      double bend = (r.nextDouble() * 2.0 - 1.0) * Math.min(dist * 0.12, 9.0);
      this.bendYaw = dist == 0.0 ? 0.0F : (float)(-dp / dist * bend);
      this.bendPitch = dist == 0.0 ? 0.0F : (float)(dy / dist * bend);
      this.length = ticks;
      this.done = 0;
      this.active = true;
   }

   void tick(Body b) {
      ThreadLocalRandom r = rnd();
      double ax = 0.0;
      double ay = 0.0;
      if (this.active) {
         double p0 = ease((double)this.done / this.length);
         double p1 = ease((double)(++this.done) / this.length);
         double h0 = hump((double)(this.done - 1) / this.length);
         double h1 = hump((double)this.done / this.length);
         ax = this.moveYaw * (p1 - p0) + this.bendYaw * (h1 - h0);
         ay = this.movePitch * (p1 - p0) + this.bendPitch * (h1 - h0);
         ax *= 0.9 + r.nextDouble() * 0.2;
         ay *= 0.9 + r.nextDouble() * 0.2;
         if (this.done >= this.length) {
            this.active = false;
            if (this.correcting) {
               this.correcting = false;
               float dy = MathHelper.wrapDegrees(this.targetYaw - (float)(b.yaw + ax));
               float dp = this.targetPitch - (float)(b.pitch + ay);
               if (Math.abs(dy) > 0.5F || Math.abs(dp) > 0.5F) {
                  this.plan(dy, dp, r.nextInt(2, 5));
               }
            }
         }
      } else if (--this.tremor <= 0) {
         this.tremor = r.nextInt(3, 14);
         ax = r.nextInt(-2, 3) * this.step;
         ay = r.nextInt(-1, 2) * this.step;
      }

      this.carryX = this.carryX + ax / this.step;
      this.carryY = this.carryY + ay / this.step;
      long px = Math.round(this.carryX);
      long py = Math.round(this.carryY);
      this.carryX -= px;
      this.carryY -= py;
      if (px != 0L || py != 0L) {
         b.yaw = b.yaw + (float)(px * this.step);
         b.pitch = MathHelper.clamp(b.pitch + (float)(py * this.step), -90.0F, 90.0F);
      }
   }

   private static double ease(double t) {
      t = Math.min(1.0, t);
      return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
   }

   private static double hump(double t) {
      t = Math.min(1.0, t);
      return Math.sin(Math.PI * t);
   }
}
