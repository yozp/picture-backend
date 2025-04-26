package com.yzj.picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * （已废弃）
 * 更贴合业务的文件上传服务 FileManager
 * 该服务提供一个上传图片并返回图片解析信息的方法
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化 !!!
 */
@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //1、校验图片
        validPicture(multipartFile);

        //2、自定义图片上传地址
        String uuid = RandomUtil.randomString(16);//随机生成16位字符
        String originFilename = multipartFile.getOriginalFilename();//获取上传文件的原始文件名
        //String类的format()方法用于创建格式化的字符串以及连接多个字符串对象
        //FileUtil.getSuffix() 获取文件后缀名，扩展名不带“.”
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));//拼接图片文件名
        //保证每个用户上传的图片都放在不同的文件夹下
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);//拼接图片完整路径

        //3、解析结果并返回
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);//将文件直接保存到指定路径(file)（本地或分布式存储）
            //上传图片（附带图片信息）
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();//获取图片信息
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();//图片宽高比
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);//图片地址
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));//mainName() 返回主文件名
            uploadPictureResult.setPicSize(FileUtil.size(file));//计算目录或文件的总大小
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());//获取图片格式

            // 返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //4、临时文件清理
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     * 由于文件校验规则较复杂，单独抽象为 validPicture 方法，对文件大小、类型进行校验
     *
     * @param multipartFile
     */
    public void validPicture(MultipartFile multipartFile) {
        //1、判空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //2、校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        //3、校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());//获取图片后缀
        //允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
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

    //---------------------------------------------------------------------------------------------------------------------------

    /**
     * 上传图片（通过url方式）
     *
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //1、校验图片
        // validPicture(multipartFile);
        validPicture(fileUrl);
        //2、图片上传地址
        String uuid = RandomUtil.randomString(16);
        //String originFilename=multipartFile.getOriginalFilename();//获取上传文件的原始文件名
        String originFilename = FileUtil.mainName(fileUrl);
        //String类的format()方法用于创建格式化的字符串以及连接多个字符串对象
        //FileUtil.getSuffix() 获取文件后缀名，扩展名不带“.”
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));//拼接图片文件名
        //保证每个用户上传的图片都放在不同的文件夹下
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);//拼接图片完整路径

        //3、解析结果并返回
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            //multipartFile.transferTo(file);//将文件直接保存到指定路径(file)（本地或分布式存储）
            HttpUtil.downloadFile(fileUrl, file);
            //上传图片（附带图片信息）
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();//获取图片信息
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();//图片宽高比
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);//图片地址
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));//mainName() 返回主文件名
            uploadPictureResult.setPicSize(FileUtil.size(file));//计算目录或文件的总大小
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());//获取图片格式

            // 返回可访问地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //4、临时文件清理
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件2
     *
     * @param fileUrl
     */
    public void validPicture(String fileUrl) {
        //1、判空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件不能为空");

        //2、校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        //3、校验url协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");

        //4、发送HEAD请求以验证文件是否存在
        HttpResponse response = null;
        try {
            //HEAD 请求用于检查资源是否存在
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断（这里不返回错误信息主要是因为这个图片不一定是不存在，只是不支持head请求）
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 6. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

}

