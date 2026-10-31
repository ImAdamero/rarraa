package pl.easybot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.AfterKeyPress;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.TextColor;
import net.minecraft.client.util.InputUtil.Type;
import pl.easybot.bot.Bot;

public final class ErrorsClient implements ClientModInitializer {
   private static volatile boolean on;
   private static Screen mine;
   private static KeyBinding key;
   private static final String[] PREFIXES = new String[]{
      "rozłączono:",
      "nie można",
      "nie znaleziono",
      "nie wysłano",
      "nie odczytano",
      "nie udało",
      "wyrzucono",
      "serwer wymaga",
      "bot zginął",
      "brak połączenia",
      "uszkodzony",
      "za długi"
   };

   public void onInitializeClient() {
      key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.easybot.errors", Type.field_1668, -1, EasyBotClient.CATEGORY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)var0 -> {
         while (key.wasPressed()) {
            if (var0.currentScreen == null) {
               open(var0, null);
            }
         }

         on = mine != null && var0.currentScreen == mine;
      });
      ScreenEvents.AFTER_INIT.register((AfterInit)(var0, var1, var2, var3) -> {
         if (var1 instanceof TitleScreen || var1 instanceof MultiplayerScreen) {
            ScreenKeyboardEvents.afterKeyPress(var1).register((AfterKeyPress)(var1x, keyInput) -> {
               if (key.matchesKey(keyInput)) {
                  open(var0, var1x);
               }
            });
         }
      });
      ErrorsConsoleScreen.started();
   }

   private static void open(MinecraftClient var0, Screen var1) {
      ErrorsConsoleScreen.show(var0, var1);
   }

   public static List<Bot.Line> lines(Bot var0) {
      List var1 = var0.lines();
      if (!on) {
         return var1;
      }

      Object[] var2;
      synchronized (var1) {
         var2 = var1.toArray();
      }

      ArrayList var3 = new ArrayList();

      for (Object var7 : var2) {
         Bot.Line var8 = (Bot.Line)var7;

         try {
            if (isError(var8.text())) {
               var3.add(var8);
            }
         } catch (Throwable var10) {
         }
      }

      return var3;
   }

   static boolean isError(Text var0) {
      String var1 = var0.getString();
      if (var1 == null) {
         return false;
      }

      var1 = var1.strip();
      if (var1.isEmpty()) {
         return false;
      }

      String var2 = var1.toLowerCase(Locale.ROOT);

      for (String var6 : PREFIXES) {
         if (var2.startsWith(var6)) {
            return true;
         }
      }

      if (!var2.startsWith("<") && !var2.contains(" szepcze")) {
         try {
            return redOrGold(var0, 0);
         } catch (Throwable var7) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean redOrGold(Text var0, int var1) {
      TextColor var2 = var0.getStyle().getColor();
      if (var2 != null) {
         int var3 = var2.getRgb() & 16777215;
         if (var3 == 16733525 || var3 == 16755200) {
            return true;
         }
      }

      if (var1 < 3) {
         for (Text var4 : var0.getSiblings()) {
            String var5 = var4.getString();
            if (var5 != null && !var5.isBlank() && redOrGold(var4, var1 + 1)) {
               return true;
            }
         }
      }

      return false;
   }
}
