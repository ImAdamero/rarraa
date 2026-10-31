package pl.easybot.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

public class PillButton extends ClickableWidget {
   private final int color;
   private final BooleanSupplier enabled;
   private final Runnable action;
   private final boolean filled;

   public PillButton(int x, int y, int w, int h, String label, int color, BooleanSupplier enabled, Runnable action) {
      this(x, y, w, h, label, color, true, enabled, action);
   }

   public PillButton(int x, int y, int w, int h, String label, int color, boolean filled, BooleanSupplier enabled, Runnable action) {
      super(x, y, w, h, Text.literal(label));
      this.color = color;
      this.filled = filled;
      this.enabled = enabled;
      this.action = action;
   }

   protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
      boolean on = this.enabled.getAsBoolean();
      this.active = on;
      boolean hover = on && this.isHovered();
      String label = this.getMessage().getString();
      if (on) {
         Draw.button(
            c,
            MinecraftClient.getInstance().textRenderer,
            this.getX(),
            this.getY(),
            this.width,
            this.height,
            label,
            this.color,
            this.filled,
            hover
         );
      } else {
         Draw.box(c, this.getX(), this.getY(), this.width, this.height, Theme.PANEL_SOLID, Theme.OUTLINE);
         int tw = MinecraftClient.getInstance().textRenderer.getWidth(label);
         c.drawText(
            MinecraftClient.getInstance().textRenderer,
            label,
            this.getX() + (this.width - tw) / 2,
            this.getY() + (this.height - 8) / 2,
            Theme.TEXT_FAINT,
            false
         );
      }
   }

   public void onClick(double mouseX, double mouseY) {
      this.action.run();
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      this.appendDefaultNarrations(builder);
   }
}
