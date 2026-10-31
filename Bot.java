package pl.easybot.bot;

import com.google.common.hash.HashCode;
import net.minecraft.util.dynamic.HashCodeOps;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.entity.EntityPosition;
import net.minecraft.util.PlayerInput;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.SharedConstants;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.StringHelper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.Uuids;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.MutableText;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.client.network.Address;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryOps;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.block.entity.SignText;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket;
import net.minecraft.client.network.ChunkBatchSizeCalculator;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.text.TextCodecs;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.client.network.ClientRegistries;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.client.resource.ClientDataPackManager;
import net.minecraft.network.packet.s2c.config.SelectKnownPacksS2CPacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.s2c.play.SetCursorItemS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.SerializableRegistries.SerializedRegistryEntry;

public class Bot implements Runnable {
   public static final String DEFAULT_SERVER = "localhost";
   private static final int MAX_LINES = 1000;
   private static final Identifier SPRINT_BOOST = Identifier.ofVanilla("sprinting");
   private static final String PASSWORD = "eloelo";
   public static volatile boolean autoLogin = true;
   public static volatile boolean autoAfk = true;
   private static final Pattern CODE = Pattern.compile("\\d{3,}");
   private static WrapperLookup lookup;
   public final String name;
   public final String server;
   private final UUID uuid;
   private final List<Bot.Line> lines = new ArrayList<>();
   private final Object writeLock = new Object();
   private final Inflater inflater = new Inflater();
   private final Deflater deflater = new Deflater();
   private final ScheduledExecutorService worker;
   private final List<String> chatTypes = new ArrayList<>();
   private final Map<Identifier, byte[]> cookies = new HashMap<>();
   private final Set<Integer> brokenPackets = new HashSet<>();
   private final Packs packs;
   private volatile Bot.State state = Bot.State.CONNECTING;
   private volatile boolean stopping;
   private volatile int version;
   private volatile int gen;
   private volatile ServerAddress transfer;
   private Socket socket;
   private DataInputStream in;
   private OutputStream out;
   private volatile int threshold = -1;
   private boolean joined;
   private boolean inWorld;
   private volatile boolean positioned;
   private final Body body = new Body();
   private World world;
   private RegistryKey<net.minecraft.world.World> dimension;
   private int entityId;
   private int loadStep;
   private boolean loaded;
   private int loadTicks;
   private long tickCount;
   private double lastX;
   private double lastY;
   private double lastZ;
   private float lastYaw;
   private float lastPitch;
   private boolean lastOnGround;
   private boolean lastHorizontal;
   private int sincePosition;
   private ChunkBatchSizeCalculator batch = new ChunkBatchSizeCalculator();
   private boolean dead;
   private String actionBar = "";
   private long connectedAt;
   private ClientRegistries clientRegistries = new ClientRegistries();
   private ClientDataPackManager dataPacks;
   private volatile Immutable registries;
   private final Object invLock = new Object();
   private ItemStack[] player = emptySlots(46);
   private Bot.Window window;
   private ItemStack cursor = ItemStack.EMPTY;
   private int selected;
   private int revision;
   private int sequence;
   private boolean using;
   private ItemStack anvilTracked = ItemStack.EMPTY;
   private String anvilName;
   private Bot.SignEdit signEdit;
   private ScheduledFuture<?> signTimeout;
   private long lastAuth;
   private int authTries;
   private boolean rulesSeen;
   private boolean rulesClicked;
   private boolean frozen;
   private int rulesTries;
   private final Entities entities = new Entities();
   private final Wander wander = new Wander();
   private PlayerInput lastInput = PlayerInput.DEFAULT;
   private boolean spectator;
   private long calmUntil;
   private long authHold;
   private boolean rulesDone;
   private boolean trust;
   private int trustClicks;
   private int trustWindow = -1;
   private int trustWait;
   private boolean trustListed;
   private boolean lastSprint;
   private final ArrayDeque<Long> setbacks = new ArrayDeque<>();
   private long walkBanUntil;
   private static BufferedWriter file;

