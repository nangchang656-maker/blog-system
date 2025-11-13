package cn.lzx.blog.integration.es;

import cn.lzx.blog.config.es.ElasticsearchProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 工具类
 * 封装常用的 ES 操作方法
 * 
 * 由于 Java 的类型擦除机制,使用 @SuppressWarnings("unchecked") 注解是标准做法。
 *
 * @author lzx
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchUtil {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchProperties elasticsearchProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     * @param mapping   映射配置 JSON 字符串
     * @return 是否成功
     */
    public boolean createIndex(String indexName, String mapping) {
        try {
            // 创建索引请求
            CreateIndexResponse response = elasticsearchClient.indices().create(c -> c
                    .index(indexName)
                    .settings(s -> s
                            .numberOfShards(String.valueOf(elasticsearchProperties.getNumberOfShards()))
                            .numberOfReplicas(String.valueOf(elasticsearchProperties.getNumberOfReplicas()))
                    )
                    .withJson(new StringReader(mapping))
            );

            log.info("创建索引成功: {}", indexName);
            return response.acknowledged();
        } catch (IOException e) {
            log.error("创建索引失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 判断索引是否存在
     *
     * @param indexName 索引名称
     * @return 是否存在
     */
    public boolean indexExists(String indexName) {
        try {
            return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            log.error("检查索引是否存在失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return 是否成功
     */
    public boolean deleteIndex(String indexName) {
        try {
            DeleteIndexResponse response = elasticsearchClient.indices().delete(d -> d.index(indexName));
            log.info("删除索引成功: {}", indexName);
            return response.acknowledged();
        } catch (IOException e) {
            log.error("删除索引失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 添加文档
     *
     * @param indexName  索引名称
     * @param documentId 文档 ID
     * @param jsonData   文档数据(JSON 格式)
     * @return 是否成功
     */
    public boolean addDocument(String indexName, String documentId, String jsonData) {
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(documentId)
                    .withJson(new StringReader(jsonData))
            );

            log.debug("添加文档成功: {} -> {}", indexName, documentId);
            return response.shards().successful().intValue() > 0;
        } catch (IOException e) {
            log.error("添加文档失败: {} -> {}", indexName, documentId, e);
            return false;
        }
    }

    /**
     * 添加文档（使用实体类）
     *
     * @param indexName  索引名称
     * @param documentId 文档 ID
     * @param document   文档实体对象
     * @return 是否成功
     */
    public <T> boolean addDocument(String indexName, String documentId, T document) {
        try {
            String jsonData = objectMapper.writeValueAsString(document);
            return addDocument(indexName, documentId, jsonData);
        } catch (Exception e) {
            log.error("添加文档失败（实体类序列化）: {} -> {}", indexName, documentId, e);
            return false;
        }
    }

    /**
     * 更新文档
     *
     * @param indexName  索引名称
     * @param documentId 文档 ID
     * @param jsonData   更新的数据(JSON 格式)
     * @return 是否成功
     */
    @SuppressWarnings("unchecked")
    public boolean updateDocument(String indexName, String documentId, String jsonData) {
        try {
            UpdateResponse<Map<String, Object>> response = elasticsearchClient.update(u -> u
                    .index(indexName)
                    .id(documentId)
                    .withJson(new StringReader("{\"doc\":" + jsonData + "}")),
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            log.debug("更新文档成功: {} -> {}", indexName, documentId);
            return response.shards().successful().intValue() > 0;
        } catch (IOException e) {
            log.error("更新文档失败: {} -> {}", indexName, documentId, e);
            return false;
        }
    }

    /**
     * 删除文档
     *
     * @param indexName  索引名称
     * @param documentId 文档 ID
     * @return 是否成功
     */
    public boolean deleteDocument(String indexName, String documentId) {
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(documentId)
            );

            log.debug("删除文档成功: {} -> {}", indexName, documentId);
            return response.shards().successful().intValue() > 0;
        } catch (IOException e) {
            log.error("删除文档失败: {} -> {}", indexName, documentId, e);
            return false;
        }
    }

    /**
     * 批量添加文档
     *
     * @param indexName 索引名称
     * @param dataList  文档数据列表 (key: documentId, value: jsonData)
     * @return 是否成功
     */
    public boolean bulkAddDocuments(String indexName, Map<String, String> dataList) {
        try {
            List<BulkOperation> bulkOperations = new ArrayList<>();

            dataList.forEach((documentId, jsonData) -> {
                bulkOperations.add(BulkOperation.of(b -> b
                        .index(i -> i
                                .index(indexName)
                                .id(documentId)
                                .withJson(new StringReader(jsonData))
                        )
                ));
            });

            BulkResponse response = elasticsearchClient.bulk(r -> r
                    .index(indexName)
                    .operations(bulkOperations)
            );

            log.info("批量添加文档成功: {} -> {} 条", indexName, dataList.size());
            return !response.errors();
        } catch (IOException e) {
            log.error("批量添加文档失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 全文搜索(支持高亮)
     *
     * @param indexName       索引名称
     * @param keyword         搜索关键词
     * @param searchFields    搜索字段列表
     * @param highlightFields 高亮字段列表
     * @param from            起始位置(分页)
     * @param size            每页数量
     * @return 搜索结果列表
     */
    public List<Map<String, Object>> search(String indexName, String keyword, String[] searchFields,
                                             String[] highlightFields, int from, int size) {
        SearchResult result = searchWithTotal(indexName, keyword, searchFields, highlightFields, from, size);
        return result.getResults();
    }

    /**
     * 全文搜索(支持高亮，返回总数)
     *
     * @param indexName       索引名称
     * @param keyword         搜索关键词
     * @param searchFields    搜索字段列表
     * @param highlightFields 高亮字段列表
     * @param from            起始位置(分页)
     * @param size            每页数量
     * @return 搜索结果（包含结果列表和总数）
     */
    @SuppressWarnings("unchecked")
    public SearchResult searchWithTotal(String indexName, String keyword, String[] searchFields,
                                        String[] highlightFields, int from, int size) {
        try {
            SearchResponse<Map<String, Object>> response = elasticsearchClient.search(s -> {
                // 基本查询配置
                s.index(indexName)
                 .from(from)
                 .size(size)
                 .query(q -> q
                         .multiMatch(m -> m
                                 .query(keyword)
                                 .fields(List.of(searchFields))
                         )
                 );

                // 添加高亮配置
                if (highlightFields != null && highlightFields.length > 0) {
                    s.highlight(h -> {
                        h.preTags("<em class='highlight'>")
                         .postTags("</em>");

                        for (String field : highlightFields) {
                            h.fields(field, f -> f);
                        }
                        return h;
                    });
                }

                return s;
            }, (Class<Map<String, Object>>) (Class<?>) Map.class);

            // 解析结果
            List<Map<String, Object>> resultList = new ArrayList<>();
            HitsMetadata<Map<String, Object>> hits = response.hits();
            long total = hits.total() != null ? hits.total().value() : 0;

            for (Hit<Map<String, Object>> hit : hits.hits()) {
                Map<String, Object> sourceMap = new HashMap<>(hit.source());

                // 处理高亮结果
                if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                    hit.highlight().forEach((key, value) -> {
                        if (!value.isEmpty()) {
                            sourceMap.put(key, value.get(0));
                        }
                    });
                }

                resultList.add(sourceMap);
            }

            log.debug("搜索成功: {} -> 关键词: {}, 结果数: {}, 总数: {}", indexName, keyword, resultList.size(), total);
            return new SearchResult(resultList, total);
        } catch (IOException e) {
            log.error("搜索失败: {} -> 关键词: {}", indexName, keyword, e);
            return new SearchResult(new ArrayList<>(), 0);
        }
    }

    /**
     * 搜索结果包装类
     */
    public static class SearchResult {
        private final List<Map<String, Object>> results;
        private final long total;

        public SearchResult(List<Map<String, Object>> results, long total) {
            this.results = results;
            this.total = total;
        }

        public List<Map<String, Object>> getResults() {
            return results;
        }

        public long getTotal() {
            return total;
        }
    }
}
