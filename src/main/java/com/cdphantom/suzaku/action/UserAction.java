package com.cdphantom.suzaku.action;

import org.apache.struts2.convention.annotation.AllowedMethods;
import org.apache.struts2.convention.annotation.ResultPath;
import org.springframework.beans.factory.annotation.Autowired;

import com.cdphantom.suzaku.business.IUserBusiness;
import com.cdphantom.suzaku.model.User;
import com.opensymphony.xwork2.ActionSupport;

@ResultPath("/content/user")
@AllowedMethods("regex:([a-zA-Z]*)")
public class UserAction extends ActionSupport {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    
    @Autowired
    private IUserBusiness userBusiness;
    
    private String message;

    public String getMessage() {
        return message;
    }
    
    public String saveUser() {
        User user = new User();
        user.setUserName("user_test");
        user.setUserPwd("test");
        
        String userId = userBusiness.addUser(user);
        message = userId;
        
        return "list";
    }
    
    public String list() {
        User user = userBusiness.getUserByUserId("1");
        message = user.getUserName();
        return "list";
    }
}
