package eu.de4a.connector.mock.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;

import java.io.Serializable;

@Profile("do")
public class DOConfig implements Serializable {

    @Getter
    @Value("${mock.do.preview.endpoint.base}")
    private String previewBaseEndpoint;
    @Getter
    @Value("${mock.do.preview.endpoint.index}")
    private String indexEndpoint;
    @Getter
    @Value("${mock.do.preview.evidence.get.endpoint}")
    private String previewEvidenceRequest;
    @Getter
    @Value("${mock.do.preview.evidence.accept.endpoint}")
    private String previewEvidenceAccept;
    @Getter
    @Value("${mock.do.preview.evidence.reject.endpoint}")
    private String previewEvidenceReject;
    @Getter
    @Value("${mock.do.preview.dt.url}")
    private String previewDTUrl;
    @Getter
    @Value("${mock.do.preview.dt.redirect.url}")
    private String previewDTRedirectUrl;
    @Getter
    @Value("${mock.do.preview.endpoint.websocket.mess}")
    private String websocketMessagesEndpoint;
    @Getter
    @Value("${mock.do.preview.endpoint.websocket.socket}")
    private String websocketSocketEndpoint;
    @Getter
    @Value("${mock.do.preview.evidence.requestId.all.endpoint}")
    private String previewRequestIdsEndpoint;
    @Getter
    @Value("${mock.do.preview.evidence.redirecturl.endpoint}")
    private String previewRedirectUrlEndpoint;
    @Getter
    @Value("${mock.do.preview.bundle.path}")
    private String bundlePath;
    @Getter
    @Value("${mock.do.preview.endpoint.subscription.base}")
    private String subscriptionBaseEndpoint;
    @Getter
    @Value("${mock.do.preview.endpoint.subscription.index}")
    private String subscriptionEndpoint;
    @Getter
    @Value("${mock.do.preview.subscription.requestId.all.endpoint}")
    private String previewSubscriptionRequestIdsEndpoint;
    @Getter
    @Value("${mock.do.preview.subscription.get.endpoint}")
    private String previewSubscriptionRequest;
    
    @Getter
    @Value("${mock.do.preview.endpoint.notification}")
    private String notificationEndpoint;
    
    @Getter
    @Value("${mock.do.create.notification}")
    private String createNotif;
    /*
    @Getter
    @Value("${mock.do.preview.endpoint.notification.summary}")
    private String notificationSummary;
    */

}
