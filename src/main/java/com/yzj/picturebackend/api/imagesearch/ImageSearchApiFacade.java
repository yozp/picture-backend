package com.yzj.picturebackend.api.imagesearch;

import com.yzj.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.yzj.picturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.yzj.picturebackend.api.imagesearch.sub.GetImageListApi;
import com.yzj.picturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 以图搜图统一接口
 * 将多个 API 整合到一个门面类中，简化调用过程（门面/外观模式）
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
