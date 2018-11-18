package com.pigeon.service.impl;

import com.enums.SearchFriendsStatusEnum;
import com.pigeon.mapper.FriendsRequestMapper;
import com.pigeon.mapper.MyFriendsMapper;
import com.pigeon.mapper.UsersMapper;
import com.pigeon.pojo.FriendsRequest;
import com.pigeon.pojo.MyFriends;
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
import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UsersMapper userMapper;

	@Autowired
	private MyFriendsMapper myFriendsMapper;

	@Autowired
	private Sid sid;

	@Autowired
	private QRCodeUtils qrCodeUtils;

	@Autowired
	private FastDFSClient fastDFSClient;

	@Autowired
	private FriendsRequestMapper friendsRequestMapper;

	
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

	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

		// 1.搜索的用户如果不存在，返回【无此用户】
		Users user = queryUserInfoByUsername(friendUsername);
		if(user == null){
			return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
		}
		// 2.搜索的用户如果是你自己，返回【不可以添加自己】
		if(user.getId().equals(myUserId)){
			return SearchFriendsStatusEnum.NOT_YOURSELF.status;
		}

		// 3.搜索的用户如果已经是你的好友，返回【该用户已经是你的好友】
		Example mfe = new Example(MyFriends.class);
		Criteria mfc = mfe.createCriteria();
		mfc.andEqualTo("myUserId", myUserId);
		mfc.andEqualTo("myFriendUserId", user.getId());
		MyFriends myFriendsRel = myFriendsMapper.selectOneByExample(mfe);
		if(myFriendsRel != null){
			return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
		}
		return SearchFriendsStatusEnum.SUCCESS.status;
	}

	@Override
	public Users queryUserInfoByUsername( String username){
		Example ue = new Example(Users.class);
		Criteria uc = ue.createCriteria();
		uc.andEqualTo("username", username);
		return userMapper.selectOneByExample(ue);
	}

	@Override
	public void sendFriendRequest(String myUserId, String friendUsername) {
		//判断这条记录存不存在，避免多次同样的请求

		//根据用户名把朋友信息查询出来
		Users friend = queryUserInfoByUsername(friendUsername);

		// 1.查询发送好友请求记录表
		Example fre = new Example(FriendsRequest.class);
		Criteria frc = fre.createCriteria();
		frc.andEqualTo("sendUserId", myUserId);
		frc.andEqualTo("acceptUserId", friend.getId());
		FriendsRequest friendRequest = friendsRequestMapper.selectOneByExample(fre);

		if(friendRequest == null){
			//2.如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
			String requestId = sid.nextShort();

			FriendsRequest request = new FriendsRequest();
			request.setId(requestId);
			request.setSendUserId(myUserId);
			request.setAcceptUserId(friend.getId());
			request.setRequestDateTime(new Date());
			friendsRequestMapper.insert(request);
		}
	}
}
