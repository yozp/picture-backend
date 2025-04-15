package com.yzj.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzj.picturebackend.annotation.AuthCheck;
import com.yzj.picturebackend.common.BaseResponse;
import com.yzj.picturebackend.common.DeleteRequest;
import com.yzj.picturebackend.common.ResultUtils;
import com.yzj.picturebackend.constant.UserConstant;
import com.yzj.picturebackend.exception.BusinessException;
import com.yzj.picturebackend.exception.ErrorCode;
import com.yzj.picturebackend.exception.ThrowUtils;
import com.yzj.picturebackend.model.dto.user.*;
import com.yzj.picturebackend.model.entity.User;
import com.yzj.picturebackend.model.vo.LoginUserVO;
import com.yzj.picturebackend.model.vo.UserVO;
import com.yzj.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

//接口测试：http://localhost:8123/api/doc.html#/home
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result= userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest==null,ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO=userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息（返回脱敏）
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        //这里先跟据request取出用户，然后再进行脱敏处理再返回数据
        User loginUser=userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销（退出登录）
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    //--------------------------------------------------------------------------------------------------------

    /**
     * 管理员创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest==null,ErrorCode.PARAMS_ERROR);
        User user = new User();
        //将userAddRequest复制给user
        BeanUtil.copyProperties(userAddRequest,user);

        //设置默认密码：12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword=userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        boolean result = userService.save(user);
        ThrowUtils.throwIf(result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 管理员跟据id获取用户（未脱敏）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 跟据id获取脱敏用户信息
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        //直接调用上面的方法，得到user之后再脱敏
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 管理员删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果该用户正在登录中，是否先取消其登录态？
        boolean b = userService.removeById(deleteRequest.getId());
        //是否需要验证删除成功？
        return ResultUtils.success(b);
    }

    /**
     * 管理员更新用户信息
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        //将要修改的信息userUpdateRequest复制到新的user中
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        //验证是否修改成功
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户脱敏信息列表（仅管理员）
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //取出分页信息，这个分页信息在PageRequest里面，userQueryRequest继承了它
        long current = userQueryRequest.getCurrent();//当前页数
        long pageSize = userQueryRequest.getPageSize();//每页大小
        //创建一个普通的分页对象，存储未脱敏的用户信息
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        //创建另一个分页对象，存储脱敏后的分页信息
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        //取出userPage里面所有的用户信息，放到getUserVOList进行脱敏
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        //脱敏后放到新的分页对象userVOPage
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
