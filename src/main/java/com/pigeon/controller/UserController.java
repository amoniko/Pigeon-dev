package com.pigeon.controller;

import com.enums.OperatorFriendRequestTypeEnum;
import com.enums.SearchFriendsStatusEnum;
import com.pigeon.pojo.Users;
import com.pigeon.pojo.bo.UsersBO;
import com.pigeon.pojo.vo.MyFriendsVO;
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

import java.util.List;

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

        return PigeonJSONResult.ok(result);
    }


    @PostMapping("/setNickname")
    public PigeonJSONResult setNickname(@RequestBody UsersBO userBO) throws Exception {

        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);

        return PigeonJSONResult.ok(result);
    }

    /**
     * 根据账号做匹配查询，而不是模糊查询
     */

    @PostMapping("/search")
    public PigeonJSONResult searchUser(String myUserId, String friendUsername) throws Exception {

        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return PigeonJSONResult.errorMsg("");
        }

        // 1.搜索的用户如果不存在，返回【无此用户】
        // 2.搜索的用户如果是你自己，返回【不可以添加自己】
        // 3.搜索的用户如果已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO userVO = new UsersVO();
            BeanUtils.copyProperties(user, userVO);
            return PigeonJSONResult.ok(userVO);

        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return PigeonJSONResult.errorMsg(errorMsg);
        }
    }

    /**
     * 添加好友请求
     */
    @PostMapping("/addFriendRequest")
    public PigeonJSONResult addFriendRequest(String myUserId, String friendUsername) throws Exception {

        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return PigeonJSONResult.errorMsg("");
        }

        // 1.搜索的用户如果不存在，返回【无此用户】
        // 2.搜索的用户如果是你自己，返回【不可以添加自己】
        // 3.搜索的用户如果已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return PigeonJSONResult.errorMsg(errorMsg);
        }

        return PigeonJSONResult.ok();
    }

   /**
    * 查询用户接受到的朋友申请
    */
    @PostMapping("/queryFriendRequests")
    public PigeonJSONResult queryFriendRequest(String userId) throws Exception {
        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(userId)) {
            return PigeonJSONResult.errorMsg("");
        }
        // 1.查询用户接受到的朋友申请
        return PigeonJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    /**
     * 接受方 通过或者忽略朋友请求
     */
    @PostMapping("/operFriendRequest")
    public PigeonJSONResult operFriendRequest(String acceptUserId, String sendUserId,
                                              Integer operType) throws Exception {
        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(sendUserId)
                ||StringUtils.isBlank(acceptUserId)
                || operType == null) {
            return PigeonJSONResult.errorMsg("");
        }

        // 1.如果操作类型没有对应的枚举值，直接抛出空错误信息
       if( StringUtils.isBlank( OperatorFriendRequestTypeEnum.getMsgByType(operType))){
            return PigeonJSONResult.errorMsg("");
        }

       if(operType == OperatorFriendRequestTypeEnum.IGNORE.type){
           // 2.判断如果忽略好友请求，则直接删除好友请求数据库记录
           userService.deleteFriendRequest(sendUserId, acceptUserId);
       }else if(operType == OperatorFriendRequestTypeEnum.PASS.type){
           // 3.判断如果忽略好友请求，则增加好友记录到数据库对应的表
           //   然后删除好友请求的数据库记录。
            userService.passFriendRequest(sendUserId, acceptUserId);
       }
        // 4. 数据库查询好友列表
        List<MyFriendsVO> myFirends = userService.queryMyFriends(acceptUserId);

        return PigeonJSONResult.ok(myFirends);

    }

    /**
     * 查询我的好友列表
     */
    @PostMapping("/myFriends")
    public PigeonJSONResult myFriends(String userId){
        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(userId)) {
            return PigeonJSONResult.errorMsg("");
        }

        // 1. 数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);

        return PigeonJSONResult.ok(myFriends);

    }

    /**
     *  用户手机端获取未签收的消息列表
     */
    @PostMapping("/getUnReadMsgList")
    public PigeonJSONResult getUnReadMsgList(String acceptUserId) {
        // 0. 判断传入参数不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return PigeonJSONResult.errorMsg("");
        }
        // 查询列表
        List<com.pigeon.pojo.ChatMsg> unreadMsgList = userService.getUnReadMsgList(acceptUserId);

        return PigeonJSONResult.ok(unreadMsgList);

    }



}
