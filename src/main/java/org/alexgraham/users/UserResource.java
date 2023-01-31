package org.alexgraham.users;

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

@Path("/users")
@ApplicationScoped
public class UserResource {

    @GET
    public Uni<List<User>> list() {
        return User.listAll(Sort.by("name"));
    }

    @POST
    public Uni<Response> create(User user) {
        return Panache.<User>withTransaction(user::persist)
        .onItem()
        .transform(newUser -> Response
                .created(URI.create("/users/" + newUser.getId()))
                .entity(newUser)
                .build());
    }
}


