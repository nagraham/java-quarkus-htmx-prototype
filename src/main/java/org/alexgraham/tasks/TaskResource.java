package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import org.alexgraham.users.User;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestHeader;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/tasks")
@ApplicationScoped
public class TaskResource {
    private static final Logger LOG = Logger.getLogger(TaskResource.class);

    @GET
    public Uni<List<Task>> list(@RestHeader("X-User-Id") String userId) {
        return Task.find("ownerid = ?1", userId).list();
    }

    @GET
    @Path("/{id}")
    public Uni<Task> getById(Long id) {
        return Task.findById(id);
    }

    @POST
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
}
