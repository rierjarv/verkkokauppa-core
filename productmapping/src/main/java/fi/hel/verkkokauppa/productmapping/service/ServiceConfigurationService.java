package fi.hel.verkkokauppa.productmapping.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;

@Component
public class ServiceConfigurationService {

    private Logger log = LoggerFactory.getLogger(ServiceConfigurationService.class);

    @Autowired
    private Environment env;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;


    public List<ServiceConfiguration> findBy(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndRestrictedNot(namespace);
        configurations = configurations.stream().filter(conf -> conf.getRestricted() == false).collect(Collectors.toList());
        return configurations;
    }

    public ServiceConfiguration findBy(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKeyAndRestrictedNot(namespace, configurationKey);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public List<ServiceConfiguration> findRestricted(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespace(namespace);
        configurations = configurations.stream().filter(conf -> conf.getRestricted() == false).collect(Collectors.toList());
        return configurations;
    }

    public ServiceConfiguration findRestricted(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKey(namespace, configurationKey);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public ServiceConfiguration createByParams(String namespace, String configurationKey, String configurationValue, boolean isRestricted) {
        String configurationId = UUIDGenerator.generateType3UUIDString(namespace, configurationKey);
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configurationId, namespace, configurationKey, configurationValue, isRestricted);
        serviceConfigurationRepository.save(serviceConfiguration);
        log.debug("created service configuration for namespace: " + namespace + " with configurationId: " + configurationId);

        return serviceConfiguration;
    }

    // generate some mock data
    public List<ServiceConfiguration> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        List<ServiceConfiguration> entities = Arrays.asList(new ServiceConfiguration[]{
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_API_VERSION, "wm3.1", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_API_KEY, "asukaspysakointi_mock_api_key", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_CURRENCY, "EUR", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_TYPE, "e-payment", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_REGISTER_CARD_TOKEN, "false", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/return_url", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/notification_url", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_LANGUAGE, "fi", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "asukaspysakointi_mock_submerchant_id", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/terms_of_use", false),

            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_API_VERSION, "wm3.1", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_API_KEY, "tilavaraus_mock_api_key", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_CURRENCY, "EUR", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_TYPE, "e-payment", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_REGISTER_CARD_TOKEN, "false", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl+"/mockserviceconfiguration/tilavaraus/return_url", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl+"/mockserviceconfiguration/tilavaraus/notification_url", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_LANGUAGE, "fi", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "tilavaraus_mock_submerchant_id", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/tilavaraus/terms_of_use", false),

            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_API_VERSION, "wm3.1", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_API_KEY, "venepaikat_mock_api_key", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_CURRENCY, "EUR", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_TYPE, "e-payment", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_REGISTER_CARD_TOKEN, "false", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/return_url", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/notification_url", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_LANGUAGE, "fi", true),
            createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "venepaikat_mock_submerchant_id", true),
            createByParams("venepaikat", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/terms_of_use", false),
        });

        serviceConfigurationRepository.saveAll(entities);
        log.debug("initialized service configurations mock data");

        return entities;
    }

}
