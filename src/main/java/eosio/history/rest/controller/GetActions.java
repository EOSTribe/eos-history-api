package eosio.history.rest.controller;

import com.google.gson.JsonObject;
import eosio.history.rest.Actions;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.Transaction;
import eosio.history.rest.config.Properties;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/history/get_actions")
public class GetActions {

    private static final transient Logger logger = LoggerFactory.getLogger(GetActions.class);

    private String get_transaction_index;
    private String get_actions_index;

    private ElasticSearchClient elasticSearchClient;
    private List accessControlAllowHeaders;
    private String accessControlAllowOrigin;

    @Autowired
    public void setProperties(Properties properties){
        this.get_transaction_index = properties.getTransactionIndex();
        this.get_actions_index = properties.getActionsIndex();
        accessControlAllowHeaders=Arrays.asList(properties.getAccessControlAllowHeaders());
        accessControlAllowOrigin=properties.getAccessControlAllowOrigin();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }
    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_actions(@RequestBody Actions actions) throws IOException {
        logger.info("Reuqest: "+actions.toString());
        int size = 1000;
        int from = 0;
        String lte = "now";
        String gte = "0";
        SortOrder sortOrder = SortOrder.ASC;
        JSONArray jsons = new JSONArray();
        JSONObject response = new JSONObject();
        SearchResponse searchResponse = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccessControlAllowOrigin(accessControlAllowOrigin);
        httpHeaders.setAccessControlAllowHeaders(accessControlAllowHeaders);

        String account_name = actions.getAccount_name();
        int pos = actions.getPos();
        int offset = actions.getOffset();
        SearchRequest searchRequest = new SearchRequest(get_actions_index);

        QueryBuilder queryBuilder =
                new BoolQueryBuilder().
                filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                        QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                QueryBuilders.matchQuery("receipt.receiver",account_name))
                ).should(
                        QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                QueryBuilders.matchQuery("act.authorization.actor",account_name))
                ));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        if (pos == 0 & offset ==0){
            from = 0;
            size = 1000;
        }else  if (pos < 0) {
                from = Math.abs(pos)-1;
                size = Math.abs(offset);
            sortOrder = SortOrder.DESC;
        }else if ( pos  > 0 ){
                from = pos-1;
                size = Math.abs(offset);
        }else if ( pos  == 0 ){
            from = pos;
            size = Math.abs(offset);
        }

        if (actions.getAfter() != null){
            gte = actions.getAfter();
        }

        if (actions.getBefore()!=null){
            lte = actions.getBefore();
        }

        if (actions.getLast()!=null){
            gte="now-"+actions.getLast();
            sortOrder = SortOrder.DESC;
        }
        ((BoolQueryBuilder) queryBuilder).filter().add(QueryBuilders.rangeQuery("block_timestamp").gte(gte).lte(lte));

        searchSourceBuilder.sort("receipt.global_sequence", sortOrder);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        searchSourceBuilder.query(queryBuilder);

        if (size+from > 10000){
            response.put("actions", scrollRequest(searchSourceBuilder, from, size));
        } else {
            try {
                searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
            } catch (ElasticsearchTimeoutException e) {
                return new ResponseEntity<>(e.getDetailedMessage(), httpHeaders, HttpStatus.REQUEST_TIMEOUT);
            } catch (ElasticsearchException e) {
                return new ResponseEntity<>(e.getDetailedMessage(), httpHeaders, HttpStatus.BAD_REQUEST);
            }
            SearchHits hits = searchResponse.getHits();
            if (hits.getTotalHits().value == 0) {
                logger.info("Reuqest: " + actions.toString() + " response: " + HttpStatus.NOT_FOUND);
                return new ResponseEntity<>("", httpHeaders, HttpStatus.NOT_FOUND);
            }
            for (SearchHit hit : hits) {
                JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
                String data = jsonObjectActions.getJSONObject("act").getString("data");
                JSONObject jsonObjectData = new JSONObject(data);
                jsonObjectActions.getJSONObject("act").put("data", jsonObjectData);
                jsonObjectActions.remove("trx");
                jsons.put(jsonObjectActions);
            }
            response.put("query_time", searchResponse.getTook().toString());
            response.put("actions", jsons);
            logger.info("Reuqest: "+actions.toString()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().toString()+" actions: "+jsons.length());
        }
        return new ResponseEntity<>(response.toString(), httpHeaders, HttpStatus.OK);
    }

    private JSONArray scrollRequest(SearchSourceBuilder searchSourceBuilder, int from, int size) throws IOException {
        int scrollPage = 1;
        int pagesSize = 10000;
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        searchSourceBuilder.size(pagesSize).from(0);
        SearchRequest searchRequest = new SearchRequest(get_actions_index);
        searchRequest.scroll(scroll);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
//        if (pagesSize*scrollPage - from > 0 ){
//           List<SearchHit> subArray = Arrays.asList(searchHits).subList(Math.abs(scrollPage-from-scrollSize), Math.abs(scrollPage-from-scrollSize)+size);
//        }
        while (searchHits != null && searchHits.length > 0) {
            List<SearchHit> searchHitsList;
            ++scrollPage;
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);
            searchResponse = elasticSearchClient.getElasticsearchClient().scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();
            if (pagesSize*scrollPage-from > 0){
                try {
                    searchHitsList = Arrays.asList(searchHits).subList(Math.abs(pagesSize*(scrollPage-1)-from), Math.abs(pagesSize*(scrollPage-1)-from)+size);
                    clearScrollRequest(scrollId);
                    logger.info(" query_time: "+searchResponse.getTook().toString()+" actions: "+searchHitsList.size());

                    return hitsProcessing(searchHitsList);
                } catch (IndexOutOfBoundsException exc){
                    searchHitsList = Arrays.asList(searchHits).subList(Math.abs(pagesSize*(scrollPage-1)-from), searchHits.length-1);
                    clearScrollRequest(scrollId);
                    return hitsProcessing(searchHitsList);
                }
            }
        }
        clearScrollRequest(scrollId);
        return new JSONArray();
    }
    private JSONArray hitsProcessing(List<SearchHit> hits){
        JSONArray result = new JSONArray();
        for (SearchHit hit : hits) {
            JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
            String data = jsonObjectActions.getJSONObject("act").getString("data");
            JSONObject jsonObjectData = new JSONObject(data);
            jsonObjectActions.getJSONObject("act").put("data", jsonObjectData);
            jsonObjectActions.remove("trx");
            result.put(jsonObjectActions);
        }
        return result;
    }

    private Boolean clearScrollRequest(String scorllId) throws IOException {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scorllId);
        ClearScrollResponse clearScrollResponse = elasticSearchClient.getElasticsearchClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        return clearScrollResponse.isSucceeded();
    }

}
