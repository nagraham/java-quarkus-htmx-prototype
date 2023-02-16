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
import javax.ws.rs.QueryParam;
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

    @ServerExceptionMapper
    public Uni<Response> mapException(IllegalArgumentException e) {
        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).build());
    }

    /**
     * JSON API for completing a Task.
     *
     * @param taskId    The Task to complete.
     * @param userId    The user ID (currently, this is a silly proxy until I have auth/sessions).
     * @return          The completed Task.
     */
    @POST
    @Path("/{id}/complete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Task> complete(
            @PathParam("id") Long taskId,
            @RestHeader("X-User-Id") String userId
    ) {
        return service.completeTask(taskId);
    }

    /**
     * The HTML endpoint for completing a Task
     *
     * @param taskId        The Task to complete.
     * @param userId        The user ID (currently, this is a silly proxy until I have auth/sessions).
     * @param isHxRequest   Whether the incoming request is via HTMX (else, it will return a standard 302 resp).
     * @return
     */
    @POST
    @Path("/{id}/complete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> complete(
            @PathParam("id") Long taskId,
            @RestCookie UUID userId,
            @RestHeader("HX-Request") boolean isHxRequest
    ) {
        return service.completeTask(taskId).map(ignored -> Response.ok().build());
    }

    // DEV NOTE: It seems we should not use @Consumes on GET APIs. I had @Consumes with the associated media type,
    // but this led to a strange bug where the browser would call the JSON API if the HTTP endpoint took in @QueryParams.

    /**
     * The JSON API for returning a List of Tasks belonging to the given User.
     * <p>
     * By default, only Tasks with an "Open" {@link Task.State} will be returned. To find Tasks in
     * other states, it is possible to filter using the state query parameters.
     *
     * @param userId    The User to find tasks for.
     * @param state     An optional list of {@link Task.State}s as a filter to the results.
     * @return          A list of Tasks belonging to the User.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)   // don't use Consume here; otherwise, the "Accept */*" will favor JSON
    public Uni<List<Task>> list(
            @RestHeader("X-User-Id") String userId,
            @QueryParam("state") final List<String> state
    ) {
        List<Task.State> states = state.stream().map(Task.State::parse).toList();
        return service.queryByOwner(userId, states);
    }

    // DEV NOTE: If you don't see any items listed, make sure you have the
    // "userId" cookie set in your browser to a test ID (check import.sql).
    @GET
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> list(
            @RestCookie String userId,
            @QueryParam("state") final List<String> state,
            @RestHeader("HX-Request") Boolean isHxRequest
    ) {
        List<Task.State> taskStates = state.stream().map(Task.State::parse).toList();
        return service.queryByOwner(userId, taskStates).onItem().transform(Template::list);
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

    /**
     * JSON endpoint for updating a Task.
     *
     * @param task              A Task object containing attributes to update. Any null attributes will be ignored.
     * @param taskId            The ID of the task to update (specified on the path).
     * @param ignored_userId    The user ID (currently, this is a silly proxy until I have auth/sessions).
     * @return                  A Response containing the updated Task.
     */
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

    /**
     * The HTML ndpoint for updating a Task.
     *
     * @param taskId            The ID of the task to update (specified on the path).
     * @param title             (Optional) The title of the task. If not provided, it is ignored.
     * @param description       (Optional) The Task description. If not provided, it is ignored.
     * @param userId            The user ID (currently, this is a silly proxy until I have auth/sessions).
     * @param isHxRequest       Whether the incoming request is via HTMX (else, it will return a standard 302 resp).
     * @param isViewingDetails  This header is passed to the template as a view control, whether the Task should
     *                          be rendered with its detail pane open. This allows the view to control this scenario.
     * @return                  A Response with the given Task template.
     */
    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> update(
            @PathParam("id") Long taskId,
            @RestForm String title,
            @RestForm String description,
            @RestCookie String userId,
            @RestHeader("HX-Request") boolean isHxRequest,
            @RestHeader("X-Override-IsViewingDetails") boolean isViewingDetails
    ) {
        LOG.info(String.format("Title: %s; Description: %s, isViewing: %b", title, description, isViewingDetails));
        return service.update(taskId, new Task().setTitle(title).setDescription(description))
                .map(updatedTask -> postResponse(isHxRequest, "/tasks",
                        Response.ok(Template.task(updatedTask).data("isViewingDetails", isViewingDetails))));
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
            return responseBuilder.build();
        } else {
            // backup for progressive enhancement
            return Response.status(Response.Status.FOUND).header("Location", path).build();
        }
    }
}
