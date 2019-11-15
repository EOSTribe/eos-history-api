package eosio.history.rest;

import eosio.history.rest.config.Properties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchClient {
    private RestHighLevelClient elasticsearchClient;
    @Autowired
    public ElasticSearchClient(Properties properties){
        this.elasticsearchClient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(properties.getEsHost1(), 9200, "http"),
                        new HttpHost(properties.getEsHost2(), 9200, "http")));
    }
    public RestHighLevelClient getElasticsearchClient(){
        return this.elasticsearchClient;
    }
}
