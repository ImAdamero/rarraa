package pl.easybot.bot;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Formatting;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public final class AutoFlow {
   static final String CLOCK = "clock";
   static final String GRASS = "grass_block";
   static final String WARP_COMMAND = "/warp afk";
   static final long START_DELAY_MS = 2500L;
   static final long AFTER_GRASS_MS = 7000L;
   static final long JITTER_MS = 6000L;
   static final long COOLDOWN_MS = 60000L;
   private static final ScheduledExecutorService EX = Executors.newScheduledThreadPool(64, var0 -> {
      Thread var1 = new Thread(var0, "EasyBot-autoflow");
      var1.setDaemon(true);
      return var1;
   });
   private static final Map<Bot, Long> LAST = new WeakHashMap<>();

   private AutoFlow() {
   }

   public static void watch(Bot var0, Text var1) {
      var0.watch(var1);
      if (!Bot.autoAfk) {
         return;
      }

      try {
         String var2 = var1.getString();
         if (var2 == null) {
            return;
         }

         String var3 = Formatting.strip(var2);
         if (var3 == null) {
            return;
         }

         var3 = var3.toLowerCase(Locale.ROOT);
         boolean var4 = var3.contains("pomyślnie") || var3.contains("pomyslnie") || var3.contains("zalogowan") || var3.contains("zarejestrowan");
         if (!var4) {
            return;
         }

         long var5 = System.currentTimeMillis();
         synchronized (LAST) {
            Long var8 = LAST.get(var0);
            if (var8 != null && var5 - var8 < 60000L) {
               return;
            }

            LAST.put(var0, var5);
         }

         say(var0, "[Auto] Zalogowano - za chwile zegar -> trawa -> /warp afk", Formatting.field_1060);
         EX.schedule(() -> run(var0), 2500L + ThreadLocalRandom.current().nextLong(6000L), TimeUnit.MILLISECONDS);
      } catch (Throwable var11) {
      }
   }

   private static void run(Bot var0) {
      try {
         Bot.Inventory var1 = var0.inventory();
         boolean var2 = var1 != null && var1.window() != null && findInWindow(var1, "grass_block") >= 0;
         if (!var2) {
            boolean var3 = false;

            for (int var4 = 1; var4 <= 6 && !var3; var4++) {
               var1 = var0.inventory();
               if (var1 == null) {
                  sleep(1000L);
               } else {
                  int var5 = findHotbar(var1, "clock");
                  if (var5 < 0) {
                     say(var0, "[Auto] Brak zegara na pasku (proba " + var4 + ") - czekam...", Formatting.field_1065);
                     sleep(1500L);
                  } else {
                     var0.select(var5);
                     sleep(450L);
                     var0.useItem();
                     say(var0, "[Auto] PPM zegarem (slot paska " + var5 + ").", Formatting.field_1060);

                     for (int var6 = 0; var6 < 15; var6++) {
                        sleep(200L);
                        Bot.Inventory var7 = var0.inventory();
                        if (var7 != null && var7.window() != null) {
                           var3 = true;
                           break;
                        }
                     }
                  }
               }
            }

            if (!var3) {
               say(var0, "[Auto] GUI po zegarze sie nie otworzylo - przerywam.", Formatting.field_1065);
               return;
            }
         }

         int var11 = -1;

         for (int var12 = 0; var12 < 25 && var11 < 0; var12++) {
            Bot.Inventory var13 = var0.inventory();
            if (var13 != null && var13.window() != null) {
               var11 = findInWindow(var13, "grass_block");
            }

            if (var11 < 0) {
               sleep(200L);
            }
         }

         if (var11 < 0) {
            say(var0, "[Auto] Nie widze bloku trawy w GUI - przerywam.", Formatting.field_1065);
            return;
         }

         sleep(400L);
         var0.click(var11, 0, SlotActionType.PICKUP);
         say(var0, "[Auto] Klikam blok trawy (slot " + var11 + ").", Formatting.field_1060);
         sleep(7000L + ThreadLocalRandom.current().nextLong(6000L));
         var0.chat("/warp afk");
         say(var0, "[Auto] Wpisano /warp afk", Formatting.field_1060);
      } catch (InterruptedException var8) {
         Thread.currentThread().interrupt();
      } catch (Throwable var9) {
         say(var0, "[Auto] Blad: " + var9, Formatting.field_1065);
      }
   }

   private static int findHotbar(Bot.Inventory var0, String var1) {
      ItemStack[] var2 = var0.player();
      if (var2 == null) {
         return -1;
      }

      for (int var3 = 0; var3 < 9 && 36 + var3 < var2.length; var3++) {
         if (is(var2[36 + var3], var1)) {
            return var3;
         }
      }

      return -1;
   }

   private static int findInWindow(Bot.Inventory var0, String var1) {
      Bot.Window var2 = var0.window();
      if (var2 != null && var2.slots() != null) {
         ItemStack[] var3 = var2.slots();
         int var4 = var3.length >= 36 ? var3.length - 36 : var3.length;

         for (int var5 = 0; var5 < var4; var5++) {
            if (is(var3[var5], var1)) {
               return var5;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static boolean is(ItemStack var0, String var1) {
      if (var0 != null && !var0.isEmpty()) {
         Identifier var2 = Registries.ITEM.getId(var0.getItem());
         return var2 != null && var1.equals(var2.getPath());
      } else {
         return false;
      }
   }

   private static void say(Bot var0, String var1, Formatting var2) {
      try {
         var0.log(Text.literal(var1).formatted(var2));
      } catch (Throwable var4) {
      }
   }

   private static void sleep(long var0) throws InterruptedException {
      Thread.sleep(var0);
   }
}
