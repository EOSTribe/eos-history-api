package eosio.history.rest.controller;


import eosio.history.rest.Actions;
import eosio.history.rest.ElasticSearchClient;
import eosio.history.rest.Key;
import eosio.history.rest.KeyConvertor;
import eosio.history.rest.config.Properties;
import one.block.eosiojava.error.utilities.EOSFormatterError;
import org.bitcoinj.core.Base58;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/history/get_key_accounts")
public class GetKeyAccounts {
    private static final transient Logger logger = LoggerFactory.getLogger(GetKeyAccounts.class);

    private KeyConvertor keyConvertor;
    private ElasticSearchClient elasticSearchClient;
    private String get_key_accounts_index;

    @Autowired
    public void setKeyConvertor(KeyConvertor keyConvertor){
        this.keyConvertor = keyConvertor;
    }
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
    ResponseEntity<?> get_actions(@RequestBody Key key) throws IOException, EOSFormatterError {
        logger.info("Reuqest: "+key.getKey());
        Set accounts = new HashSet();
        JSONObject response = new JSONObject();
        String publicKeyK1;
        if (key.getKey().substring(0,3).equals("EOS")){
            publicKeyK1 = keyConvertor.fromLegacyToK1PublicKey(key.getKey());
        }else {
            publicKeyK1 = key.getKey();
        }
        QueryBuilder queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("act.data",publicKeyK1)));
        SearchRequest searchRequest = new SearchRequest(get_key_accounts_index);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        response.put("query_time", searchResponse.getTook().getMillis()+"ms");
        if (searchHits.getTotalHits().value == 0){
            logger.info("Reuqest: "+key.getKey()+" response: "+HttpStatus.NOT_FOUND +" query_time: "+searchResponse.getTook().millis()+"ms ");
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
                accounts.add(jsonObjectData.getString("name"));

            }catch (JSONException jse){
                logger.warn(jse.getMessage());
            }
            try {

                accounts.add(jsonObjectData.getString("account"));
            }catch (JSONException jse){
                logger.warn(jse.getMessage());
            }
        }
        response.put("accounts",accounts);
        logger.info("Reuqest: "+key.getKey()+" response: "+HttpStatus.OK +" query_time: "+searchResponse.getTook().millis()+"ms ");
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }
}
