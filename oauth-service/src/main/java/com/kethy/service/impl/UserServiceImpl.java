package com.kethy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kethy.constant.MessageConstant;
import com.kethy.domain.entity.User;
import com.kethy.mapper.UserMapper;
import com.kethy.principal.UserPrincipal;
import com.kethy.service.UserService;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            throw new UsernameNotFoundException(MessageConstant.USERNAME_PASSWORD_ERROR);
        }
        UserPrincipal userPrincipal = new UserPrincipal(user);
        if (!userPrincipal.isEnabled()) {
            throw new DisabledException(MessageConstant.ACCOUNT_DISABLED);
        } else if (!userPrincipal.isAccountNonLocked()) {
            throw new LockedException(MessageConstant.ACCOUNT_LOCKED);
        } else if (!userPrincipal.isAccountNonExpired()) {
            throw new AccountExpiredException(MessageConstant.ACCOUNT_EXPIRED);
        } else if (!userPrincipal.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(MessageConstant.CREDENTIALS_EXPIRED);
        }
        return userPrincipal;
    }
}
