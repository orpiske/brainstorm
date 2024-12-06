package org.brainstorm.service.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Base64;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.vertx.core.eventbus.EventBus;
import org.brainstorm.service.util.BrainstormConfiguration;
import  org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;

@Transactional
@Path("/api/v1/acquisition/service")
public class AcquisitionResource {
    private static final Logger LOG = Logger.getLogger(AcquisitionResource.class);
    static final String BASE_URI = "/api/v1/acquisition/service";

    @Inject
    EventBus eventBus;

    @Inject
    BrainstormConfiguration config;

    @POST
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Path("/")
    public Response add(String body) {
        byte[] decodedBytes = Base64.getDecoder().decode(body.trim());
        String route = new String(decodedBytes);

        LOG.infof("Adding service %s", route);

        try {
            eventBus.publish("acquisition", route);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_OCTET_STREAM})
    @Path("/package")
//    public Response addPackage(@RestForm("package")InputStream body) {
    public Response addPackage(InputStream body) {
        LOG.infof("Adding package");

        try {
            final String cachePath = config.cache().path();
            Paths.get(cachePath).toFile().mkdirs();

            final java.nio.file.Path tempFile = Files.createTempFile(Paths.get(cachePath), "brainstorm", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                body.transferTo(fos);
            }

            eventBus.publish("package", tempFile);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

}
