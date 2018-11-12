package com.pigeon.controller;

import com.pigeon.pojo.Users;
import com.pigeon.pojo.vo.UsersVO;
import com.pigeon.service.UserService;
import com.pigeon.utils.MD5Utils;
import com.pigeon.utils.PigeonJSONResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("u")
public class UserController {
	
	@Autowired
	private UserService userService;



	/**
	 * @Description: 用户注册/登录
	 */
	@RequestMapping(value = "/registOrLogin")
	public PigeonJSONResult registOrLogin(@RequestBody Users user) throws Exception {

		System.out.println("message coming in");
		// 0. 判断用户名和密码不能为空
		if (StringUtils.isBlank(user.getUsername())
				|| StringUtils.isBlank(user.getPassword())) {
			return PigeonJSONResult.errorMsg("用户名或密码不能为空...");
		}

		// 1. 判断用户名是否存在，如果存在就登录，如果不存在则注册
		boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
		Users userResult = null;
		if (usernameIsExist) {
			// 1.1 登录
			userResult = userService.queryUserForLogin(user.getUsername(),
									MD5Utils.getMD5Str(user.getPassword()));
			if (userResult == null) {
				return PigeonJSONResult.errorMsg("用户名或密码不正确...");
			}
		} else {
			// 1.2 注册
			user.setNickname(user.getUsername());
			user.setFaceImage("");
			user.setFaceImageBig("");
			user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
			userResult = userService.saveUser(user);
		}

		UsersVO userVO = new UsersVO();
		BeanUtils.copyProperties(userResult, userVO);

		return PigeonJSONResult.ok(userVO);
	}
	

}
