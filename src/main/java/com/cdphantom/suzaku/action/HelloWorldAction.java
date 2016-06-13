package com.cdphantom.suzaku.action;

import org.springframework.beans.factory.annotation.Autowired;

import com.cdphantom.suzaku.business.IUserBusiness;
import com.cdphantom.suzaku.model.User;
import com.opensymphony.xwork2.ActionSupport;

/**
 * HelloWorld
 * @author cdphantom
 *
 */
public class HelloWorldAction extends ActionSupport {
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

    public String execute() {
        message = "Hello World!";
        return SUCCESS;
    }

    public String list() {
        User user = userBusiness.getUserByUserId("1");
        message = user.getUserName();
        return "list";
    }
}
