package fi.hel.verkkokauppa.payment.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;


@Component
public class ServiceConfigurationClient {

    public static final int CONNECT_TIMEOUT = 30000;
    private Logger log = LoggerFactory.getLogger(ServiceConfigurationClient.class);

    @Autowired
    private Environment env;

    
    public JSONObject getAllServiceConfiguration(WebClient client, String namespace) {
        String serviceMappingUrl = env.getProperty("serviceconfiguration.url")+"restricted/getAll?namespace="+namespace;
        JSONObject namespaceServiceConfiguration = queryJsonService(client, serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration;
    }

    public WebClient getClient() {
        // expect a response within a few seconds
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
            .responseTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)));

        WebClient client = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        return client;
    }

    public JSONObject queryJsonService(WebClient client, String url) {
        String jsonResponse = client.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        JSONObject jsonObject = new JSONObject(jsonResponse);

        return jsonObject;
    }
    
}
    
