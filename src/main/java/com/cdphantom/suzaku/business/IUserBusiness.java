package com.cdphantom.suzaku.business;

import com.cdphantom.suzaku.model.User;

public interface IUserBusiness {
    
    /**
     * ����userId��ȡUser
     * @param userId
     * @return
     */
    User getUserByUserId(String userId);
    
    /**
     * ����û�
     * @param user
     * @return
     */
    String addUser(User user);
}
