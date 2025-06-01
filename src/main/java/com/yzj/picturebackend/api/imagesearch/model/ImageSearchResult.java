package com.yzj.picturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * 图片搜索结果类
 * 用于接受 API 的返回值
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
