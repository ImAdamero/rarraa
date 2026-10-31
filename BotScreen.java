package pl.easybot.gui;

import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Formatting;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.registry.Registries;
import net.minecraft.item.Item.TooltipContext;
import pl.easybot.EasyBotClient;
import pl.easybot.ErrorsClient;
import pl.easybot.bot.Bot;

public class BotScreen extends Screen {
   private static final int HEADER = 30;
   private static final int LINE_H = 10;
   private static final int SLOT = 18;
   private static final int GRID = 162;
   private static final int TILE_HEAD = 16;
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
   private static final List<String> history = new ArrayList<>();
   private static String draft = "";
   private static int scroll;
   private static int tab;
   private static boolean toAll = true;
   private static int mainScroll;
   private final Screen parent;
   private TextFieldWidget address;
   private TextFieldWidget countField;
   private final List<TextFieldWidget> nickFields = new ArrayList<>();
   private TextFieldWidget input;
   private PillButton holdButton;
   private PillButton closeButton;
   private int historyPos;
   private int px;
   private int py;
   private int pw;
   private int ph;
   private int ax;
   private int ay;
   private int aw;
   private int ah;
   private int addrW;
   private int countX;
   private int tabsY;
   private int pickerX = -1;
   private final List<BotScreen.Tile> tiles = new ArrayList<>();
   private BotScreen.Tile main;
   private long mainSig = -1L;
   private int mainWidth = -1;
   private List<BotScreen.WLine> mainLines = List.of();
   private final Map<Integer, BotScreen.Cache> caches = new HashMap<>();
   private List<BotScreen.WLine> wrapped = List.of();
   private final Map<BotScreen.WLine, int[]> offsets = new IdentityHashMap<>();
   private int cx;
   private int cy;
   private int cw;
   private int ch;
   private int sx;
   private int sy;
   private int sw;
   private int sh;
   private float ks = 1.0F;
   private int drawStart;
   private int drawEnd;
   private int drawY0;
   private int[] selA;
   private int[] selB;
   private boolean selecting;
   private long lastConsoleClick;
   private long copiedAt;
   private boolean rebuild;
   private final List<BotScreen.Hot> hots = new ArrayList<>();
   private int hoveredSlot = -1;
   private ItemStack hoveredStack = ItemStack.EMPTY;
   private long lastSlotClick;
   private int lastSlot = -1;
   private int leftX;
   private int rightX;
   private int midX;
   private int midW;

   public BotScreen(Screen parent) {
      super(Text.literal("EasyBot"));
      this.parent = parent;
   }

   public boolean shouldPause() {
      return false;
   }

   private static Bot bot() {
      return EasyBotClient.bot();
   }

   private static boolean online() {
      return bot() != null && bot().state() == Bot.State.PLAY;
   }

   private static int count() {
      return EasyBotClient.count;
   }

   protected void init() {
      this.pw = Math.min(560, this.width - 16);
      this.ph = Math.min(320, this.height - 16);
      this.px = (this.width - this.pw) / 2;
      this.py = (this.height - this.ph) / 2;
      int rowY = this.py + 30 + 8;
      int clearX = this.px + this.pw - 8 - 62;
      int stopX = clearX - 6 - 70;
      int sendX = stopX - 6 - 96;
      this.countX = sendX - 6 - 54;
      this.addrW = this.countX - 6 - (this.px + 8);
      this.address = new TextFieldWidget(this.textRenderer, this.px + 14, rowY + 6, this.addrW - 12, 10, Text.literal("Adres"));
      this.address.setDrawsBackground(false);
      this.address.setMaxLength(128);
      this.address.setEditableColor(Theme.TEXT);
      this.address.setText(EasyBotClient.address);
      this.address.setChangedListener(v -> EasyBotClient.address = v.strip());
      this.address.setPlaceholder(Text.literal("Adres serwera, np. localhost:25565").withColor(Theme.TEXT_FAINT));
      this.addDrawableChild(this.address);
      this.countField = new TextFieldWidget(this.textRenderer, this.countX + 34, rowY + 6, 16, 10, Text.literal("Ilość botów"));
      this.countField.setDrawsBackground(false);
      this.countField.setMaxLength(2);
      this.countField.setEditableColor(Theme.TEXT);
      this.countField.setTextPredicate(s -> s.matches("\\d*"));
      this.countField.setText(String.valueOf(count()));
      this.countField.setChangedListener(v -> {
         if (!v.isEmpty()) {
            int n = Math.clamp(Integer.parseInt(v), 1, 50);
            if (n != count()) {
               EasyBotClient.setCount(n);
               this.rebuild = true;
            }
         }
      });
      this.countField.setTooltip(Tooltip.of(Text.literal("Ile botów ma wejść (1-50)")));
      this.addDrawableChild(this.countField);
      this.addDrawableChild(new PillButton(sendX, rowY, 96, 20, count() > 1 ? "Wyślij boty" : "Wyślij bota", Theme.ACCENT, EasyBotClient::canLaunch, this::launch));
      this.addDrawableChild(new PillButton(stopX, rowY, 70, 20, "Rozłącz", Theme.RED, EasyBotClient::anyBusy, EasyBotClient::disconnectAll));
      this.addDrawableChild(new PillButton(clearX, rowY, 62, 20, "Wyczyść", Theme.TEXT_DIM, false, () -> tab == 0, () -> {
         for (int i = 0; i < count(); i++) {
            if (EasyBotClient.bots[i] != null) {
               EasyBotClient.bots[i].clear();
            }
         }

         scroll = 0;
         this.selA = this.selB = null;
      }));
      this.tabsY = rowY + 20 + 6;
      int afkX = this.px + this.pw - 8 - 78;
      int loginX = afkX - 4 - 78;
      PillButton loginBtn = new PillButton(
         loginX, this.tabsY, 78, 12, "Auto login: " + (Bot.autoLogin ? "ON" : "OFF"), Theme.ACCENT, Bot.autoLogin, () -> true, () -> {
            Bot.autoLogin = !Bot.autoLogin;
            this.rebuild = true;
         }
      );
      loginBtn.setTooltip(Tooltip.of(Text.literal("Wpisuje tylko /register albo /login, gdy serwer o to prosi")));
      this.addDrawableChild(loginBtn);
      PillButton afkBtn = new PillButton(
         afkX, this.tabsY, 78, 12, "Auto AFK: " + (Bot.autoAfk ? "ON" : "OFF"), Theme.ACCENT, Bot.autoAfk, () -> true, () -> {
            Bot.autoAfk = !Bot.autoAfk;
            this.rebuild = true;
         }
      );
      afkBtn.setTooltip(Tooltip.of(Text.literal("ON: bot robi wszystko sam (regulamin, warp afk, chodzenie). OFF: stoi i nic sam nie wpisuje")));
      this.addDrawableChild(afkBtn);
      PillButton themeBtn = new PillButton(
         loginX - 4 - 100, this.tabsY, 100, 12, "Motyw: " + Theme.name(), Theme.GOLD, false, () -> true, () -> {
            Theme.next();
            this.rebuild = true;
         }
      );
      themeBtn.setTooltip(Tooltip.of(Text.literal("Zmień motyw kolorów (Noc, Granat, Fiolet, Różany, Bursztyn, Lód, AMOLED)")));
      this.addDrawableChild(themeBtn);
      this.ax = this.px + 8;
      this.ay = this.tabsY + 16;
      this.aw = this.pw - 16;
      this.ah = this.py + this.ph - 8 - this.ay;
      this.pickerX = -1;
      if (count() > 1) {
         this.pickerX = this.tabX(1) + this.textRenderer.getWidth(this.tabLabel(1)) + 24;
         this.addDrawableChild(new PillButton(this.pickerX, this.tabsY, 14, 12, "<", Theme.TEXT_DIM, false, () -> true, () -> this.pick(-1)));
         this.addDrawableChild(new PillButton(this.pickerX + 18 + 96, this.tabsY, 14, 12, ">", Theme.TEXT_DIM, false, () -> true, () -> this.pick(1)));
      }

      this.nickFields.clear();
      this.tiles.clear();
      if (tab == 0) {
         this.initConsole();
      } else {
         this.initGui();
      }

      this.setInitialFocus(
         (Element)(tab == 0 && online()
            ? this.input
            : (tab == 0 && !this.nickFields.isEmpty() ? (Element)this.nickFields.get(EasyBotClient.selected) : this.address))
      );
      this.historyPos = history.size();
   }

