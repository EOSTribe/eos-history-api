package eosio.history.rest.controller;


import com.google.gson.JsonObject;
import eosio.history.rest.Account;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.Key;
import eosio.history.rest.KeyConvertor;
import eosio.history.rest.config.Properties;
import one.block.eosiojava.error.utilities.EOSFormatterError;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.search.MultiMatchQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/v1/history/get_controlled_accounts")
public class GetControlledAcctounts {
    private static final transient Logger logger = LoggerFactory.getLogger(GetControlledAcctounts.class);

    private ElasticSearchClient elasticSearchClient;
    private String get_key_accounts_index;

    @Autowired
    public void setProperties(Properties properties){
        this.get_key_accounts_index = properties.getNewAccountIndex();
    }
    @Autowired
    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient){
        this.elasticSearchClient = elasticSearchClient;
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    ResponseEntity<?> get_actions(@RequestBody Account account) throws IOException, EOSFormatterError {
        JSONObject response = new JSONObject();
        QueryBuilder queryBuilder = new BoolQueryBuilder().filter(
                new MultiMatchQueryBuilder(account.getControlling_account()));
        SearchRequest searchRequest = new SearchRequest(get_key_accounts_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        response.put("query_time", searchResponse.getTook().getMillis()+"ms");

        if (searchHits.getTotalHits().value == 0){
            logger.info("Reuqest: "+account.getControlling_account()+" response: "+HttpStatus.NOT_FOUND +" query_time: "+searchResponse.getTook().millis()+"ms ");
            return new ResponseEntity<>(response.toString(), HttpStatus.NOT_FOUND);
        }
        for (SearchHit hit : searchHits) {
            JSONObject jsonObjectData = null;
            JSONObject jsonObjectActions = new JSONObject(hit.getSourceAsString());
            try {
                String data = jsonObjectActions.getJSONObject("act").getString("data");
                jsonObjectData = new JSONObject(data);
            }catch (JSONException jse){
                logger.error(jse.getMessage());
            }

            try {
                jsonObjectData.getString("name");

            }catch (JSONException jse){
                logger.warn(jse.getMessage());
            }
            try {

//                accounts.add(jsonObjectData.getString("account"));
            }catch (JSONException jse){
                logger.warn(jse.getMessage());
            }
        }
//        response.put("accounts",accounts);
//        logger.info("Reuqest: "+key.getKey()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().millis()+"ms ");
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }
}
