package com.recruit.airecruitsystem.dto.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private long total;
    private List<T> items;

    public static <T> PageResponse<T> of(PageInfo<T> pageInfo) {
        PageResponse<T> response = new PageResponse<>();
        response.setTotal(pageInfo.getTotal());
        response.setItems(pageInfo.getList());
        return response;
    }
}