   private void initConsole() {
      this.holdButton = null;
      this.closeButton = null;
      int inputY = this.py + this.ph - 8 - 20;
      int toggleW = count() > 1 ? 92 : 0;
      int inputW = this.aw - 80 - (toggleW > 0 ? toggleW + 6 : 0);
      this.input = new TextFieldWidget(this.textRenderer, this.px + 14, inputY + 6, inputW - 12, 10, Text.literal("Czat"));
      this.input.setDrawsBackground(false);
      this.input.setMaxLength(256);
      this.input.setEditableColor(Theme.TEXT);
      this.input.setText(draft);
      this.input.setChangedListener(v -> draft = v);
      this.input
         .setPlaceholder(
            Text.literal(count() > 1 && toAll ? "Komenda lub wiadomość do wszystkich botów" : "Napisz jako bot albo wpisz /komendę i Enter")
               .withColor(Theme.TEXT_FAINT)
         );
      this.addDrawableChild(this.input);
      if (toggleW > 0) {
         PillButton toggle = new PillButton(
            this.ax + inputW + 6, inputY, toggleW, 20, toAll ? "Do wszystkich" : "Do wybranego", Theme.GOLD, false, () -> true, () -> {
               toAll = !toAll;
               this.rebuild = true;
            }
         );
         toggle.setTooltip(Tooltip.of(Text.literal("Czy wiadomość z pola czatu wysyłają wszystkie boty, czy tylko zaznaczony")));
         this.addDrawableChild(toggle);
      }

      this.addDrawableChild(
         new PillButton(
            this.px + this.pw - 8 - 74, inputY, 74, 20, "Wyślij", Theme.ACCENT, () -> anyOnline() && !this.input.getText().isBlank(), this::sendChat
         )
      );
      int area = this.ah - 26;
      this.main = null;
      if (count() > 1) {
         int mh = Math.clamp((int)(area * 0.38), 60, 130);
         area -= mh + 4;
         this.main = new BotScreen.Tile(this.ax, this.ay + area + 4, this.aw, mh, mh >= 90 ? 1.0F : 0.5F);
         PillButton all = new PillButton(
            this.ax + this.aw - 4 - 124, this.main.y() + 2, 124, 12, "Wyrzuć eq wszystkim", Theme.RED, false, BotScreen::anyOnline, this::throwAllBots
         );
         all.setTooltip(Tooltip.of(Text.literal("Każdy bot na serwerze wyrzuca cały ekwipunek, zbroję i drugą rękę")));
         this.addDrawableChild(all);
      }

      this.layoutTiles(this.ax, this.ay, this.aw, area);

      for (int i = 0; i < count(); i++) {
         BotScreen.Tile t = this.tiles.get(i);
         int fw = Math.max(8, t.w() - 22);
         TextFieldWidget f = new TextFieldWidget(this.textRenderer, t.x() + 5, t.y() + 4, fw, 10, Text.literal("Nick " + (i + 1)));
         f.setDrawsBackground(false);
         f.setMaxLength(16);
         f.setEditableColor(Theme.TEXT);
         f.setUneditableColor(Theme.TEXT);
         f.setTextPredicate(s -> s.matches("[A-Za-z0-9_]*"));
         f.setText(NickSync.initial(f, i));
         int index = i;
         f.setChangedListener(s -> NickSync.onNick(index, s));
         f.setPlaceholder(Text.literal(count() > 1 ? "Nick " + (i + 1) : "Nick bota").withColor(Theme.TEXT_FAINT));
         this.nickFields.add(f);
         this.addDrawableChild(f);
      }
   }

   private void throwAllBots() {
      for (int i = 0; i < count(); i++) {
         Bot b = EasyBotClient.bots[i];
         if (b != null && b.state() == Bot.State.PLAY) {
            b.throwAll();
         }
      }
   }

   private void mainWindow(DrawContext c, int mx, int my) {
      BotScreen.Tile t = this.main;
      Draw.box(c, t.x(), t.y(), t.w(), t.h(), Theme.FIELD, t.in(mx, my) ? Theme.OUTLINE_HOVER : Theme.OUTLINE);
      c.fill(t.x() + 1, t.y() + 16 - 1, t.x() + t.w() - 1, t.y() + 16, Theme.OUTLINE);
      c.drawText(
         this.textRenderer,
         Draw.trim(this.textRenderer, "GŁÓWNE OKNO · wszystkie boty (" + EasyBotClient.online() + "/" + count() + " online)", t.w() - 140),
         t.x() + 5,
         t.y() + 4,
         Theme.TEXT_FAINT,
         false
      );
      float s = t.scale();
      int innerW = (int)((t.w() - 2) / s) - 14;
      long sig = count();

      for (int i = 0; i < count(); i++) {
         Bot b = EasyBotClient.bots[i];
         sig = sig * 31L + (b == null ? 0 : System.identityHashCode(b) + b.version());
      }

      if (sig != this.mainSig || innerW != this.mainWidth) {
         int before = this.mainLines.size();
         this.mainSig = sig;
         this.mainWidth = innerW;
         this.mainLines = this.merged(innerW);
         if (mainScroll > 0) {
            mainScroll = Math.max(0, mainScroll + this.mainLines.size() - before);
         }
      }

      int bx = t.x() + 1;
      int by = t.textY();
      int bw = t.w() - 2;
      int bh = t.textH() - 1;
      int lh = (int)(bh / s);
      int visible = Math.max(1, (lh - 6) / 10);
      mainScroll = Math.clamp(mainScroll, 0, Math.max(0, this.mainLines.size() - visible));
      if (this.mainLines.isEmpty()) {
         this.center(c, "Tu widać, co dzieje się ze wszystkimi botami naraz.", "Pole niżej wysyła wiadomość albo /komendę od wszystkich botów.", bx, by, bw, bh);
      } else {
         int end = this.mainLines.size() - mainScroll;
         int start = Math.max(0, end - visible);
         int y = by + 3 + (visible - (end - start)) * 10;
         c.enableScissor(bx, by, bx + bw, by + bh);
         c.getMatrices().pushMatrix();
         c.getMatrices().translate(bx, by);
         c.getMatrices().scale(s, s);
         c.getMatrices().translate(-bx, -by);

         for (int k = start; k < end; k++) {
            c.drawText(this.textRenderer, this.mainLines.get(k).text(), bx + 4, y, Theme.TEXT, false);
            y += 10;
         }

         int lw = (int)(bw / s);
         if (this.mainLines.size() > visible) {
            int trackH = lh - 4;
            int thumbH = Math.max(12, trackH * visible / this.mainLines.size());
            int max = Math.max(1, this.mainLines.size() - visible);
            int thumbY = by + 2 + (trackH - thumbH) * (max - mainScroll) / max;
            c.fill(bx + lw - 4, thumbY, bx + lw - 2, thumbY + thumbH, mainScroll > 0 ? Theme.TEXT_DIM : Theme.OUTLINE_HOVER);
         }

         c.getMatrices().popMatrix();
         c.disableScissor();
      }
   }

