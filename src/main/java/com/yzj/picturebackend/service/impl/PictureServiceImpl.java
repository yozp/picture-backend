package com.yzj.picturebackend.service.impl;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzj.picturebackend.exception.ErrorCode;
import com.yzj.picturebackend.exception.ThrowUtils;
import com.yzj.picturebackend.manager.FileManager;
import com.yzj.picturebackend.model.dto.file.UploadPictureResult;
import com.yzj.picturebackend.model.dto.picture.PictureQueryRequest;
import com.yzj.picturebackend.model.dto.picture.PictureUploadRequest;
import com.yzj.picturebackend.model.entity.Picture;
import com.yzj.picturebackend.model.entity.User;
import com.yzj.picturebackend.model.vo.PictureVO;
import com.yzj.picturebackend.model.vo.UserVO;
import com.yzj.picturebackend.service.PictureService;
import com.yzj.picturebackend.mapper.PictureMapper;
import com.yzj.picturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 杨钲键
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-04-22 15:53:44
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile,
                                   PictureUploadRequest pictureUploadRequest, User loginUser) {
        //1、判断操作和是否登录
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NO_AUTH_ERROR);
        //判断是新增还是更新图片
        Long pictureId=null;
        if(pictureUploadRequest!=null){
            pictureId=pictureUploadRequest.getId();
        }
        //如果是更新图片，先校验图片是否存在
        if(pictureId!=null){
            boolean exists=this.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        }

        //2、上传图片，得到信息
        //跟据用户id来划分存储目录
        String uploadPathPrefix=String.format("public/%s",loginUser.getId());
        UploadPictureResult uploadPictureResult=fileManager.uploadPicture(multipartFile,uploadPathPrefix);

        //3、构造要入库的图片信息
        Picture picture=new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        //之所以有第二次判断id，是因为两次操作的内容不能同时进行
        if(pictureId!=null){
            //如果是更新，需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //4、更新或者新增图片信息
        boolean result=this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");

        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        //1、判空
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        if(pictureQueryRequest==null){
            return queryWrapper;
        }

        //2、从对象取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        //3、判断是否需要多字段搜索
        if(StrUtil.isNotBlank(searchText)){
            //拼接查询条件(图片名和简介)
            //Lambda 表达式 qw -> ...
            //qw 是 QueryWrapper 类型的一个临时变量，代表当前正在构建的子查询条件
            queryWrapper.and(qw->qw.like("name",searchText)
                    .or() //将前后两个条件用 OR 连接起来
                    .like("introduction",searchText)
            );
        }

        //4、拼接常规条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        //数组查询
        if(CollUtil.isNotEmpty(tags)){
            for(String tag:tags){
                queryWrapper.like("tags","\""+tag+"\"");
            }
        }

        //5、排序后返回结果
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象转封装
        PictureVO pictureVO=PictureVO.objToVo(picture);
        //关联查询用户
        Long userId=picture.getUserId();
        if(userId!=null&&userId>0){
            User user=userService.getById(userId);//跟据id取出用户
            UserVO userVO=userService.getUserVO(user);//封装用户（脱敏）
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        //1、取值并判空
        List<Picture> pictureList=picturePage.getRecords();
        Page<PictureVO> pictureVOPage=new Page<>(picturePage.getCurrent(),picturePage.getSize(),picturePage.getTotal());
        //之所以这里先创建Page对象，是因为判断为空的情况下可以立马返回一个对象值
        if(CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }

        //2、对象列表 => 封装对象列表
        List<PictureVO> pictureVOList=pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        //3、关联查询用户
        //先将所有图片的用户取出放进set去重，然后跟据id查询对应的用户，将按id分组放进map里面
        Set<Long> userIdSet=pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //使用map的主要原因时方便跟据id查出对应的user，方便接下来的填充用户信息操作
        //注意listByIds()方法返回一个list，将这个list的元素类型User的id成员分组，再放进map里面
        Map<Long, List<User>> userIdUserListMap=userService.listByIds(userIdSet)
                .stream().collect(Collectors.groupingBy(User::getId));

        //4、循环填充用户信息
        pictureVOList.forEach(pictureVO -> {
            //取出用户id
            Long userId=pictureVO.getUserId();
            User user=null;
            //在map中搜索是否存在该id，存在则取出第一个user
            if(userIdUserListMap.containsKey(userId)){
                user=userIdUserListMap.get(userId).get(0);
            }
            //存储用户信息(脱敏)
            pictureVO.setUser(userService.getUserVO(user));
        });

        //5、返回结果
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        //1、判空
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR);
        //2、从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //3、对取出来的值进行再次校验
        //修改数据时，id不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id),ErrorCode.PARAMS_ERROR,"id 不能为空");
        if(StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if(StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }
}




