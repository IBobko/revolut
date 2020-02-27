package revolut.resource;

import io.swagger.annotations.Api;
import revolut.model.Holder;
import revolut.service.HolderService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/holders")
@Api
public class HolderResource {
    @Inject
    private HolderService holderService;

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Long, Holder> holders() {
        return holderService.getHolders();
    }

    @GET
    @Path("id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Holder holder(@PathParam("id") Long id) {
        return holderService.getHolderById(id);
    }

}
