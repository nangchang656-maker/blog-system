package cn.lzx.constants;

/**
 * 文章排序相关常量
 *
 * @author
 */
public final class ArticleOrderConstants {

    private ArticleOrderConstants() {
    }

    public static final class OrderBy {
        public static final String CREATE_TIME = "create_time";
        public static final String VIEW_COUNT = "view_count";
        public static final String LIKE_COUNT = "like_count";

        private OrderBy() {
        }
    }

    public static final class OrderType {
        public static final String ASC = "asc";
        public static final String DESC = "desc";

        private OrderType() {
        }
    }
}
