package fu.de190025.post.service.impl;

import fu.de190025.post.client.UserClient;
import fu.de190025.post.common.*;
import fu.de190025.post.dto.*;
import fu.de190025.post.entity.*;
import fu.de190025.post.repository.PostRepository;
import fu.de190025.post.service.PostService;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostServiceImpl implements PostService {
  private final PostRepository repo;
  private final UserClient users;

  public PostServiceImpl(PostRepository repo, UserClient users) {
    this.repo = repo;
    this.users = users;
  }

  private Post find(Long id) {
    return repo.findById(id).orElseThrow(() -> new ApiException(4, "Post not found"));
  }

  private UserDTO user(Long id) {
    try {
      var response = users.getUser(id);
      return response == null ? null : response.data();
    } catch (feign.FeignException.NotFound e) {
      return null;
    }
  }

  public PostDTO create(PostDTO d) {
    Post p = new Post();
    apply(p, d);
    p.setStatus(PostStatus.SCHEDULED);
    validate(p);
    UserDTO u = user(p.getUserId());
    if (u == null) throw new ApiException(4, "User ID is not found");
    if (!"ACTIVE".equals(u.getStatus()))
      throw new ApiException(5, "User is not ACTIVE for posting");
    capacity(p, u);
    cost(p, u);
    return dto(repo.saveAndFlush(p));
  }

  public PostDTO update(Long id, PostDTO d) {
    Post current = find(id);
    // Validate a detached candidate before mutating the managed entity.
    Post candidate = new Post();
    apply(candidate, dto(current));
    apply(candidate, d);
    validate(candidate);
    UserDTO u = user(candidate.getUserId());
    if (u == null) throw new ApiException(4, "User ID is not found");
    capacity(candidate, u);
    cost(candidate, u);
    apply(current, dto(candidate));
    current.setTotalCost(candidate.getTotalCost());
    return dto(repo.saveAndFlush(current));
  }

  public PostDetailDTO get(Long id) {
    return detail(find(id));
  }

  public void cancel(Long id) {
    find(id).setStatus(PostStatus.CANCELLED);
  }

  public PageDTO list(int page, int size, String advertiserName, String status) {
    var pageable = Validation.page(page, size);
    PostStatus s = status == null ? null : PostStatus.valueOf(status);
    var result =
        repo.findAll(
            (root, q, cb) -> {
              var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
              if (advertiserName != null && !advertiserName.isBlank())
                predicates.add(
                    cb.like(
                        cb.lower(root.get("advertiserName")),
                        "%"
                            + advertiserName
                                .toLowerCase(java.util.Locale.ROOT)
                                .replace("\\", "\\\\")
                                .replace("%", "\\%")
                                .replace("_", "\\_")
                            + "%",
                        '\\'));
              if (s != null) predicates.add(cb.equal(root.get("status"), s));
              return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            pageable);
    return PageDTO.of(result, result.getContent().stream().map(this::detail).toList());
  }

  private void validate(Post p) {
    Validation.text(p.getAdvertiserName(), 100, "advertiserName");
    Validation.email(p.getAdvertiserEmail());
    Validation.text(p.getAdvertiserPhone(), 20, "advertiserPhone");
    Validation.require(
        p.getStartDate() != null
            && p.getEndDate() != null
            && p.getEndDate().after(p.getStartDate()),
        "endDate must be after startDate");
    Validation.require(
        p.getNumberOfPosts() != null && p.getNumberOfPosts() >= 1, "Invalid numberOfPosts");
    Validation.require(p.getUserId() != null && p.getUserId() > 0, "userId is required");
  }

  private void capacity(Post p, UserDTO u) {
    Validation.require(
        p.getNumberOfPosts() <= u.getMaxDailyPosts(), "numberOfPosts exceeds maxDailyPosts");
  }

  private void cost(Post p, UserDTO u) {
    long days =
        ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(p.getStartDate().getTime())
                .atZone(ZoneOffset.UTC)
                .toLocalDate(),
            java.time.Instant.ofEpochMilli(p.getEndDate().getTime())
                .atZone(ZoneOffset.UTC)
                .toLocalDate());
    BigDecimal cost = u.getRatePerPost().multiply(BigDecimal.valueOf(days));
    Validation.require(
        cost.precision() - cost.scale() <= 16, "totalCost exceeds database precision");
    p.setTotalCost(cost);
  }

  private void apply(Post p, PostDTO d) {
    if (d.getAdvertiserName() != null) p.setAdvertiserName(d.getAdvertiserName());
    if (d.getAdvertiserEmail() != null) p.setAdvertiserEmail(d.getAdvertiserEmail());
    if (d.getAdvertiserPhone() != null) p.setAdvertiserPhone(d.getAdvertiserPhone());
    if (d.getStartDate() != null) p.setStartDate(d.getStartDate());
    if (d.getEndDate() != null) p.setEndDate(d.getEndDate());
    if (d.getNumberOfPosts() != null) p.setNumberOfPosts(d.getNumberOfPosts());
    if (d.getStatus() != null) p.setStatus(PostStatus.valueOf(d.getStatus()));
    if (d.getUserId() != null) p.setUserId(d.getUserId());
  }

  private PostDTO dto(Post p) {
    PostDTO d = new PostDTO();
    d.setPostId(p.getPostId());
    d.setAdvertiserName(p.getAdvertiserName());
    d.setAdvertiserEmail(p.getAdvertiserEmail());
    d.setAdvertiserPhone(p.getAdvertiserPhone());
    d.setStartDate(p.getStartDate());
    d.setEndDate(p.getEndDate());
    d.setNumberOfPosts(p.getNumberOfPosts());
    d.setTotalCost(p.getTotalCost());
    d.setStatus(p.getStatus().name());
    d.setUserId(p.getUserId());
    return d;
  }

  private PostDetailDTO detail(Post p) {
    PostDetailDTO d = new PostDetailDTO();
    d.setPostId(p.getPostId());
    d.setAdvertiserName(p.getAdvertiserName());
    d.setAdvertiserEmail(p.getAdvertiserEmail());
    d.setAdvertiserPhone(p.getAdvertiserPhone());
    d.setStartDate(p.getStartDate());
    d.setEndDate(p.getEndDate());
    d.setNumberOfPosts(p.getNumberOfPosts());
    d.setTotalCost(p.getTotalCost());
    d.setStatus(p.getStatus().name());
    d.setUser(user(p.getUserId()));
    return d;
  }
}
