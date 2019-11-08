package eosio.history.rest;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import static org.junit.Assert.*;


import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class GetTransactionQuery {

    @Autowired
    private ElasticSearchClient elasticSearchClient;
    private String transactionId = "6cc9f7ac50e48a36bb5040bafc3834c7aec93a17fe9f08449a22863cba875249";
    private String get_transaction_index = "eos-mainnet-transaction";

    @Before
public void init() throws IOException {
//        System.out.println(elasticSearchClient.getElasticsearchClient().ping(RequestOptions.DEFAULT));

}
   @Test
   public void serverResposnse() throws IOException {
        assertTrue(elasticSearchClient.getElasticsearchClient().ping(RequestOptions.DEFAULT));
   }
   @Test
   public void getTransaction() throws IOException {
       QueryBuilder queryBuilder = new BoolQueryBuilder().filter(QueryBuilders.boolQuery().minimumShouldMatch(1).should(QueryBuilders.matchQuery("id",transactionId)));
       SearchRequest searchRequest = new SearchRequest(get_transaction_index);
       SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
       searchSourceBuilder.query(queryBuilder);
       searchRequest.source(searchSourceBuilder);
       SearchResponse searchResponse = elasticSearchClient.getElasticsearchClient().search(searchRequest, RequestOptions.DEFAULT);
       SearchHits hits = searchResponse.getHits();
       for (SearchHit hit : hits) {
           // do something with the SearchHit
           System.out.println(hit.getSourceAsString());
       }


   }
}
