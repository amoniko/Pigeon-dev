package com.pigeon.mapper;

import com.pigeon.pojo.Users;
import com.pigeon.pojo.vo.FriendRequestVO;
import com.pigeon.pojo.vo.MyFriendsVO;
import com.pigeon.utils.MyMapper;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public interface UsersMapperCustom extends MyMapper<Users> {

    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    public List<MyFriendsVO> queryMyFriends(String userId);

    public void batchUpdateMsgSigned(List<String> msgIdList);
}