package org.alexgraham;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Resource for managing the root for this application.
 */
@Path("")
public class AppResource {

    /**
     * For the purposes of this prototype, simply re-direct to the Tasks app.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get() {
        return Response.temporaryRedirect(URI.create("/my-tasks")).build();
    }
}
