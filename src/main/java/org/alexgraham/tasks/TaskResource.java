package org.alexgraham.tasks;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/tasks")
@ApplicationScoped
public class TaskResource {

    @GET
    public Uni<List<Task>> list() {
        return Task.listAll(Sort.by("id"));
    }

    @GET
    @Path("/{id}")
    public Uni<Task> getById(Long id) {
        return Task.findById(id);
    }

    @POST
    public Uni<Response> create(Task task) {
        return Panache.<Task>withTransaction(task::persist)
                .onItem()
                .transform(newTask -> Response
                        .created(URI.create("/tasks/" + newTask.id))
                        .entity(newTask)
                        .build());
    }
}
