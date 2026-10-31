package pl.easybot.bot;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.util.UndashedUuid;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.SharedConstants;
import net.minecraft.util.Language;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.NetworkUtils;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.GameVersion;
import net.minecraft.resource.LifecycledResourceManagerImpl;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ZipResourcePack.ZipBackedFactory;
import net.minecraft.util.NetworkUtils.DownloadListener;
import net.minecraft.client.network.ServerInfo.ResourcePackPolicy;

class Packs {
   static final int LOADED = 0;
   static final int DECLINED = 1;
   static final int FAILED_DOWNLOAD = 2;
   static final int ACCEPTED = 3;
   static final int DOWNLOADED = 4;
   static final int INVALID_URL = 5;
   private final String name;
   private final UUID uuid;
   private final String server;
   private final BiConsumer<UUID, Integer> status;
   private final Map<UUID, Path> applied = new LinkedHashMap<>();
   private final ExecutorService io;
   private volatile Language language;

   Packs(String name, UUID uuid, String server, BiConsumer<UUID, Integer> status) {
      this.name = name;
      this.uuid = uuid;
      this.server = server;
      this.status = status;
      this.io = Executors.newSingleThreadExecutor(r -> {
         Thread t = new Thread(r, "EasyBot-packs-" + name);
         t.setDaemon(true);
         return t;
      });
   }

   void shutdown() {
      this.io.shutdownNow();
   }

   void push(UUID id, String url, String hash) {
      URL parsed = parse(url);
      if (parsed == null) {
         this.status.accept(id, 5);
      } else {
         this.io.execute(() -> {
            try {
               if (this.prompted()) {
                  Thread.sleep(ThreadLocalRandom.current().nextLong(1400L, 3200L));
               }

               this.status.accept(id, 3);
               Path file = this.download(id, parsed, hash);
               if (file == null) {
                  this.status.accept(id, 2);
                  return;
               }

               this.status.accept(id, 4);
               long start = System.currentTimeMillis();
               synchronized (this.applied) {
                  this.applied.put(id, file);
               }

               this.language = this.build();
               long rest = ThreadLocalRandom.current().nextLong(900L, 2200L) - (System.currentTimeMillis() - start);
               if (rest > 0L) {
                  Thread.sleep(rest);
               }

               this.status.accept(id, 0);
            } catch (InterruptedException var10) {
            }
         });
      }
   }

   void pop(UUID id) {
      this.io.execute(() -> {
         synchronized (this.applied) {
            if (id == null) {
               this.applied.clear();
            } else {
               this.applied.remove(id);
            }
         }

         this.language = this.build();
      });
   }

   String translate(Text text) {
      Language lang = this.language();
      MinecraftClient mc = MinecraftClient.getInstance();
      Supplier<String> job = () -> {
         Language old = Language.getInstance();
         Language.setInstance(lang);

         try {
            return text.getString();
         } finally {
            Language.setInstance(old);
         }
      };
      if (mc.isOnThread()) {
         return job.get();
      }

      try {
         return (String)mc.submit(job).get(5L, TimeUnit.SECONDS);
      } catch (Exception e) {
         return text.getString();
      }
   }

   private Language language() {
      Language l = this.language;
      if (l == null) {
         this.language = l = this.build();
      }

      return l;
   }

   private boolean prompted() {
      try {
         ServerList list = new ServerList(MinecraftClient.getInstance());
         list.loadFile();

         for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            String address = info.address.toLowerCase();
            if (address.equals(this.server) || address.startsWith(this.server + ":")) {
               return info.getResourcePackPolicy() != ResourcePackPolicy.ENABLED;
            }
         }
      } catch (Exception var5) {
      }

      return true;
   }

   private Path download(UUID id, URL url, String hash) {
      MinecraftClient mc = MinecraftClient.getInstance();
      GameVersion v = SharedConstants.getGameVersion();
      Map<String, String> headers = Map.of(
         "X-Minecraft-Username",
         this.name,
         "X-Minecraft-UUID",
         UndashedUuid.toString(this.uuid),
         "X-Minecraft-Version",
         v.name(),
         "X-Minecraft-Version-ID",
         v.id(),
         "X-Minecraft-Pack-Format",
         String.valueOf(v.packVersion(ResourceType.field_14188).major()),
         "User-Agent",
         "Minecraft Java/" + v.name()
      );
      HashCode code = null;
      if (hash != null && hash.matches("[0-9a-fA-F]{40}")) {
         code = HashCode.fromString(hash.toLowerCase());
      }

      Path dir = mc.runDirectory.toPath().resolve("downloads").resolve("easybot-" + UndashedUuid.toString(this.uuid)).resolve(id.toString());

      try {
         return NetworkUtils.download(dir, url, headers, Hashing.sha1(), code, 262144000, mc.getNetworkProxy(), new DownloadListener() {
            public void onStart() {
            }

            public void onContentLength(OptionalLong contentLength) {
            }

            public void onProgress(long writtenBytes) {
            }

            public void onFinish(boolean success) {
            }
         });
      } catch (Exception e) {
         return null;
      }
   }

   private Language build() {
      MinecraftClient mc = MinecraftClient.getInstance();
      List<ResourcePack> packs = new ArrayList<>();
      packs.add(mc.getDefaultResourcePack());
      List<ResourcePack> opened = new ArrayList<>();
      synchronized (this.applied) {
         for (Entry<UUID, Path> e : this.applied.entrySet()) {
            try {
               ResourcePack pack = new ZipBackedFactory(e.getValue().toFile())
                  .open(new ResourcePackInfo("server/" + e.getKey(), Text.literal("server"), ResourcePackSource.SERVER, Optional.empty()));
               packs.add(pack);
               opened.add(pack);
            } catch (Exception var17) {
            }
         }
      }

      String code = mc.options.language;
      List<String> defs = new ArrayList<>(List.of("en_us"));
      boolean rtl = false;
      LanguageDefinition def = mc.getLanguageManager().getLanguage(code);
      if (!"en_us".equals(code) && def != null) {
         defs.add(code);
         rtl = def.comp_1200();
      }

      try {
         return TranslationStorage.load(new LifecycledResourceManagerImpl(ResourceType.field_14188, packs), defs, rtl);
      } finally {
         for (ResourcePack p : opened) {
            p.close();
         }
      }
   }

   private static URL parse(String url) {
      try {
         URL u = new URL(url);
         return !"http".equals(u.getProtocol()) && !"https".equals(u.getProtocol()) ? null : u;
      } catch (MalformedURLException e) {
         return null;
      }
   }
}
