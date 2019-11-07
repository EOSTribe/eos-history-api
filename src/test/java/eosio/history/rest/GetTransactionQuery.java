package eosio.history.rest;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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

    @Before
public void init() throws IOException {
//        System.out.println(elasticSearchClient.getElasticsearchClient().ping(RequestOptions.DEFAULT));
}
   @Test
   public void serverResposnse() throws IOException {
        assertTrue(elasticSearchClient.getElasticsearchClient().ping(RequestOptions.DEFAULT));
   }
   @Test void getTransaction(){

//       SearchRequestBuilder searchRequestBuilder = new SearchRequestBuilder(elasticSearchClient.getElasticsearchClient(), )

   }
}
