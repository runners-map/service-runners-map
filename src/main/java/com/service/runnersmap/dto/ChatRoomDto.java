package com.service.runnersmap.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;

@Getter
public class ChatRoomDto {

  private String roomId;
  private String roomName;
  private Set<WebSocketSession> sessions = new HashSet<>();

  public static ChatRoomDto create(String roomName) {
    ChatRoomDto room = new ChatRoomDto();
    room.roomId = UUID.randomUUID().toString();
    room.roomName = roomName;
    return room;

  }

}