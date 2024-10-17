package com.service.runnersmap.service;

import com.service.runnersmap.dto.CommentDto;
import com.service.runnersmap.entity.Comment;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.entity.UserPostPK;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.CommentRepository;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final UserPostRepository userPostRepository;

  /**
   * 댓글 작성 - 해당 모집글에 참여한 사용자만 작성 가능
   */
  @Transactional
  public CommentDto createComment(Long postId, Long userId, CommentDto commentDto) {

    log.info("댓글 작성 요청");

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    UserPost userPost = userPostRepository.findById(new UserPostPK(userId, postId))
        .orElseThrow(() -> {
          log.error("사용자가 해당 모집글에 포함되어 있지 않습니다 - 게시글 ID: {}, 사용자 ID: {}", postId, userId);
          return new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER);
        });

    if (!userPost.getValid_yn()) {
      throw new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER);
    }

    // 댓글 작성
    Comment createdComment = Comment.builder()
        .post(post)
        .user(user)
        .content(commentDto.getContent())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    commentRepository.save(createdComment);
    log.info("댓글 작성 완료: {}", createdComment.getContent());

    return new CommentDto(
        createdComment.getUser().getNickname(),
        createdComment.getContent(),
        createdComment.getCreatedAt()
    );
  }

  /**
   * 특정 모집글의 댓글 조회 - 해당 모집글에 참여한 사용자만 조회 가능
   */
  @Transactional(readOnly = true)
  public Page<CommentDto> getComments(Long postId, Long userId, Pageable pageable) {
    log.info("댓글 조회 요청 - 게시글 ID: {}", postId);

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    UserPost userPost = userPostRepository.findById(new UserPostPK(userId, postId))
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER));

    if (!userPost.getValid_yn()) {
      throw new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER);
    }

    return commentRepository.findByPost(post, pageable)
        .map(comment -> new CommentDto(
            comment.getUser().getNickname(),
            comment.getContent(),
            comment.getCreatedAt()
        ));
  }

  /**
   * 댓글 수정 댓글 작성자만 수정 가능
   */
  @Transactional
  public CommentDto updateComment(Long commentId, Long userId, CommentDto commentDto) {
    log.info("댓글 수정 요청 - 댓글 ID: {}, 사용자 ID: {}", commentId, userId);

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_COMMENT));

    log.info("댓글 작성자 ID: {}", comment.getUser().getId());

    if (!comment.getUser().getId().equals(userId)) {
      log.error("댓글 작성자만 댓글 수정 가능 - 사용자 ID: {}", userId);
      throw new RunnersMapException(ErrorCode.WRITER_ONLY_ACCESS_COMMENT_DATA);
    }

    comment.setContent(commentDto.getContent());
    comment.setUpdatedAt(LocalDateTime.now());
    Comment updatedComment = commentRepository.save(comment);
    log.info("댓글 수정 완료");
    return new CommentDto(
        updatedComment.getUser().getNickname(),
        updatedComment.getContent(),
        updatedComment.getUpdatedAt()
    );
  }

  /**
   * 댓글 삭제 댓글 작성자만 삭제 가능
   */
  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    log.info("댓글 삭제 요청 - 댓글 ID: {}, 사용자 ID: {}", commentId, userId);

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_COMMENT));

    if (!comment.getUser().getId().equals(userId)) {
      log.error("댓글 작성자만 댓글 삭제 가능");
      throw new RunnersMapException(ErrorCode.WRITER_ONLY_ACCESS_COMMENT_DATA);
    }

    commentRepository.delete(comment);
    log.info("댓글 삭제 완료");
  }
}