package com.stephen.cloud.post.convert;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.stephen.cloud.api.post.model.dto.post.PostAddRequest;
import com.stephen.cloud.api.post.model.dto.post.PostEditRequest;
import com.stephen.cloud.api.post.model.dto.post.PostUpdateRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.post.model.entity.Post;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * 帖子转换器
 *
 * @author StephenQiu30
 */
public class PostConvert {

    /**
     * 对象转视图
     *
     * @param post 帖子实体
     * @return 帖子视图
     */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        if (StringUtils.isNotBlank(post.getTags())) {
            postVO.setTags(JSONUtil.toList(post.getTags(), String.class));
        }
        return postVO;
    }

    /**
     * 视图转对象
     *
     * @param postVO 帖子视图
     * @return 帖子实体
     */
    public static Post voToObj(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postVO, post);
        List<String> tagList = postVO.getTags();
        if (tagList != null && !tagList.isEmpty()) {
            post.setTags(JSONUtil.toJsonStr(tagList));
        }
        return post;
    }

    /**
     * 新增请求转对象
     *
     * @param postAddRequest 新增请求
     * @return 帖子实体
     */
    public static Post addRequestToObj(PostAddRequest postAddRequest) {
        if (postAddRequest == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postAddRequest, post);
        List<String> tags = postAddRequest.getTags();
        if (ObjectUtils.isNotEmpty(tags)) {
            post.setTags(JSONUtil.toJsonStr(tags));
        }
        return post;
    }

    /**
     * 更新请求转对象
     *
     * @param postUpdateRequest 更新请求
     * @return 帖子实体
     */
    public static Post updateRequestToObj(PostUpdateRequest postUpdateRequest) {
        if (postUpdateRequest == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postUpdateRequest, post);
        List<String> tags = postUpdateRequest.getTags();
        if (ObjectUtils.isNotEmpty(tags)) {
            post.setTags(JSONUtil.toJsonStr(tags));
        }
        return post;
    }

    /**
     * 编辑请求转对象
     *
     * @param postEditRequest 编辑请求
     * @return 帖子实体
     */
    public static Post editRequestToObj(PostEditRequest postEditRequest) {
        if (postEditRequest == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postEditRequest, post);
        List<String> tags = postEditRequest.getTags();
        if (ObjectUtils.isNotEmpty(tags)) {
            post.setTags(JSONUtil.toJsonStr(tags));
        }
        return post;
    }

    /**
     * 对象转 ES 包装类
     *
     * @param post   帖子实体
     * @param userVO 用户视图
     * @return 帖子 ES 包装类
     */
    public static PostEsDTO objToEsDTO(Post post, UserVO userVO) {
        if (post == null) {
            return null;
        }
        PostEsDTO postEsDTO = new PostEsDTO();
        BeanUtils.copyProperties(post, postEsDTO);
        if (StringUtils.isNotBlank(post.getTags())) {
            postEsDTO.setTags(JSONUtil.toList(post.getTags(), String.class));
        }
        postEsDTO.setUserVO(userVO);
        return postEsDTO;
    }
}
