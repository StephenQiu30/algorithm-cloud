package com.stephen.cloud.user.convert;

import cn.hutool.core.util.DesensitizedUtil;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserAddRequest;
import com.stephen.cloud.api.user.model.dto.UserEditRequest;
import com.stephen.cloud.api.user.model.dto.UserUpdateRequest;
import com.stephen.cloud.api.user.model.enums.EmailVerifiedEnum;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.user.model.entity.User;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户转换器
 *
 * @author StephenQiu30
 */
public class UserConvert {

    /**
     * 对象转视图
     *
     * @param user 用户实体
     * @return 用户视图
     */
    public static UserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        // 敏感字段脱敏处理
        userVO.setUserEmail(DesensitizedUtil.email(userVO.getUserEmail()));
        userVO.setUserPhone(DesensitizedUtil.mobilePhone(userVO.getUserPhone()));
        return userVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param userList 用户对象列表
     * @return 用户视图列表
     */
    public static List<UserVO> getUserVO(List<User> userList) {
        return userList.stream().map(UserConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 对象转 ES 包装类
     *
     * @param user 用户实体
     * @return 用户 ES 包装类
     */
    public static UserEsDTO objToEsDTO(User user) {
        if (user == null) {
            return null;
        }
        UserEsDTO userEsDTO = new UserEsDTO();
        BeanUtils.copyProperties(user, userEsDTO);
        return userEsDTO;
    }

    /**
     * 对象转登录视图
     *
     * @param user 用户实体
     * @return 登录用户视图
     */
    public static LoginUserVO objToLoginVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 创建请求转实体对象
     *
     * @param userAddRequest 创建请求
     * @return 用户实体
     */
    public static User addRequestToObj(UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 填充默认值
        user.setUserAvatar(Optional.ofNullable(user.getUserAvatar()).orElse(UserConstant.USER_AVATAR));
        if (user.getEmailVerified() == null) {
            user.setEmailVerified(EmailVerifiedEnum.UNVERIFIED.getValue());
        }
        return user;
    }

    /**
     * 更新请求转实体对象
     *
     * @param userUpdateRequest 更新请求
     * @return 用户实体
     */
    public static User updateRequestToObj(UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        return user;
    }

    /**
     * 编辑请求转实体对象
     *
     * @param userEditRequest 编辑请求
     * @return 用户实体
     */
    public static User editRequestToObj(UserEditRequest userEditRequest) {
        if (userEditRequest == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userEditRequest, user);
        return user;
    }
}
