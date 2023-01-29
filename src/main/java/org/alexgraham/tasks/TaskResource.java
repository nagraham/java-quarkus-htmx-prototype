package org.alexgraham.tasks;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.List;

@Path("/tasks")
@ApplicationScoped
public class TaskResource {

    @GET
    public Uni<List<Task>> list() {
        return Task.listAll(Sort.by("id"));
    }

}
