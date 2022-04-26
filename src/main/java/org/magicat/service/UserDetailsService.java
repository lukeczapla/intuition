package org.magicat.service;

import org.magicat.model.User;

import java.util.Map;

public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {

    User registerNewAccount(User newUser) throws Exception;
    Map<String, Object> fetchLDAP(String userEmail, String password);

}
