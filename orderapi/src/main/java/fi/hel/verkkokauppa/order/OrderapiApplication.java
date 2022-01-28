package fi.hel.verkkokauppa.order;

import fi.hel.verkkokauppa.shared.repository.jpa.BaseRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories(basePackages = "fi.hel.verkkokauppa.order.repository.jpa", repositoryBaseClass = BaseRepositoryImpl.class)
@ComponentScan({"fi.hel.verkkokauppa.common.elastic", "fi.hel.verkkokauppa.common.error", "fi.hel.verkkokauppa.common.events", "fi.hel.verkkokauppa.common.rest", "fi.hel.verkkokauppa.common.configuration", "fi.hel.verkkokauppa.order", "fi.hel.verkkokauppa.common.history"})
public class OrderapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderapiApplication.class, args);
    }

}
