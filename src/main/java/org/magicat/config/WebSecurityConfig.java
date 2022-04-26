package org.magicat.config;

//import edu.dnatools.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

//    @Autowired
//    private UserRepository userRepository;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
        auth.authenticationProvider(authenticationProvider());
    }

    public void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and().csrf().disable().authorizeRequests()
                .antMatchers(PUT, "/submit").hasAnyRole("USER", "ADMIN")
                .antMatchers(PUT, "/addprotein").hasAnyRole("USER", "ADMIN")
                .antMatchers(GET, "/conf/userlist").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()
                .cors().configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues()).and().logout(logout -> logout
                .permitAll()
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK)));
    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}

