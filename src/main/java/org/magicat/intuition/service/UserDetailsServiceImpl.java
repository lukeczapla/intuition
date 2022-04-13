package org.magicat.intuition.service;

import org.magicat.intuition.model.Role;
import org.magicat.intuition.model.User;
import org.magicat.intuition.model.UserDetails;
import org.magicat.intuition.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    @Autowired
    private UserRepository repository;

    @Override
    public User registerNewAccount(User newUser) throws Exception {
        if (repository.findByEmailAddress(newUser.getEmailAddress()) != null) {
            throw new Exception("Account already exists " + newUser.getEmailAddress());
        }
        newUser.setRole(Role.ROLE_USER);
        repository.save(newUser);
        return newUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Getting information");
        User user = repository.findByEmailAddress(username);
        log.info("Done getting user: " + (user != null ? user.toString(): "NO USER/NULL"));
        if (user == null) {
            return null;
        }
        log.info("Found user: " + user.toString());

        UserDetails principal = UserDetails.getBuilder().firstName(user.getFirstName()).lastName(user.getLastName())
                .id(user.getEmailAddress()).password(null).role(user.getRole()).username(user.getEmailAddress()).build();
        return principal;
    }

    @Override
    public Map<String, Object> fetchLDAP(String userEmail, String password) {
        ArrayList<String> members = new ArrayList<>();

        String ldapUsername = userEmail;
        String ldapPassword = password;
        String servername = "ldap://ldapha.mskcc.root.mskcc.org:3268";
        String searchbase = "dc=MSKCC,dc=ROOT,dc=MSKCC,dc=ORG";
        String searchfilter = "(cn=" + userEmail.substring(0, userEmail.indexOf('@')) + ")";

        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, servername);

        Map<String, Object> keyMap = new HashMap<>();

        // the following is helpful in debugging errors
        // env.put("com.sun.jndi.ldap.trace.ber", System.err);

        LdapContext ctx;
        byte[] photo;
        try {
            ctx = new InitialLdapContext(env, null);

            NamingEnumeration results = null;
            try {
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                results = ctx.search(searchbase, searchfilter, controls);

                while (results.hasMore()) {
                    SearchResult searchResult = (SearchResult) results.next();
                    Attributes attributes = searchResult.getAttributes();
                    NamingEnumeration<String> names = attributes.getIDs();
                    while (names.hasMore()) {
                        String n = names.next();
                        Attribute attr = attributes.get(n);
                        //if (n.equals("thumbnailPhoto")) System.out.println("FOUND IT: " + attr.size());
                        for (int i = 0; i < attr.size(); i++) {
                            if (n.equals("thumbnailPhoto")) {
                                //System.out.println("FOUND IT: " + attr.get(i).getClass().getSimpleName());
                                photo = (byte[])attr.get(i);
                                //File outputFile = new File("thumbnail.png");
                                keyMap.put(n, attr.get(i));
                            } else {
                                members.add(n + "=" + attr.get(i).toString());
                                keyMap.put(n, attr.get(i).toString());
                            }
                        }
                    }
                }
            }
            catch (NameNotFoundException e) {
                // The base context was not found.
                // Just clean up and exit.
            }
            catch (NamingException e) {
                throw new RuntimeException(e);
            }
            finally {
                if (results != null) {
                    try
                    {
                        results.close();
                    }
                    catch (Exception e)
                    {
                        // Never mind this.
                    }
                }
            }

            // Loop through the memebers of the and print them out
            //for (int i = 0; i < members.size(); i++) {
            //    System.out.println(members.get(i));
            //}
        }
        catch (NamingException e) {
            log.info("WE ARE HERE IN NAMINGEXCEPTION, YOU HAD INVALID CREDENTIAL");
            return null;
        }
        return keyMap;
    }


}
