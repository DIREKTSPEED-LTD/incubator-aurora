package com.twitter.mesos.scheduler.httphandlers;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Value.Text;
import org.apache.mesos.Protos.Value.Type;

import com.twitter.mesos.scheduler.async.TaskScheduler;

/**
 * Servlet that exposes resource offers that the scheduler is currently retaining.
 */
@Path("/offers")
public class Offers {

  private final TaskScheduler scheduler;

  @Inject
  Offers(TaskScheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Dumps the offers queued in the scheduler.
   *
   * @return HTTP response.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOffers() {
    Offer offer = Offer.newBuilder()
        .setFrameworkId(FrameworkID.newBuilder().setValue("framework-id"))
        .setSlaveId(SlaveID.newBuilder().setValue("slave-id"))
        .setHostname("hostname")
        .setId(OfferID.newBuilder().setValue("offer-id"))
        .addAttributes(Attribute.newBuilder().setName("attr")
            .setType(Type.TEXT)
            .setText(Text.newBuilder().setValue("some text")))
        .addResources(Resource.newBuilder().setName("cpu")
            .setType(Type.SCALAR)
            .setScalar(Scalar.newBuilder().setValue(16.7)))
        .build();

    return Response.ok(
        FluentIterable.from(ImmutableList.<Offer>builder().addAll(scheduler.getOffers())
            .add(offer).build()).transform(TO_BEAN).toImmutableList()).build();
  }

  private static final Function<ExecutorID, String> EXECUTOR_ID_TOSTRING =
      new Function<ExecutorID, String>() {
        @Override public String apply(ExecutorID id) {
          return id.getValue();
        }
      };

  private static final Function<Range, Object> RANGE_TO_BEAN = new Function<Range, Object>() {
    @Override public Object apply(Range range) {
      return range.getBegin() + "-" + range.getEnd();
    }
  };

  private static final Function<Attribute, Object> ATTRIBUTE_TO_BEAN =
      new Function<Attribute, Object>() {
        @Override public Object apply(Attribute attr) {
          ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
          builder.put("name", attr.getName());
          if (attr.hasScalar()) {
            builder.put("scalar", attr.getScalar().getValue());
          }
          if (attr.hasRanges()) {
            builder.put("ranges", immutable(attr.getRanges().getRangeList(), RANGE_TO_BEAN));
          }
          if (attr.hasSet()) {
            builder.put("set", attr.getSet().getItemList());
          }
          if (attr.hasText()) {
            builder.put("text", attr.getText().getValue());
          }
          return builder.build();
        }
      };

  private static final Function<Resource, Object> RESOURCE_TO_BEAN =
      new Function<Resource, Object>() {
        @Override public Object apply(Resource resource) {
          ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
          builder.put("name", resource.getName());
          if (resource.hasScalar()) {
            builder.put("scalar", resource.getScalar().getValue());
          }
          if (resource.hasRanges()) {
            builder.put("ranges", immutable(resource.getRanges().getRangeList(), RANGE_TO_BEAN));
          }
          if (resource.hasSet()) {
            builder.put("set", resource.getSet().getItemList());
          }
          return builder.build();
        }
      };

  private static <A, B> Iterable<B> immutable(Iterable<A> iterable, Function<A, B> transform) {
    return FluentIterable.from(iterable).transform(transform).toImmutableList();
  }

  private static final Function<Offer, Map<String, ?>> TO_BEAN =
      new Function<Offer, Map<String, ?>>() {
        @Override public Map<String, ?> apply(Offer offer) {
          return ImmutableMap.<String, Object>builder()
              .put("id", offer.getId().getValue())
              .put("framework_id", offer.getFrameworkId().getValue())
              .put("slave_id", offer.getSlaveId().getValue())
              .put("hostname", offer.getHostname())
              .put("resources", immutable(offer.getResourcesList(), RESOURCE_TO_BEAN))
              .put("attributes", immutable(offer.getAttributesList(), ATTRIBUTE_TO_BEAN))
              .put("executor_ids", immutable(offer.getExecutorIdsList(), EXECUTOR_ID_TOSTRING))
              .build();
        }
      };
}