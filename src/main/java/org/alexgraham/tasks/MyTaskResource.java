package org.alexgraham.tasks;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestCookie;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/my-tasks")
@ApplicationScoped
public class MyTaskResource {

    @Inject
    TaskService service;

    @CheckedTemplate
    public static class Template {
        public static native TemplateInstance show(List<Task> tasks);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> show(@RestCookie String userId) {
        return service.queryByOwner(userId, List.of())
                .map(Template::show);
    }

}
