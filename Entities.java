package pl.easybot.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket.Operation;

class Entities {
   private final Map<Integer, Entities.Mob> mobs = new HashMap<>();
   private final Map<UUID, String> names = new HashMap<>();
   private final Map<UUID, GameMode> modes = new HashMap<>();
   private final Map<String, String> members = new HashMap<>();
   private final Map<String, String> rules = new HashMap<>();

   void clearWorld() {
      this.mobs.clear();
   }

   void clearAll() {
      this.mobs.clear();
      this.names.clear();
      this.modes.clear();
      this.members.clear();
      this.rules.clear();
   }

   void spawn(int id, UUID uuid, EntityType<?> type, double x, double y, double z) {
      if (type != EntityType.field_6131 && (type == EntityType.field_6097 || DefaultAttributeRegistry.hasDefinitionFor(type))) {
         this.mobs.put(id, new Entities.Mob(uuid, type, x, y, z));
      } else {
         this.mobs.remove(id);
      }
   }

   void moveBy(int id, double dx, double dy, double dz) {
      Entities.Mob m = this.mobs.get(id);
      if (m != null) {
         m.x += dx;
         m.y += dy;
         m.z += dz;
      }
   }

   void moveTo(int id, double x, double y, double z) {
      Entities.Mob m = this.mobs.get(id);
      if (m != null) {
         m.x = x;
         m.y = y;
         m.z = z;
      }
   }

   Entities.Mob get(int id) {
      return this.mobs.get(id);
   }

   void remove(int id) {
      this.mobs.remove(id);
   }

   void playerList(PlayerListS2CPacket p) {
      for (Entry e : p.getEntries()) {
         if (p.getActions().contains(Action.field_29136) && e.comp_1107() != null) {
            this.names.put(e.comp_1106(), e.comp_1107().name());
         }

         if (p.getActions().contains(Action.field_29137)) {
            this.modes.put(e.comp_1106(), e.comp_1110());
         }
      }
   }

   void playerRemove(Iterable<UUID> ids) {
      for (UUID id : ids) {
         this.names.remove(id);
         this.modes.remove(id);
      }
   }

   void team(TeamS2CPacket p) {
      String team = p.getTeamName();
      if (p.getTeamOperation() == Operation.field_29156) {
         this.rules.remove(team);
         this.members.values().removeIf(team::equals);
      } else {
         p.getTeam().ifPresent(t -> this.rules.put(team, t.getCollisionRule().name));
         if (p.getPlayerListOperation() == Operation.field_29155) {
            for (String name : p.getPlayerNames()) {
               this.members.put(name, team);
            }
         } else if (p.getPlayerListOperation() == Operation.field_29156) {
            for (String name : p.getPlayerNames()) {
               this.members.remove(name, team);
            }
         }
      }
   }

   private String teamOf(Entities.Mob m) {
      String key = m.type == EntityType.field_6097 ? this.names.get(m.uuid) : m.uuid.toString();
      return key == null ? null : this.members.get(key);
   }

   private String rule(String team) {
      return team == null ? "always" : this.rules.getOrDefault(team, "always");
   }

   void pushes(Body body, String self, boolean spectator) {
      if (!spectator && !body.flying) {
         Box me = body.box();
         String myTeam = this.members.get(self);
         String myRule = this.rule(myTeam);
         if (!myRule.equals("never")) {
            for (Entities.Mob m : this.mobs.values()) {
               if ((m.type != EntityType.field_6097 || this.modes.get(m.uuid) != GameMode.field_9219) && m.box().intersects(me)) {
                  String team = this.teamOf(m);
                  String rule = this.rule(team);
                  if (!rule.equals("never")) {
                     boolean same = team != null && team.equals(myTeam);
                     if ((!rule.equals("pushOwnTeam") && !myRule.equals("pushOwnTeam") || !same)
                        && (!rule.equals("pushOtherTeams") && !myRule.equals("pushOtherTeams") || same)) {
                        body.push(m.x, m.z);
                     }
                  }
               }
            }
         }
      }
   }

   static final class Mob {
      final UUID uuid;
      final EntityType<?> type;
      double x;
      double y;
      double z;

      Mob(UUID uuid, EntityType<?> type, double x, double y, double z) {
         this.uuid = uuid;
         this.type = type;
         this.x = x;
         this.y = y;
         this.z = z;
      }

      Box box() {
         float w = this.type.getWidth() / 2.0F;
         float h = this.type.getHeight();
         return new Box(this.x - w, this.y, this.z - w, this.x + w, this.y + h, this.z + w);
      }
   }
}
