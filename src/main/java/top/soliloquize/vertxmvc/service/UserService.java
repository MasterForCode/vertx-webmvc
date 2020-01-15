package top.soliloquize.vertxmvc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.soliloquize.vertxmvc.entity.User;
import top.soliloquize.vertxmvc.repository.UserRepository;

import java.util.List;

/**
 * Simple Spring service bean to expose the results of a trivial database call
 */
@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    public List<User> getAllUsers() {
        return repo.findAll();
    }

    public User getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public User addOne(User user) {
        return repo.save(user);
    }
}
