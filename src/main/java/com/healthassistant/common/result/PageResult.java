package com.healthassistant.common.result;

import org.springframework.data.domain.Page;
import java.util.List;

public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;

    private PageResult() {}

    public static <T> PageResult<T> of(Page<T> springPage) {
        PageResult<T> r = new PageResult<>();
        r.records = springPage.getContent();
        r.total = springPage.getTotalElements();
        r.page = springPage.getNumber();
        r.size = springPage.getSize();
        return r;
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }

    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
