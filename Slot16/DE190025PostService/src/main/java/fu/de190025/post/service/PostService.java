package fu.de190025.post.service;

import fu.de190025.post.dto.*;

public interface PostService {
  PostDTO create(PostDTO dto);

  PostDTO update(Long id, PostDTO dto);

  PostDetailDTO get(Long id);

  void cancel(Long id);

  PageDTO list(int page, int size, String advertiserName, String status);
}
