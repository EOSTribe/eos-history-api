package eosio.history.rest.controller;

import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.Transaction;
import eosio.history.rest.config.Properties;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("v1/history/get_transaction")
public class GetTransaction {
    private static final transient Logger logger = LoggerFactory.getLogger(GetTransaction.class);

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
    ResponseEntity<?> get_transaction(@RequestBody Transaction transaction) throws IOException {
        List<JSONObject> jsonObjectList = new ArrayList<>();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccessControlAllowOrigin(accessControlAllowOrigin);
        httpHeaders.setAccessControlAllowHeaders(accessControlAllowHeaders);

        if (transaction.getId().length() != 64){
            logger.info("id: "+transaction.getId()+" status: "+HttpStatus.BAD_REQUEST.toString());
            return new ResponseEntity<>("", httpHeaders, HttpStatus.BAD_REQUEST);
        }

        QueryBuilder transactionQueryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("id",transaction.getId())));
        SearchRequest transactionSearchRequest = new SearchRequest(get_transaction_index);
        SearchSourceBuilder transactionSearchSourceBuilder = new SearchSourceBuilder();
        transactionSearchSourceBuilder.query(transactionQueryBuilder);
        transactionSearchRequest.source(transactionSearchSourceBuilder);

        QueryBuilder actionsQueryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("trx",transaction.getId())));
        SearchRequest actionsSearchRequest = new SearchRequest(get_actions_index);
        SearchSourceBuilder actionsSearchSourceBuilder = new SearchSourceBuilder();
        actionsSearchSourceBuilder.size(90);
        actionsSearchSourceBuilder.timeout(new TimeValue(30, TimeUnit.SECONDS));
        actionsSearchSourceBuilder.query(actionsQueryBuilder);
        actionsSearchRequest.source(actionsSearchSourceBuilder);

        MultiSearchRequest multiSearchRequest  = new MultiSearchRequest().
                add(transactionSearchRequest).
                add(actionsSearchRequest);
        MultiSearchResponse.Item[] multiSearchResponse = elasticSearchClient.getElasticsearchClient().msearch(multiSearchRequest, RequestOptions.DEFAULT).getResponses();

        if ( multiSearchResponse[0].getResponse().getHits().getTotalHits().value == 0){
            logger.info("id: "+transaction.getId()+" status: "+HttpStatus.NOT_FOUND.toString());

            return new ResponseEntity<>("", httpHeaders, HttpStatus.NOT_FOUND);
        }

        JSONObject jsonObjectTransaction = new JSONObject(multiSearchResponse[0].getResponse().getHits().getAt(0).getSourceAsString());

        for (SearchHit hit : multiSearchResponse[1].getResponse().getHits()) {
            JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
            String data = jsonObjectActions.getJSONObject("act").getString("data");
            JSONObject jsonObjectData = new JSONObject(data);
            jsonObjectActions.getJSONObject("act").put("data", jsonObjectData);
            jsonObjectActions.remove("trx");
            jsonObjectList.add(jsonObjectActions);
        }

        jsonObjectTransaction.getJSONObject("trace").put("action_traces",jsonObjectList);
        logger.info("id: "+transaction.getId()+" status: "+HttpStatus.OK.toString());

        return new ResponseEntity<>(jsonObjectTransaction.toString(), httpHeaders, HttpStatus.OK);
    }
}
