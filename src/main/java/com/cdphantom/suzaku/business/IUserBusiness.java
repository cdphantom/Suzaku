package com.cdphantom.suzaku.business;

import com.cdphantom.suzaku.model.User;

public interface IUserBusiness {
    
    /**
     * 根据userId获取User
     * @param userId
     * @return
     */
    User getUserByUserId(String userId);
    
    /**
     * 添加用户
     * @param user
     * @return
     */
    String addUser(User user);
}
