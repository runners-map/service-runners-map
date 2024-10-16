package com.service.runnersmap.service;

import com.service.runnersmap.dto.PostDto;
import com.service.runnersmap.dto.PostInDto;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.entity.UserPostPK;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PostService {

  private final PostRepository postRepository;

  private final UserPostRepository userPostRepository;

  private final UserRepository userRepository;


  @Transactional(readOnly = true)
  public Page<Post> searchPost(PostInDto inDto, Pageable pageable) throws Exception {

    return postRepository.findAllWithin2Km(
        inDto.getLat(),
        inDto.getLng(),
        inDto.getGender(),
        inDto.getPaceMinStart(),
        inDto.getPaceMinEnd(),
        inDto.getDistanceStart(),
        inDto.getDistanceEnd(),
        inDto.getStartDate(),
        inDto.getStartTime(),
        inDto.getLimitMemberCnt(),
        pageable
    );
  }

  @Transactional(readOnly = true)
  public Optional<Post> searchDetailPost(Long postId) throws Exception {
    return postRepository.findById(postId);
  }

  public Post registerPost(PostDto postDto) throws Exception {

    // admin(그룹장)이 유효한 사용자인지 확인
    User user = userRepository.findById(postDto.getAdminId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // 그룹장은 진행중인 건이 러닝 내역이 있다면 완료 혹은 삭제 후에 추가 가능하도록 변경.
    boolean dupYn = postRepository.existsByAdminIdAndArriveYnFalse(postDto.getAdminId());
    if (dupYn) {
      throw new RunnersMapException(ErrorCode.ALREADY_EXISTS_POST_DATA);
    }

    // post 테이블 insert
    Post post = postRepository.save(Post.builder()
        .admin(user)
        .title(postDto.getTitle())
        .content(postDto.getContent())
        .limitMemberCnt(postDto.getLimitMemberCnt())
        .gender(postDto.getGender())
        .startDateTime(postDto.getStartDateTime())
        .startPosition(postDto.getStartPosition())
        .distance(postDto.getDistance())
        .paceMin(postDto.getPaceMin())
        .paceSec(postDto.getPaceSec())
        .path(postDto.getPath())
        .lat(postDto.getCenterLat())
        .lng(postDto.getCenterLng())
        .departureYn(false)
        .arriveYn(false)
        .build());

    log.info("[RUNNERS LOG] register postId: {} ", post.getPostId());

    // 그룹 사용자에 그룹장을 추가한다.
    UserPost userPost = new UserPost();
    userPost.setId(new UserPostPK(user.getId(), post.getPostId()));
    userPost.setValid_yn(true);
    userPostRepository.save(userPost);

    log.info("[RUNNERS LOG] register userPost userId : {} ", user.getId());

    return post;

  }

  public Post modifyPost(PostDto postDto) throws Exception {
    Optional<Post> postItem = postRepository.findById(postDto.getPostId());
    if (postItem.isPresent()) {
      Post post = postItem.get();

      // 변경 가능 상태인지 체크
      validatePost(post);

      post.setTitle(postDto.getTitle());
      post.setContent(postDto.getContent());
      post.setLimitMemberCnt(postDto.getLimitMemberCnt());
      post.setGender(postDto.getGender());
      post.setStartDateTime(postDto.getStartDateTime());
      post.setStartPosition(postDto.getStartPosition());
      post.setDistance(postDto.getDistance());
      post.setPaceMin(postDto.getPaceMin());
      post.setPaceSec(postDto.getPaceSec());
      post.setPath(postDto.getPath());
      postRepository.save(post);

      log.info("[RUNNERS LOG] modify postId : {} ", post.getPostId());
      return post;

    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA);
    }
  }

  public void deletePost(Long postId) throws Exception {
    /*
     * 그룹장 권한의 방삭제 기능 처리시에는 실제 db delete 처리한다.
     * (그룹 방 생성만 하고 아무것도 하지 않은 상태일 때만 삭제 가능하기 때문)
     * userPost 관련 로직은 delete 처리 하지 않고 유효여부 false 처리 한다.
     */
    Optional<Post> postItem = postRepository.findById(postId);
    if (postItem.isPresent()) {
      Post post = postItem.get();

      // 변경 가능 상태인지 체크
      validatePost(post);

      // 1. userPost 데이터 삭제 (post에 참여한 모든 사용자 - 최소한 그룹장은 등록되어 있음 )
      int delCnt = userPostRepository.deleteById_PostId(postId);
      if (delCnt < 0) {
        throw new RunnersMapException(ErrorCode.NOT_FOUND_USER);
      }
      log.info("[RUNNERS LOG] delete userPost Cnt : {} ", delCnt);

      // 2. Post 데이터 삭제
      postRepository.deleteById(post.getPostId());

      log.info("[RUNNERS LOG] delete postId : {} ", postId);

    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA);
    }
  }

  private void validatePost(Post post) {
    // 해당 채팅방의 그룹장 이외에는 불가
//    if (!post.getAdmin().getId().equals(post.getAdminId())) {
//      throw new RunnersMapException(ErrorCode.OWNER_ONLY_ACCESS_POST_DATA);
//    }

    // 시작 전의 러닝 글에 대해서만 가능
    if (post.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_START_POST_DATA);
    }
    // 완료 전의 러닝 글에 대해서만 가능
    if (post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }
  }

}
