package eu.de4a.connector.mock;

import eu.de4a.iem.jaxb.common.types.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Helper {

    public static RequestExtractEvidenceIMType buildDoImRequest(RequestTransferEvidenceUSIIMDRType drRequest) {
        RequestExtractEvidenceIMType req = new RequestExtractEvidenceIMType();
        req.setRequestId(drRequest.getRequestId());
        req.setSpecificationId(drRequest.getSpecificationId());
        req.setTimeStamp(LocalDateTime.now());
        req.setProcedureId(drRequest.getProcedureId());
        req.setDataEvaluator(drRequest.getDataEvaluator());
        req.setDataOwner(drRequest.getDataOwner());
        req.setDataRequestSubject(drRequest.getDataRequestSubject());
        req.setRequestGrounds(drRequest.getRequestGrounds());
        req.setCanonicalEvidenceTypeId(drRequest.getCanonicalEvidenceTypeId());
        req.setAdditionalParameters(drRequest.getAdditionalParameters());
        return req;
    }

    public static RequestExtractEvidenceUSIType buildDoUsiRequest(RequestTransferEvidenceUSIIMDRType drRequest) {
        RequestExtractEvidenceUSIType req = new RequestExtractEvidenceUSIType();
        req.setRequestId(drRequest.getRequestId());
        req.setSpecificationId(drRequest.getSpecificationId());
        req.setTimeStamp(LocalDateTime.now());
        req.setProcedureId(drRequest.getProcedureId());
        req.setDataEvaluator(drRequest.getDataEvaluator());
        req.setDataOwner(drRequest.getDataOwner());
        req.setDataRequestSubject(drRequest.getDataRequestSubject());
        req.setRequestGrounds(drRequest.getRequestGrounds());
        req.setCanonicalEvidenceTypeId(drRequest.getCanonicalEvidenceTypeId());
        req.setAdditionalParameters(drRequest.getAdditionalParameters());
        return req;
    }
    public static RequestTransferEvidenceUSIDTType buildDtUsiRequest(RequestExtractEvidenceUSIType doRequest, CanonicalEvidenceType canonicalEvidence, DomesticsEvidencesType domesticEvidences, ErrorListType errorListType) {
        RequestTransferEvidenceUSIDTType req = new RequestTransferEvidenceUSIDTType();
        req.setRequestId(doRequest.getRequestId());
        req.setSpecificationId(doRequest.getSpecificationId());
        req.setTimeStamp(LocalDateTime.now());
        req.setProcedureId(doRequest.getProcedureId());
        req.setDataEvaluator(doRequest.getDataEvaluator());
        req.setDataOwner(doRequest.getDataOwner());
        req.setDataRequestSubject(doRequest.getDataRequestSubject());
        req.setCanonicalEvidence(canonicalEvidence);
        req.setDomesticEvidenceList(domesticEvidences);
        req.setErrorList(errorListType);
        return req;
    }

    public static RequestForwardEvidenceType buildDeUriRequest(RequestTransferEvidenceUSIDTType dtRequest) {
        RequestForwardEvidenceType req = new RequestForwardEvidenceType();
        req.setRequestId(dtRequest.getRequestId());
        req.setTimeStamp(LocalDateTime.now());
        req.setCanonicalEvidence(dtRequest.getCanonicalEvidence());
        req.setDomesticEvidenceList(dtRequest.getDomesticEvidenceList());
        req.setErrorList(dtRequest.getErrorList());
        return req;
    }

    public static String getStackTrace(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static CompletableFuture<Boolean> sendRequest(String recipient, InputStream bodyStream, Consumer<String> onFailure) {
        HttpResponse dtResp;
        try {
            dtResp = Request.Post(recipient)
                    .bodyStream(bodyStream, ContentType.APPLICATION_XML)
                    .execute().returnResponse();
        } catch (IOException ex) {
            onFailure.accept(String.format("Failed to send request to dt: %s", ex.getMessage()));
            return CompletableFuture.completedFuture(false);
        }
        if (dtResp.getStatusLine().getStatusCode() != 200) {
            onFailure.accept(String.format("Request sent to dt (%s) got status code %s",
                    recipient,
                    dtResp.getStatusLine().getStatusCode()));
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }
}
