package eosio.history.rest.controller;

import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.config.Properties;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("v1/history/get_transaction")
public class GetTransaction {
    private String get_transaction_index ;
    private String get_actions_index ;

    private ElasticSearchClient elasticSearchClient;
    private List AccessControlAllowHeaders = new ArrayList();


    @Autowired
    public void setProperties(Properties properties){
        this.get_transaction_index = properties.getTransactionIndex();
        this.get_actions_index = properties.getActionsIndex();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_transaction(@RequestBody String id) throws IOException {
        AccessControlAllowHeaders.add("*");
        CompletableFuture.supplyAsync(() -> getElasticTransactionHits(id))
                    .thenApply(hits -> getElasticActions(hits))
                    .thenAccept(json -> returnResul(json));
    }

    private ResponseEntity<String> returnResul(JSONObject result) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccessControlAllowOrigin("*");
        httpHeaders.setAccessControlAllowHeaders(AccessControlAllowHeaders);
        if(result != null) {
            return new ResponseEntity<>(result.toString(), httpHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("", httpHeaders, HttpStatus.NOT_FOUND);
        }
    }

    private JSONObject getElasticActions(SearchHits hits) throws IOException {
        // If no hits - return null:
        if (hits.getTotalHits().value == 0){
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setAccessControlAllowOrigin("*");
            httpHeaders.setAccessControlAllowHeaders(AccessControlAllowHeaders);
            return null;
        }

        JSONObject jsonObjectTransaction = new JSONObject(hits.getAt(0).getSourceAsString());
        List<JSONObject> jsonObjectList = new ArrayList<>();
        QueryBuilder queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("trx",id)));
        SearchRequest searchRequest = new SearchRequest(get_actions_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            JSONObject jsonObjectActions =new JSONObject(hit.getSourceAsString());
            String data = jsonObjectActions.getJSONObject("act").getString("data");
            JSONObject jsonObjectData = new JSONObject(data);
            jsonObjectActions.getJSONObject("act").put("data", jsonObjectData);
            jsonObjectActions.remove("trx");
            jsonObjectList.add(jsonObjectActions);
        }
        jsonObjectTransaction.getJSONObject("trace").put("action_traces",jsonObjectList);

        return jsonObjectTransaction;
    }

    private SearchHits getElasticTransactionHits(String id) throws IOException {
        QueryBuilder queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("id",id)));
        SearchRequest searchRequest = new SearchRequest(get_transaction_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getHits();
    }
}
