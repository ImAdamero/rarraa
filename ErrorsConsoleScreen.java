package pl.easybot;

import net.minecraft.client.input.KeyInput;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import pl.easybot.bot.Bot;

public class ErrorsConsoleScreen extends Screen {
   private static final int LINE_H = 10;
   private static final int PAD = 8;
   private static final int TOP = 22;
   private static final int MAX_ENTRIES = 400;
   private final Screen parent;
   private int scroll = 0;
   private int lastTotal = -1;
   private long clearedAt = 0L;

   public static void show(MinecraftClient var0, Screen var1) {
      System.out.println("[EasyBot] klawisz bledow nacisniety - otwieram konsole bledow");

      try {
         var0.setScreen(new ErrorsConsoleScreen(var1));
      } catch (Throwable var3) {
         System.out.println("[EasyBot] nie udalo sie otworzyc konsoli bledow:");
         var3.printStackTrace();
      }
   }

   public static void started() {
      System.out.println("[EasyBot] ErrorsClient zaladowany - konsola bledow pod klawiszem B");
   }

   public ErrorsConsoleScreen(Screen var1) {
      super(Text.literal("Bledy botow"));
      this.parent = var1;
   }

   private List<ErrorsConsoleScreen.Entry> collect() {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < EasyBotClient.bots.length; var2++) {
         Bot var3 = EasyBotClient.bots[var2];
         if (var3 != null) {
            String var4 = var2 < EasyBotClient.nicks.length ? EasyBotClient.nicks[var2] : null;
            String var5 = var4 != null && !var4.isBlank() ? var4 : var3.name;

            Object[] var6;
            try {
               var6 = new ArrayList<>(var3.lines()).toArray();
            } catch (Throwable var13) {
               continue;
            }

            for (Object var10 : var6) {
               try {
                  Bot.Line var11 = (Bot.Line)var10;
                  if (var11.time() > this.clearedAt && ErrorsClient.isError(var11.text())) {
                     var1.add(new ErrorsConsoleScreen.Entry(var11.time(), var5, var11.text()));
                  }
               } catch (Throwable var12) {
               }
            }
         }
      }

      var1.sort(Comparator.comparingLong(ErrorsConsoleScreen.Entry::time));
      return var1.size() > 400 ? var1.subList(var1.size() - 400, var1.size()) : var1;
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
      var1.fill(0, 0, this.width, this.height, -267382768);
      TextRenderer var5 = this.textRenderer;
      List<ErrorsConsoleScreen.Entry> var6 = this.collect();
      var1.drawText(
         var5, "Bledy wszystkich botow (" + var6.size() + ")   |   kolko/PgUp/PgDn/Home/End - przewijanie, C - wyczysc, ESC - powrot", 8, 7, -43691, true
      );
      var1.fill(8, 20, this.width - 8, 21, 1728053247);
      int var7 = Math.max(40, this.width - 16);
      ArrayList var8 = new ArrayList();

      for (ErrorsConsoleScreen.Entry var10 : var6) {
         try {
            MutableText var11 = Text.literal("[" + var10.nick() + "] ").withColor(11184810);
            List var12 = var5.wrapLines(var11.append(var10.text()), var7);
            if (var12 != null) {
               var8.addAll(var12);
            }
         } catch (Throwable var15) {
         }
      }

      if (var8.isEmpty()) {
         var1.drawText(var5, "Brak bledow - zaden bot nie zglosil problemu.", 8, 26, -7811960, true);
      } else {
         int var16 = Math.max(1, (this.height - 22 - 8) / 10);
         int var17 = var8.size();
         if (this.lastTotal >= 0 && this.scroll > 0 && var17 > this.lastTotal) {
            this.scroll = this.scroll + (var17 - this.lastTotal);
         }

         this.lastTotal = var17;
         this.scroll = Math.min(Math.max(this.scroll, 0), Math.max(0, var17 - var16));
         int var18 = var17 - this.scroll;
         int var19 = Math.max(0, var18 - var16);
         byte var13 = 22;

         for (int var14 = var19; var14 < var18; var14++) {
            var1.drawText(var5, (OrderedText)var8.get(var14), 8, var13, -1, true);
            var13 += 10;
         }

         if (this.scroll > 0) {
            var1.drawText(var5, "^ " + this.scroll + " nizej", this.width - 8 - 70, 7, -22016, true);
         }
      }
   }

   public boolean mouseScrolled(double var1, double var3, double var5, double var7) {
      this.scroll = Math.max(0, this.scroll + (int)Math.signum(var7) * 3);
      return true;
   }

   public boolean keyPressed(KeyInput input) {
      return this.keyPressedOld(input.getKeycode(), 0, 0);
   }

   private boolean keyPressedOld(int var1, int var2, int var3) {
      switch (var1) {
         case 67:
            this.clearedAt = System.currentTimeMillis();
            this.scroll = 0;
            return true;
         case 256:
            this.close();
            return true;
         case 266:
            this.scroll += 10;
            return true;
         case 267:
            this.scroll = Math.max(0, this.scroll - 10);
            return true;
         case 268:
            this.scroll = 1073741823;
            return true;
         case 269:
            this.scroll = 0;
            return true;
         default:
            return false;
      }
   }

   public void close() {
      MinecraftClient var1 = this.client;
      if (var1 != null) {
         var1.setScreen(this.parent);
      }
   }

   public boolean shouldPause() {
      return false;
   }

   private record Entry(long time, String nick, Text text) {
   }
}
