package pl.easybot.gui;

import net.minecraft.client.input.KeyInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import pl.easybot.EasyBotClient;
import pl.easybot.bot.Bot;

public class BotConsoleScreen extends Screen {
   private static final int LINE_H = 10;
   private static final int PAD = 8;
   private static final int TOP = 22;
   private final Screen parent;
   private final int idx;
   private int scroll = 0;
   private int lastTotal = -1;

   public BotConsoleScreen(Screen var1, int var2) {
      super(Text.literal("Konsola bota " + (var2 + 1)));
      this.parent = var1;
      this.idx = var2;
   }

   private Bot bot() {
      return this.idx >= 0 && this.idx < EasyBotClient.bots.length ? EasyBotClient.bots[this.idx] : null;
   }

   private String nick() {
      String var1 = this.idx >= 0 && this.idx < EasyBotClient.nicks.length ? EasyBotClient.nicks[this.idx] : null;
      return var1 != null && !var1.isBlank() ? var1 : "Bot " + (this.idx + 1);
   }

   public void render(DrawContext var1, int var2, int var3, float var4) {
      var1.fill(0, 0, this.width, this.height, -267382768);
      TextRenderer var5 = this.textRenderer;
      Bot var6 = this.bot();
      var1.drawText(
         var5,
         "Konsola bota: "
            + this.nick()
            + (var6 == null ? "  [jeszcze nie wystartowal]" : (var6.active() ? "" : "  [offline]"))
            + "   |   kolko/PgUp/PgDn/Home/End - przewijanie, ESC - powrot",
         8,
         7,
         -1,
         true
      );
      var1.fill(8, 20, this.width - 8, 21, 1728053247);
      int var7 = Math.max(40, this.width - 16);
      ArrayList var8 = new ArrayList();

      Object[] var9;
      try {
         var9 = var6 == null ? new Object[0] : new ArrayList<>(var6.lines()).toArray();
      } catch (Throwable var18) {
         var9 = new Object[0];
      }

      for (Object var13 : var9) {
         try {
            Bot.Line var14 = (Bot.Line)var13;
            List var15 = var5.wrapLines(var14.text(), var7);
            if (var15 != null && !var15.isEmpty()) {
               var8.addAll(var15);
            }
         } catch (Throwable var17) {
         }
      }

      int var19 = Math.max(1, (this.height - 22 - 8) / 10);
      int var20 = var8.size();
      if (this.lastTotal >= 0 && this.scroll > 0 && var20 > this.lastTotal) {
         this.scroll = this.scroll + (var20 - this.lastTotal);
      }

      this.lastTotal = var20;
      int var21 = Math.max(0, var20 - var19);
      this.scroll = Math.min(Math.max(this.scroll, 0), var21);
      int var22 = var20 - this.scroll;
      int var23 = Math.max(0, var22 - var19);
      byte var24 = 22;

      for (int var16 = var23; var16 < var22; var16++) {
         var1.drawText(var5, (OrderedText)var8.get(var16), 8, var24, -1, true);
         var24 += 10;
      }

      if (this.scroll > 0) {
         var1.drawText(var5, "^ " + this.scroll + " nizej", this.width - 8 - 70, 7, -22016, true);
      }
   }

   public boolean mouseScrolled(double var1, double var3, double var5, double var7) {
      this.scroll = this.scroll + (int)Math.signum(var7) * 3;
      if (this.scroll < 0) {
         this.scroll = 0;
      }

      return true;
   }

   public boolean keyPressed(KeyInput input) {
      return this.keyPressedOld(input.getKeycode(), 0, 0);
   }

   private boolean keyPressedOld(int var1, int var2, int var3) {
      switch (var1) {
         case 256:
            this.close();
            return true;
         case 257:
         case 258:
         case 259:
         case 260:
         case 261:
         case 262:
         case 263:
         case 264:
         case 265:
         default:
            return false;
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
}
