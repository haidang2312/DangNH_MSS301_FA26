package fu.de190025.post.repository;
import fu.de190025.post.entity.Post;
import org.springframework.data.jpa.repository.*;
public interface PostRepository extends JpaRepository<Post,Long>, JpaSpecificationExecutor<Post> {}