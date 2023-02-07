package org.alexgraham.tasks;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/tasks")
@ApplicationScoped
public class TaskResource {
    private static final Logger LOG = Logger.getLogger(TaskResource.class);

    @Inject
    TaskService service;

    @CheckedTemplate
    public static class Template {
        public static native TemplateInstance list(List<Task> tasks);
        public static native TemplateInstance task(Task task);
    }

    @ServerExceptionMapper
    public Uni<Response> mapException(TaskNotFoundException e) {
        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
    }

    // The "foo" rest header is just to make this API not clash with the html list
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Task>> list(@RestHeader("X-User-Id") String userId, @RestHeader("X-Foo") String foo) {
        return service.queryByOwner(userId);
    }

    // DEV NOTE: If you don't see any items listed, make sure you have the
    // "userId" cookie set in your browser to a test ID (check import.sql).
    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> list(@RestCookie String userId) {
        LOG.debug("Listing Tasks for User=" + userId);
        return service.queryByOwner(userId)
                .onItem()
                .transform(Template::list);
    }

    @GET
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Task> getById(Long id) {
        return Task.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> create(Task task, @RestHeader("X-User-Id") String userId) {
        return service.persist(task.getTitle(), UUID.fromString(userId))
                .onItem()
                .transform(newTask -> Response
                        .created(URI.create("/tasks/" + newTask.id))
                        .entity(newTask)
                        .build());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> create(
            @RestForm String title,
            @RestCookie String userId,
            @RestHeader("HX-Request") boolean isHxRequest
    ) {
        return service.persist(title, UUID.fromString(userId))
                .map(newTask -> postResponse(isHxRequest, "/tasks", Response.ok(Template.task(newTask))
                        .header("HX-Trigger", "clear-add-task")));
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Task> update(
            Task task,
            @PathParam("id") Long taskId,
            @RestHeader("X-User-Id") UUID ignored_userId
    ) {
        return service.update(taskId, task);
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> update(
            @PathParam("id") Long taskId,
            @RestForm String title,
            @RestCookie String userId,
            @RestHeader("HX-Request") boolean isHxRequest
    ) {
        return service.update(taskId, new Task().setTitle(title))
                .map(updatedTask -> postResponse(isHxRequest, "/tasks", Response.ok(Template.task(updatedTask))));
    }

    private record RerankParams(List<Long> rankings) {}

    @POST
    @Path("/rerank")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> rerank(
            RerankParams params,
            @RestHeader("X-User-Id") UUID userId
    ) {
        LOG.info("rankings: " + params.rankings.stream().map(Object::toString).collect(Collectors.joining(", ")));
        return service.saveTaskRankings(userId, params.rankings())
                .map(ignored -> Response.ok().build());
    }

    @POST
    @Path("/rerank")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> rerank(
            @RestForm("item") List<Long> ranks,
            @RestCookie UUID userId,
            @RestHeader("HX-Request") boolean isHxRequest
    ) {
        LOG.info("item: " + ranks.stream().map(Object::toString).collect(Collectors.joining(", ")));
        return service.saveTaskRankings(userId, ranks)
                .map(ignored -> postResponse(isHxRequest, "/tasks", Response.noContent()));
    }

    /**
     * A helper function for returning an HTML post response.
     *
     * @param isHxRequest       Input header from the front-end; if false, we will fall back to a standard
     *                          302 response based on a form submit.
     * @param path              The re-direct path (for a 302 redirect). HTMX will put this path into
     *                          the browser's nav bar.
     * @param responseBuilder   The partially built Response builder from
     * @return                  The Response.
     */
    private Response postResponse(boolean isHxRequest, String path, Response.ResponseBuilder responseBuilder) {
        if (isHxRequest) {
            return responseBuilder.header("HX-Push", path).build();
        } else {
            // backup for progressive enhancement
            return Response.status(Response.Status.FOUND).header("Location", path).build();
        }
    }
}
