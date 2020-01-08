package eosio.history.rest.controller;

import eosio.history.rest.Actions;
import eosio.history.rest.TransferDeduplicator;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.config.Properties;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RestController
//@RequestMapping("/v1/history/get_actions")
public class GetActions {
    private static final transient Logger logger = LoggerFactory.getLogger(GetActions.class);

    private String actionsIndex;
    private String trasnferActionsIndex;
    private ElasticSearchClient elasticSearchClient;

    @Autowired
    public void setProperties(Properties properties){
        this.trasnferActionsIndex=properties.getTransferActionsIndex();
        this.actionsIndex = properties.getActionsIndex();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }
    @CrossOrigin
    @RequestMapping(value="/v1/history/get_actions", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_actions(@RequestBody Actions actions) throws IOException {
        logger.info("Reuqest: "+actions.toString());
        int size = 1000;
        int from = 0;
        String lte = "now";
        String gte = "0";
        SortOrder sortOrder = SortOrder.ASC;
        JSONObject response;

        String account_name = actions.getAccount_name();
        int pos = actions.getPos();
        int offset = actions.getOffset();

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
            response = scrollRequest(searchSourceBuilder, from, size, actions);
        } else {
            response = regularReuqest(searchSourceBuilder, actions);
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    private JSONObject scrollRequest(SearchSourceBuilder searchSourceBuilder, int from, int size, Actions actions) throws IOException, ElasticsearchException {
        JSONObject result = new JSONObject();
        int scrollPage = 1;
        int pagesSize = 10000;
        long requestDurationSec;
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        searchSourceBuilder.size(pagesSize).from(0);
        SearchRequest searchRequest = new SearchRequest(actionsIndex);
        searchRequest.scroll(scroll);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        requestDurationSec = searchResponse.getTook().millis();

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
            requestDurationSec = requestDurationSec+searchResponse.getTook().millis();

            if (pagesSize*scrollPage-from > 0){
                try {
                    searchHitsList = Arrays.asList(searchHits).subList(Math.abs(pagesSize*(scrollPage-1)-from), Math.abs(pagesSize*(scrollPage-1)-from)+size);
                    clearScrollRequest(scrollId);
                    logger.info(" query_time: "+requestDurationSec+"ms " +" actions: "+searchHitsList.size());
                    result.put("query_time", requestDurationSec+"ms");
                    result.put("actions", hitsProcessing(searchHitsList));
                    return result;
                } catch (IndexOutOfBoundsException exc){
                    searchHitsList = Arrays.asList(searchHits).subList(Math.abs(pagesSize*(scrollPage-1)-from), searchHits.length-1);
                    clearScrollRequest(scrollId);
                    result.put("query_time", requestDurationSec+"ms");
                    result.put("actions", hitsProcessing(searchHitsList));
                    logger.info("Reuqest: "+actions.toString()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().millis()+"ms "+" actions: "+searchHitsList.size());
                    return result;
                }
            }
        }
        clearScrollRequest(scrollId);
        return result;
    }

    private JSONArray hitsProcessing(List<SearchHit> hits){
        JSONArray result = new JSONArray();
        for (SearchHit hit : hits) {
            JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
            String data = jsonObjectActions.getJSONObject("act").getString("data");
            try {
                JSONObject jsonObjectData = new JSONObject(data);
                jsonObjectActions.getJSONObject("act").put("data", jsonObjectData);
            }catch (JSONException jse){
                logger.error(jse.getMessage());
            }
            jsonObjectActions.remove("trx");
            result.put(jsonObjectActions);
        }
        return result;
    }

    private JSONObject regularReuqest(SearchSourceBuilder searchSourceBuilder, Actions actions) throws IOException, ElasticsearchException {
        JSONObject result = new JSONObject();
        JSONArray actionsArray = new JSONArray();
        SearchRequest searchRequest = new SearchRequest(actionsIndex);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);

        List<SearchHit> searchHits = Arrays.asList(searchResponse.getHits().getHits());
        result.put("query_time", searchResponse.getTook().getMillis()+"ms");
        result.put("actions", hitsProcessing(searchHits));
        logger.info("Reuqest: "+actions.toString()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().millis()+"ms "+" actions: "+actionsArray.length());
        return result;
    }

    private Boolean clearScrollRequest(String scorllId) throws IOException {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scorllId);
        ClearScrollResponse clearScrollResponse = elasticSearchClient.getElasticsearchClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        return clearScrollResponse.isSucceeded();
    }

    @CrossOrigin
    @RequestMapping(value="/v2/history/get_actions", method = RequestMethod.POST, consumes="application/json" ,produces = "application/json")
    ResponseEntity<?> get_actions_v2(@RequestBody Actions actions) throws IOException {
        logger.info("Reuqest: "+actions.toString());
        int size = 30;
        int from = 0;
        String lte = "now";
        String gte = "0";
        SortOrder sortOrder = SortOrder.DESC;
        String account_name = actions.getAccount_name();
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        QueryBuilder queryBuilder =
                new BoolQueryBuilder().
                        filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                        QueryBuilders.matchQuery("receipt.receiver",account_name))
                        ).should(
                                QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                        QueryBuilders.matchQuery("act.authorization.actor",account_name))
                        ));

//                QueryBuilder queryBuilder =
//                new BoolQueryBuilder().
//                        filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(
//                                 QueryBuilders.termQuery("receipt.receiver",account_name)
//                        ).should(QueryBuilders.termQuery("act.authorization.actor",account_name))
//                        );

//        logger.info(((BoolQueryBuilder) queryBuilder).filter().toString());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        ((BoolQueryBuilder) queryBuilder).filter().add(QueryBuilders.rangeQuery("block_timestamp").gte(gte).lte(lte));

        searchSourceBuilder.sort("receipt.global_sequence", sortOrder);
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        searchSourceBuilder.timeout(TimeValue.timeValueSeconds(90));
        searchSourceBuilder.query(queryBuilder);
        SearchRequest searchRequest = new SearchRequest(trasnferActionsIndex);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        if( searchHits.length > 0) {
            TransferDeduplicator transferDeduplicator;
            HashSet hashSet = new HashSet();
            for (SearchHit hit:searchHits) {
                try {
                    JSONObject jsonObject = new JSONObject(hit.getSourceAsString());
                    String block_timestamp = jsonObject.getString("block_timestamp");
                    String block_num = jsonObject.get("block_num").toString();
                    String quantity = jsonObject.getJSONObject("act").getJSONObject("data").getString("quantity");
                    JSONObject actData = jsonObject.getJSONObject("act").getJSONObject("data");
                    String actName = jsonObject.getJSONObject("act").getString("name");
                    transferDeduplicator = new TransferDeduplicator(block_timestamp, block_num, quantity);
                    if (!hashSet.contains(transferDeduplicator)) {
                        JSONObject action = new JSONObject();
                        action.put("act", new JSONObject());
                        action.put("block_time", block_timestamp);
                        action.getJSONObject("act").put("data", actData);
                        action.getJSONObject("act").put("name", actName);
                        jsonArray.put(action);
                        response.put("actions", jsonArray);
                        hashSet.add(transferDeduplicator);
                    }
                }catch(JSONException jse){
                    logger.error(jse.getMessage());
                    logger.error(hit.getSourceAsString());
                    }
                }
            return new ResponseEntity<>(response.toString(), HttpStatus.OK);
        }
        response.put("actions",jsonArray);
        return new ResponseEntity<>(response.toString(), HttpStatus.NOT_FOUND);
    }
}
