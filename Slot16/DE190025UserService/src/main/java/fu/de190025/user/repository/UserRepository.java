package fu.de190025.user.repository;
import fu.de190025.user.entity.User;
import org.springframework.data.jpa.repository.*;
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
 boolean existsByUsernameAndUserIdNot(String username,Long userId);
}