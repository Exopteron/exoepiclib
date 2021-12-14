/* 
Copyright (c) 2021 Exopteron 

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.exopteron.exoepiclib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    protected static void registerClientNetHandler(ExoNetworkManager netManager) {
        ClientPlayNetworking.registerGlobalReceiver(netManager.channel, (client, handler, buf, responseSender) -> {
            int packetID = buf.readVarInt();
            Class<? extends ExoPacket> packet = netManager.idToPackets.get(packetID);
            if (packet != null) {
                try {
                    ExoPacket instance = packet.getConstructor().newInstance();
                    instance.read(buf);
                    client.execute(() -> {
                            instance.handle(client.player, Side.LOGICAL_CLIENT);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO handle gracefully
                }
            }
        });
    }
}
