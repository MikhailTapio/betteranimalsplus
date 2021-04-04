package its_meow.betteranimalsplus.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Charsets;

import dev.itsmeow.imdlib.entity.util.EntityTypeContainer;
import its_meow.betteranimalsplus.common.entity.EntityCoyote;
import its_meow.betteranimalsplus.common.entity.util.EntityTypeContainerBAPTameable;
import its_meow.betteranimalsplus.init.ModEntities;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public class ClientConfigurationPacket {
    
    public ClientConfigurationPacket() {}
    
    public boolean coyoteHostileDaytime = false;
    public Map<String, String[]> tameItems = new HashMap<>();
    
    public ClientConfigurationPacket(boolean coyoteHostileDaytime, Map<String, String[]> tameItems) {
        this.coyoteHostileDaytime = coyoteHostileDaytime;
        this.tameItems = tameItems;
    }
    
    public static void encode(ClientConfigurationPacket pkt, PacketBuffer buf) {
        buf.writeBoolean(pkt.coyoteHostileDaytime);
        buf.writeInt(pkt.tameItems.size());
        for(String key : pkt.tameItems.keySet()) {
            buf.writeInt(key.length());
            buf.writeCharSequence(key, Charsets.UTF_8);
            String[] items = pkt.tameItems.get(key);
            if(items != null) {
                buf.writeInt(items.length);
                for(String value : items) {
                    buf.writeInt(value.length());
                    buf.writeCharSequence(value, Charsets.UTF_8);
                }
            } else {
                buf.writeInt(0);
            }
        }
    }

    public static ClientConfigurationPacket decode(PacketBuffer buf) {
        boolean coyote = buf.readBoolean();
        Map<String, String[]> tames = new HashMap<>();
        int mapSize = buf.readInt();
        for(int l = 0; l < mapSize; l++) {
            int keyL = buf.readInt();
            String key = String.valueOf(buf.readCharSequence(keyL, Charsets.UTF_8));
            int tameItemSize = buf.readInt();
            String[] value = new String[tameItemSize];
            for(int i = 0; i < tameItemSize; i++) {
                int valueL = buf.readInt();
                value[i] = String.valueOf(buf.readCharSequence(valueL, Charsets.UTF_8));
            }
            tames.put(key, value);
        }
        return new ClientConfigurationPacket(coyote, tames);
    }
    
    public static class Handler {
        public static void handle(ClientConfigurationPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ctx.get().enqueueWork(() -> {
                    EntityCoyote.HOSTILE_DAYTIME = msg.coyoteHostileDaytime;
                    for(String key : msg.tameItems.keySet()) {
                        String[] items = msg.tameItems.get(key);
                        EntityTypeContainer<?> container = ModEntities.H.getEntityTypeContainer(key);
                        if(container instanceof EntityTypeContainerBAPTameable) {
                            EntityTypeContainerBAPTameable<?> cont2 = (EntityTypeContainerBAPTameable<?>) container;
                            cont2.setTameItems(items);
                        }
                    }
                });
            }
            ctx.get().setPacketHandled(true);
        }
    }

}