   private List<BotScreen.WLine> merged(int innerW) {
      List<BotScreen.Entry> all = new ArrayList<>();

      for (int i = 0; i < count(); i++) {
         Bot b = EasyBotClient.bots[i];
         if (b != null) {
            for (Bot.Line line : ErrorsClient.lines(b)) {
               all.add(new BotScreen.Entry(line.time(), i, line.text()));
            }
         }
      }

      all.sort((p, q) -> Long.compare(p.time(), q.time()));
      List<BotScreen.Entry> tail = all.subList(Math.max(0, all.size() - 400), all.size());
      List<BotScreen.WLine> out = new ArrayList<>();

      for (BotScreen.Entry e : tail) {
         Text full = Text.literal(TIME.format(Instant.ofEpochMilli(e.time())) + " ")
            .withColor(Theme.TEXT_FAINT)
            .append(Text.literal(EasyBotClient.bots[e.bot()].name + " » ").withColor(Theme.ACCENT))
            .append(Text.empty().withColor(Theme.TEXT).append(e.text()));
         boolean first = true;

         for (OrderedText t : this.textRenderer.wrapLines(full, Math.max(20, innerW))) {
            out.add(new BotScreen.WLine(t, new int[0], first));
            first = false;
         }
      }

      return out;
   }

   private void layoutTiles(int x, int y, int w, int h) {
      int n = count();
      int gap = 4;
      int best = 1;
      double score = -1.0;

      for (int cols = 1; cols <= n; cols++) {
         int rows = (n + cols - 1) / cols;
         double tw = (double)(w - (cols - 1) * gap) / cols;
         double th = (double)(h - (rows - 1) * gap) / rows;
         double s = Math.min(tw, th * 2.6);
         if (s > score) {
            score = s;
            best = cols;
         }
      }

      int cols = best;
      int rows = (n + cols - 1) / cols;
      int tw = (w - (cols - 1) * gap) / cols;
      int th = (h - (rows - 1) * gap) / rows;
      float scale = tw >= 200 && th >= 70 ? 1.0F : 0.5F;

      for (int i = 0; i < n; i++) {
         int c = i % cols;
         int r = i / cols;
         this.tiles.add(new BotScreen.Tile(x + c * (tw + gap), y + r * (th + gap), tw, th, scale));
      }
   }

   private void initGui() {
      this.input = null;
      this.leftX = this.ax + 4;
      this.rightX = this.ax + this.aw - 4 - 162;
      this.midX = this.leftX + 162 + 10;
      this.midW = this.rightX - 10 - this.midX;
      boolean middle = this.midW >= 84;
      this.closeButton = (PillButton)this.addDrawableChild(
         new PillButton(
            this.leftX + 162 - 64,
            this.ay + 1,
            64,
            14,
            "Zamknij GUI",
            Theme.RED,
            false,
            () -> online() && bot().inventory().window() != null,
            () -> bot().closeWindow()
         )
      );
      int bh = 18;
      List<int[]> spots = new ArrayList<>();
      if (middle) {
         int bw = Math.min(110, this.midW);
         int bx = this.midX + (this.midW - bw) / 2;
         int by = this.ay + 66;

         for (int i = 0; i < 5; i++) {
            spots.add(new int[]{bx, by + i * (bh + 4), bw});
         }
      } else {
         int bw = 38;
         int by = this.ay + 120;

         for (int i = 0; i < 4; i++) {
            spots.add(new int[]{this.rightX + i * (bw + 3), by, bw});
         }

         spots.add(new int[]{this.rightX, by + bh + 4, 162});
      }

      PillButton lpm = new PillButton(spots.get(0)[0], spots.get(0)[1], spots.get(0)[2], bh, "LPM", Theme.ACCENT, false, this::canAct, () -> bot().swing());
      lpm.setTooltip(Tooltip.of(Text.literal("Bot macha ręką / klika lewym przyciskiem trzymając przedmiot")));
      PillButton ppm = new PillButton(spots.get(1)[0], spots.get(1)[1], spots.get(1)[2], bh, "PPM", Theme.ACCENT, false, this::canAct, () -> bot().useItem());
      ppm.setTooltip(Tooltip.of(Text.literal("Bot klika prawym przyciskiem trzymanym przedmiotem (np. otwiera menu z itemu)")));
      this.holdButton = new PillButton(
         spots.get(2)[0],
         spots.get(2)[1],
         spots.get(2)[2],
         bh,
         middle ? "Trzymaj PPM" : "Trzymaj",
         Theme.GOLD,
         false,
         this::canAct,
         () -> bot().holdUse(!bot().inventory().using())
      );
      this.holdButton
         .setTooltip(Tooltip.of(Text.literal("Przytrzymuje prawy przycisk (jedzenie, łuk, tarcza) aż klikniesz ponownie")));
      PillButton drop = new PillButton(
         spots.get(3)[0], spots.get(3)[1], spots.get(3)[2], bh, middle ? "Wyrzuć (Q)" : "Q", Theme.RED, false, this::canAct, () -> bot().drop(this.ctrl())
      );
      drop.setTooltip(Tooltip.of(Text.literal("Wyrzuca 1 sztukę trzymanego przedmiotu, z Ctrl cały stack")));
      this.addDrawableChild(lpm);
      this.addDrawableChild(ppm);
      this.addDrawableChild(this.holdButton);
      this.addDrawableChild(drop);
      PillButton dropAll = new PillButton(
         spots.get(4)[0], spots.get(4)[1], spots.get(4)[2], bh, "Wyrzuć całe eq", Theme.RED, this::canAct, () -> bot().throwAll()
      );
      dropAll.setTooltip(Tooltip.of(Text.literal("Wyrzuca po kolei wszystkie przedmioty z ekwipunku, zbroję i drugą rękę")));
      this.addDrawableChild(dropAll);
   }

   public void tick() {
      if (this.rebuild) {
         this.rebuild = false;
         boolean countFocused = this.getFocused() == this.countField;
         int cursor = this.countField.getCursor();
         this.clearAndInit();
         if (countFocused) {
            this.setFocused(this.countField);
            this.countField.setCursor(Math.min(cursor, this.countField.getText().length()), false);
         }
      }
   }

   private boolean canAct() {
      return online() && bot().inventory().registries() != null;
   }

   private static boolean anyOnline() {
      for (int i = 0; i < count(); i++) {
         Bot b = EasyBotClient.bots[i];
         if (b != null && b.state() == Bot.State.PLAY) {
            return true;
         }
      }

      return false;
   }

   private void switchTab(int t) {
      if (tab != t) {
         tab = t;
         this.selA = this.selB = null;
         this.selecting = false;
         this.clearAndInit();
      }
   }

   private void select(int i) {
      if (i != EasyBotClient.selected) {
         EasyBotClient.selected = i;
         scroll = 0;
         this.selA = this.selB = null;
         this.selecting = false;
      }
   }

