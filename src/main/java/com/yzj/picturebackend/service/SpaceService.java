package com.yzj.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzj.picturebackend.model.dto.space.SpaceAddRequest;
import com.yzj.picturebackend.model.dto.space.SpaceQueryRequest;
import com.yzj.picturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yzj.picturebackend.model.entity.User;
import com.yzj.picturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 杨钲键
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-05-12 19:46:06
 */
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间信息
     * 主要校验具体操作、空间名称、空间级别
     *
     * @param space
     * @param add   add 参数用来区分是创建数据时校验还是编辑时校验
     */
    void validSpace(Space space, boolean add);

    /**
     * 自动填充限额数据（复用）
     *
     * @param space
     */
    public void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验操作空间的权限
     *
     * @param loginUser
     * @param oldSpace
     */
    void checkSpaceAuth(User loginUser, Space oldSpace);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
}
