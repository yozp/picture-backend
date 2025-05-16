package com.yzj.picturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 给前端展示所有的空间级别信息
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
