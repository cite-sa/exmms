package gr.cite.earthserver.wcs.adapter.test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import gr.cite.earthserver.wcs.adapter.WCSAdapter;
import gr.cite.earthserver.wcs.adapter.api.WCSAdapterAPI;
import gr.cite.earthserver.wcs.adapter.request.WCSAdapterCoverages;
import gr.cite.earthserver.wcs.adapter.request.WCSAdapterRequest;
import gr.cite.earthserver.wcs.adapter.request.WCSAdapterServers;
import gr.cite.earthserver.wcs.core.Coverage;
import gr.cite.earthserver.wcs.core.Server;
import gr.cite.earthserver.wcs.core.WCSResponse;
import gr.cite.earthserver.wcs.utils.ParseException;
import gr.cite.femme.client.FemmeClient;
import gr.cite.femme.client.FemmeClientException;
import gr.cite.femme.client.FemmeDatastoreException;
import gr.cite.femme.client.api.FemmeClientAPI;
import gr.cite.femme.client.query.QueryClient;
import gr.cite.femme.model.DataElement;
import gr.cite.femme.query.api.Criterion;
import gr.cite.femme.query.api.Query;
import gr.cite.femme.query.api.QueryOptionsFields;
import gr.cite.femme.utils.Pair;

public class WCSAdapterTest {
	
	private WCSAdapterAPI wcsAdapter;
	
	@Before
	public void init() {
		this.wcsAdapter = new WCSAdapter("http://localhost:8081/femme-application");
//		this.wcsAdapter = new WCSAdapter("http://es-devel1.local.cite.gr:8080/femme-application-0.0.1-SNAPSHOT");
	}
	
//	@Test
	public String insertServer(String endpoint, String name, WCSResponse server) throws ParseException, FemmeDatastoreException {
		return null;
	}

//	@Test
	public String insertCoverage(WCSResponse coverage) throws ParseException, FemmeDatastoreException {
		return null;
	}
	
//	@Test
	public String addCoverage(WCSResponse coverage, String collectionId) throws ParseException, FemmeDatastoreException {
		return null;
	}
	
	@Test
	public void getServers() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getServers().size() > 0);
	}
	
	@Test
	public void getServersLimitOffset()  throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getServers(1, 0).size() == 1);
	}
	
//	@Test
	public void getServersXPath()  throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getServers(1, 0).size() == 1);
	}
	
	@Test
	public void getServerEndpoint() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getServer("https://rsg.pml.ac.uk/rasdaman/ows") != null);
	}
	
	@Test
	public void getServerName() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getServer("1412f147-7468-4dcc-bcd4-5858c816b81b") != null);
	}

//	@Test
	public <T extends Criterion> List<Server> findServers(Query<T> query, QueryOptionsFields options, String xPath)
			throws FemmeDatastoreException, FemmeClientException {
				return null;
	}
	
	@Test
	public void getCoverages() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoverages().size() == 59);
	}
	
	@Test
	public void getCoveragesLimitOffset() throws FemmeDatastoreException, FemmeClientException {
		List<Coverage> coverages = this.wcsAdapter.getCoverages(1, 5);
		assertTrue(coverages.size() ==1 && coverages.get(0).getCoverageId().equals("small_no_nulls"));
	}
	
//	@Test
	public List<Coverage> getCoverages(Integer limit, Integer offset, String xPath) throws FemmeDatastoreException, FemmeClientException {
		return null;
	}
	
	@Test
	public void getCoverageById() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoverageById("582493bfcd42310e58c7d4b7").getCoverageId().equals("small_no_nulls"));
	}
	
