package gr.cite.femme.geo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.cite.femme.core.geo.CoverageGeo;
import gr.cite.femme.geo.api.GeoServiceApi;
import gr.cite.femme.geo.core.FemmeGeoException;
import gr.cite.femme.geo.engine.mongodb.MongoGeoDatastore;
import gr.cite.femme.geo.utils.GeoUtils;
import org.geojson.GeoJsonObject;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
public class GeoService implements GeoServiceApi {
	private static final Logger logger = LoggerFactory.getLogger(GeoService.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private MongoGeoDatastore mongoGeoDatastore;
	
	@Inject
	public GeoService(MongoGeoDatastore mongoGeoDatastore) {
		this.mongoGeoDatastore = mongoGeoDatastore;
	}
	
	@Override
	public List<CoverageGeo> getCoveragesByBboxString(String bBox) throws FemmeGeoException {
		try {
			GeoJsonObject geoJson = GeoUtils.getBBoxFromString(bBox);
			return mongoGeoDatastore.getCoveragesByPolygon(geoJson);
		} catch (IOException | FactoryException e) {
			throw new FemmeGeoException("Error on querying coverage by bbox", e);
		}
	}
	
	@Override
	public List<CoverageGeo> getCoveragesByPoint(Double longitude, Double latitude, Double radius) throws FemmeGeoException {
		try {
			return mongoGeoDatastore.getCoverageByCoords(longitude, latitude, radius);
		} catch (Exception e) {
			throw new FemmeGeoException("Error on querying coverage by point", e);
		}
		
	}
	
	
}
