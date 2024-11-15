package org.brainstorm.service.resources;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.brainstorm.api.dto.AcquisitionService;
import org.brainstorm.service.repository.AcquisitionServiceRepository;
import org.brainstorm.service.util.RequestResponseUtil;
import org.jboss.logging.Logger;

@Transactional
@Path("/api/v1/acquisition/service")
public class AcquisitionResource {
    private static final Logger LOG = Logger.getLogger(AcquisitionResource.class);
    static final String BASE_URI = "/api/v1/acquisition/service";

    @Inject
    AcquisitionServiceRepository repository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllServices() {
        try {
            final List<AcquisitionService> qnaList = repository.listAll();

            return Response.ok(qnaList).build();
        } catch (Exception e) {
            LOG.errorf("Unable to list activities: %s", e.getMessage(), e);
            return Response.serverError().build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/id/{id}")
    public Response getById(@PathParam("id") Long id) {
        LOG.infof("Getting service info for %d", id);

        try {
            final var record = repository.findById(id);
            return RequestResponseUtil.validateFetchedObject(record);
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response add(AcquisitionService service) {
        LOG.infof("Adding service %s", service.getName());

        try {
            repository.persist(service);
            return Response.created(RequestResponseUtil.toLocation(BASE_URI, service.getId())).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

}
