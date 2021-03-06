package io.javaoperatorsdk.operator;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.sample.event.EventSourceTestCustomResource;
import io.javaoperatorsdk.operator.sample.event.EventSourceTestCustomResourceController;
import io.javaoperatorsdk.operator.sample.event.EventSourceTestCustomResourceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventSourceIT {

  private static final Logger log = LoggerFactory.getLogger(EventSourceIT.class);

  public static final int EXPECTED_TIMER_EVENT_COUNT = 3;
  private final IntegrationTestSupport integrationTestSupport = new IntegrationTestSupport();

  @BeforeEach
  public void initAndCleanup() {
    KubernetesClient k8sClient = new DefaultKubernetesClient();
    integrationTestSupport.initialize(
        k8sClient, new EventSourceTestCustomResourceController(), "eventsource-test-crd.yaml");
    integrationTestSupport.cleanup();
  }

  @Test
  public void receivingPeriodicEvents() {
    integrationTestSupport.teardownIfSuccess(
        () -> {
          EventSourceTestCustomResource resource = createTestCustomResource("1");
          integrationTestSupport
              .getCrOperations()
              .inNamespace(IntegrationTestSupport.TEST_NAMESPACE)
              .create(resource);

          Thread.sleep(
              EventSourceTestCustomResourceController.TIMER_DELAY
                  + EXPECTED_TIMER_EVENT_COUNT
                      * EventSourceTestCustomResourceController.TIMER_PERIOD);

          assertThat(integrationTestSupport.numberOfControllerExecutions())
              .isGreaterThanOrEqualTo(EXPECTED_TIMER_EVENT_COUNT + 1);
        });
  }

  public EventSourceTestCustomResource createTestCustomResource(String id) {
    EventSourceTestCustomResource resource = new EventSourceTestCustomResource();
    resource.setMetadata(
        new ObjectMetaBuilder()
            .withName("eventsource-" + id)
            .withNamespace(IntegrationTestSupport.TEST_NAMESPACE)
            .withFinalizers(EventSourceTestCustomResourceController.FINALIZER_NAME)
            .build());
    resource.setKind("Eventsourcesample");
    resource.setSpec(new EventSourceTestCustomResourceSpec());
    resource.getSpec().setValue(id);
    return resource;
  }
}
