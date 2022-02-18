package eu.de4a.connector.mock.preview;

import eu.de4a.iem.jaxb.common.types.RequestTransferEvidenceUSIDTType;
import eu.de4a.iem.xml.de4a.IDE4ACanonicalEvidenceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@Profile("do")
public class SubscriptionStorage {
// TODO 
    // For concurrency handling, I'm assuming the requestId is unique. Since the data is not changed it can be considered
    // immutable no further assumptions are needed.
    private ConcurrentHashMap<String, Preview<RequestTransferEvidenceUSIDTType>> subscriptionToPreview;
    private TaskScheduler taskScheduler;

    public SubscriptionStorage(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        this.subscriptionToPreview = new ConcurrentHashMap<>();
        this.taskScheduler.scheduleWithFixedDelay(() -> this.pruneOld(Duration.ofMinutes(35)), Duration.ofSeconds(20));
    }

    private Preview<RequestTransferEvidenceUSIDTType> getRequestLockPair(String requestId) {
        if (!subscriptionToPreview.containsKey(requestId)) {
            subscriptionToPreview.put(requestId, new Preview<>(null, "", null));
        }
        return subscriptionToPreview.get(requestId);
    }

    public void addRequestToPreview(RequestTransferEvidenceUSIDTType request) {
        Preview<RequestTransferEvidenceUSIDTType> preview = getRequestLockPair(request.getRequestId());
        synchronized (preview.lock) {
            preview.object = request;
            preview.lock.notifyAll();
        }
    }

    public CompletableFuture<RequestTransferEvidenceUSIDTType> getRequest(String requestId) throws InterruptedException {
        Preview<RequestTransferEvidenceUSIDTType> preview =  getRequestLockPair(requestId);
        while (subscriptionToPreview.containsKey(requestId)) {
            synchronized (preview.lock) {
                if (preview.object != null) {
                    return CompletableFuture.completedFuture(preview.object);
                }
                log.debug("wait");
                preview.lock.wait();
            }
        }
        return CompletableFuture.failedFuture(new IOException("Subscription preview pruned, too old"));
    }

    public List<String> getAllRequestIds() {
        return subscriptionToPreview.entrySet().stream()
                .filter(entry -> entry.getValue().object != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void pruneOld(TemporalAmount oldThreshold) {
        subscriptionToPreview.entrySet().stream()
                .filter(entry -> entry.getValue().timeStamp.plus(oldThreshold).isBefore(Instant.now()))
                .forEach(entry -> {
                    synchronized (entry.getValue().lock) {
                        subscriptionToPreview.remove(entry.getKey());
                    }});
    }

    public void removePreview(String requestId) {
        subscriptionToPreview.remove(requestId);
    }

    private static class Preview<T> {
        private final Object lock;
        private T object;
        private Instant timeStamp;

        private Preview(T object, String redirectionUrl, IDE4ACanonicalEvidenceType canonicalEvidenceType) {
            this.lock = new Object();
            this.object = object;
            this.timeStamp = Instant.now();
        }
    }
}
