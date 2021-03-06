package gr.cite.earthserver.wcs.geo;

import gr.cite.femme.client.FemmeException;
import gr.cite.femme.core.dto.FemmeResponse;
import gr.cite.femme.core.geo.CoverageGeo;
import gr.cite.femme.core.geo.ServerGeo;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class GeoRequests {
	private static final Logger logger = LoggerFactory.getLogger(GeoRequests.class);
	
	private WebTarget webTarget;
	private Client client;
	
	private static final String GEO_URL = "http://localhost:8084/femme-geo-application-devel";
	
	public GeoRequests() {
		new GeoRequests(GeoRequests.GEO_URL);
	}
	
	public GeoRequests(String femmeGeoUrl) {
		client = ClientBuilder.newClient().register(JacksonFeature.class);
		webTarget = client.target(femmeGeoUrl);
	}
	
	public String insert(CoverageGeo coverageGeo) throws FemmeException {
		
		Response response = webTarget.path("admin/coverages")
				.request().post(Entity.entity(coverageGeo, MediaType.APPLICATION_JSON));
		
		String id = response.readEntity(String.class);
		if (response.getStatus() != 200) {
			//logger.error(femmeResponse.getMessage());
			throw new FemmeException("Error on geo insert");
		}
		//logger.debug("CoverageGeo " + femmeResponse.getEntity().getBody() + " has been successfully inserted");
		return id;
	}

	public String insertServerGeo(String collectionId, String collectionName) throws FemmeException {
		ServerGeo server = new ServerGeo();
		server.setCollectionId(collectionId);
		server.setServerName(collectionName);
		
		String id = null;
		try {
			
			Response response = webTarget.path("admin/servers").request().post(Entity.entity(server, MediaType.APPLICATION_JSON));
			
			id = response.readEntity(String.class);
			if (response.getStatus() != 200) {
				//logger.error(femmeResponse.getMessage());
				logger.error("Error on inserting server in geo service");
			}
		} catch (Exception e) {
			logger.error("Error on inserting server in geo service", e);
		}
		//logger.debug("CoverageGeo " + femmeResponse.getEntity().getBody() + " has been successfully inserted");
		return id;
	}
	
}
