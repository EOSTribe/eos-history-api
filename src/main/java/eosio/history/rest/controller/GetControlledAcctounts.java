package eosio.history.rest.controller;


import eosio.history.rest.Account;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.config.Properties;
import one.block.eosiojava.error.utilities.EOSFormatterError;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

@RestController
@RequestMapping("/v1/history/get_controlled_accounts")
public class GetControlledAcctounts {
    private static final transient Logger logger = LoggerFactory.getLogger(GetControlledAcctounts.class);

    private ElasticSearchClient elasticSearchClient;
    private String get_key_accounts_index;

    @Autowired
    public void setProperties(Properties properties){
        this.get_key_accounts_index = properties.getAccountActionsIndex();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }
    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_actions(@RequestBody Account account) throws IOException, EOSFormatterError {
        JSONObject response = new JSONObject();
        JSONArray controlledAccounts = new JSONArray();
        QueryBuilder queryBuilder =
                new BoolQueryBuilder().
                        filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                QueryBuilders.boolQuery().minimumShouldMatch(1).should(
                                        QueryBuilders.matchQuery("act.data.active.accounts.permission.actor",
                                                account.getControlling_account()))
                        ));
        SearchRequest searchRequest = new SearchRequest(get_key_accounts_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        response.put("query_time", searchResponse.getTook().getMillis()+"ms");

        if (searchHits.getTotalHits().value == 0){
            logger.info("Reuqest: "+account.getControlling_account()+" response: "+HttpStatus.NOT_FOUND +" query_time: "+searchResponse.getTook().millis()+"ms ");
            response.put("controlled_accounts",new JSONArray());
            return new ResponseEntity<>(response.toString(), HttpStatus.NOT_FOUND);
        }
        for (SearchHit hit : searchHits) {
            JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
            try {
                String controlledAccount = jsonObjectActions.getJSONObject("act").getJSONObject("data").getString("name");
                controlledAccounts.put(controlledAccount);
            }catch (JSONException jse){
                logger.error(jse.getMessage());
            }
        }
        response.put("controlled_accounts",controlledAccounts);
        logger.info("Reuqest: "+account.getControlling_account()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().millis()+"ms");
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }
}