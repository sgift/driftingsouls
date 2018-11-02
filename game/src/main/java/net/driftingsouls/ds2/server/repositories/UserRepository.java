package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
    User findByName(String username);
}