   public Bot(String name, String server) {
      this.name = name;
      this.server = server;
      this.uuid = Uuids.getOfflinePlayerUuid(name);
      this.packs = new Packs(name, this.uuid, server, this::packStatus);
      this.worker = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread tx = new Thread(r, "EasyBot-tick-" + name);
         tx.setDaemon(true);
         return tx;
      });
      Thread t = new Thread(this, "EasyBot-" + name);
      t.setDaemon(true);
      t.start();
   }

   public Bot.State state() {
      return this.state;
   }

   public boolean active() {
      return this.state != Bot.State.OFFLINE;
   }

   public int version() {
      return this.version;
   }

   public List<Bot.Line> lines() {
      synchronized (this.lines) {
         return new ArrayList<>(this.lines);
      }
   }

   public void clear() {
      synchronized (this.lines) {
         this.lines.clear();
      }

      this.version++;
   }

   public long onlineMillis() {
      return this.state == Bot.State.PLAY && this.connectedAt > 0L ? System.currentTimeMillis() - this.connectedAt : 0L;
   }

   public String position() {
      return this.positioned ? String.format(Locale.ROOT, "%.0f %.0f %.0f", this.body.x, this.body.y, this.body.z) : null;
   }

   @Override
   public void run() {
      this.worker.scheduleAtFixedRate(this::tick, 50L, 50L, TimeUnit.MILLISECONDS);

      try {
         while (true) {
            try {
               this.connect();

               while (!this.stopping) {
                  PacketByteBuf b = this.read();
                  if (this.state == Bot.State.LOGIN) {
                     this.handle(b);
                  } else {
                     int g = this.gen;
                     this.worker.execute(() -> {
                        if (g == this.gen && !this.stopping) {
                           this.safely(b);
                        }
                     });
                  }
               }

               return;
            } catch (Exception e) {
               if (this.transfer == null || this.stopping) {
                  if (!this.stopping) {
                     this.log(Text.literal("Rozłączono: " + describe(e)).formatted(Formatting.field_1061));
                  }
                  break;
               }
            }
         }
      } finally {
         this.closeSocket();
         this.state = Bot.State.OFFLINE;
         this.worker.shutdownNow();
         this.packs.shutdown();
         this.version++;
      }
   }

   public void disconnect() {
      if (!this.stopping) {
         this.stopping = true;
         this.log(Text.literal("Rozłączono bota.").formatted(Formatting.field_1080));
         this.closeSocket();
      }
   }

   public void chat(String message) {
      String msg = message.strip();
      if (!msg.isEmpty()) {
         this.worker.execute(() -> {
            if (this.state != Bot.State.PLAY) {
               this.log(Text.literal("Bot nie jest jeszcze na serwerze.").formatted(Formatting.field_1061));
            } else {
               try {
                  if (msg.startsWith("/")) {
                     this.send(5, b -> b.writeString(msg.substring(1), 32767));
                  } else {
                     this.send(7, b -> {
                        b.writeString(msg.length() > 256 ? msg.substring(0, 256) : msg, 256);
                        b.writeInstant(Instant.now());
                        b.method_52974(ThreadLocalRandom.current().nextLong());
                        b.method_52964(false);
                        b.writeVarInt(0);
                        b.writeBitSet(new BitSet(), 20);
                     });
                  }

                  this.log(Text.literal("> " + msg).withColor(9419007));
               } catch (IOException e) {
                  this.log(Text.literal("Nie wysłano: " + describe(e)).formatted(Formatting.field_1061));
               }
            }
         });
      }
   }

   public Bot.Inventory inventory() {
      synchronized (this.invLock) {
         Bot.Window w = this.window == null
            ? null
            : new Bot.Window(
               this.window.id(), this.window.type(), this.window.title(), this.window.slots() == null ? null : (ItemStack[])this.window.slots().clone()
            );
         return new Bot.Inventory((ItemStack[])this.player.clone(), w, this.cursor, this.selected, this.using, this.registries);
      }
   }

   public void click(int slot, int button, SlotActionType type) {
      this.action(() -> {
         Int2ObjectOpenHashMap<ItemStack> changed = new Int2ObjectOpenHashMap();
         int syncId;
         int rev;
         ItemStack carried;
         synchronized (this.invLock) {
            syncId = this.window != null ? this.window.id() : 0;
            rev = this.revision;
            if (type == SlotActionType.PICKUP) {
               this.predictPickup(slot, button, changed);
            }

            carried = this.cursor.copy();
         }

         this.version++;
         this.send(16, ClickSlotC2SPacket.CODEC, new ClickSlotC2SPacket(syncId, rev, (short)slot, (byte)button, type, this.hashes(changed), this.hash(carried)));
      });
   }

   private void predictPickup(int slot, int button, Int2ObjectOpenHashMap<ItemStack> changed) {
      if (slot == -999) {
         if (!this.cursor.isEmpty()) {
            this.cursor = button == 0 ? ItemStack.EMPTY : this.cursor.copyWithCount(this.cursor.getCount() - 1);
         }
      } else {
         ItemStack[] slots = this.window != null ? this.window.slots() : this.player;
         if (slots != null && slot >= 0 && slot < slots.length) {
            ItemStack s = slots[slot];
            ItemStack c = this.cursor;
            ItemStack newSlot;
            ItemStack newCursor;
            if (c.isEmpty()) {
               if (s.isEmpty()) {
                  return;
               }

               int take = button == 0 ? s.getCount() : (s.getCount() + 1) / 2;
               newCursor = s.copyWithCount(take);
               newSlot = s.getCount() - take <= 0 ? ItemStack.EMPTY : s.copyWithCount(s.getCount() - take);
            } else if (s.isEmpty()) {
               int put = button == 0 ? c.getCount() : 1;
               newSlot = c.copyWithCount(put);
               newCursor = c.getCount() - put <= 0 ? ItemStack.EMPTY : c.copyWithCount(c.getCount() - put);
            } else if (ItemStack.areItemsAndComponentsEqual(s, c)) {
               int put = Math.min(s.getMaxCount() - s.getCount(), button == 0 ? c.getCount() : 1);
               if (put <= 0) {
                  return;
               }

               newSlot = s.copyWithCount(s.getCount() + put);
               newCursor = c.getCount() - put <= 0 ? ItemStack.EMPTY : c.copyWithCount(c.getCount() - put);
            } else {
               newSlot = c;
               newCursor = s;
            }

            slots[slot] = newSlot;
            this.cursor = newCursor;
            changed.put(slot, newSlot.copy());
            if (this.window != null && slots.length >= 36 && slot >= slots.length - 36) {
               this.player[9 + slot - (slots.length - 36)] = newSlot;
            }
         }
      }
   }

   public void closeWindow() {
      this.action(() -> {
         int id;
         synchronized (this.invLock) {
            if (this.window == null) {
               return;
            }

            id = this.window.id();
            this.window = null;
         }

         this.version++;
         this.send(17, CloseHandledScreenC2SPacket.CODEC, new CloseHandledScreenC2SPacket(id));
         this.log(Text.literal("[GUI] Zamknięto okno.").formatted(Formatting.field_1063));
      });
   }

   public void select(int slot) {
      this.action(() -> {
         synchronized (this.invLock) {
            if (this.selected == slot) {
               return;
            }

            this.selected = slot;
         }

         this.version++;
         this.send(51, UpdateSelectedSlotC2SPacket.CODEC, new UpdateSelectedSlotC2SPacket(slot));
      });
   }

   public void useItem() {
      this.action(() -> {
         this.send(61, PlayerInteractItemC2SPacket.CODEC, new PlayerInteractItemC2SPacket(Hand.field_5808, ++this.sequence, this.body.yaw, this.body.pitch));
         this.send(58, HandSwingC2SPacket.CODEC, new HandSwingC2SPacket(Hand.field_5808));
         this.log(Text.literal("[Akcja] PPM: ").formatted(Formatting.field_1063).append(this.heldName()));
      });
   }

   public void swing() {
      this.action(() -> {
         this.send(58, HandSwingC2SPacket.CODEC, new HandSwingC2SPacket(Hand.field_5808));
         this.log(Text.literal("[Akcja] LPM: ").formatted(Formatting.field_1063).append(this.heldName()));
      });
   }

   public void holdUse(boolean hold) {
      this.action(() -> {
         if (hold) {
            this.send(61, PlayerInteractItemC2SPacket.CODEC, new PlayerInteractItemC2SPacket(Hand.field_5808, ++this.sequence, this.body.yaw, this.body.pitch));
            this.log(Text.literal("[Akcja] Trzyma PPM: ").formatted(Formatting.field_1063).append(this.heldName()));
         } else {
            this.send(39, PlayerActionC2SPacket.CODEC, new PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.field_12974, BlockPos.ORIGIN, Direction.field_11033));
            this.log(Text.literal("[Akcja] Puścił PPM.").formatted(Formatting.field_1063));
         }

         synchronized (this.invLock) {
            this.using = hold;
         }

         this.version++;
      });
   }

   public void drop(boolean all) {
      this.action(
         () -> {
            Text held = this.heldName();
            this.send(
               39,
               PlayerActionC2SPacket.CODEC,
               new PlayerActionC2SPacket(all ? net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.field_12970 : net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.field_12975, BlockPos.ORIGIN, Direction.field_11033)
            );
            synchronized (this.invLock) {
               ItemStack stack = this.player[36 + this.selected];
               ItemStack left = !all && stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
               this.player[36 + this.selected] = left;
               if (this.window != null && this.window.slots() != null && this.window.slots().length >= 36) {
                  this.window.slots()[this.window.slots().length - 9 + this.selected] = left;
               }
            }

            this.version++;
            this.log(Text.literal("[Akcja] Wyrzucił: ").formatted(Formatting.field_1063).append(held));
         }
      );
   }

   public void throwAll() {
      this.action(() -> {
         int open;
         synchronized (this.invLock) {
            open = this.window != null ? this.window.id() : -1;
            this.window = null;
         }

         if (open >= 0) {
            this.version++;
            this.send(17, CloseHandledScreenC2SPacket.CODEC, new CloseHandledScreenC2SPacket(open));
         }

         List<Integer> slots = new ArrayList<>();
         boolean carrying;
         synchronized (this.invLock) {
            for (int i = 5; i < 46; i++) {
               if (!this.player[i].isEmpty()) {
                  slots.add(i);
               }
            }

            carrying = !this.cursor.isEmpty();
         }

         if (slots.isEmpty() && !carrying) {
            this.log(Text.literal("[Akcja] Ekwipunek jest pusty.").formatted(Formatting.field_1063));
         } else {
            this.log(Text.literal("[Akcja] Wyrzucam cały ekwipunek (" + slots.size() + " stacków).").formatted(Formatting.field_1065));
            int g = this.gen;
            long delay = ThreadLocalRandom.current().nextLong(150L, 300L);
            if (carrying) {
               this.worker.schedule(() -> this.throwSlot(g, -999), delay, TimeUnit.MILLISECONDS);
               delay += ThreadLocalRandom.current().nextLong(60L, 140L);
            }

            for (int s : slots) {
               this.worker.schedule(() -> this.throwSlot(g, s), delay, TimeUnit.MILLISECONDS);
               delay += ThreadLocalRandom.current().nextLong(60L, 140L);
            }
         }
      });
   }

   private void throwSlot(int g, int slot) {
      if (g == this.gen && this.state == Bot.State.PLAY) {
         Int2ObjectOpenHashMap<ItemStack> changed = new Int2ObjectOpenHashMap();
         int rev;
         ItemStack carried;
         synchronized (this.invLock) {
            if (this.window != null) {
               return;
            }

            if (slot == -999) {
               if (this.cursor.isEmpty()) {
                  return;
               }

               this.cursor = ItemStack.EMPTY;
            } else {
               if (this.player[slot].isEmpty()) {
                  return;
               }

               this.player[slot] = ItemStack.EMPTY;
               changed.put(slot, ItemStack.EMPTY);
            }

            rev = this.revision;
            carried = this.cursor.copy();
         }

         this.version++;

         try {
            this.send(
               16,
               ClickSlotC2SPacket.CODEC,
               slot == -999
                  ? new ClickSlotC2SPacket(0, rev, (short)-999, (byte)0, SlotActionType.PICKUP, this.hashes(changed), this.hash(carried))
                  : new ClickSlotC2SPacket(0, rev, (short)slot, (byte)1, SlotActionType.THROW, this.hashes(changed), this.hash(carried))
            );
         } catch (IOException var8) {
         }
      }
   }

   private Text heldName() {
      synchronized (this.invLock) {
         ItemStack stack = this.player[36 + this.selected];
         return stack.isEmpty()
            ? Text.literal("pusta ręka")
            : Text.empty().append(stack.getName()).append(stack.getCount() > 1 ? " x" + stack.getCount() : "");
      }
   }

   private void action(Bot.Action a) {
      this.worker.execute(() -> {
         if (this.state == Bot.State.PLAY && this.registries != null) {
            try {
               a.run();
            } catch (IOException e) {
               this.log(Text.literal("Nie wysłano: " + describe(e)).formatted(Formatting.field_1061));
            }
         }
      });
   }

   private static ItemStack[] emptySlots(int n) {
      ItemStack[] a = new ItemStack[n];
      Arrays.fill(a, ItemStack.EMPTY);
      return a;
   }

   private void resetInventory() {
      synchronized (this.invLock) {
         this.player = emptySlots(46);
         this.window = null;
         this.cursor = ItemStack.EMPTY;
         this.using = false;
      }

      this.version++;
   }

   private void startConfig() {
      this.clientRegistries = new ClientRegistries();
      this.dataPacks = null;
   }

   private void buildRegistries() {
      Immutable base = ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager();

      try {
         if (this.dataPacks != null) {
            LifecycledResourceManager resources = this.dataPacks.createResourceManager();

            try {
               this.registries = this.clientRegistries.createRegistryManager(resources, base, false);
            } catch (Throwable var6) {
               if (resources != null) {
                  try {
                     resources.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (resources != null) {
               resources.close();
            }
         } else {
            this.registries = this.clientRegistries.createRegistryManager(ResourceFactory.MISSING, base, false);
         }
      } catch (Exception e) {
         this.registries = null;
         this.log(
            Text.literal("Nie udało się wczytać rejestrów serwera - podgląd ekwipunku będzie niedostępny (" + describe(e) + ")")
               .formatted(Formatting.field_1065)
         );
      }
   }

   private void connect() throws IOException {
      ServerAddress address = this.transfer != null ? this.transfer : ServerAddress.parse(this.server);
      int intent = this.transfer != null ? 3 : 2;
      this.transfer = null;
      this.gen++;
      this.closeSocket();
      this.threshold = -1;
      this.inflater.reset();
      this.deflater.reset();
      this.state = Bot.State.CONNECTING;
      this.inWorld = false;
      this.world = null;
      this.positioned = false;
      this.registries = null;
      this.resetInventory();
      this.version++;
      this.log(
         Text.literal(
               (intent == 3 ? "Serwer przenosi bota na " : "Szukam adresu ")
                  + address.getAddress()
                  + (address.getPort() != 25565 ? ":" + address.getPort() : "")
                  + "..."
            )
            .formatted(Formatting.field_1080)
      );
      ops();
      Optional<Address> resolved = AllowedAddressResolver.DEFAULT.resolve(address);
      if (resolved.isEmpty()) {
         throw new IOException("nie znaleziono serwera " + address.getAddress());
      }

      InetSocketAddress target = resolved.get().getInetSocketAddress();
      this.log(
         Text.literal("Łączę z " + target.getHostString() + ":" + target.getPort() + " (" + resolved.get().getHostAddress() + ")")
            .formatted(Formatting.field_1080)
      );
      Socket s = new Socket();
      s.setTcpNoDelay(true);
      s.connect(target, 10000);
      s.setSoTimeout(30000);
      synchronized (this.writeLock) {
         this.socket = s;
         this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
         this.out = new BufferedOutputStream(s.getOutputStream());
      }

      if (this.stopping) {
         throw new SocketException("zatrzymano");
      }

      this.state = Bot.State.LOGIN;
      this.version++;
      this.send(0, b -> {
         b.writeVarInt(SharedConstants.getProtocolVersion());
         b.writeString(target.getHostName(), 255);
         b.method_52998(target.getPort());
         b.writeVarInt(intent);
      });
      this.send(0, b -> {
         b.writeString(this.name, 16);
         b.writeUuid(this.uuid);
      });
      this.log(Text.literal("Połączono, loguję jako " + this.name + "...").formatted(Formatting.field_1080));
   }

   private void safely(PacketByteBuf b) {
      try {
         this.handle(b);
      } catch (EOFException e) {
         this.closeSocket();
      } catch (Exception e) {
         b.method_52988(0);
         int id = b.readVarInt();
         if (this.brokenPackets.add(id)) {
            this.log(Text.literal("Nie odczytano pakietu 0x" + Integer.toHexString(id) + ": " + describe(e)).formatted(Formatting.field_1063));
         }
      }
   }

   private void handle(PacketByteBuf b) throws IOException {
      int id = b.readVarInt();
      switch (this.state) {
         case LOGIN:
            this.login(id, b);
            break;
         case CONFIG:
            this.config(id, b);
            break;
         case PLAY:
            this.play(id, b);
      }
   }

   private void login(int id, PacketByteBuf b) throws IOException {
      switch (id) {
         case 0:
            this.kicked(this.jsonText(b.readString()));
            break;
         case 1:
            this.log(
               Text.literal("Serwer wymaga konta premium (szyfrowanie) - bot z dowolnym nickiem tu nie wejdzie.").formatted(Formatting.field_1061)
            );
            this.stopping = true;
            throw new SocketException("serwer w trybie online-mode");
         case 2:
            UUID accepted = b.readUuid();
            String nick = b.readString(16);
            this.log(Text.literal("Zalogowano jako " + nick + " (" + accepted + ")").formatted(Formatting.field_1060));
            this.send(3, x -> {});
            this.startConfig();
            this.state = Bot.State.CONFIG;
            this.version++;
            this.sendClientInfo();
            break;
         case 3:
            this.threshold = b.readVarInt();
            this.log(Text.literal("Kompresja włączona (" + this.threshold + ")").formatted(Formatting.field_1063));
            break;
         case 4:
            int query = b.readVarInt();
            this.send(2, x -> {
               x.writeVarInt(query);
               x.method_52964(false);
            });
            break;
         case 5:
            this.cookie(4, b);
      }
   }

   private void config(int id, PacketByteBuf b) throws IOException {
      switch (id) {
         case 0:
            this.cookie(1, b);
         case 1:
         case 6:
         case 12:
         default:
            break;
         case 2:
            this.kicked(this.nbtText(b));
            break;
         case 3:
            this.buildRegistries();
            this.send(3, x -> {});
            this.state = Bot.State.PLAY;
            this.version++;
            this.log(Text.literal("Konfiguracja zakończona, wchodzę do świata...").formatted(Formatting.field_1080));
            break;
         case 4: {
            long key = b.readLong();
            this.send(4, x -> x.method_52974(key));
            break;
         }
         case 5: {
            int key = b.readInt();
            this.send(5, x -> x.method_53002(key));
            break;
         }
         case 7: {
            DynamicRegistriesS2CPacket p = (DynamicRegistriesS2CPacket)DynamicRegistriesS2CPacket.CODEC.decode(b);
            this.clientRegistries.putDynamicRegistry(p.comp_2258(), p.comp_2259());
            if (p.comp_2258().equals(RegistryKeys.MESSAGE_TYPE)) {
               this.chatTypes.clear();

               for (SerializedRegistryEntry e : p.comp_2259()) {
                  this.chatTypes.add(e.comp_2256().toString());
               }
            }
            break;
         }
         case 8:
            this.packs.pop(b.readBoolean() ? b.readUuid() : null);
            break;
         case 9:
            this.resourcePack(b);
            break;
         case 10:
            this.storeCookie(b);
            break;
         case 11:
            this.transfer(b);
            break;
         case 13:
            this.clientRegistries.putTags(((SynchronizeTagsS2CPacket)SynchronizeTagsS2CPacket.CODEC.decode(b)).getGroups());
            break;
         case 14: {
            SelectKnownPacksS2CPacket p = (SelectKnownPacksS2CPacket)SelectKnownPacksS2CPacket.CODEC.decode(b);
            this.dataPacks = new ClientDataPackManager();
            this.send(7, SelectKnownPacksC2SPacket.CODEC, new SelectKnownPacksC2SPacket(this.dataPacks.getCommonKnownPacks(p.comp_2356())));
         }
      }
   }

   private ComponentChangesHash.ComponentHasher hasher() {
      RegistryOps<HashCode> ops = this.registries.getOps(HashCodeOps.INSTANCE);
      return c -> c.encode(ops).getOrThrow(s -> new IllegalStateException("hash: " + s)).asInt();
   }

   private ItemStackHash hash(ItemStack st) {
      return ItemStackHash.fromItemStack(st, this.hasher());
   }

   private Int2ObjectMap<ItemStackHash> hashes(Int2ObjectMap<ItemStack> m) {
      Int2ObjectOpenHashMap<ItemStackHash> r = new Int2ObjectOpenHashMap<>();
      for (Int2ObjectMap.Entry<ItemStack> e : m.int2ObjectEntrySet()) {
         r.put(e.getIntKey(), this.hash(e.getValue()));
      }

      return r;
   }

   private RegistryByteBuf reg(PacketByteBuf b) {
      return new RegistryByteBuf(b, this.registries);
   }

   private void play(int id, PacketByteBuf b) throws IOException {
      switch (id) {
         case 1: {
            if (this.registries == null) {
               return;
            }

            EntitySpawnS2CPacket p = (EntitySpawnS2CPacket)EntitySpawnS2CPacket.CODEC.decode(this.reg(b));
            this.entities.spawn(p.getEntityId(), p.getUuid(), p.getEntityType(), p.getX(), p.getY(), p.getZ());
         }
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
         case 10:
         case 11:
         case 14:
         case 15:
         case 16:
         case 17:
         case 20:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 31:
         case 36:
         case 37:
         case 38:
         case 41:
         case 42:
         case 43:
         case 45:
         case 46:
         case 49:
         case 50:
         case 51:
         case 52:
         case 56:
         case 57:
         case 60:
         case 61:
         case 62:
         case 65:
         case 68:
         case 69:
         case 70:
         case 72:
         case 73:
         case 77:
         case 79:
         case 80:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 91:
         case 92:
         case 93:
         case 94:
         case 96:
         case 97:
         case 100:
         case 101:
         case 104:
         case 105:
         case 107:
         case 109:
         case 110:
         case 111:
         case 113:
         case 116:
         case 117:
         case 118:
         case 120:
         case 121:
         case 123:
         default:
            break;
         case 7: {
            if (this.world == null || this.registries == null) {
               return;
            }

            BlockEntityUpdateS2CPacket p = (BlockEntityUpdateS2CPacket)BlockEntityUpdateS2CPacket.CODEC.decode(this.reg(b));
            this.world.blockEntity(p.getPos(), p.getBlockEntityType(), p.getNbt());
            break;
         }
         case 9: {
            if (this.world == null || this.registries == null) {
               return;
            }

            BlockUpdateS2CPacket p = (BlockUpdateS2CPacket)BlockUpdateS2CPacket.CODEC.decode(this.reg(b));
            this.world.set(p.getPos(), p.getState());
            break;
         }
         case 12:
            this.batch.onChunkSent(b.readVarInt());
            float desired = this.batch.getDesiredChunksPerTick();
            this.send(9, x -> x.method_52941(desired));
            break;
         case 13:
            this.batch.onStartChunkSend();
            break;
         case 18:
            CloseScreenS2CPacket.CODEC.decode(b);
            this.finishSign();
            boolean had;
            synchronized (this.invLock) {
               had = this.window != null;
               this.window = null;
            }

            this.version++;
            if (had) {
               this.log(Text.literal("[GUI] Serwer zamknął okno.").formatted(Formatting.field_1063));
            }

            this.calm(20, 50);
            if (this.rulesClicked && !this.rulesDone) {
               this.rulesDone = true;
               this.log(Text.literal("[Bot] Regulamin zaakceptowany.").formatted(Formatting.field_1060));
            }
            break;
         case 19:
            if (this.registries == null) {
               return;
            }

            InventoryS2CPacket px = (InventoryS2CPacket)InventoryS2CPacket.CODEC.decode(this.reg(b));
            ItemStack[] slots = px.contents().toArray(new ItemStack[0]);
            synchronized (this.invLock) {
               this.revision = px.revision();
               this.cursor = px.cursorStack();
               if (px.syncId() == 0) {
                  if (slots.length == 46) {
                     this.player = slots;
                  }
               } else if (this.window != null && this.window.id() == px.syncId()) {
                  this.window = new Bot.Window(this.window.id(), this.window.type(), this.window.title(), slots);
                  if (slots.length >= 36) {
                     System.arraycopy(slots, slots.length - 36, this.player, 9, 36);
                  }
               }
            }

            this.version++;
            this.checkAnvil();
            if (this.trustSlot()) {
               return;
            }

            this.checkRules();
            break;
         case 21:
            if (this.registries == null) {
               return;
            }

            ScreenHandlerSlotUpdateS2CPacket px71 = (ScreenHandlerSlotUpdateS2CPacket)ScreenHandlerSlotUpdateS2CPacket.CODEC.decode(this.reg(b));
            synchronized (this.invLock) {
               this.revision = px71.getRevision();
               int slot = px71.getSlot();
               if (px71.getSyncId() == -1 && slot == -1) {
                  this.cursor = px71.getStack();
               } else if (px71.getSyncId() == 0 && slot >= 0 && slot < 46) {
                  this.player[slot] = px71.getStack();
               } else if (this.window != null
                  && this.window.id() == px71.getSyncId()
                  && this.window.slots() != null
                  && slot >= 0
                  && slot < this.window.slots().length) {
                  ItemStack[] slots84 = this.window.slots84();
                  slots84[slot] = px71.getStack();
                  int base = slots84.length - 36;
                  if (slots84.length >= 36 && slot >= base) {
                     this.player[9 + slot - base] = px71.getStack();
                  }
               }
            }

            this.version++;
            this.checkAnvil();
            if (this.trustSlot()) {
               return;
            }

            this.checkRules();
            break;
         case 22:
            this.cookie(19, b);
            break;
         case 29:
            this.kicked(this.nbtText(b));
            break;
         case 30:
            Text message = this.nbtText(b);
            int type = b.readVarInt();
            if (type == 0) {
               this.log(message);
               AutoFlow.watch(this, message);
               return;
            }

            Text sender = this.nbtText(b);
            Text target = b.readBoolean() ? this.nbtText(b) : null;
            this.log(this.decorate(type, sender, target, message));
            break;
         case 32: {
            EntityPositionSyncS2CPacket p = (EntityPositionSyncS2CPacket)EntityPositionSyncS2CPacket.CODEC.decode(b);
            Vec3d v = p.comp_3224().comp_3148();
            this.entities.moveTo(p.comp_3223(), v.x, v.y, v.z);
            break;
         }
         case 33: {
            if (this.registries == null) {
               return;
            }

            ExplosionS2CPacket p = (ExplosionS2CPacket)ExplosionS2CPacket.CODEC.decode(this.reg(b));
            p.comp_2884().ifPresent(vx -> {
               this.body.vx = this.body.vx + vx.x;
               this.body.vy = this.body.vy + vx.y;
               this.body.vz = this.body.vz + vx.z;
            });
            break;
         }
         case 34: {
            if (this.world == null) {
               return;
            }

            UnloadChunkS2CPacket p = (UnloadChunkS2CPacket)UnloadChunkS2CPacket.CODEC.decode(b);
            this.world.unload(p.comp_1726().x, p.comp_1726().z);
            break;
         }
         case 35: {
            GameStateChangeS2CPacket p = (GameStateChangeS2CPacket)GameStateChangeS2CPacket.CODEC.decode(b);
            if (p.getReason() == GameStateChangeS2CPacket.INITIAL_CHUNKS_COMING && this.loadStep == 0) {
               this.loadStep = 1;
            }

            if (p.getReason() == GameStateChangeS2CPacket.GAME_MODE_CHANGED) {
               this.spectator = GameMode.byIndex(MathHelper.floor(p.getValue() + 0.5F)) == GameMode.field_9219;
            }
            break;
         }
         case 39: {
            long key = b.readLong();
            this.send(26, x -> x.method_52974(key));
            break;
         }
         case 40: {
            if (this.world == null || this.registries == null) {
               return;
            }

            ChunkDataS2CPacket p = (ChunkDataS2CPacket)ChunkDataS2CPacket.CODEC.decode(this.reg(b));
            this.world.load(p.getChunkX(), p.getChunkZ(), p.getChunkData().getSectionsDataBuf(), this.tickCount);
            p.getChunkData().getBlockEntities(p.getChunkX(), p.getChunkZ()).accept(this.world::blockEntity);
            break;
         }
         case 44: {
            if (this.registries == null) {
               return;
            }

            GameJoinS2CPacket p = (GameJoinS2CPacket)GameJoinS2CPacket.CODEC.decode(this.reg(b));
            this.entityId = p.comp_88();
            this.enterWorld(p.comp_1727(), true);
            this.inWorld = true;
            if (!this.joined) {
               this.joined = true;
               this.connectedAt = System.currentTimeMillis();
               this.log(
                  Text.literal("Bot " + this.name + " wszedł na serwer!").formatted(new Formatting[]{Formatting.field_1060, Formatting.field_1067})
               );
            }
            break;
         }
         case 47:
         case 48:
            int e = b.readVarInt();
            this.entities.moveBy(e, b.readShort() / 4096.0, b.readShort() / 4096.0, b.readShort() / 4096.0);
            break;
         case 53:
            if (this.registries == null) {
               return;
            }

            OpenScreenS2CPacket px02 = (OpenScreenS2CPacket)OpenScreenS2CPacket.CODEC.decode(this.reg(b));
            this.finishSign();
            synchronized (this.invLock) {
               this.window = new Bot.Window(px02.getSyncId(), px02.getScreenHandlerType(), px02.getName(), null);
               this.using = false;
               this.anvilTracked = ItemStack.EMPTY;
               this.anvilName = null;
            }

            this.version++;
            this.log(Text.literal("[GUI] Otwarto: ").formatted(Formatting.field_1075).append(px02.getName()));
            if (this.trust) {
               this.trustWindow = px02.getSyncId();
               this.trustWait = ThreadLocalRandom.current().nextInt(10, 16);
            }
            break;
         case 54:
            this.openSign((SignEditorOpenS2CPacket)SignEditorOpenS2CPacket.CODEC.decode(b));
            break;
         case 55: {
            int key = b.readInt();
            this.send(43, x -> x.method_53002(key));
            break;
         }
         case 58: {
            PlayerAbilitiesS2CPacket p = (PlayerAbilitiesS2CPacket)PlayerAbilitiesS2CPacket.CODEC.decode(b);
            this.body.flying = p.isFlying();
            this.body.flySpeed = p.getFlySpeed();
            break;
         }
         case 59:
            this.playerChat(b);
            break;
         case 63:
            this.entities.playerRemove(((PlayerRemoveS2CPacket)PlayerRemoveS2CPacket.CODEC.decode(b)).comp_1105());
            break;
         case 64: {
            if (this.registries == null) {
               return;
            }

            PlayerListS2CPacket p = (PlayerListS2CPacket)PlayerListS2CPacket.CODEC.decode(this.reg(b));
            this.entities.playerList(p);
            if (p.getActions().contains(net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.field_29137)) {
               for (net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry ex : p.getEntries()) {
                  if (ex.comp_1106().equals(this.uuid)) {
                     this.spectator = ex.comp_1110() == GameMode.field_9219;
                  }
               }
            }
            break;
         }
         case 66:
            this.teleport((PlayerPositionLookS2CPacket)PlayerPositionLookS2CPacket.CODEC.decode(b));
            break;
         case 67:
            this.body.yaw = b.readFloat();
            this.body.pitch = b.readFloat();
            this.sendLook(false);
            break;
         case 71:
            ((EntitiesDestroyS2CPacket)EntitiesDestroyS2CPacket.CODEC.decode(b)).getEntityIds().forEach(this.entities::remove);
            break;
         case 74:
            this.packs.pop(b.readBoolean() ? b.readUuid() : null);
            break;
         case 75:
            this.resourcePack(b);
            break;
         case 76:
            if (this.registries == null) {
               return;
            }

            PlayerRespawnS2CPacket px76 = (PlayerRespawnS2CPacket)PlayerRespawnS2CPacket.CODEC.decode(this.reg(b));
            this.enterWorld(px76.comp_1728(), false);
            this.dead = false;
            this.finishSign();
            synchronized (this.invLock) {
               this.window = null;
               this.using = false;
            }

            this.version++;
            break;
         case 78:
            if (this.world == null) {
               return;
            }

            ((ChunkDeltaUpdateS2CPacket)ChunkDeltaUpdateS2CPacket.CODEC.decode(b)).visitUpdates(this.world::set);
            break;
         case 81:
            Text textx = this.nbtText(b);
            String plain = textx.getString();
            if (!plain.isBlank() && !plain.equals(this.actionBar)) {
               this.log(Text.literal("[Action bar] ").formatted(Formatting.field_1063).append(textx));
               AutoFlow.watch(this, textx);
            }

            this.actionBar = plain;
            break;
         case 90:
            if (this.registries == null) {
               return;
            }

            ItemStack stack = ((SetCursorItemS2CPacket)SetCursorItemS2CPacket.CODEC.decode(this.reg(b))).comp_2890();
            synchronized (this.invLock) {
               this.cursor = stack;
            }

            this.version++;
            break;
         case 95: {
            EntityVelocityUpdateS2CPacket p = (EntityVelocityUpdateS2CPacket)EntityVelocityUpdateS2CPacket.CODEC.decode(b);
            if (p.getEntityId() != this.entityId) {
               return;
            }

            this.body.vx = p.getVelocity().x;
            this.body.vy = p.getVelocity().y;
            this.body.vz = p.getVelocity().z;
            break;
         }
         case 98:
            float health = b.readFloat();
            this.body.food = b.readVarInt();
            if (health <= 0.0F && !this.dead) {
               this.dead = true;
               this.log(Text.literal("Bot zginął - odradzam.").formatted(Formatting.field_1061));
               int g = this.gen;
               this.worker.schedule(() -> {
                  if (g == this.gen && this.dead && this.state == Bot.State.PLAY) {
                     try {
                        this.send(10, x -> x.writeVarInt(0));
                     } catch (IOException var3x) {
                     }
                  }
               }, ThreadLocalRandom.current().nextLong(1200L, 2600L), TimeUnit.MILLISECONDS);
            } else if (health > 0.0F) {
               this.dead = false;
            }
            break;
         case 99:
            int slot = ((UpdateSelectedSlotS2CPacket)UpdateSelectedSlotS2CPacket.CODEC.decode(b)).comp_3325();
            synchronized (this.invLock) {
               if (slot >= 0 && slot < 9) {
                  this.selected = slot;
               }
            }

            this.version++;
            break;
         case 102:
            if (this.registries == null) {
               return;
            }

            SetPlayerInventoryS2CPacket px61 = (SetPlayerInventoryS2CPacket)SetPlayerInventoryS2CPacket.CODEC.decode(this.reg(b));
            int i = px61.comp_2891();
            int slot63 = i < 9 ? 36 + i : (i < 36 ? i : (i < 40 ? 8 - (i - 36) : (i == 40 ? 45 : -1)));
            synchronized (this.invLock) {
               if (slot63 >= 0) {
                  this.player[slot63] = px61.comp_2892();
               }
            }

            this.version++;
            break;
         case 103:
            if (this.registries == null) {
               return;
            }

            this.entities.team((TeamS2CPacket)TeamS2CPacket.CODEC.decode(this.reg(b)));
            break;
         case 106:
            this.title("Podtytuł", this.nbtText(b));
            break;
         case 108:
            this.title("Tytuł", this.nbtText(b));
            break;
         case 112:
            this.send(14, x -> {});
            this.state = Bot.State.CONFIG;
            this.inWorld = false;
            this.world = null;
            this.positioned = false;
            this.authHold = 0L;
            this.entities.clearAll();
            this.finishSign();
            this.startConfig();
            this.resetInventory();
            this.log(Text.literal("Serwer przełącza bota (rekonfiguracja)...").formatted(Formatting.field_1080));
            break;
         case 114:
            this.storeCookie(b);
            break;
         case 115:
            Text text = this.nbtText(b);
            if (!b.readBoolean()) {
               this.log(text);
               AutoFlow.watch(this, text);
            }
            break;
         case 119: {
            EntityPositionS2CPacket p = (EntityPositionS2CPacket)EntityPositionS2CPacket.CODEC.decode(b);
            Entities.Mob m = this.entities.get(p.comp_3237());
            if (m == null) {
               return;
            }

            EntityPosition next = EntityPosition.apply(
               new EntityPosition(new Vec3d(m.x, m.y, m.z), Vec3d.ZERO, 0.0F, 0.0F), p.comp_3238(), p.comp_3239()
            );
            this.entities.moveTo(p.comp_3237(), next.comp_3148().x, next.comp_3148().y, next.comp_3148().z);
            break;
         }
         case 122:
            this.transfer(b);
            break;
         case 124: {
            if (this.registries == null) {
               return;
            }

            EntityAttributesS2CPacket p = (EntityAttributesS2CPacket)EntityAttributesS2CPacket.CODEC.decode(this.reg(b));
            if (p.getEntityId() == this.entityId) {
               for (net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.Entry ex : p.getEntries()) {
                  this.attribute(ex);
               }
            }
         }
      }
   }

   private void enterWorld(CommonPlayerSpawnInfo info, boolean join) {
      DimensionType type = (DimensionType)info.comp_1553().comp_349();
      if (join || this.world == null || !info.comp_1554().equals(this.dimension)) {
         this.world = new World(this.registries, type.comp_651(), type.comp_652());
      }

      this.dimension = info.comp_1554();
      this.spectator = info.comp_1556() == GameMode.field_9219;
      this.entities.clearWorld();
      this.wander.reset();
      Wander.release(this.body);
      this.lastInput = PlayerInput.DEFAULT;
      this.lastSprint = false;
      this.calm(40, 90);
      this.body.vx = this.body.vy = this.body.vz = 0.0;
      this.body.onGround = false;
      this.body.horizontalCollision = false;
      if (join) {
         this.body.x = this.body.y = this.body.z = 0.0;
         this.body.yaw = this.body.pitch = 0.0F;
         this.positioned = false;
         this.batch = new ChunkBatchSizeCalculator();
      }

      this.lastX = this.body.x;
      this.lastY = this.body.y;
      this.lastZ = this.body.z;
      this.lastYaw = this.body.yaw;
      this.lastPitch = this.body.pitch;
      this.lastOnGround = false;
      this.lastHorizontal = false;
      this.sincePosition = 0;
      this.loadStep = 0;
      this.loaded = false;
      this.loadTicks = 60;
   }

   private void teleport(PlayerPositionLookS2CPacket p) throws IOException {
      boolean first = !this.positioned;
      EntityPosition current = new EntityPosition(
         new Vec3d(this.body.x, this.body.y, this.body.z), new Vec3d(this.body.vx, this.body.vy, this.body.vz), this.body.yaw, this.body.pitch
      );
      EntityPosition next = EntityPosition.apply(current, p.comp_3228(), p.comp_3229());
      this.body.x = next.comp_3148().x;
      this.body.y = next.comp_3148().y;
      this.body.z = next.comp_3148().z;
      this.body.vx = next.comp_3149().x;
      this.body.vy = next.comp_3149().y;
      this.body.vz = next.comp_3149().z;
      this.body.yaw = next.comp_3150();
      this.body.pitch = next.comp_3151();
      this.send(0, x -> x.writeVarInt(p.comp_3133()));
      this.send(29, x -> {
         x.method_52940(this.body.x);
         x.method_52940(this.body.y);
         x.method_52940(this.body.z);
         x.method_52941(this.body.yaw);
         x.method_52941(this.body.pitch);
         x.method_52997(0);
      });
      this.positioned = true;
      if (first) {
         this.log(Text.literal("Pozycja: " + this.position()).formatted(Formatting.field_1063));
      } else if (this.body.forward != 0.0F || this.body.jumping) {
         this.wander.interrupt(this.body);
         this.calm(30, 60);
         long now = System.currentTimeMillis();
         this.setbacks.addLast(now);

         while (!this.setbacks.isEmpty() && now - this.setbacks.peekFirst() > 30000L) {
            this.setbacks.pollFirst();
         }

         if (this.setbacks.size() >= 4) {
            this.walkBanUntil = this.tickCount + 1200L;
            this.setbacks.clear();
            this.log(Text.literal("[Bot] Serwer kilka razy cofnął pozycję - przez minutę tylko się rozglądam.").formatted(Formatting.field_1065));
         }
      }
   }

   private void tick() {
      if (this.state == Bot.State.PLAY && this.inWorld && this.out != null && this.world != null) {
         try {
            this.tickCount++;
            if (!this.loaded) {
               this.loadTicks--;
            }

            if ((this.loaded || this.loadTicks <= 0) && this.positioned) {
               if (autoAfk && this.trust) {
                  this.trustClick();
               }

               if (autoAfk && this.free()) {
                  this.wander.tick(this.body, this.world);
               } else {
                  Wander.release(this.body);
               }

               if (this.tickCount < this.walkBanUntil) {
                  this.body.forward = 0.0F;
                  this.body.jumping = false;
                  this.body.sprinting = false;
               }

               if (!this.spectator) {
                  if (!this.dead && !this.body.climbing(this.world)) {
                     this.entities.pushes(this.body, this.name, this.spectator);
                  }

                  this.body.tick(this.world);
               }

               this.sendInput();
               this.sendMovement();
            }

            if (this.loadStep == 1) {
               int bx = MathHelper.floor(this.body.x);
               int by = MathHelper.floor(this.body.y);
               int bz = MathHelper.floor(this.body.z);
               if (this.world.isOutOfHeightLimit(by) || this.dead || this.tickCount - this.world.arrivedAt(bx, bz) >= 3L) {
                  this.loadStep = 2;
               }
            }

            if (this.loadStep == 2 && !this.loaded) {
               this.loaded = true;
               this.send(42, x -> {});
            }

            this.send(11, x -> {});
         } catch (IOException e) {
            this.closeSocket();
         }
      }
   }

   private boolean free() {
      synchronized (this.invLock) {
         if (this.window != null) {
            return false;
         }
      }

      return !this.frozen && !this.dead && !this.trust && this.signEdit == null && this.tickCount >= this.calmUntil && this.tickCount >= this.authHold;
   }

   private void trustClick() throws IOException {
      synchronized (this.invLock) {
         if (this.window != null || this.registries == null) {
            return;
         }
      }

      if (--this.trustWait <= 0) {
         this.trustWait = ThreadLocalRandom.current().nextInt(5, 9);
         if (++this.trustClicks > 40) {
            this.trust = false;
            this.log(Text.literal("[Bot] Po 20 s klikania PPM nie otworzyło się żadne GUI - przestaję.").formatted(Formatting.field_1065));
         } else {
            this.send(61, PlayerInteractItemC2SPacket.CODEC, new PlayerInteractItemC2SPacket(Hand.field_5808, ++this.sequence, this.body.yaw, this.body.pitch));
         }
      }
   }

   private boolean trustSlot() {
      int slot = -1;
      String items = null;
      synchronized (this.invLock) {
         if (!this.trust || this.window == null || this.window.id() != this.trustWindow || this.window.slots() == null || this.window.slots().length == 0) {
            return false;
         }

         ItemStack[] slots = this.window.slots();
         int end = slots.length >= 36 ? slots.length - 36 : slots.length;

         for (int i = 0; i < end && slot < 0; i++) {
            if (slots[i].isOf(Items.field_8264)) {
               slot = i;
            }
         }

         if (slot >= 0) {
            this.trust = false;
         } else {
            if (this.trustListed) {
               return false;
            }

            this.trustListed = true;
            StringBuilder s = new StringBuilder();

            for (int i = 0; i < end; i++) {
               if (!slots[i].isEmpty()) {
                  s.append(s.isEmpty() ? "" : ", ").append(i).append("=").append(Registries.ITEM.getId(slots[i].getItem()).getPath());
               }
            }

            items = s.toString();
         }
      }

      if (slot < 0) {
         this.log(Text.literal("[Bot] W GUI nie ma czerwonego barwnika, czekam: " + items).formatted(Formatting.field_1065));
         return false;
      } else {
         this.click(slot, 0, SlotActionType.PICKUP);
         this.log(Text.literal("[Bot] Klikam czerwony barwnik (slot " + slot + ").").formatted(Formatting.field_1060));
         return true;
      }
   }

   private void calm(int min, int max) {
      this.calmUntil = Math.max(this.calmUntil, this.tickCount + ThreadLocalRandom.current().nextInt(min, max));
   }

   private void sendInput() throws IOException {
      PlayerInput input = new PlayerInput(
         this.body.forward > 0.0F,
         this.body.forward < 0.0F,
         this.body.sideways > 0.0F,
         this.body.sideways < 0.0F,
         this.body.jumping,
         false,
         this.body.sprinting
      );
      if (!input.equals(this.lastInput)) {
         this.lastInput = input;
         this.send(41, PlayerInputC2SPacket.CODEC, new PlayerInputC2SPacket(input));
      }
   }

   private void attribute(net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket.Entry e) {
      double d = e.comp_2178();
      List<EntityAttributeModifier> mods = e.comp_2179().stream().filter(mx -> !mx.comp_2447().equals(SPRINT_BOOST)).toList();

      for (EntityAttributeModifier m : mods) {
         if (m.comp_2450() == Operation.field_6328) {
            d += m.comp_2449();
         }
      }

      double v = d;

      for (EntityAttributeModifier m : mods) {
         if (m.comp_2450() == Operation.field_6330) {
            v += d * m.comp_2449();
         }
      }

      for (EntityAttributeModifier m : mods) {
         if (m.comp_2450() == Operation.field_6331) {
            v *= 1.0 + m.comp_2449();
         }
      }

      RegistryEntry<EntityAttribute> a = e.comp_2177();
      v = ((EntityAttribute)a.comp_349()).clamp(v);
      if (a.getKey().equals(EntityAttributes.field_23719.getKey())) {
         this.body.speed = v;
      } else if (a.getKey().equals(EntityAttributes.field_23728.getKey())) {
         this.body.jumpStrength = v;
      } else if (a.getKey().equals(EntityAttributes.field_47761.getKey())) {
         this.body.stepHeight = v;
      } else if (a.getKey().equals(EntityAttributes.field_49078.getKey())) {
         this.body.gravity = v;
      }
   }

   private void sendMovement() throws IOException {
      if (this.body.sprinting != this.lastSprint) {
         this.lastSprint = this.body.sprinting;
         this.send(40, x -> {
            x.writeVarInt(this.entityId);
            x.writeVarInt(this.lastSprint ? 3 : 4);
            x.writeVarInt(0);
         });
      }

      double dx = this.body.x - this.lastX;
      double dy = this.body.y - this.lastY;
      double dz = this.body.z - this.lastZ;
      double dyaw = this.body.yaw - this.lastYaw;
      double dpitch = this.body.pitch - this.lastPitch;
      this.sincePosition++;
      boolean moved = dx * dx + dy * dy + dz * dz > 4.0E-8 || this.sincePosition >= 20;
      boolean rotated = dyaw != 0.0 || dpitch != 0.0;
      int flags = (this.body.onGround ? 1 : 0) | (this.body.horizontalCollision ? 2 : 0);
      if (moved && rotated) {
         this.send(29, x -> {
            x.method_52940(this.body.x);
            x.method_52940(this.body.y);
            x.method_52940(this.body.z);
            x.method_52941(this.body.yaw);
            x.method_52941(this.body.pitch);
            x.method_52997(flags);
         });
      } else if (moved) {
         this.send(28, x -> {
            x.method_52940(this.body.x);
            x.method_52940(this.body.y);
            x.method_52940(this.body.z);
            x.method_52997(flags);
         });
      } else if (rotated) {
         this.sendLook(true);
      } else if (this.lastOnGround != this.body.onGround || this.lastHorizontal != this.body.horizontalCollision) {
         this.send(31, x -> x.method_52997(flags));
      }

      if (moved) {
         this.lastX = this.body.x;
         this.lastY = this.body.y;
         this.lastZ = this.body.z;
         this.sincePosition = 0;
      }

      if (rotated) {
         this.lastYaw = this.body.yaw;
         this.lastPitch = this.body.pitch;
      }

      this.lastOnGround = this.body.onGround;
      this.lastHorizontal = this.body.horizontalCollision;
   }

   private void sendLook(boolean real) throws IOException {
      int flags = real ? (this.body.onGround ? 1 : 0) | (this.body.horizontalCollision ? 2 : 0) : 0;
      this.send(30, x -> {
         x.method_52941(this.body.yaw);
         x.method_52941(this.body.pitch);
         x.method_52997(flags);
      });
   }

   private void checkAnvil() throws IOException {
      ItemStack stack;
      synchronized (this.invLock) {
         if (this.window == null || this.window.type() != ScreenHandlerType.field_17329 || this.window.slots() == null || this.window.slots().length == 0) {
            return;
         }

         stack = this.window.slots()[0];
         if (stack.isEmpty() || !EnchantmentHelper.canHaveEnchantments(stack) || ItemStack.areEqual(stack, this.anvilTracked)) {
            return;
         }

         this.anvilTracked = stack.copy();
      }

      String shown = this.packs.translate(stack.getName());
      String name = stack.contains(DataComponentTypes.field_49631) ? shown : "";
      String clean = StringHelper.stripInvalidChars(name);
      if (clean.length() <= 50 && !clean.equals(this.anvilName)) {
         this.anvilName = clean;
         this.send(46, RenameItemC2SPacket.CODEC, new RenameItemC2SPacket(name));
      }
   }

   private void openSign(SignEditorOpenS2CPacket p) {
      World.Sign sign = this.world == null ? null : this.world.sign(p.getPos());
      if (sign != null) {
         this.finishSign();
         boolean filtered = MinecraftClient.getInstance().shouldFilterText();
         SignText text = p.isFront() ? sign.front() : sign.back();
         String[] lines = new String[4];

         for (int i = 0; i < 4; i++) {
            lines[i] = this.packs.translate(text.getMessage(i, filtered));
         }

         this.signEdit = new Bot.SignEdit(p.getPos(), p.isFront(), lines);
         this.log(Text.literal("[GUI] Otwarto edycję tabliczki.").formatted(Formatting.field_1075));
         int g = this.gen;
         this.signTimeout = this.worker.schedule(() -> {
            if (g == this.gen) {
               this.finishSign();
            }
         }, ThreadLocalRandom.current().nextLong(3000L, 7000L), TimeUnit.MILLISECONDS);
      }
   }

   private void finishSign() {
      Bot.SignEdit edit = this.signEdit;
      if (edit != null) {
         this.signEdit = null;
         if (this.signTimeout != null) {
            this.signTimeout.cancel(false);
         }

         if (this.state == Bot.State.PLAY) {
            try {
               this.send(
                  57, UpdateSignC2SPacket.CODEC, new UpdateSignC2SPacket(edit.pos(), edit.front(), edit.lines()[0], edit.lines()[1], edit.lines()[2], edit.lines()[3])
               );
            } catch (IOException var3) {
            }
         }
      }
   }

   private void playerChat(PacketByteBuf b) {
      b.readUuid();
      b.readVarInt();
      if (b.readBoolean()) {
         b.method_52994(256);
      }

      String content = b.readString(256);
      b.readLong();
      b.readLong();
      int seen = b.readVarInt();

      for (int i = 0; i < seen; i++) {
         if (b.readVarInt() == 0) {
            b.method_52994(256);
         }
      }

      Text unsigned = b.readBoolean() ? this.nbtText(b) : null;
      if (b.readVarInt() == 2) {
         b.readBitSet();
      }

      Text message = (Text)(unsigned != null ? unsigned : Text.literal(content));
      int type = b.readVarInt();
      if (type == 0) {
         this.log(message);
      } else {
         Text sender = this.nbtText(b);
         Text target = b.readBoolean() ? this.nbtText(b) : null;
         this.log(this.decorate(type, sender, target, message));
      }
   }

   private Text decorate(int type, Text sender, Text target, Text message) {
      String kind = type - 1 < this.chatTypes.size() ? this.chatTypes.get(type - 1) : "minecraft:chat";
      MutableText t = Text.empty();
      switch (kind) {
         case "minecraft:say_command":
            t.append("[").append(sender).append("] ").append(message);
            break;
         case "minecraft:emote_command":
            t.append("* ").append(sender).append(" ").append(message);
            break;
         case "minecraft:msg_command_incoming":
            t.append(
                  Text.empty()
                     .append(sender)
                     .append(" szepcze do bota: ")
                     .formatted(new Formatting[]{Formatting.field_1080, Formatting.field_1056})
               )
               .append(message);
            break;
         case "minecraft:msg_command_outgoing":
            t.append(
                  Text.literal("Bot szepcze do ")
                     .append((Text)(target == null ? Text.empty() : target))
                     .append(": ")
                     .formatted(new Formatting[]{Formatting.field_1080, Formatting.field_1056})
               )
               .append(message);
            break;
         case "minecraft:team_msg_command_incoming":
         case "minecraft:team_msg_command_outgoing":
            t.append((Text)(target == null ? Text.empty() : target))
               .append(" <")
               .append(sender)
               .append("> ")
               .append(message);
            break;
         default:
            t.append("<").append(sender).append("> ").append(message);
      }

      return t;
   }

   private void title(String label, Text text) {
      if (!text.getString().isBlank()) {
         this.log(Text.literal("[" + label + "] ").formatted(Formatting.field_1065).append(text));
         AutoFlow.watch(this, text);
      }
   }

   public void watch(Text text) {
      String plain = Formatting.strip(text.getString());
      if (plain != null) {
         String lower = plain.toLowerCase(Locale.ROOT);
         if (lower.contains("regulamin")) {
            this.rulesSeen = true;
         }

         if (lower.contains("trwa przenoszenie") && !this.frozen) {
            this.frozen = true;
            this.log(Text.literal("[Bot] Przenoszenie - od teraz bot stoi w miejscu.").formatted(Formatting.field_1065));
         }

         if (lower.contains("zweryfikowano czynnik zaufania") && !this.trust) {
            this.trust = true;
            this.trustClicks = 0;
            this.trustWindow = -1;
            this.trustWait = 0;
            this.trustListed = false;
            this.log(Text.literal("[Bot] Zweryfikowano - klikam PPM aż otworzy się GUI.").formatted(Formatting.field_1060));
         }

         if (lower.contains("pomyślnie") || lower.contains("zalogowan") || lower.contains("zarejestrowan")) {
            this.authHold = 0L;
         }

         if (!this.rulesClicked) {
            boolean register = lower.contains("/register");
            if (register || lower.contains("/login")) {
               this.authHold = Math.max(this.authHold, this.tickCount + 300L);
               long now = System.currentTimeMillis();
               if (autoLogin && this.authTries < 3 && now - this.lastAuth >= 10000L) {
                  this.lastAuth = now;
                  this.authTries++;
                  String code = null;
                  Matcher m = CODE.matcher(plain);

                  while (m.find()) {
                     code = m.group();
                  }

                  String cmd = register ? "/register eloelo" + (code != null ? " " + code : "") : "/login eloelo";
                  this.worker.schedule(() -> this.chat(cmd), 1800 + ThreadLocalRandom.current().nextInt(1600), TimeUnit.MILLISECONDS);
               }
            }
         }
      }
   }

   private void checkRules() {
      if (!autoAfk) {
         return;
      }

      int slot = -1;
      int id;
      synchronized (this.invLock) {
         if (this.rulesClicked || this.window == null || this.window.slots() == null) {
            return;
         }

         String title = Formatting.strip(this.window.title().getString());
         if (!this.rulesSeen && (title == null || !title.toLowerCase(Locale.ROOT).contains("regulamin"))) {
            return;
         }

         ItemStack[] slots = this.window.slots();
         int end = slots.length >= 36 ? slots.length - 36 : slots.length;

         for (int i = 0; i < end && slot < 0; i++) {
            if (slots[i].isOf(Items.GREEN_CONCRETE)) {
               slot = i;
            }
         }

         for (int i = 0; i < end && slot < 0; i++) {
            if (slots[i].isOf(Items.LIME_CONCRETE)) {
               slot = i;
            }
         }

         if (slot < 0) {
            return;
         }

         id = this.window.id();
         this.rulesClicked = true;
      }

      int s = slot;
      this.log(Text.literal("[Bot] Akceptuję regulamin...").formatted(Formatting.field_1060));
      this.worker.schedule(() -> this.clickRules(id, s), 1100 + ThreadLocalRandom.current().nextInt(1200), TimeUnit.MILLISECONDS);
   }

   private void clickRules(int id, int slot) {
      synchronized (this.invLock) {
         if (this.window == null || this.window.id() != id) {
            return;
         }
      }

      this.click(slot, 0, SlotActionType.PICKUP);
      this.worker.schedule(() -> {
         synchronized (this.invLock) {
            if (this.window == null || this.window.id() != id || ++this.rulesTries >= 3) {
               return;
            }
         }

         this.clickRules(id, slot);
      }, 3000L, TimeUnit.MILLISECONDS);
   }

   private void resourcePack(PacketByteBuf b) {
      UUID id = b.readUuid();
      String url = b.readString();
      String hash = b.readString(40);
      boolean required = b.readBoolean();
      this.log(Text.literal("Serwer wysłał paczkę zasobów" + (required ? " (wymaganą)" : "") + " - pobieram ją.").formatted(Formatting.field_1063));
      this.packs.push(id, url, hash);
   }

   private void packStatus(UUID id, int status) {
      int g = this.gen;
      this.worker.execute(() -> {
         if (g == this.gen) {
            int packet = this.state == Bot.State.CONFIG ? 6 : (this.state == Bot.State.PLAY ? 47 : -1);
            if (packet >= 0) {
               try {
                  this.send(packet, x -> {
                     x.writeUuid(id);
                     x.writeVarInt(status);
                  });
               } catch (IOException var6) {
               }

               if (status == 0) {
                  this.log(Text.literal("Paczka zasobów wczytana.").formatted(Formatting.field_1063));
               }

               if (status == 2) {
                  this.log(Text.literal("Nie udało się pobrać paczki zasobów.").formatted(Formatting.field_1061));
               }
            }
         }
      });
   }

   private void cookie(int reply, PacketByteBuf b) throws IOException {
      Identifier key = b.readIdentifier();
      byte[] value = this.cookies.get(key);
      this.send(reply, x -> {
         x.writeIdentifier(key);
         x.method_52964(value != null);
         if (value != null) {
            x.writeByteArray(value);
         }
      });
   }

   private void storeCookie(PacketByteBuf b) {
      Identifier key = b.readIdentifier();
      this.cookies.put(key, b.readByteArray(5120));
   }

   private void transfer(PacketByteBuf b) {
      String host = b.readString();
      int port = b.readVarInt();
      this.transfer = new ServerAddress(host, port);
      this.gen++;
      this.closeSocket();
   }

   private void sendClientInfo() throws IOException {
      this.send(2, x -> {
         x.writeIdentifier(Identifier.ofVanilla("brand"));
         x.writeString("vanilla");
      });

      SyncedClientOptions options;
      try {
         options = MinecraftClient.getInstance().options.getSyncedOptions();
      } catch (Exception e) {
         options = SyncedClientOptions.createDefault();
      }

      SyncedClientOptions o = options;
      this.send(0, o::write);
   }

   private void kicked(Text reason) throws IOException {
      this.log(Text.literal("Wyrzucono z serwera: ").formatted(Formatting.field_1061).append(reason));
      this.stopping = true;
      throw new EOFException("kick");
   }

   private Text nbtText(PacketByteBuf b) {
      NbtElement nbt = b.readNbt(NbtSizeTracker.ofUnlimitedBytes());
      return (Text)(nbt == null
         ? Text.empty()
         : TextCodecs.CODEC
            .parse(RegistryOps.of(NbtOps.INSTANCE, this.lookup()), nbt)
            .result()
            .orElseGet(() -> Text.literal(nbt.toString())));
   }

   private Text jsonText(String json) {
      try {
         return TextCodecs.CODEC
            .parse(RegistryOps.of(JsonOps.INSTANCE, this.lookup()), JsonParser.parseString(json))
            .result()
            .orElseGet(() -> Text.literal(json));
      } catch (Exception e) {
         return Text.literal(json);
      }
   }

   private WrapperLookup lookup() {
      DynamicRegistryManager r = this.registries;
      return (WrapperLookup)(r != null ? r : ops());
   }

   private static synchronized WrapperLookup ops() {
      if (lookup == null) {
         lookup = BuiltinRegistries.createWrapperLookup();
      }

      return lookup;
   }

   private PacketByteBuf read() throws IOException {
      int length = this.readVarInt();
      byte[] data = new byte[length];
      this.in.readFully(data);
      PacketByteBuf b = new PacketByteBuf(Unpooled.wrappedBuffer(data));
      if (this.threshold < 0) {
         return b;
      }

      int size = b.readVarInt();
      if (size == 0) {
         return new PacketByteBuf(Unpooled.wrappedBuffer(data, b.readerIndex(), data.length - b.readerIndex()));
      }

      byte[] raw = new byte[size];
      this.inflater.setInput(data, b.readerIndex(), data.length - b.readerIndex());

      try {
         int off = 0;

         while (off < size) {
            int n = this.inflater.inflate(raw, off, size - off);
            if (n == 0 && (this.inflater.needsInput() || this.inflater.finished())) {
               throw new IOException("uszkodzony pakiet");
            }

            off += n;
         }
      } catch (DataFormatException e) {
         throw new IOException("uszkodzony pakiet", e);
      } finally {
         this.inflater.reset();
      }

      return new PacketByteBuf(Unpooled.wrappedBuffer(raw));
   }

   private int readVarInt() throws IOException {
      int value = 0;

      for (int i = 0; i < 5; i++) {
         int k = this.in.readUnsignedByte();
         value |= (k & 127) << i * 7;
         if ((k & 128) == 0) {
            return value;
         }
      }

      throw new IOException("za długi VarInt");
   }

   private void send(int id, Consumer<PacketByteBuf> writer) throws IOException {
      PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
      b.writeVarInt(id);
      writer.accept(b);
      byte[] body = new byte[b.readableBytes()];
      b.method_52979(body);
      synchronized (this.writeLock) {
         if (this.out == null) {
            throw new SocketException("brak połączenia");
         }

         PacketByteBuf frame = new PacketByteBuf(Unpooled.buffer());
         if (this.threshold < 0) {
            frame.method_52983(body);
         } else if (body.length < this.threshold) {
            frame.writeVarInt(0);
            frame.method_52983(body);
         } else {
            frame.writeVarInt(body.length);
            frame.method_52983(this.deflate(body));
         }

         PacketByteBuf full = new PacketByteBuf(Unpooled.buffer());
         full.writeVarInt(frame.readableBytes());
         full.method_52975(frame);
         byte[] bytes = new byte[full.readableBytes()];
         full.method_52979(bytes);
         this.out.write(bytes);
         this.out.flush();
      }
   }

   private <T> void send(int id, PacketCodec<? super RegistryByteBuf, T> codec, T packet) throws IOException {
      DynamicRegistryManager r = this.registries != null ? this.registries : ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager();
      this.send(id, b -> codec.encode(new RegistryByteBuf(b, r), packet));
   }

   private byte[] deflate(byte[] body) {
      this.deflater.setInput(body);
      this.deflater.finish();
      ByteArrayOutputStream result = new ByteArrayOutputStream(body.length / 2 + 16);
      byte[] buffer = new byte[8192];

      while (!this.deflater.finished()) {
         result.write(buffer, 0, this.deflater.deflate(buffer));
      }

      this.deflater.reset();
      return result.toByteArray();
   }

   private void closeSocket() {
      Socket s;
      synchronized (this.writeLock) {
         this.out = null;
         s = this.socket;
      }

      try {
         if (s != null) {
            s.close();
         }
      } catch (IOException var4) {
      }
   }

   private static String describe(Exception e) {
      if (e instanceof SocketTimeoutException) {
         return "przekroczono czas oczekiwania";
      }

      if (e instanceof EOFException) {
         return "serwer zamknął połączenie";
      }

      if (e instanceof UnknownHostException) {
         return "nieznany host";
      }

      if (e instanceof ConnectException) {
         return "nie można połączyć (" + e.getMessage() + ")";
      }

      String msg = e.getMessage();
      return msg == null ? e.getClass().getSimpleName() : msg;
   }

   public void log(Text text) {
      long now = System.currentTimeMillis();
      synchronized (this.lines) {
         this.lines.add(new Bot.Line(now, text));
         if (this.lines.size() > 1000) {
            this.lines.subList(0, this.lines.size() - 1000).clear();
         }
      }

      this.version++;
      this.writeFile(now, text.getString());
   }

   private void writeFile(long time, String line) {
      synchronized (Bot.class) {
         try {
            if (file == null) {
               Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve("logs").resolve("easybot.log");
               Files.createDirectories(path.getParent());
               file = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            file.write("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time)) + "] [" + this.name + "] " + line.replace("\n", "\n    "));
            file.newLine();
            file.flush();
         } catch (IOException var7) {
         }
      }
   }

   private interface Action {
      void run() throws IOException;
   }

   public record Inventory(ItemStack[] player, Bot.Window window, ItemStack cursor, int selected, boolean using, DynamicRegistryManager registries) {
   }

   public record Line(long time, Text text) {
   }

   private record SignEdit(BlockPos pos, boolean front, String[] lines) {
   }

   public enum State {
      CONNECTING("Łączenie", -865972),
      LOGIN("Logowanie", -865972),
      CONFIG("Konfiguracja", -878004),
      PLAY("Na serwerze", -13643391),
      OFFLINE("Offline", -1090725);

      public final String label;
      public final int color;

      State(String label, int color) {
         this.label = label;
         this.color = color;
      }
   }

   public record Window(int id, ScreenHandlerType<?> type, Text title, ItemStack[] slots) {
   }
}
