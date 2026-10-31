package pl.easybot.gui;

import net.minecraft.client.gui.widget.TextFieldWidget;
import pl.easybot.EasyBotClient;

public final class NickSync {
   private static final TextFieldWidget[] FIELDS = new TextFieldWidget[50];
   private static boolean propagating;

   private NickSync() {
   }

   public static String initial(TextFieldWidget var0, int var1) {
      String[] var2 = EasyBotClient.nicks;
      if (var1 >= 0 && var1 < var2.length) {
         FIELDS[var1] = var0;
         String var3 = var2[var1];
         if (var3 != null && !var3.isEmpty()) {
            return var3;
         }

         if (var1 > 0) {
            String var4 = derive(var2[0], var1);
            if (!var4.isEmpty()) {
               var2[var1] = var4;
               return var4;
            }
         }

         return "";
      } else {
         return "";
      }
   }

   public static void onNick(int var0, String var1) {
      String[] var2 = EasyBotClient.nicks;
      if (var0 >= 0 && var0 < var2.length) {
         var2[var0] = var1;
         if (var0 == 0 && !propagating) {
            propagating = true;

            try {
               int var3 = Math.min(EasyBotClient.count, var2.length);

               for (int var4 = 1; var4 < var2.length; var4++) {
                  String var5 = derive(var1, var4);
                  var2[var4] = var5;
                  if (var4 < var3 && FIELDS[var4] != null) {
                     FIELDS[var4].setText(var5);
                  }
               }
            } finally {
               propagating = false;
            }
         }
      }
   }

   static String derive(String var0, int var1) {
      if (var0 == null) {
         return "";
      }

      int var2 = var0.length();

      while (var2 > 0 && Character.isDigit(var0.charAt(var2 - 1))) {
         var2--;
      }

      String var3 = var0.substring(0, var2);
      if (var3.isEmpty()) {
         return "";
      }

      String var4 = String.valueOf(var1 + 1);
      if (var3.length() + var4.length() > 16) {
         var3 = var3.substring(0, 16 - var4.length());
      }

      return var3 + var4;
   }
}
