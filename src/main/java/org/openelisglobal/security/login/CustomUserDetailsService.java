package org.openelisglobal.security.login;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userDetailsService")
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    LoginUserService loginService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginName) {
        LoginUser user = loginService.getMatch("loginName", loginName).orElseThrow(() -> new UsernameNotFoundException(
                "Unique Username not found, could be duplicates in database or it doesn't" + " exist"));

        boolean disabled = user.getAccountDisabled().equalsIgnoreCase(IActionConstants.YES);
        boolean locked = user.getAccountLocked().equalsIgnoreCase(IActionConstants.YES);
        boolean credentialsExpired = user.getPasswordExpiredDayNo() <= 0;
        return new org.springframework.security.core.userdetails.User(user.getLoginName(), user.getPassword(),
                !disabled, true, !credentialsExpired, !locked, getGrantedAuthorities(user));
    }

    // TODO flesh this out so we can do permissions solely through granted
    // authorities
    // for sso and form login methods
    private List<GrantedAuthority> getGrantedAuthorities(LoginUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        return authorities;
    }
}
