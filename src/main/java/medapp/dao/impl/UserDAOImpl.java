package medapp.dao.impl;

import medapp.dao.api.UserDAO;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        return null;
    }
}
