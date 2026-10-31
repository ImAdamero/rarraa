package pl.easybot.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import pl.easybot.EasyBotClient;

public final class ConsoleOpener {
   private static final long DOUBLE_MS = 350L;
   private static long lastTime = 0L;
   private static int lastIdx = -1;

   private ConsoleOpener() {
   }

   public static boolean headerClick(Screen var0, MinecraftClient var1, int var2) {
      try {
         long var3 = System.currentTimeMillis();
         boolean var5 = var2 == lastIdx && var3 - lastTime < 350L;
         if (!var5) {
            lastIdx = var2;
            lastTime = var3;
            return false;
         } else {
            lastIdx = -1;
            if (var1 != null && var2 >= 0 && var2 < EasyBotClient.nicks.length) {
               var1.setScreen(new BotConsoleScreen(var0, var2));
               return true;
            } else {
               return false;
            }
         }
      } catch (Throwable var6) {
         var6.printStackTrace();
         return false;
      }
   }
}
