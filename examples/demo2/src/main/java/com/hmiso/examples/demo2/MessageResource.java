package com.hmiso.examples.demo2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/message")
public class MessageResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response message() {
        return Response.ok(Map.of("message", "Bonjour depuis demo2")).build();
    }
}
