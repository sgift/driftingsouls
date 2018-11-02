package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

@Service
public class DSUserDetailsManager implements UserDetailsManager {
    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void createUser(UserDetails user) {

    }

    @Override
    public void updateUser(UserDetails user) {

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    @Override
    public boolean userExists(String username) {
        return userRepository.findByName(username) != null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        net.driftingsouls.ds2.server.entities.User dsUser = userRepository.findByName(username);

        if(dsUser == null) {
            throw new UsernameNotFoundException("Spieler nicht gefunden");
        }

        return new User(username, dsUser.getPassword(), !dsUser.getDisabled(), true, true, true,
                AuthorityUtils.NO_AUTHORITIES);
    }
}
