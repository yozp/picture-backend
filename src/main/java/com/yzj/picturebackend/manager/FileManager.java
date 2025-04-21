package com.yzj.picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yzj.picturebackend.common.ResultUtils;
import com.yzj.picturebackend.config.CosClientConfig;
import com.yzj.picturebackend.exception.BusinessException;
import com.yzj.picturebackend.exception.ErrorCode;
import com.yzj.picturebackend.exception.ThrowUtils;
import com.yzj.picturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 更贴合业务的文件上传服务 FileManager
 * 该服务提供一个上传图片并返回图片解析信息的方法
 */
@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix){
        //1、校验图片
        validPicture(multipartFile);

        //2、自定义图片上传地址
        String uuid= RandomUtil.randomString(16);//随机生成16位字符
        String originFilename=multipartFile.getOriginalFilename();//获取上传文件的原始文件名
        //String类的format()方法用于创建格式化的字符串以及连接多个字符串对象
        //FileUtil.getSuffix() 获取文件后缀名，扩展名不带“.”
        String uploadFilename=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
            FileUtil.getSuffix(originFilename));//拼接图片文件名
        //保证每个用户上传的图片都放在不同的文件夹下
        String uploadPath=String.format("/%s/%s",uploadPathPrefix,uploadFilename);//拼接图片完整路径

        //3、解析结果并返回
        File file=null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);//将文件直接保存到指定路径(file)（本地或分布式存储）
            //上传图片（附带图片信息）
            PutObjectResult putObjectResult=cosManager.putPictureObject(uploadPath,file);
            ImageInfo imageInfo=putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();//获取图片信息
            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            int picWidth=imageInfo.getWidth();
            int picHeight=imageInfo.getHeight();
            double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();//图片宽高比
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);//图片地址
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));//mainName() 返回主文件名
            uploadPictureResult.setPicSize(FileUtil.size(file));//计算目录或文件的总大小
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());//获取图片格式

            // 返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("file upload error, filepath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //4、临时文件清理
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     * 由于文件校验规则较复杂，单独抽象为 validPicture 方法，对文件大小、类型进行校验
     * @param multipartFile
     */
    public void validPicture(MultipartFile multipartFile){
        //1、判空
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //2、校验文件大小
        long fileSize=multipartFile.getSize();
        final long ONE_M=1024*1024L;
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
        //3、校验文件后缀
        String fileSuffix= FileUtil.getSuffix(multipartFile.getOriginalFilename());//获取图片后缀
        //允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}