//	@Test
	public <T extends Criterion> List<Coverage> findCoverages(Query<T> query, QueryOptionsFields options, String xPath)
			throws FemmeDatastoreException, FemmeClientException, FemmeClientException {
				return null;
	}
	
	@Test
	public void getCoverageIds() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoverageIds().size() == 59);
	}
	
	@Test
	public void getCoverageIdsInServer() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoverageIdsInServer(Arrays.asList("1412f147-7468-4dcc-bcd4-5858c816b81b")).size() == 59);
		assertTrue(this.wcsAdapter.getCoverageIdsInServer(Arrays.asList("https://rsg.pml.ac.uk/rasdaman/ows")).size() == 59);
	}
	
	@Test
	public void getCoverageIdsInServerLimitOffset() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoverageIdsInServer(Arrays.asList("https://rsg.pml.ac.uk/rasdaman/ows"), 1, null, null).size() == 1);
	}
	
	@Test
	public void getCoveragesByCoverageId() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoveragesByCoverageId("small_no_nulls").size() == 1);
	}

	@Test
	public void getCoveragesInServer() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoveragesInServer(Arrays.asList("1412f147-7468-4dcc-bcd4-5858c816b81b")).size() == 59);
		assertTrue(this.wcsAdapter.getCoveragesInServer(Arrays.asList("https://rsg.pml.ac.uk/rasdaman/ows")).size() == 59);
	}
	
	@Test
	public void getCoveragesInServerLimitOffset() throws FemmeDatastoreException, FemmeClientException {
		assertTrue(this.wcsAdapter.getCoveragesInServer(Arrays.asList("1412f147-7468-4dcc-bcd4-5858c816b81b"), 5, null, null).size() == 5);
		assertTrue(this.wcsAdapter.getCoveragesInServer(Arrays.asList("https://rsg.pml.ac.uk/rasdaman/ows"), 5, null, null).size() == 5);
	}
	
	@Test
	public void getCoverageByCoverageIdInServer() throws FemmeDatastoreException, FemmeClientException {
		assertEquals(this.wcsAdapter.getCoverageByCoverageIdInServer("1412f147-7468-4dcc-bcd4-5858c816b81b", "small_no_nulls").getCoverageId(), "small_no_nulls");
		assertEquals(this.wcsAdapter.getCoverageByCoverageIdInServer("https://rsg.pml.ac.uk/rasdaman/ows", "small_no_nulls").getCoverageId(), "small_no_nulls");
	}
	
	
	
//	@Test
	public void test() throws JsonProcessingException, FemmeDatastoreException {
//		serverProperties.put("endpoint", "endpoint2");
//		coverageProperties.put("name", "frt00014174_07_if166s_trr3");
//		coverageProperties.put("id", "2");
		
		/*WCSAdapterServers servers = new WCSAdapterServers();
		WCSAdapterCoverages coverages = new WCSAdapterCoverages();
		
		servers
			.or()
			.attribute(new Pair<String, String>("endpoint", "http://access.planetserver.eu:8080/rasdaman/ows"))
			.attribute(new Pair<String, String>("endpoint", "https://rsg.pml.ac.uk/rasdaman/ows"));
		
		coverages
		.or()
		.attribute(new Pair<String, String>("name", "frt00014174_07_if166s_trr3"));
		
		WCSAdapterRequest request = new WCSAdapterRequest(servers, coverages);
		
		ObjectMapper mapper = new ObjectMapper();
		
		String jsonRequest = mapper.writeValueAsString(request);
		System.out.println(jsonRequest);
		
		
		QueryClient query = request.mapToQuery();
		
		String jsonQuery = mapper.writeValueAsString(query);
		System.out.println(jsonQuery);*/
		
		
		
		/*WCSAdapter adapter = new WCSAdapter("http://es-devel1.local.cite.gr:8080/femme-application-0.0.1-SNAPSHOT");
		List<Coverage> coverages = adapter.getCoverages(5, null);
		
		System.out.println(coverages);
		
		FemmeClientAPI femmeClient = new FemmeClient("http://es-devel1.local.cite.gr:8080/femme-application-0.0.1-SNAPSHOT");
		List<DataElement> dataElements = femmeClient.findDataElements(null, 5, null, null);
		
		System.out.println(dataElements);*/
		
		/*List<Coverage> coverages = this.wcsAdapter.getCoveragesByCoverageId("frt0000cc22_07_if165l_trr3");*/
		/*System.out.println(coverages);*/
		
	}
}