   private void pick(int dir) {
      this.select(Math.floorMod(EasyBotClient.selected + dir, count()));
   }

   private void launch() {
      if (EasyBotClient.canLaunch()) {
         EasyBotClient.launch();
         scroll = 0;
         this.selA = this.selB = null;
         if (this.input != null) {
            this.setFocused(this.input);
         }
      }
   }

   private void sendChat() {
      String text = this.input.getText();
      if (!text.isBlank()) {
         boolean sent = false;
         if (count() > 1 && toAll) {
            for (int i = 0; i < count(); i++) {
               Bot b = EasyBotClient.bots[i];
               if (b != null && b.state() == Bot.State.PLAY) {
                  b.chat(text);
                  sent = true;
               }
            }
         } else if (online()) {
            bot().chat(text);
            sent = true;
         }

         if (sent) {
            if (history.isEmpty() || !history.getLast().equals(text)) {
               history.add(text);
            }

            this.historyPos = history.size();
            this.input.setText("");
            scroll = 0;
         }
      }
   }

   private KeyInput curKey;
   private Click curClick;
   private boolean curDoubled;
   private double curDx;
   private double curDy;

   private boolean ctrl() {
      long h = this.client.getWindow().getHandle();
      return GLFW.glfwGetKey(h, 341) == 1 || GLFW.glfwGetKey(h, 345) == 1;
   }

   private boolean shift() {
      long h = this.client.getWindow().getHandle();
      return GLFW.glfwGetKey(h, 340) == 1 || GLFW.glfwGetKey(h, 344) == 1;
   }

   public boolean keyPressed(KeyInput input) {
      this.curKey = input;
      return this.keyPressedOld(input.getKeycode(), 0, 0);
   }

   private boolean keyPressedOld(int key, int scancode, int modifiers) {
      if (this.curKey.isCopy() && this.hasSelection()) {
         boolean fieldSelection = this.getFocused() instanceof TextFieldWidget f && !f.getSelectedText().isEmpty();
         if (!fieldSelection) {
            this.copySelection();
            return true;
         }
      }

      if (tab == 1 && online() && this.guiKey(key, scancode)) {
         return true;
      } else {
         boolean enter = key == 257 || key == 335;
         if (enter && this.input != null && this.getFocused() == this.input) {
            this.sendChat();
            return true;
         } else if (enter && this.getFocused() instanceof TextFieldWidget) {
            this.launch();
            return true;
         } else if (this.input != null && this.getFocused() == this.input && (key == 265 || key == 264) && !history.isEmpty()) {
            this.historyPos = Math.clamp(this.historyPos + (key == 265 ? -1 : 1), 0, history.size());
            this.input.setText(this.historyPos < history.size() ? history.get(this.historyPos) : "");
            return true;
         } else {
            return super.keyPressed(this.curKey);
         }
      }
   }

   private boolean guiKey(int key, int scancode) {
      boolean typing = this.getFocused() instanceof TextFieldWidget;
      int digit = key >= 49 && key <= 57 ? key - 49 : -1;
      if (this.hoveredSlot >= 0) {
         if (digit >= 0) {
            bot().click(this.hoveredSlot, digit, SlotActionType.SWAP);
            return true;
         }

         if (this.client.options.swapHandsKey.matchesKey(this.curKey)) {
            bot().click(this.hoveredSlot, 40, SlotActionType.SWAP);
            return true;
         }

         if (this.client.options.dropKey.matchesKey(this.curKey) && !typing) {
            bot().click(this.hoveredSlot, this.ctrl() ? 1 : 0, SlotActionType.THROW);
            return true;
         }
      } else if (digit >= 0 && !typing) {
         bot().select(digit);
         return true;
      }

      return false;
   }

   private int tileAt(double mx, double my) {
      for (int i = 0; i < this.tiles.size(); i++) {
         if (this.tiles.get(i).in(mx, my)) {
            return i;
         }
      }

      return -1;
   }

   private double localX(double mx) {
      return this.sx + (mx - this.sx) / this.ks;
   }

   private double localY(double my) {
      return this.sy + (my - this.sy) / this.ks;
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      this.curClick = click;
      this.curDoubled = doubled;
      return this.mouseClickedOld(click.x(), click.y(), click.button());
   }

   private boolean mouseClickedOld(double mx, double my, int button) {
      for (int i = 0; i < 2; i++) {
         if (this.inTab(i, mx, my)) {
            this.switchTab(i);
            return true;
         }
      }

      if (tab == 0) {
         int t = this.tileAt(mx, my);
         if (t >= 0) {
            boolean was = t == EasyBotClient.selected;
            this.select(t);
            BotScreen.Tile tile = this.tiles.get(t);
            if (my < tile.textY() && ConsoleOpener.headerClick(this, this.client, t)) {
               return true;
            }

            if (my >= tile.textY()) {
               this.setFocused(null);
               if (button == 1 && this.hasSelection()) {
                  this.copySelection();
                  return true;
               }

               if (button == 0 && was && !this.wrapped.isEmpty()) {
                  this.setBounds(tile);
                  int[] hit = this.hit(this.localX(mx), this.localY(my));
                  long now = System.currentTimeMillis();
                  if (now - this.lastConsoleClick < 250L && this.selA != null && hit[0] == this.selA[0]) {
                     this.selectWord(hit);
                     this.selecting = false;
                  } else {
                     this.selA = hit;
                     this.selB = hit;
                     this.selecting = true;
                  }

                  this.lastConsoleClick = now;
               }

               return true;
            }
         }
      }

      if (tab == 1 && online()) {
         for (BotScreen.Hot h : this.hots) {
            if (h.in(mx, my)) {
               if (h.kind() == 1) {
                  bot().select(h.slot());
                  return true;
               }

               this.slotClick(h.slot(), button);
               return true;
            }
         }
      }

      if (super.mouseClicked(this.curClick, this.curDoubled)) {
         return true;
      } else if (tab == 1
         && online()
         && mx >= this.ax
         && mx < this.ax + this.aw
         && my >= this.ay
         && my < this.ay + this.ah
         && !bot().inventory().cursor().isEmpty()
         && button <= 1) {
         bot().click(-999, button, SlotActionType.PICKUP);
         return true;
      } else {
         return false;
      }
   }

   private void slotClick(int slot, int button) {
      long now = System.currentTimeMillis();
      boolean dbl = button == 0 && slot == this.lastSlot && now - this.lastSlotClick < 250L;
      this.lastSlot = slot;
      this.lastSlotClick = now;
      if (button == 2) {
         bot().click(slot, 2, SlotActionType.CLONE);
      } else if (this.shift()) {
         bot().click(slot, button, SlotActionType.QUICK_MOVE);
      } else if (dbl && !bot().inventory().cursor().isEmpty()) {
         bot().click(slot, 0, SlotActionType.PICKUP_ALL);
      } else if (button <= 1) {
         bot().click(slot, button, SlotActionType.PICKUP);
      }
   }

   public boolean mouseDragged(Click click, double dx, double dy) {
      this.curClick = click;
      this.curDx = dx;
      this.curDy = dy;
      return this.mouseDraggedOld(click.x(), click.y(), click.button(), dx, dy);
   }

