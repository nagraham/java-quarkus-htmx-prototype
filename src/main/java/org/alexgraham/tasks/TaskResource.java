package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/tasks")
@ApplicationScoped
public class TaskResource {
    private static final Logger LOG = Logger.getLogger(TaskResource.class);

    @CheckedTemplate
    public static class Template {
        public static native TemplateInstance list(List<Task> tasks);
        public static native TemplateInstance task(Task task);
    }

    // The "foo" rest header is just to make this API not clash with the html list
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Task>> list(@RestHeader("X-User-Id") String userId, @RestHeader("X-Foo") String foo) {
        return Task.find("ownerid = ?1", userId).list();
    }

    // DEV NOTE: If you don't see any items listed, make sure you have the
    // "userId" cookie set in your browser to a test ID (check import.sql).
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> list(@RestCookie String userId) {
        LOG.debug("Listing Tasks for User=" + userId);
        return Task.<Task>find("ownerid = ?1", userId)
                .list().onItem().transform(Template::list);
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
    public Uni<Response> create(Task task, @RestHeader("X-User-Id") String ownerId) {
        return User.<User>findById(UUID.fromString(ownerId))
                .onItem().transform(user -> new Task(task.getTitle(), user))
                .flatMap(taskToPersist -> Panache.<Task>withTransaction(taskToPersist::persist))
                .onItem()
                .transform(newTask -> Response
                        .created(URI.create("/tasks/" + newTask.id))
                        .entity(newTask)
                        .build());
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    public Uni<Response> create(
            @RestForm String title,
            @RestCookie String userId,
            @RestHeader("HX-Request") boolean isHxRequest
    ) {
        return User.<User>findById(UUID.fromString(userId))
                .onItem().transform(user -> new Task(title, user))
                .flatMap(taskToPersist -> Panache.<Task>withTransaction(taskToPersist::persist))
                .onItem()
                .transform(newTask -> {
                    if (isHxRequest) {
                        return Response.ok(Template.task(newTask))
                                .header("HX-Trigger", "clear-add-task")
                                .build();
                    } else {
                        return Response.status(Response.Status.FOUND)
                                .header("Location", "/tasks")
                                .build();
                    }
                });
    }
}
