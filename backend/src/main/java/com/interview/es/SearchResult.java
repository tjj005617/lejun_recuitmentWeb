package com.interview.es;

import lombok.Data;
import java.util.List;

/**
 * 搜索结果封装
 */
@Data
public class SearchResult<T> {

    /** 总数 */
    private long total;

    /** 总数关系：eq=精确, gte=大于等于（ES默认上限10000） */
    private String totalHitsRelation;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private int page;

    /** 每页数量 */
    private int size;

    /** 总页数 */
    private int totalPages;

    public static <T> SearchResult<T> of(List<T> records, long total, int page, int size) {
        SearchResult<T> result = new SearchResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        result.setTotalHitsRelation("eq");
        return result;
    }

    public static <T> SearchResult<T> of(List<T> records, long total, String totalHitsRelation, int page, int size) {
        SearchResult<T> result = new SearchResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setTotalHitsRelation(totalHitsRelation);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }
}
