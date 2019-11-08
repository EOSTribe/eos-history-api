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
        List<JSONObject> jsonObjectList = new ArrayList<>();
        AccessControlAllowHeaders.add("*");

        QueryBuilder queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("id",id)));
        SearchRequest searchRequest = new SearchRequest(get_transaction_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        if (hits.getTotalHits().value == 0){
            HttpHeaders httpHeaders = new HttpHeaders();

            httpHeaders.setAccessControlAllowOrigin("*");
            httpHeaders.setAccessControlAllowHeaders(AccessControlAllowHeaders);
            return new ResponseEntity<>("", httpHeaders, HttpStatus.NOT_FOUND);
        }

        JSONObject jsonObjectTransaction = new JSONObject(hits.getAt(0).getSourceAsString());

        queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("trx",id)));
        searchRequest = new SearchRequest(get_actions_index);
        searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);
        searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
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

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccessControlAllowOrigin("*");
        httpHeaders.setAccessControlAllowHeaders(AccessControlAllowHeaders);
        return new ResponseEntity<>(jsonObjectTransaction.toString(), httpHeaders, HttpStatus.OK);
    }
}
