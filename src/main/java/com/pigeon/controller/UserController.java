package com.pigeon.controller;

import com.pigeon.pojo.Users;
import com.pigeon.pojo.bo.UsersBO;
import com.pigeon.pojo.vo.UsersVO;
import com.pigeon.service.UserService;
import com.pigeon.utils.FastDFSClient;
import com.pigeon.utils.FileUtils;
import com.pigeon.utils.MD5Utils;
import com.pigeon.utils.PigeonJSONResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("u")
public class UserController {

	//i don't care if field injection is not recommended, i like it.
	@Autowired
	private UserService userService;

	@Autowired
	private FastDFSClient fastDFSClient;

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

	@PostMapping("/uploadFaceBase64")
	public PigeonJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {

		// 获取前端传过来的base64字符串, 然后转换为文件对象再上传
		String base64Data = userBO.getFaceData();
		String userFacePath = "D:\\" + userBO.getUserId() + "userface64.png";
		FileUtils.base64ToFile(userFacePath, base64Data);

		// 上传文件到Fastdfs
		MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
		String url = fastDFSClient.uploadBase64(faceFile);
		System.out.println(url);

//		"dhawuzxc-xzcidasdasduh3u8Fp9u98432.png"  大图
//		"dhawuzxc-xzcidasdasduh3u89u98432_80x80.png"   小图

		// 获取缩略图的url
		String thump = "_80x80.";
		String arr[] = url.split("\\.");
		String thumpImgUrl = arr[0] + thump + arr[1];

		// 更细用户头像
		Users user = new Users();
		user.setId(userBO.getUserId());
		user.setFaceImage(thumpImgUrl);
		user.setFaceImageBig(url);

		Users result = userService.updateUserInfo(user);

		return PigeonJSONResult .ok(result);
	}


	@PostMapping("/setNickname")
	public PigeonJSONResult setNickname(@RequestBody UsersBO userBO) throws Exception{

		Users user = new Users();
		user.setId(userBO.getUserId());
		user.setNickname(userBO.getNickname());


		Users result = userService.updateUserInfo(user);

		return PigeonJSONResult .ok(result);
	}


}
