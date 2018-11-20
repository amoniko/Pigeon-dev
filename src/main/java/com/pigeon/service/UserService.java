package com.pigeon.service;

import com.pigeon.netty.ChatMsg;
import com.pigeon.pojo.Users;
import com.pigeon.pojo.vo.FriendRequestVO;
import com.pigeon.pojo.vo.MyFriendsVO;

import java.util.List;


public interface UserService {

	/**
	 * @Description: 判断用户名是否存在
	 */
	public boolean queryUsernameIsExist(String username);
	
	/**
	 * @Description: 查询用户是否存在
	 */
	public Users queryUserForLogin(String username, String pwd);
	
	/**
	 * @Description: 用户注册
	 */
	public Users saveUser(Users user);

	/**
	 * 修改用户记录
	 */
	public Users updateUserInfo(Users user);

	/**
	 * 	搜索朋友的前置条件
	 */
	public Integer preconditionSearchFriends(String myUserId, String friendUsername);

	/**
	 * 	根据用户名查询用户对象
	 */
	public Users queryUserInfoByUsername( String username);

	/**
	 * 	发送好友请求，记录保存到数据库
	 */
	public void sendFriendRequest(String myUserId, String friendUsername);

	/**
	 * 	查询好友请求
	 */
	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

	/**
	 * 	删除好友请求
	 */
	public void deleteFriendRequest(String sendUserId, String acceptUserId);

	/**
	 * 通过好友请求
	 * 1.保存好友
	 * 2.对面也保存
	 * 3.删除请求记录
	 */
	public void passFriendRequest(String sendUserId, String acceptUserId);

	/**
	 * 	查询好友列表
 	 */
	public List<MyFriendsVO> queryMyFriends(String userId);

	/**
	 * 	保存聊天消息到数据库
	 */
	public String saveMsg(ChatMsg chatMsg);

	/**
	 * 批量签收信息
 	 */
	public void updateMsgSigned(List<String> msgIdList);

	/**
	 * 	获取未签收消息列表
	 */
	public List<com.pigeon.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}


