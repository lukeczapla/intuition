package org.magicat.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import io.swagger.annotations.ApiOperation;
import org.magicat.config.GoogleProperties;
import org.magicat.model.User;
import org.magicat.repository.UserRepository;
import org.magicat.service.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ApiIgnore
@RestController
public class UserController {

    private final static Logger log = LoggerFactory.getLogger(UserController.class);
    private static final JsonFactory jacksonFactory = new GsonFactory();

    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final GoogleProperties googleProperties;

    @Autowired
    public UserController(UserDetailsService userDetailsService, UserRepository userRepository, GoogleProperties googleProperties) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.googleProperties = googleProperties;
    }

    @ApiOperation("Check user status")
    @RequestMapping(value = "/conf/user", method = RequestMethod.GET)
    public String user(Principal user) {
        if (user != null) return user.getName();
        return "NONE";
    }

    @ApiOperation(value = "Log out current session", notes = "Uses MSKCC LDAP on domain controller")
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @ApiOperation(value = "Authenticate the provided user", notes = "User information obtained from MSKCC LDAP on DC")
    @RequestMapping(value = "/conf/user", method = RequestMethod.POST)
    public String login(@RequestBody User user) throws Exception {
        if (user == null || user.getEmailAddress() == null || !user.getEmailAddress().contains("@mskcc.org")) {
            return "Invalid data";
        }
        /*Map<String, Object> items = userDetailsService.fetchLDAP(user.getEmailAddress(), user.getPassword());
        if (items == null) {
            return "Invalid username/password combination";
        } else {
            if (items.containsKey("givenName")) user.setFirstName((String)items.get("givenName"));
            if (items.containsKey("displayName")) {
                String displayName = items.get("displayName").toString();
                user.setLastName(displayName.substring(0, displayName.indexOf(',')));
            }
            if (items.containsKey("manager")) {
                String manager = items.get("manager").toString();
                user.setGroup(manager.substring(manager.indexOf('=')+1, manager.indexOf(",")));
            }
            if (items.containsKey("title")) user.setJobTitle(items.get("title").toString());
            if (items.containsKey("thumbnailPhoto")) {
                user.setImage(new Binary((byte[])items.get("thumbnailPhoto")));
                user.setContentType("image/png");
                user.setImageFileName("thumbnail.png");
            }
            if (items.containsKey("telephoneNumber")) user.setMobilePhone(items.get("telephoneNumber").toString());
            if (items.containsKey("streetAddress")) user.setOfficeLocation(items.get("streetAddress").toString());
            log.info("Verified account");
        } */
        log.info("Trying to loadUserByUsername");
        if (userDetailsService.loadUserByUsername(user.getEmailAddress()) != null) {

            log.info("Found user already");
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user.getEmailAddress(), null,
                    userDetailsService.loadUserByUsername(user.getEmailAddress()).getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authRequest);

            return "Finished, authenticated";

        } else {
            return "Invalid email address";
            /*
            log.info("Creating new account for " + user.getEmailAddress());
            try {
                userDetailsService.registerNewAccount(user);
            } catch (Exception e) {
                e.printStackTrace();
                return "Invalid email address";
            }
            log.info("Registered account for " + user.getEmailAddress());

            Set<GrantedAuthority> grant = new HashSet<GrantedAuthority>();
            grant.add(new SimpleGrantedAuthority(user.getRole().toString()));
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user.getEmailAddress(), null, grant);
            SecurityContextHolder.getContext().setAuthentication(authRequest);
            log.info("Success creating account for " + user.getEmailAddress());
            return "Created, authenticated";*/
        }
    }

    @RequestMapping(value = "/conf/user/me", method = RequestMethod.GET)
    public ResponseEntity<User> checkAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            log.info(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
            return new ResponseEntity<>(userRepository.findByEmailAddress(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ApiOperation(value = "Authenticate the provided user", notes = "User information obtained from Google")
    @RequestMapping(value = "/conf/usergoogle", method = RequestMethod.POST)
    public ResponseEntity loginGoogle(@RequestBody User user) throws Exception {
        if (user == null || user.getEmailAddress() == null || user.getTokenId() == null) {
            return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
        }
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jacksonFactory)
                .setAudience(Collections.singletonList(googleProperties.getClientId()))
                .build();
        log.info("Login Controller: " + user.getEmailAddress() + " " + user.getTokenId().substring(0,5));
        log.info(user.getTokenId());
        GoogleIdToken idToken = verifier.verify(user.getTokenId());

        if (idToken == null) {
            return new ResponseEntity("Invalid token data", HttpStatus.BAD_REQUEST);
        } else {
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (payload == null || !payload.getEmail().equals(user.getEmailAddress())) {
                return new ResponseEntity("Invalid email address", HttpStatus.BAD_REQUEST);
            }
            log.info("Verified account");
        }

        if (userDetailsService.loadUserByUsername(user.getEmailAddress()) != null) {

            UsernamePasswordAuthenticationToken authrequest = new UsernamePasswordAuthenticationToken(user.getEmailAddress(), null,
                    userDetailsService.loadUserByUsername(user.getEmailAddress()).getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authrequest);

            return new ResponseEntity("Finished, authenticated", HttpStatus.OK);

        } else {
            try {
                userDetailsService.registerNewAccount(user);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity("Invalid email address", HttpStatus.BAD_REQUEST);
            }

            Set<GrantedAuthority> grant = new HashSet<>();
            grant.add(new SimpleGrantedAuthority(user.getRole().toString()));
            UsernamePasswordAuthenticationToken authrequest = new UsernamePasswordAuthenticationToken(user.getEmailAddress(), null, grant);
            SecurityContextHolder.getContext().setAuthentication(authrequest);
            return new ResponseEntity("Created, authenticated", HttpStatus.OK);

        }
        //return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
    }



}
