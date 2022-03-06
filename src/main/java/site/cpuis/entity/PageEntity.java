package site.cpuis.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class PageEntity<T> {
    private List<T> pageData;
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;

    private PageEntity(List<T> pageData, Integer currentPage, Integer pageSize, Integer totalPages) {
        this.pageData = pageData;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public static <Q> PageEntity<Q> byAllDataNum(List<Q> data, Integer allDataNum, Integer currentPage, Integer pageSize) {
        return byTotalPages (data, allDataNum % pageSize == 0 ? allDataNum / pageSize : allDataNum / pageSize + 1, currentPage, pageSize);
    }

    public static <Q> PageEntity<Q> byTotalPages(List<Q> data, Integer totalPages, Integer currentPage, Integer pageSize) {
//        if (currentPage < 0 || currentPage > totalPages) {
//            throw new IllegalArgumentException ();
//        }
//        if (!Objects.equals (currentPage, totalPages) && data.size () != pageSize) {
//            throw new IllegalArgumentException ();
//        }
        return new PageEntity<> (data, currentPage, pageSize, totalPages);
    }
}
