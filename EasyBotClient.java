package pl.easybot;

import net.minecraft.util.Identifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.AfterKeyPress;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.util.InputUtil.Type;
import pl.easybot.bot.Bot;
import pl.easybot.gui.BotScreen;

public class EasyBotClient implements ClientModInitializer {
   public static final int MAX_BOTS = 50;
   private static final long STAGGER = 30000L;
   private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("easybot.txt");
   public static KeyBinding openKey;
   public static final Bot[] bots = new Bot[50];
   public static final String[] nicks = new String[50];
   private static final long[] queued = new long[50];
   public static int count = 1;
   public static int selected;
   public static String address = "localhost";
   public static KeyBinding errKey;
   public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("easybot", "main"));

   public void onInitializeClient() {
      load();
      openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.easybot.open", Type.field_1668, 88, CATEGORY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (errKey != null && errKey.wasPressed() && client.currentScreen == null) {
            ErrorsConsoleScreen.show(client, null);
         }

         while (openKey.wasPressed()) {
            if (client.currentScreen == null) {
               client.setScreen(new BotScreen(null));
            }
         }

         long now = System.currentTimeMillis();

         for (int i = 0; i < 50; i++) {
            if (queued[i] != 0L && now >= queued[i]) {
               queued[i] = 0L;
               if (i < count && (bots[i] == null || !bots[i].active())) {
                  bots[i] = new Bot(nicks[i], address);
               }
            }
         }
      });
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, w, h) -> {
         if (screen instanceof TitleScreen || screen instanceof MultiplayerScreen) {
            ScreenKeyboardEvents.afterKeyPress(screen).register((AfterKeyPress)(s, keyInput) -> {
               if (openKey.matchesKey(keyInput)) {
                  client.setScreen(new BotScreen(s));
               }
            });
         }
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> disconnectAll());
      errKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.easybot.errors.b", Type.field_1668, 66, CATEGORY));
   }

   public static Bot bot() {
      return bots[selected];
   }

   public static boolean queued(int i) {
      return queued[i] > 0L;
   }

   public static boolean busy(int i) {
      return queued(i) || bots[i] != null && bots[i].active();
   }

   public static boolean anyBusy() {
      for (int i = 0; i < 50; i++) {
         if (busy(i)) {
            return true;
         }
      }

      return false;
   }

   public static int online() {
      int n = 0;

      for (int i = 0; i < count; i++) {
         if (bots[i] != null && bots[i].state() == Bot.State.PLAY) {
            n++;
         }
      }

      return n;
   }

   public static boolean blocked() {
      return count > 1 && address.toLowerCase(Locale.ROOT).contains("mimi");
   }

   public static String problem() {
      if (address.isBlank()) {
         return "Wpisz adres serwera";
      }

      if (blocked()) {
         return "Na ten serwer tylko 1 bot";
      }

      for (int i = 0; i < count; i++) {
         if (!busy(i)) {
            String n = nicks[i] == null ? "" : nicks[i];
            String who = count > 1 ? "Bot " + (i + 1) + ": " : "";
            if (n.length() < 3 || n.length() > 16) {
               return who + "nick musi mieć 3-16 znaków";
            }

            for (int j = 0; j < count; j++) {
               if (j != i && n.equalsIgnoreCase(nicks[j])) {
                  return who + "nick się powtarza";
               }
            }
         }
      }

      return null;
   }

   public static boolean hasIdle() {
      for (int i = 0; i < count; i++) {
         if (!busy(i)) {
            return true;
         }
      }

      return false;
   }

   public static boolean canLaunch() {
      if (problem() != null) {
         return false;
      }

      for (int i = 0; i < count; i++) {
         if (!busy(i)) {
            return true;
         }
      }

      return false;
   }

   public static void launch() {
      if (canLaunch()) {
         long at = System.currentTimeMillis();

         for (int i = 0; i < count; i++) {
            if (!busy(i)) {
               queued[i] = at;
               at += Stagger.next();
            }
         }

         save();
      }
   }

   public static void disconnectAll() {
      for (int i = 0; i < 50; i++) {
         queued[i] = 0L;
         if (bots[i] != null && bots[i].active()) {
            bots[i].disconnect();
         }
      }
   }

   public static void setCount(int n) {
      n = Math.clamp(n, 1, 50);

      for (int i = n; i < 50; i++) {
         queued[i] = 0L;
         if (bots[i] != null && bots[i].active()) {
            bots[i].disconnect();
         }
      }

      count = n;
      if (selected >= n) {
         selected = n - 1;
      }

      fillNicks();
   }

   private static void fillNicks() {
      String first = nicks[0] == null ? "" : nicks[0];
      String base = first.replaceAll("\\d+$", "");
      if (base.length() < 2) {
         base = "maciekjacu";
      }

      for (int i = 1; i < count; i++) {
         if (nicks[i] == null || nicks[i].isEmpty()) {
            String num = String.valueOf(i + 1);
            nicks[i] = base.substring(0, Math.min(base.length(), 16 - num.length())) + num;
         }
      }

      if (nicks[0] == null) {
         nicks[0] = "";
      }
   }

   private static void load() {
      try {
         if (Files.exists(CONFIG)) {
            List<String> lines = Files.readAllLines(CONFIG, StandardCharsets.UTF_8);
            int nick = 0;

            for (String line : lines) {
               line = line.strip();
               if (!line.isEmpty()) {
                  int eq = line.indexOf(61);
                  if (eq < 0) {
                     nicks[0] = line;
                  } else {
                     String key = line.substring(0, eq);
                     String value = line.substring(eq + 1).strip();
                     switch (key) {
                        case "address":
                           address = value;
                           break;
                        case "count":
                           count = Math.clamp(Integer.parseInt(value), 1, 50);
                           break;
                        case "nick":
                           if (nick < 50) {
                              nicks[nick++] = value;
                           }
                     }
                  }
               }
            }
         }
      } catch (Exception var9) {
      }

      fillNicks();
   }

   public static void save() {
      List<String> lines = new ArrayList<>();
      lines.add("address=" + address);
      lines.add("count=" + count);

      for (int i = 0; i < count; i++) {
         lines.add("nick=" + (nicks[i] == null ? "" : nicks[i]));
      }

      try {
         Files.write(CONFIG, lines, StandardCharsets.UTF_8);
      } catch (Exception var2) {
      }
   }
}
