package net.dungeonrealms.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.dungeonrealms.network.discord.DiscordAPI;
import net.dungeonrealms.network.discord.DiscordChannel;
import net.dungeonrealms.network.packet.type.BasicMessagePacket;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Handles all master server responses.
 * Since some of this code contains stuff like Discord tokens,
 * it's best not to keep all of it in a shared project like "network"
 * because anyone who gets any of the jars could then take the credentials.
 * 
 * Created March 11th, 2017.
 * @author Kneesnap
 */
public class MasterServerListener extends Listener {
	
	@Override
    public void received(Connection connection, Object object) {
		if(object instanceof BasicMessagePacket) {
			BasicMessagePacket packet = (BasicMessagePacket) object;
			
			byte[] data = packet.data;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            try {
            	String task = in.readUTF();
            	
            	switch(task) {
            		case "GMMessage":
            			DiscordAPI.sendMessage(DiscordChannel.NOTIFICATIONS, in.readUTF());
            			break;
            		case "DEVMessage":
            			DiscordAPI.sendMessage(DiscordChannel.DEVELOPMENT, in.readUTF());
            			break;
            		case "BanMessage":
            			//DiscordAPI.sendMessage(DiscordChannel.STAFF_REPORTS, in.readUTF());
            			break;
            	}
            } catch(Exception e) {
            	e.printStackTrace();
            }
		}
	}
}