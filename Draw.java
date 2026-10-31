package pl.easybot.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Draw {
   private Draw() {
   }

   public static void rect(DrawContext c, int x, int y, int w, int h, int color) {
      c.fill(x + 1, y, x + w - 1, y + h, color);
      c.fill(x, y + 1, x + 1, y + h - 1, color);
      c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
   }

   public static void box(DrawContext c, int x, int y, int w, int h, int fill, int outline) {
      rect(c, x, y, w, h, outline);
      c.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
   }

   public static void shadow(DrawContext c, int x, int y, int w, int h) {
      rect(c, x - 3, y - 3, w + 6, h + 6, 402653184);
      rect(c, x - 2, y - 2, w + 4, h + 4, 671088640);
      rect(c, x - 1, y - 1, w + 2, h + 2, 1073741824);
   }

   public static void toggle(DrawContext c, int x, int y, boolean on, boolean hover) {
      int track = on ? (hover ? lighten(Theme.ACCENT) : Theme.ACCENT) : (hover ? Theme.OUTLINE_HOVER : Theme.OFF);
      rect(c, x, y, 20, 10, track);
      int kx = on ? x + 11 : x + 2;
      rect(c, kx, y + 2, 7, 6, on ? -1 : -4866869);
   }

   public static void button(DrawContext c, TextRenderer tr, int x, int y, int w, int h, String label, int color, boolean filled, boolean hover) {
      if (filled) {
         box(c, x, y, w, h, hover ? lighten(color) : color, hover ? lighten(color) : color);
      } else {
         box(c, x, y, w, h, hover ? mix(Theme.SUB, color, 0.18F) : Theme.SUB, hover ? color : mix(Theme.OUTLINE, color, 0.45F));
      }

      int tw = tr.getWidth(label);
      c.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, filled ? Theme.INK : (hover ? lighten(color) : color), false);
   }

   public static String trim(TextRenderer tr, String s, int w) {
      if (w <= 0) {
         return "";
      } else {
         return tr.getWidth(s) <= w ? s : tr.trimToWidth(s, Math.max(0, w - tr.getWidth("…"))) + "…";
      }
   }

   public static int lighten(int color) {
      return mix(color, -1, 0.2F);
   }

   public static int mix(int a, int b, float t) {
      int aa = a >>> 24;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return (int)(aa + (ba - aa) * t) << 24 | (int)(ar + (br - ar) * t) << 16 | (int)(ag + (bg - ag) * t) << 8 | (int)(ab + (bb - ab) * t);
   }

   public static int alpha(int color, int alpha) {
      return alpha << 24 | color & 16777215;
   }
}
