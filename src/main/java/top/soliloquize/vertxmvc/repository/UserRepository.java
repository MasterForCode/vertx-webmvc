package top.soliloquize.vertxmvc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.soliloquize.vertxmvc.entity.User;

/**
 * @author wb
 * @date 2019/9/26
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
