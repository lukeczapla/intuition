package org.mskcc.knowledge.service;

import org.mskcc.knowledge.model.User;

import java.util.Map;

public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {

    User registerNewAccount(User newUser) throws Exception;
    Map<String, Object> fetchLDAP(String userEmail, String password);

}