   private boolean mouseDraggedOld(double mx, double my, int button, double dx, double dy) {
      if (this.selecting && tab == 0) {
         if (my < this.sy) {
            scroll = Math.clamp(scroll + 1, 0, this.maxScroll());
         } else if (my > this.sy + this.sh) {
            scroll = Math.clamp(scroll - 1, 0, this.maxScroll());
         }

         this.selB = this.hit(this.localX(mx), this.localY(my));
         return true;
      } else {
         return super.mouseDragged(this.curClick, this.curDx, this.curDy);
      }
   }

   public boolean mouseReleased(Click click) {
      this.curClick = click;
      return this.mouseReleasedOld(click.x(), click.y(), click.button());
   }

   private boolean mouseReleasedOld(double mx, double my, int button) {
      if (this.selecting) {
         this.selecting = false;
         if (this.selA != null && this.selB != null && this.selA[0] == this.selB[0] && this.selA[1] == this.selB[1]) {
            this.selA = this.selB = null;
         }

         return true;
      } else {
         return super.mouseReleased(this.curClick);
      }
   }

   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      if (tab == 0 && this.main != null && this.main.in(mx, my)) {
         mainScroll = Math.max(0, mainScroll + (int)Math.signum(vertical) * 3);
         return true;
      }

      if (tab == 0) {
         int t = this.tileAt(mx, my);
         if (t >= 0 && my >= this.tiles.get(t).textY()) {
            this.select(t);
            scroll = Math.clamp(scroll + (int)Math.signum(vertical) * 3, 0, this.maxScroll());
            return true;
         }
      }

