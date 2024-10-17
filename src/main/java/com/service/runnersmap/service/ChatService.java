package com.service.runnersmap.service;

import com.service.runnersmap.dto.ChatMessageDto;
import com.service.runnersmap.entity.ChatMessage;
import com.service.runnersmap.entity.ChatRoom;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.entity.UserPostPK;
import com.service.runnersmap.repository.ChatMessageRepository;
import com.service.runnersmap.repository.ChatRoomRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final UserRepository userRepository;
  private final UserPostRepository userPostRepository;
  private final SimpMessagingTemplate template;


//  /**
//   * 사용자가 채팅방에 들어왔을 때 입장 알림 메시지 전송
//   */
//  public void handleUserEnter(ChatMessageDto chatMessageDto) {
//
//    User sender = userRepository.findById(chatMessageDto.getSenderId())
//        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
//
//    ChatRoom chatRoom = chatRoomRepository.findByPost_PostId(chatMessageDto.getChatRoomId())
//        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방"));
//
//    Post post = chatRoom.getPost();
//
//    // UserPost를 조회해서 사용자의 채팅방 참여상태 확인
//    UserPostPK userPostPK = new UserPostPK(sender.getId(), post.getPostId());
//    UserPost userPost = userPostRepository.findById(userPostPK).orElse(null);
////    UserPost userPost = userPostRepository.findById(new UserPostPK(sender, post))
////        .orElse(null); // 없으면 null
//
//    // null인 경우 = 첫 입장
//    if (userPost == null) {
//      userPost = UserPost.builder()
//          .id(userPostPK)
//          .valid_yn(true) //
//          .build();
//    } else {
//      // 퇴장한 적이 있는 경우, false => 다시 입장하면 true로 업데이트
//      if (!userPost.getValid_yn()) {
//        userPost.setValid_yn(true);
//      }
//    }
//    userPostRepository.save(userPost);
//
//    String enterMessage = sender.getNickname() + "님이 채팅방에 입장하셨습니다.";
//    chatMessageDto = ChatMessageDto.builder()
//        .message(enterMessage)
//        .build();
//    template.convertAndSend("/sub/chat/room/" + chatRoom.getId(), chatMessageDto);
//
//    // 이전 메시지들 불러오기
//    List<ChatMessageDto> previousMessages = getMessages(chatRoom.getId());
//    for (ChatMessageDto previousMessage : previousMessages) {
//      template.convertAndSend("/sub/chat/room/" + chatRoom.getId(), previousMessage);
//    }
//
//  }
//
//
//  /**
//   * 사용자가 퇴장시 퇴장 알림 메시지 전송 메서드
//   */
//  public void handleUserExit(ChatMessageDto chatMessageDto) {
//
//    User sender = userRepository.findById(chatMessageDto.getSenderId())
//        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));
//
//    ChatRoom chatRoom = chatRoomRepository.findByPost_PostId(chatMessageDto.getChatRoomId())
//        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방"));
//
//    Post post = chatRoom.getPost();
//
//    // UserPost에서 valid_yn을 false로 변경
//    UserPostPK userPostPK = new UserPostPK(sender.getId(), post.getPostId());
//    UserPost userPost = userPostRepository.findById(userPostPK)
//        .orElseThrow(() -> new RuntimeException("참여기록 없음"));
//    userPost.setValid_yn(false);
//    userPostRepository.save(userPost);
//
//    String exitMessage = sender.getNickname() + "님이 채팅방을 나갔습니다.";
//    chatMessageDto.builder()
//        .message(exitMessage)
//        .build();
//
//    template.convertAndSend("/sub/chat/room/" + chatRoom.getId(), chatMessageDto);
//  }


  /**
   * 클라이언트가 보낸 메시지를 저장하고 브로드캐스트하는 메서드
   */
  public void saveAndBroadcastMessage(ChatMessageDto chatMessageDto) {

    ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방입니다."));

    User sender = userRepository.findById(chatMessageDto.getSenderId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    // 데이터베이스에 저장될 메시지
    ChatMessage message = ChatMessage.builder()
        .chatRoom(chatRoom)
        .sender(sender)
        .message(chatMessageDto.getMessage())
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);

    // 클라이언트에게 전달할 메시지
    ChatMessageDto responseDto = ChatMessageDto.builder()
        .chatRoomId(chatRoom.getId())
        .senderId(sender.getId())
        .senderNickname(sender.getNickname())
        .message(message.getMessage())
        .sentAt(message.getSentAt())
        .build();
    template.convertAndSend("/sub/chat/room/" + chatMessageDto.getChatRoomId(), responseDto);
  }


  /**
   * 메시지를 조회하는 메서드
   */
  public List<ChatMessageDto> getMessages(Long chatRoomId) {

    List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId);

    return messages.stream()
        .map(message -> ChatMessageDto.builder()
            .chatRoomId(message.getChatRoom().getId())
            .senderId(message.getSender().getId())
            .senderNickname(message.getSender().getNickname())
            .message(message.getMessage())
            .sentAt(message.getSentAt())
            .build())
        .collect(Collectors.toList());
  }
}
