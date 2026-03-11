package com.yusheng.aiagentproject.tool;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 SearchAPI（Baidu 引擎）的联网搜索工具。
 * <p>设计目标：
 * 1. 对外返回结构化结果，便于长期维护和二次处理。
 * 2. 对接口异常、空结果做显式处理，避免 NPE。
 * 3. 兼容 AI Tool Calling 场景，支持可选搜索参数。
 */
@Component
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private static final String SEARCH_ENGINE = "baidu";
    private static final int DEFAULT_RESULT_COUNT = 5;
    private static final int MAX_RESULT_COUNT = 50;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_CT = 0;

    private final String apiKey;

    public WebSearchTool(@Value("${searchapi.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /**
     * 便捷调用：使用默认参数执行搜索。
     */
    public WebSearchResponse searchWeb(String query) {
        return searchWeb(query, DEFAULT_RESULT_COUNT, DEFAULT_PAGE, DEFAULT_CT);
    }

    /**
     * Tool Calling 主入口。
     *
     * @param query 搜索关键词
     * @param resultCount 期望返回数量，范围 1-50
     * @param page 搜索页码，从 1 开始
     * @param ct 百度 ct 参数，0=简繁都搜，1=简体，2=繁体
     */
    @Tool(description = "使用百度搜索引擎联网搜索并返回结构化结果")
    public WebSearchResponse searchWeb(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "期望返回数量，范围 1-50，默认 5") Integer resultCount,
            @ToolParam(description = "搜索页码，从 1 开始，默认 1") Integer page,
            @ToolParam(description = "百度 ct 参数：0=简繁都搜，1=简体，2=繁体，默认 0") Integer ct) {

        if (query == null || query.isBlank()) {
            return WebSearchResponse.error("query must not be blank");
        }
        if (apiKey.isBlank()) {
            return WebSearchResponse.error("SEARCHAPI_API_KEY is not configured");
        }

        int normalizedCount = normalizeResultCount(resultCount);
        int normalizedPage = normalizePage(page);
        int normalizedCt = normalizeCt(ct);

        Map<String, Object> queryParams = Map.of(
                "engine", SEARCH_ENGINE,
                "q", query.trim(),
                "api_key", apiKey,
                "num", normalizedCount,
                "page", normalizedPage,
                "ct", normalizedCt
        );

        try {
            String responseText = HttpUtil.get(SEARCH_API_URL, queryParams, 20_000);
            JSONObject root = JSONUtil.parseObj(responseText);

            List<SearchResultItem> organicResults = parseOrganicResults(root.getJSONArray("organic_results"), normalizedCount);
            List<TopStoryItem> topStories = parseTopStories(root.getJSONArray("top_stories"), normalizedCount);
            String answerBoxSummary = extractAnswerBoxSummary(root.getJSONObject("answer_box"));

            boolean hasAnyResult = !organicResults.isEmpty()
                    || !topStories.isEmpty()
                    || (answerBoxSummary != null && !answerBoxSummary.isBlank());
            if (!hasAnyResult) {
                String apiError = extractApiError(root);
                if (apiError != null) {
                    return WebSearchResponse.error(apiError);
                }
                return WebSearchResponse.error("no usable search result returned");
            }

            return WebSearchResponse.success(
                    query.trim(),
                    normalizedCount,
                    normalizedPage,
                    normalizedCt,
                    organicResults,
                    topStories,
                    answerBoxSummary
            );
        } catch (Exception e) {
            return WebSearchResponse.error("request failed: " + e.getMessage());
        }
    }

    private int normalizeResultCount(Integer resultCount) {
        if (resultCount == null) {
            return DEFAULT_RESULT_COUNT;
        }
        if (resultCount < 1) {
            return 1;
        }
        return Math.min(resultCount, MAX_RESULT_COUNT);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeCt(Integer ct) {
        if (ct == null) {
            return DEFAULT_CT;
        }
        if (ct == 1 || ct == 2) {
            return ct;
        }
        return DEFAULT_CT;
    }

    /**
     * 解析自然结果列表，兼容 snippet/description 字段差异。
     */
    private List<SearchResultItem> parseOrganicResults(JSONArray array, int limit) {
        List<SearchResultItem> results = new ArrayList<>();
        if (array == null || array.isEmpty()) {
            return results;
        }

        int size = Math.min(limit, array.size());
        for (int i = 0; i < size; i++) {
            Object item = array.get(i);
            if (!(item instanceof JSONObject object)) {
                continue;
            }

            results.add(new SearchResultItem(
                    object.getInt("position"),
                    object.getStr("title"),
                    object.getStr("link"),
                    firstNonBlank(object.getStr("snippet"), object.getStr("description")),
                    parseSource(object)
            ));
        }
        return results;
    }

    /**
     * 解析热点新闻列表。
     */
    private List<TopStoryItem> parseTopStories(JSONArray array, int limit) {
        List<TopStoryItem> results = new ArrayList<>();
        if (array == null || array.isEmpty()) {
            return results;
        }

        int size = Math.min(limit, array.size());
        for (int i = 0; i < size; i++) {
            Object item = array.get(i);
            if (!(item instanceof JSONObject object)) {
                continue;
            }

            results.add(new TopStoryItem(
                    object.getStr("title"),
                    object.getStr("link"),
                    parseSource(object),
                    firstNonBlank(object.getStr("date"), object.getStr("time")),
                    firstNonBlank(object.getStr("snippet"), object.getStr("description"))
            ));
        }
        return results;
    }

    private String extractAnswerBoxSummary(JSONObject answerBox) {
        if (answerBox == null || answerBox.isEmpty()) {
            return null;
        }
        return firstNonBlank(
                answerBox.getStr("answer"),
                answerBox.getStr("snippet"),
                answerBox.getStr("result"),
                answerBox.getStr("title")
        );
    }

    private String extractApiError(JSONObject root) {
        if (root == null || root.isEmpty()) {
            return null;
        }
        return firstNonBlank(root.getStr("error"), root.getStr("message"), root.getStr("detail"));
    }

    private String parseSource(JSONObject object) {
        if (object == null) {
            return null;
        }

        Object sourceObj = object.get("source");
        if (sourceObj instanceof JSONObject sourceJson) {
            return firstNonBlank(sourceJson.getStr("name"), sourceJson.getStr("title"));
        }
        if (sourceObj instanceof String sourceText && !sourceText.isBlank()) {
            return sourceText;
        }
        return object.getStr("displayed_link");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 结构化搜索返回。
     */
    public record WebSearchResponse(
            boolean success,
            String query,
            String engine,
            int requestedResultCount,
            int page,
            int ct,
            List<SearchResultItem> organicResults,
            List<TopStoryItem> topStories,
            String answerBoxSummary,
            String errorMessage) {

        public static WebSearchResponse success(String query,
                                                int requestedResultCount,
                                                int page,
                                                int ct,
                                                List<SearchResultItem> organicResults,
                                                List<TopStoryItem> topStories,
                                                String answerBoxSummary) {
            return new WebSearchResponse(
                    true,
                    query,
                    SEARCH_ENGINE,
                    requestedResultCount,
                    page,
                    ct,
                    organicResults == null ? List.of() : List.copyOf(organicResults),
                    topStories == null ? List.of() : List.copyOf(topStories),
                    answerBoxSummary,
                    null
            );
        }

        public static WebSearchResponse error(String errorMessage) {
            return new WebSearchResponse(
                    false,
                    null,
                    SEARCH_ENGINE,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    null,
                    errorMessage
            );
        }

        public boolean hasAnyResult() {
            return !organicResults.isEmpty()
                    || !topStories.isEmpty()
                    || (answerBoxSummary != null && !answerBoxSummary.isBlank());
        }
    }

    /**
     * 自然搜索结果条目。
     */
    public record SearchResultItem(
            Integer position,
            String title,
            String link,
            String snippet,
            String source) {
    }

    /**
     * 热点新闻条目。
     */
    public record TopStoryItem(
            String title,
            String link,
            String source,
            String date,
            String snippet) {
    }
}