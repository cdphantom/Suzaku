package com.cdphantom.suzaku.business.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cdphantom.suzaku.business.IUserBusiness;
import com.cdphantom.suzaku.model.User;
import com.cdphantom.suzaku.service.impl.BaseService;

@Transactional
@Service("userBusiness")
public class UserBusinessImpl extends BaseService implements IUserBusiness {

    @Override
    public User getUserByUserId(String userId) {
        return this.get(User.class, userId);
    }

    @Override
    public String addUser(User user) {
        return this.getBaseDAO().save(user);
    }
    
}
