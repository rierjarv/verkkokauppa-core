package fi.hel.verkkokauppa.productmapping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
@ComponentScan({"fi.hel.verkkokauppa.productmapping", "fi.hel.verkkokauppa.common"})
public class ProductmappingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductmappingApplication.class, args);
	}

}