      if (tab == 1 && online() && mx >= this.rightX && mx < this.rightX + 162 && my >= this.ay && my < this.ay + this.ah && vertical != 0.0) {
         int sel = bot().inventory().selected();
         bot().select(Math.floorMod(sel - (int)Math.signum(vertical), 9));
         return true;
      } else {
         return super.mouseScrolled(mx, my, horizontal, vertical);
      }
   }

   private int tabX(int i) {
      return i == 0 ? this.ax : this.ax + this.textRenderer.getWidth("Konsola") + 16;
   }

   private String tabLabel(int i) {
      return i == 0 ? "Konsola" : "GUI bota";
   }

   private boolean inTab(int i, double mx, double my) {
      int x = this.tabX(i);
      return mx >= x - 2 && mx < x + this.textRenderer.getWidth(this.tabLabel(i)) + 12 && my >= this.tabsY && my < this.tabsY + 14;
   }

   private int visibleLines() {
      return Math.max(1, (this.ch - (this.ks < 1.0F ? 6 : 8)) / 10);
   }

   private int maxScroll() {
      return Math.max(0, this.wrapped.size() - this.visibleLines());
   }

   public void close() {
      EasyBotClient.save();
      this.client.setScreen(this.parent);
   }

   public void render(DrawContext c, int mx, int my, float delta) {
      for (int i = 0; i < this.nickFields.size(); i++) {
         this.nickFields.get(i).setEditable(!EasyBotClient.busy(i));
      }

      if (this.closeButton != null) {
         this.closeButton.visible = online() && bot().inventory().window() != null;
      }

      if (this.holdButton != null && bot() != null) {
         boolean using = bot().inventory().using();
         boolean wide = this.midW >= 84;
         this.holdButton.setMessage(Text.literal(using ? (wide ? "Puść PPM" : "Puść") : (wide ? "Trzymaj PPM" : "Trzymaj")));
      }

      super.render(c, mx, my, delta);
      if (tab == 1 && bot() != null) {
         Bot.Inventory inv = bot().inventory();
         if (!inv.cursor().isEmpty()) {
            c.getMatrices().pushMatrix();
            c.getMatrices().translate(0.0F, 0.0F);
            c.drawItem(inv.cursor(), mx - 8, my - 8);
            c.drawStackOverlay(this.textRenderer, inv.cursor(), mx - 8, my - 8);
            c.getMatrices().popMatrix();
         } else if (!this.hoveredStack.isEmpty()) {
            try {
               List<Text> lines = this.hoveredStack
                  .getTooltip(
                     TooltipContext.create(inv.registries()), null, this.client.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC
                  );
               c.drawTooltip(this.textRenderer, lines, this.hoveredStack.getTooltipData(), mx, my);
            } catch (Exception e) {
               c.drawTooltip(this.textRenderer, this.hoveredStack.getName(), mx, my);
            }
         }
      }
   }

   public void renderBackground(DrawContext c, int mx, int my, float delta) {
      super.renderBackground(c, mx, my, delta);
      Draw.shadow(c, this.px, this.py, this.pw, this.ph);
      Draw.box(c, this.px, this.py, this.pw, this.ph, Theme.PANEL, Theme.OUTLINE);
      c.fillGradient(this.px + 1, this.py + 1, this.px + this.pw - 1, this.py + 30, Theme.HEAD_TOP, Theme.HEAD_BOTTOM);
      c.fill(this.px + 1, this.py + 30, this.px + this.pw - 1, this.py + 30 + 1, Theme.OUTLINE);
      Text title = Text.literal("EasyBot").formatted(Formatting.field_1067);
      c.drawText(this.textRenderer, title, this.px + 10, this.py + 11, Theme.TEXT, false);
      int titleEnd = this.px + 10 + this.textRenderer.getWidth(title) + 6;
      int statusX = this.status(c);
      String server = EasyBotClient.address.isBlank() ? "brak adresu" : EasyBotClient.address;
      c.drawText(this.textRenderer, Draw.trim(this.textRenderer, "· " + server, statusX - 16 - titleEnd), titleEnd, this.py + 11, Theme.TEXT_DIM, false);
      int rowY = this.py + 30 + 8;
      Draw.box(c, this.px + 8, rowY, this.addrW, 20, Theme.FIELD, this.address.isFocused() ? Theme.ACCENT_DARK : Theme.OUTLINE);
      Draw.box(c, this.countX, rowY, 54, 20, Theme.FIELD, this.countField.isFocused() ? Theme.ACCENT_DARK : Theme.OUTLINE);
      c.drawText(this.textRenderer, "Boty:", this.countX + 6, rowY + 6, Theme.TEXT_DIM, false);
      this.tabs(c, mx, my);
      if (tab == 0) {
         for (int i = 0; i < this.tiles.size(); i++) {
            this.tile(c, i, mx, my);
         }

         if (this.main != null) {
            this.mainWindow(c, mx, my);
         }

         int inputY = this.py + this.ph - 8 - 20;
         int inputW = this.aw - 80 - (count() > 1 ? 98 : 0);
         Draw.box(c, this.px + 8, inputY, inputW, 20, Theme.FIELD, this.input.isFocused() ? Theme.ACCENT_DARK : Theme.OUTLINE);
      } else {
         this.gui(c, mx, my);
      }
   }

   private void tabs(DrawContext c, int mx, int my) {
      Bot bot = bot();
      boolean windowOpen = bot != null && bot.inventory().window() != null;

      for (int i = 0; i < 2; i++) {
         int x = this.tabX(i);
         boolean on = tab == i;
         boolean hover = this.inTab(i, mx, my);
         c.drawText(this.textRenderer, this.tabLabel(i), x, this.tabsY + 3, on ? Theme.TEXT : (hover ? Draw.lighten(Theme.TEXT_DIM) : Theme.TEXT_DIM), false);
         int w = this.textRenderer.getWidth(this.tabLabel(i));
         if (i == 1 && windowOpen) {
            Draw.rect(c, x + w + 4, this.tabsY + 4, 5, 6, -10696449);
         }

         if (on) {
            c.fill(x, this.tabsY + 13, x + w, this.tabsY + 15, Theme.ACCENT);
         }
      }

      c.fill(this.ax, this.tabsY + 15, this.ax + this.aw, this.tabsY + 16, Theme.OUTLINE);
      int leftEnd = this.tabX(1) + this.textRenderer.getWidth(this.tabLabel(1)) + 12;
      if (this.pickerX >= 0) {
         int i = EasyBotClient.selected;
         String n = EasyBotClient.nicks[i] != null && !EasyBotClient.nicks[i].isEmpty() ? EasyBotClient.nicks[i] : "Bot " + (i + 1);
         n = Draw.trim(this.textRenderer, i + 1 + ". " + n, 90);
         c.drawText(this.textRenderer, n, this.pickerX + 18 + (92 - this.textRenderer.getWidth(n)) / 2, this.tabsY + 2, Theme.TEXT, false);
         leftEnd = this.pickerX + 18 + 96 + 14 + 10;
      }

      String right = null;
      int color = Theme.TEXT_FAINT;
      String problem = EasyBotClient.problem();
      boolean typed = EasyBotClient.nicks[0] != null && !EasyBotClient.nicks[0].isEmpty() || count() > 1;
      if (problem != null && typed && EasyBotClient.hasIdle()) {
         right = problem;
         color = Theme.ORANGE;
      } else if (System.currentTimeMillis() - this.copiedAt < 1500L) {
         right = "Skopiowano do schowka";
         color = Theme.ACCENT;
      } else if (tab == 0 && this.hasSelection()) {
         right = "Ctrl+C lub PPM - kopiuj";
      } else if (tab == 1 && online()) {
         right = "Shift - szybkie przenoszenie · 1-9 - zamiana · Q - wyrzuć";
      } else if (tab == 0 && count() > 1) {
         right = "Kliknij konsolę, żeby wybrać bota";
      }

      if (right != null) {
         right = Draw.trim(this.textRenderer, right, this.ax + this.aw - leftEnd);
         c.drawText(this.textRenderer, right, this.ax + this.aw - this.textRenderer.getWidth(right), this.tabsY + 3, color, false);
      }
   }

   private int status(DrawContext c) {
      Bot bot = bot();
      Bot.State state = bot == null ? Bot.State.OFFLINE : bot.state();
      String label = bot == null ? (EasyBotClient.queued(EasyBotClient.selected) ? "W kolejce" : "Brak bota") : state.label;
      if (bot != null && state == Bot.State.PLAY) {
         long s = bot.onlineMillis() / 1000L;
         label = label + String.format(" · %d:%02d", s / 60L, s % 60L);
         String pos = bot.position();
         if (pos != null) {
            label = label + " · " + pos;
         }
      }

      if (count() > 1) {
         label = "Online " + EasyBotClient.online() + "/" + count() + " · " + (EasyBotClient.selected + 1) + ": " + label;
      }

      int w = this.textRenderer.getWidth(label);
      int x = this.px + this.pw - 10 - w;
      c.drawText(this.textRenderer, label, x, this.py + 11, state == Bot.State.OFFLINE ? Theme.TEXT_DIM : Theme.TEXT, false);
      Draw.rect(c, x - 10, this.py + 11, 6, 7, bot == null ? Theme.OFF : state.color);
      return x - 10;
   }

   private void center(DrawContext c, String a, String b, int x, int y, int w, int h) {
      c.drawText(this.textRenderer, a, x + (w - this.textRenderer.getWidth(a)) / 2, y + h / 2 - 10, Theme.TEXT_DIM, false);
      c.drawText(this.textRenderer, b, x + (w - this.textRenderer.getWidth(b)) / 2, y + h / 2 + 2, Theme.TEXT_FAINT, false);
   }

   private void setBounds(BotScreen.Tile t) {
      this.ks = t.scale();
      this.sx = t.x() + 1;
      this.sy = t.textY();
      this.sw = t.w() - 2;
      this.sh = t.textH() - 1;
      this.cx = this.sx;
      this.cy = this.sy;
      this.cw = (int)(this.sw / this.ks);
      this.ch = (int)(this.sh / this.ks);
   }

   private void tile(DrawContext c, int i, int mx, int my) {
      BotScreen.Tile t = this.tiles.get(i);
      boolean sel = i == EasyBotClient.selected;
      boolean hover = t.in(mx, my);
      Bot bot = EasyBotClient.bots[i];
      TextFieldWidget field = this.nickFields.get(i);
      Draw.box(c, t.x(), t.y(), t.w(), t.h(), Theme.FIELD, sel && count() > 1 ? Theme.ACCENT_DARK : (field.isFocused() ? Theme.ACCENT_DARK : (hover ? Theme.OUTLINE_HOVER : Theme.OUTLINE)));
      c.fill(t.x() + 1, t.y() + 16 - 1, t.x() + t.w() - 1, t.y() + 16, Theme.OUTLINE);
      int dot = EasyBotClient.queued(i) ? Theme.GOLD : (bot == null ? Theme.OFF : bot.state().color);
      Draw.rect(c, t.x() + t.w() - 12, t.y() + 5, 6, 7, dot);
      BotScreen.Cache cache = this.caches.computeIfAbsent(i, k -> new BotScreen.Cache());
      int innerW = (int)((t.w() - 2) / t.scale()) - (t.scale() < 1.0F ? 8 : 14);
      if (bot != cache.bot || bot != null && bot.version() != cache.version || innerW != cache.width) {
         int before = cache.lines.size();
         if (sel && (bot != cache.bot || innerW != cache.width)) {
            this.selA = this.selB = null;
         }

         cache.bot = bot;
         cache.version = bot == null ? -1 : bot.version();
         cache.width = innerW;
         cache.lines = bot == null ? List.of() : this.wrap(bot, innerW);
         if (sel) {
            this.offsets.clear();
            if (cache.lines.size() < before) {
               this.selA = this.selB = null;
            }

            if (scroll > 0 && !this.selecting) {
               scroll = Math.max(0, scroll + cache.lines.size() - before);
            }
         }
      }

      if (sel) {
         this.wrapped = cache.lines;
         this.setBounds(t);
         scroll = Math.clamp(scroll, 0, this.maxScroll());
      }

      int bx = t.x() + 1;
      int by = t.textY();
      int bw = t.w() - 2;
      int bh = t.textH() - 1;
      if (bot == null) {
         if (count() == 1) {
            this.center(
               c,
               "Wpisz nick i kliknij „Wyślij bota”.",
               "Bot wejdzie na "
                  + (EasyBotClient.address.isBlank() ? "podany serwer" : EasyBotClient.address)
                  + ", a tutaj zobaczysz wszystko, co się z nim dzieje.",
               bx,
               by,
               bw,
               bh
            );
         } else {
            String s = EasyBotClient.queued(i) ? "W kolejce..." : "Nie wysłany";
            c.getMatrices().pushMatrix();
            c.getMatrices().translate(bx + 4, by + 4);
            c.getMatrices().scale(t.scale(), t.scale());
            c.drawText(this.textRenderer, s, 0, 0, Theme.TEXT_FAINT, false);
            c.getMatrices().popMatrix();
         }
      } else {
         float s = t.scale();
         int lw = (int)(bw / s);
         int lh = (int)(bh / s);
         int pad = s < 1.0F ? 3 : 4;
         int visible = Math.max(1, (lh - pad * 2) / 10);
         List<BotScreen.WLine> lines = cache.lines;
         int off = sel ? Math.min(scroll, Math.max(0, lines.size() - visible)) : 0;
         int end = lines.size() - off;
         int start = Math.max(0, end - visible);
         c.enableScissor(bx, by, bx + bw, by + bh);
         c.getMatrices().pushMatrix();
         c.getMatrices().translate(bx, by);
         c.getMatrices().scale(s, s);
         c.getMatrices().translate(-bx, -by);
         int y = by + pad + (visible - (end - start)) * 10;
         if (sel) {
            this.drawStart = start;
            this.drawEnd = end;
            this.drawY0 = y;
         }

         int[] a = null;
         int[] b = null;
         if (sel && this.hasSelection()) {
            boolean swap = this.selA[0] > this.selB[0] || this.selA[0] == this.selB[0] && this.selA[1] > this.selB[1];
            a = swap ? this.selB : this.selA;
            b = swap ? this.selA : this.selB;
         }

         for (int k = start; k < end; k++) {
            BotScreen.WLine line = lines.get(k);
            if (a != null && k >= a[0] && k <= b[0]) {
               int[] o = this.offsets(line);
               int from = k == a[0] ? a[1] : 0;
               int to = k == b[0] ? b[1] : line.cps().length;
               int x0 = bx + 4 + o[Math.min(from, o.length - 1)];
               int x1 = bx + 4 + o[Math.min(to, o.length - 1)] + (k < b[0] ? 3 : 0);
               if (x1 > x0) {
                  c.fill(x0, y - 1, x1, y + 10 - 1, -1439868002);
               }
            }

            c.drawText(this.textRenderer, line.text(), bx + 4, y, Theme.TEXT, false);
            y += 10;
         }

         if (lines.size() > visible) {
            int trackH = lh - 4;
            int thumbH = Math.max(12, trackH * visible / lines.size());
            int max = Math.max(1, lines.size() - visible);
            int thumbY = by + 2 + (trackH - thumbH) * (max - off) / max;
            c.fill(bx + lw - 4, thumbY, bx + lw - 2, thumbY + thumbH, off > 0 ? Theme.TEXT_DIM : Theme.OUTLINE_HOVER);
         }

         c.getMatrices().popMatrix();
         c.disableScissor();
      }
   }

   private List<BotScreen.WLine> wrap(Bot bot, int innerW) {
      List<BotScreen.WLine> out = new ArrayList<>();

      for (Bot.Line line : ErrorsClient.lines(bot)) {
         Text full = Text.literal(TIME.format(Instant.ofEpochMilli(line.time())) + " ")
            .withColor(Theme.TEXT_FAINT)
            .append(Text.empty().withColor(Theme.TEXT).append(line.text()));
         boolean first = true;

         for (OrderedText t : this.textRenderer.wrapLines(full, Math.max(20, innerW))) {
            List<Integer> cps = new ArrayList<>();
            t.accept((index, style, cp) -> {
               cps.add(cp);
               return true;
            });
            out.add(new BotScreen.WLine(t, cps.stream().mapToInt(Integer::intValue).toArray(), first));
            first = false;
         }
      }

      return out;
   }

   private int[] offsets(BotScreen.WLine line) {
      return this.offsets.computeIfAbsent(line, l -> {
         int[] off = new int[l.cps().length + 1];
         int[] i = new int[]{0};
         l.text().accept((index, style, cp) -> {
            off[i[0] + 1] = off[i[0]] + this.textRenderer.getWidth(OrderedText.styled(cp, style));
            i[0]++;
            return true;
         });
         return off;
      });
   }

   private int[] hit(double mx, double my) {
      if (this.wrapped.isEmpty()) {
         return new int[]{0, 0};
      }

      int line = this.drawStart + (int)Math.floor((my - this.drawY0) / 10.0);
      if (my < this.cy) {
         line = this.drawStart;
      }

      if (line < this.drawStart) {
         line = this.drawStart;
      }

      if (line >= this.drawEnd) {
         line = Math.max(this.drawStart, this.drawEnd - 1);
         if (my > this.cy + this.ch || my >= this.drawY0 + (this.drawEnd - this.drawStart) * 10) {
            return new int[]{line, this.wrapped.get(line).cps().length};
         }
      }

      int[] off = this.offsets(this.wrapped.get(line));
      double x = mx - (this.cx + 4);
      int col = off.length - 1;

      for (int i = 0; i < off.length - 1; i++) {
         if (x < (off[i] + off[i + 1]) / 2.0) {
            col = i;
            break;
         }
      }

      return new int[]{line, col};
   }

   private void selectWord(int[] hit) {
      int[] cps = this.wrapped.get(hit[0]).cps();
      int s = Math.min(hit[1], cps.length);
      int e = s;

      while (s > 0 && !Character.isWhitespace(cps[s - 1])) {
         s--;
      }

      while (e < cps.length && !Character.isWhitespace(cps[e])) {
         e++;
      }

      this.selA = new int[]{hit[0], s};
      this.selB = new int[]{hit[0], e};
   }

   private boolean hasSelection() {
      return tab == 0
         && this.selA != null
         && this.selB != null
         && (this.selA[0] != this.selB[0] || this.selA[1] != this.selB[1])
         && this.selA[0] < this.wrapped.size()
         && this.selB[0] < this.wrapped.size();
   }

   private void copySelection() {
      boolean swap = this.selA[0] > this.selB[0] || this.selA[0] == this.selB[0] && this.selA[1] > this.selB[1];
      int[] a = swap ? this.selB : this.selA;
      int[] b = swap ? this.selA : this.selB;
      StringBuilder sb = new StringBuilder();

      for (int i = a[0]; i <= b[0]; i++) {
         int[] cps = this.wrapped.get(i).cps();
         int from = Math.min(i == a[0] ? a[1] : 0, cps.length);
         int to = Math.min(i == b[0] ? b[1] : cps.length, cps.length);
         if (to > from) {
            sb.append(new String(cps, from, to - from));
         }

         if (i < b[0]) {
            sb.append(this.wrapped.get(i + 1).start() ? "\n" : " ");
         }
      }

      this.client.keyboard.setClipboard(sb.toString());
      this.copiedAt = System.currentTimeMillis();
   }

   private void gui(DrawContext c, int mx, int my) {
      this.hots.clear();
      this.hoveredSlot = -1;
      this.hoveredStack = ItemStack.EMPTY;
      Draw.box(c, this.ax, this.ay, this.aw, this.ah, Theme.FIELD, Theme.OUTLINE);
      Bot bot = bot();
      if (!online()) {
         this.center(c, "Bot nie jest na serwerze.", "Wyślij bota, a tutaj zobaczysz jego ekwipunek i otwarte GUI.", this.ax, this.ay, this.aw, this.ah);
      } else {
         Bot.Inventory inv = bot.inventory();
         if (inv.registries() == null) {
            this.center(
               c, "Podgląd ekwipunku niedostępny.", "Nie udało się wczytać rejestrów serwera - szczegóły w konsoli.", this.ax, this.ay, this.aw, this.ah
            );
         } else {
            c.enableScissor(this.ax + 1, this.ay + 1, this.ax + this.aw - 1, this.ay + this.ah - 1);
            Bot.Window w = inv.window();
            c.drawText(this.textRenderer, w != null ? "OTWARTE GUI" : "BRAK OTWARTEGO GUI", this.leftX + 1, this.ay + 4, Theme.TEXT_FAINT, false);
            ItemStack[] shown = w != null && w.slots() != null ? w.slots() : inv.player();
            int playerBase = w != null && w.slots() != null ? w.slots().length - 36 : 9;
            if (w != null) {
               Identifier typeId = Registries.SCREEN_HANDLER.getId(w.type());
               Text wt = Text.empty().append(w.title());
               if (wt.getString().isBlank()) {
                  c.drawText(this.textRenderer, "(bez tytułu)", this.leftX + 1, this.ay + 17, Theme.TEXT_DIM, false);
               } else {
                  c.drawText(this.textRenderer, (OrderedText)this.textRenderer.wrapLines(wt, 162).getFirst(), this.leftX + 1, this.ay + 17, Theme.TEXT, false);
               }

               if (typeId != null) {
                  c.drawText(this.textRenderer, typeId.getPath(), this.leftX + 1, this.ay + 28, Theme.TEXT_FAINT, false);
               }

               if (w.slots() == null) {
                  c.drawText(this.textRenderer, "Wczytywanie zawartości...", this.leftX + 1, this.ay + 44, Theme.TEXT_DIM, false);
               } else {
                  int n = Math.max(0, w.slots().length - 36);
                  int cols = n % 9 == 0 ? 9 : Math.min(n, 9);

                  for (int i = 0; i < n; i++) {
                     this.slot(c, mx, my, this.leftX + i % cols * 18, this.ay + 40 + i / cols * 18, i, w.slots()[i], false);
                  }
               }
            } else {
               c.drawText(this.textRenderer, "Bot nie ma otwartego okna.", this.leftX + 1, this.ay + 17, Theme.TEXT_DIM, false);
               c.drawText(this.textRenderer, "Zbroja i druga ręka", this.leftX + 1, this.ay + 34, Theme.TEXT_FAINT, false);

               for (int i = 0; i < 4; i++) {
                  this.slot(c, mx, my, this.leftX + i * 18, this.ay + 45, 5 + i, inv.player()[5 + i], false);
               }

               this.slot(c, mx, my, this.leftX + 72 + 8, this.ay + 45, 45, inv.player()[45], false);
               c.drawText(this.textRenderer, "Craftowanie 2x2", this.leftX + 1, this.ay + 70, Theme.TEXT_FAINT, false);

               for (int i = 0; i < 4; i++) {
                  this.slot(c, mx, my, this.leftX + i % 2 * 18, this.ay + 81 + i / 2 * 18, 1 + i, inv.player()[1 + i], false);
               }

               c.drawText(this.textRenderer, "→", this.leftX + 36 + 6, this.ay + 81 + 13, Theme.TEXT_DIM, false);
               this.slot(c, mx, my, this.leftX + 36 + 20, this.ay + 81 + 9, 0, inv.player()[0], false);
            }

            c.drawText(this.textRenderer, "EKWIPUNEK BOTA", this.rightX + 1, this.ay + 4, Theme.TEXT_FAINT, false);

            for (int i = 0; i < 27; i++) {
               this.slot(c, mx, my, this.rightX + i % 9 * 18, this.ay + 16 + i / 9 * 18, playerBase + i, shown[playerBase + i], false);
            }

            int hotY = this.ay + 16 + 54 + 4;

            for (int i = 0; i < 9; i++) {
               this.slot(c, mx, my, this.rightX + i * 18, hotY, playerBase + 27 + i, shown[playerBase + 27 + i], i == inv.selected());
            }

            int selY = hotY + 18 + 2;

            for (int i = 0; i < 9; i++) {
               int x = this.rightX + i * 18 + 1;
               boolean on = i == inv.selected();
               boolean hover = mx >= x && mx < x + 18 - 2 && my >= selY && my < selY + 10;
               Draw.box(c, x, selY, 16, 10, on ? Theme.ACCENT : (hover ? Theme.ROW_HOVER : Theme.SUB), on ? Theme.ACCENT : Theme.OUTLINE);
               String d = String.valueOf(i + 1);
               c.drawText(this.textRenderer, d, x + (16 - this.textRenderer.getWidth(d)) / 2, selY + 1, on ? Theme.INK : Theme.TEXT_DIM, false);
               this.hots.add(new BotScreen.Hot(x, selY, 16, 10, i, 1));
            }

            ItemStack held = inv.player()[36 + inv.selected()];
            if (this.midW >= 84) {
               c.drawText(this.textRenderer, "W RĘCE", this.midX + (this.midW - this.textRenderer.getWidth("W RĘCE")) / 2, this.ay + 4, Theme.TEXT_FAINT, false);
               int bx = this.midX + (this.midW - 36) / 2;
               Draw.box(c, bx, this.ay + 16, 36, 36, Theme.SUB, inv.using() ? Theme.GOLD : Theme.OUTLINE);
               if (!held.isEmpty()) {
                  c.getMatrices().pushMatrix();
                  c.getMatrices().translate(bx + 2, this.ay + 18);
                  c.getMatrices().scale(2.0F, 2.0F);
                  c.drawItem(held, 0, 0);
                  c.drawStackOverlay(this.textRenderer, held, 0, 0);
                  c.getMatrices().popMatrix();
               }

               String name = held.isEmpty() ? "pusta ręka" : held.getName().getString();
               name = Draw.trim(this.textRenderer, name, this.midW);
               c.drawText(
                  this.textRenderer,
                  name,
                  this.midX + (this.midW - this.textRenderer.getWidth(name)) / 2,
                  this.ay + 55,
                  held.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT,
                  false
               );
            } else {
               String name = "W ręce: " + (held.isEmpty() ? "nic" : held.getName().getString());
               c.drawText(this.textRenderer, Draw.trim(this.textRenderer, name, 162), this.rightX + 1, selY + 13, Theme.TEXT_DIM, false);
            }

            if (!inv.cursor().isEmpty()) {
               String s = "Na kursorze: " + inv.cursor().getName().getString() + " (kliknij obok, by wyrzucić)";
               c.drawText(this.textRenderer, Draw.trim(this.textRenderer, s, this.aw - 12), this.ax + 5, this.ay + this.ah - 12, Theme.GOLD, false);
            }

            c.disableScissor();
         }
      }
   }

   private void slot(DrawContext c, int mx, int my, int x, int y, int index, ItemStack stack, boolean selected) {
      boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
      Draw.box(c, x, y, 18, 18, hover ? Theme.ROW_HOVER : Theme.SUB, selected ? Theme.ACCENT : Theme.OUTLINE);
      if (!stack.isEmpty()) {
         c.drawItem(stack, x + 1, y + 1);
         c.drawStackOverlay(this.textRenderer, stack, x + 1, y + 1);
      }

      if (hover) {
         this.hoveredSlot = index;
         this.hoveredStack = stack;
      }

      this.hots.add(new BotScreen.Hot(x, y, 18, 18, index, 0));
   }

   private static final class Cache {
      Bot bot;
      int version = -1;
      int width = -1;
      List<BotScreen.WLine> lines = List.of();
   }

   private record Entry(long time, int bot, Text text) {
   }

   private record Hot(int x, int y, int w, int h, int slot, int kind) {
      boolean in(double mx, double my) {
         return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
      }
   }

   private record Tile(int x, int y, int w, int h, float scale) {
      boolean in(double mx, double my) {
         return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
      }

      int textY() {
         return this.y + 16;
      }

      int textH() {
         return this.h - 16;
      }
   }

   private record WLine(OrderedText text, int[] cps, boolean start) {
   }
}
