package fi.hel.verkkokauppa.mockproductmanagement.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MockServiceConfigurationController {
    // AP = asukaspysakointi
    // TV = tilavaraus
    // VP = venepaikat

    @GetMapping("/mockserviceconfiguration/asukaspysakointi/return_url")
    public String getMockPaymentReturnUrlAP() { return "asukaspysakointi mock payment return url"; }

    @GetMapping("/mockserviceconfiguration/asukaspysakointi/notification_url")
    public String getMockPaymentNotificationUrlAP() { return "asukaspysakointi mock payment notification url"; }

    @GetMapping("/mockserviceconfiguration/asukaspysakointi/terms_of_use")
    public String getMockTermsOfUseUrlAP() { return "asukaspysakointi mock terms of use url"; }

    @PostMapping("/mockserviceconfiguration/asukaspysakointi/merchant_payment_webhook")
    public String getMerchantPaymentWebhookUrlAP() { return "asukaspysakointi mock merchantPaymentWebhookUrl"; }

    @GetMapping("/mockserviceconfiguration/tilavaraus/return_url")
    public String getMockPaymentReturnUrlTV() { return "tilavaraus mock payment return url"; }

    @GetMapping("/mockserviceconfiguration/tilavaraus/notification_url")
    public String getMockPaymentNotificationUrlTV() { return "tilavaraus mock payment notification url"; }

    @GetMapping("/mockserviceconfiguration/tilavaraus/terms_of_use")
    public String getMockTermsOfUseUrlTV() { return "tilavaraus mock terms of use url"; }


    @GetMapping("/mockserviceconfiguration/venepaikat/return_url")
    public String getMockPaymentReturnUrlVP() { return "venepaikat mock payment return url"; }

    @GetMapping("/mockserviceconfiguration/venepaikat/notification_url")
    public String getMockPaymentNotificationUrlVP() { return "venepaikat mock payment notification url"; }

    @GetMapping("/mockserviceconfiguration/venepaikat/terms_of_use")
    public String getMockTermsOfUseUrlVP() { return "venepaikat mock terms of use url"; }

}
