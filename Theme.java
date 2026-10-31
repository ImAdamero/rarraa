package pl.easybot.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Motywy kolorystyczne GUI. Kolory sa polami (nie stalymi), zeby dalo sie je zmieniac w locie. */
public final class Theme {
   public static int PANEL = -233827559;
   public static int PANEL_SOLID = -15657959;
   public static int HEAD_TOP = -14933455;
   public static int HEAD_BOTTOM = -15591904;
   public static int SUB = -15986924;
   public static int FIELD = -16250354;
   public static int ROW_HOVER = -15196630;
   public static int ROW_SELECTED = -15063754;
   public static int OUTLINE = -14275269;
   public static int OUTLINE_HOVER = -12892839;
   public static int ACCENT = -13643391;
   public static int ACCENT_DARK = -14910899;
   public static int GOLD = -865972;
   public static int RED = -1090725;
   public static int RED_DARK = -8769997;
   public static int ORANGE = -878004;
   public static int TEXT = -1512205;
   public static int TEXT_DIM = -7629657;
   public static int TEXT_FAINT = -10919566;
   public static int OFF = -13288632;
   public static int INK = -16379124;

   public static final String[] NAMES = {"Noc", "Granat", "Fiolet", "Różany", "Bursztyn", "Lód", "AMOLED"};
   private static final int[][] SETS = {
      {-233827559, -15657959, -14933455, -15591904, -15986924, -16250354, -15196630, -15063754, -14275269, -12892839, -13643391, -14910899, -865972, -1090725, -8769997, -878004, -1512205, -7629657, -10919566, -13288632, -16379124},
      {-233827303, -15657959, -14932687, -15591392, -15986924, -16250354, -15196374, -15063498, -14274757, -12892327, -14186791, -15249793, -865972, -1090725, -8769997, -878004, -1511949, -7628889, -10919054, -13288120, -16446188},
      {-233566183, -15462119, -14345167, -15199712, -15791084, -16119794, -14739414, -14280138, -13687237, -12108967, -6741027, -10939007, -865972, -1090725, -8769997, -878004, -1185549, -6845529, -10266254, -12765880, -15858412},
      {-233238508, -15134443, -13558745, -14675431, -15463408, -15857653, -14018527, -13231576, -12900815, -10929334, -2023568, -8187070, -865972, -1090725, -8769997, -878004, -792338, -5796967, -9283226, -12044993, -15465205},
      {-233237488, -15133679, -13556452, -14673902, -15462644, -15857144, -14016744, -13228518, -12898522, -10926021, -2780885, -8628966, -865972, -1090725, -8769997, -878004, -791064, -5793909, -9280679, -12042955, -15528442},
      {-233826535, -15656935, -14930639, -15589856, -15985900, -16249586, -15194326, -15060682, -14272709, -12888999, -13647407, -14913158, -865972, -1090725, -8769997, -878004, -1510925, -7626073, -10916238, -13286072, -16379373},
      {-234288886, -16185078, -15724269, -16119028, -16316664, -16513787, -15921648, -15724268, -15461097, -14737116, -13643392, -14910901, -865972, -1090725, -8769997, -878004, -1512205, -7629657, -10919566, -15000546, -16379123}
   };
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("easybot-theme.txt");
   private static int index;

   static {
      int i = 0;
      try {
         if (Files.exists(FILE)) {
            i = Integer.parseInt(Files.readString(FILE).strip());
         }
      } catch (Exception ignored) {
      }

      apply(i);
   }

   private Theme() {
   }

   public static int index() {
      return index;
   }

   public static String name() {
      return NAMES[index];
   }

   public static void next() {
      apply(index + 1);
      try {
         Files.writeString(FILE, String.valueOf(index));
      } catch (Exception ignored) {
      }
   }

   public static void apply(int i) {
      index = Math.floorMod(i, SETS.length);
      int[] s = SETS[index];
      PANEL = s[0];
      PANEL_SOLID = s[1];
      HEAD_TOP = s[2];
      HEAD_BOTTOM = s[3];
      SUB = s[4];
      FIELD = s[5];
      ROW_HOVER = s[6];
      ROW_SELECTED = s[7];
      OUTLINE = s[8];
      OUTLINE_HOVER = s[9];
      ACCENT = s[10];
      ACCENT_DARK = s[11];
      GOLD = s[12];
      RED = s[13];
      RED_DARK = s[14];
      ORANGE = s[15];
      TEXT = s[16];
      TEXT_DIM = s[17];
      TEXT_FAINT = s[18];
      OFF = s[19];
      INK = s[20];
   }
}
