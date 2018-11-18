package com.pigeon.service.impl;

import com.pigeon.mapper.UsersMapper;
import com.pigeon.pojo.Users;
import com.pigeon.service.UserService;
import com.pigeon.utils.FastDFSClient;
import com.pigeon.utils.FileUtils;
import com.pigeon.utils.QRCodeUtils;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.entity.Example.Criteria;

import java.io.IOException;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UsersMapper userMapper;

	@Autowired
	private Sid sid;

	@Autowired
	private QRCodeUtils qrCodeUtils;

	@Autowired
	private FastDFSClient fastDFSClient;

	
	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public boolean queryUsernameIsExist(String username) {
		
		Users user = new Users();
		user.setUsername(username);
		
		Users result = userMapper.selectOne(user);
		
		return result != null ? true : false;
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public Users queryUserForLogin(String username, String pwd) {
		
		Example userExample = new Example(Users.class);
		Criteria criteria = userExample.createCriteria();
		
		criteria.andEqualTo("username", username);
		criteria.andEqualTo("password", pwd);
		
		Users result = userMapper.selectOneByExample(userExample);
		
		return result;
	}

	//i want to set it to private, but idea says no
	@Transactional(propagation = Propagation.SUPPORTS)
	Users queryUserById(String userId){

		return userMapper.selectByPrimaryKey(userId);
	}



	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public Users updateUserInfo(Users user) {
		//根据主键做更新，selective的话如果有没用的属性就不会更新，所以一般用这个
		userMapper.updateByPrimaryKeySelective(user);
		return queryUserById(user.getId());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public Users saveUser(Users user) {

		String userId = sid.nextShort();

		//为每个用户生成唯一的二维码(包含文字，url，json都可以)

		String qrCodePath = "D://user" + userId + "qrcode.png";
		//pigeon_qrcode:[username]
		qrCodeUtils.createQRCode(qrCodePath,"pigeon_qrcode:" + user.getUsername() );
		MultipartFile qrcodeFile = FileUtils.fileToMultipart(qrCodePath);

		String qrCodeUrl = "";
		try {
			qrCodeUrl = fastDFSClient.uploadQRCode(qrcodeFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		user.setQrcode(qrCodeUrl);

		user.setId(userId);
		userMapper.insert(user);

		return user;
	}

}
