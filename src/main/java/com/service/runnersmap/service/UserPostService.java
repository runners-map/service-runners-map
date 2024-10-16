package com.service.runnersmap.service;

import com.service.runnersmap.dto.UserPostDto;
import com.service.runnersmap.dto.UserPostSearchDto;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.entity.UserPostPK;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class UserPostService {

  private final PostRepository postRepository;

  private final UserPostRepository userPostRepository;

  private final UserRepository userRepository;

  /*
  * 사용자별 러닝 참여 리스트 조회
  */
  public List<Post> listParticipatePost(Long userId) throws Exception {
    return postRepository.findAllByUserId(userId);
  }

  /*
   * 러닝 참가하기
   */
  public UserPost participate(Long postId, Long userId) throws Exception {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    if(post.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_DEPARTURE_POST_DATA);
    }
    if(post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }

    boolean existYn = userPostRepository.existsById(new UserPostPK(userId, postId));
    if(existYn) {
      throw new RunnersMapException(ErrorCode.ALREADY_PARTICIPATE_USER);
    }

    UserPost newUserPost = new UserPost();
    newUserPost.setId(new UserPostPK(userId, postId));
    newUserPost.setValid_yn(true);
    newUserPost.setTotalDistance(post.getDistance());
    userPostRepository.save(newUserPost);
    return newUserPost;
  }

  /*
   * 러닝 나가기
   */
  public void participateOut(Long postId, Long userId) throws Exception {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    UserPost userPost = userPostRepository.findById(new UserPostPK(userId, postId))
        .orElseThrow(()-> new RunnersMapException(ErrorCode.NOT_FOUND_PARTICIPATE_USER));

    userPostRepository.deleteById(userPost.getId());

  }


  /*
   * 러닝기록 - 시작 버튼 (그룹장권한 / 한번 호출로 모든 메이트들의 정보를 처리한다)
   * 1. post 테이블에 출발 업데이트 처리한다.
   * 2. userPost 테이블에 실제 출발 시간을 업데이트 처리한다.
   */
  public void startRecord(Long postId) throws Exception {

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));
    if(post.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_DEPARTURE_POST_DATA);
    }

    if(post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }

    // post 테이블 update
    post.setDepartureYn(true); // 출발여부
    postRepository.save(post);

    // 모집글에 참여중인 사용자 조회 (유효한 사용자)
    List<UserPost> userList = userPostRepository.findAllByPostIdAndValidYn(postId);
    if(userList == null || userList.size() <= 0) {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_USER);
    }

    // userPost 테이블 update
    for(UserPost userItem : userList) {
      if(!userItem.getValid_yn()) {
        throw new RunnersMapException(ErrorCode.NOT_VALID_USER);
      }

      UserPost userPost = userPostRepository.findById(new UserPostPK(userItem.getId().getUserId(), postId))
          .orElseThrow(()-> new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER));

      userPost.setActualStartTime(LocalDateTime.now());
      userPostRepository.save(userPost);
    }
  }

  /*
   * 러닝기록 - 메이트들의 각각 러닝 정보 저장(도착할때마다 호출된다)
   * 1. post 테이블에 도착완료 업데이트 처리한다.
   * 2. userPost 테이블에 최종 도착 시간, 실제 달린 시간, 최종 달린 거리를 업데이트 처리 한다.
   */
  public UserPost completeRecord(UserPostDto recordDto) throws Exception {

    Post post = postRepository.findById(recordDto.getPostId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    if(!post.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.NOT_DEPARTURE_POST_DATA);
    }

    if(post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }

    User user = userRepository.findById(recordDto.getUserId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // userPost 테이블 update
    UserPost userPost = userPostRepository.findById(new UserPostPK(user.getId(), recordDto.getPostId()))
        .orElseThrow(()-> new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER));
    if(!userPost.getValid_yn()) {
      throw new RunnersMapException(ErrorCode.NOT_VALID_USER);
    }

    userPost.setTotalDistance(post.getDistance());
    userPost.setActualEndTime(LocalDateTime.now());
    userPost.setRunningDuration(Duration.between(userPost.getActualStartTime(), LocalDateTime.now()));
    userPostRepository.save(userPost);

    // 미완료 러너 존재여부  -> true : 미도착, false : 도착
    boolean existsIncompleteUser = userPostRepository.existsPostIdAndValidYnTrueAndActualEndTimeIsNull(recordDto.getPostId());
    if(!existsIncompleteUser) {
      // 모든 사용자가 도착하면 도착 처리(post 테이블 도착처리)
      // 만약에 사용자가 모두 도착하지 않았는데 비정상 종료처리가 되어야 한다면 그룹장이 모집글 방삭제를 해야한다.
      post.setArriveYn(true); // 도착여부
      postRepository.save(post);
    }

    return userPost;
  }


  /*
   * 러닝기록 - 조회
   * 1. ALL   : 누적달린 거리
   * 2. MONTH : 입력받은 월의 총 달린 거리
   * 3. DAY   : 입력받은 월의 달린거리
   */
  @Transactional(readOnly = true)
  public List<UserPostSearchDto> searchRunningData(Long userId, int year, int month) throws Exception {

    List<UserPostSearchDto> result = new ArrayList<>();

    result.add(new UserPostSearchDto("ALL", userPostRepository.findTotalDistanceByUserId(userId)));
    result.add(new UserPostSearchDto("MONTH", userPostRepository.findTotalDistanceByUserIdAndMonth(userId, year, month)));

    List<UserPostDto> runningMonths = userPostRepository.findAllTotalDistanceByUserId(userId, year, month)
        .stream()
        .map(up -> new UserPostDto(
            up.getId().getPostId(),
            up.getId().getUserId(),
            up.getTotalDistance(),
            DurationToStringConverter.convert(up.getRunningDuration()),
            up.getActualEndTime().getDayOfMonth()
        ))
        .collect(Collectors.toList());
    result.add(new UserPostSearchDto("DAY", runningMonths));

    return result;
  }

  public class DurationToStringConverter {
    public static String convert(Duration duration) {
      if (duration == null) {
        return "00:00:00";
      }
      long seconds = duration.getSeconds();
      return String.format("%02d:%02d:%02d",
          (seconds / 3600), // 시간
          (seconds % 3600) / 60, // 분
          (seconds % 60)); // 초
    }
  }
}
