package gr.cite.commons.converter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import java.io.IOException;

public class XmlToJsonConverterTest {
	@Test
	public void convert() throws XMLStreamException, IOException {
		Client client = ClientBuilder.newClient();
		//WebTarget webTarget = client.target("http://access.planetserver.eu:8080/rasdaman/ows");
		WebTarget webTarget = client.target("http://earthserver.ecmwf.int/rasdaman/ows");

		 
		 String xml = webTarget
				 .queryParam("service", "WCS")
				 .queryParam("version", "2.0.1")
				 .queryParam("request", "DescribeCoverage")
				 //.queryParam("coverageId", "hrl0000c067_07_if185l_trr3")
				 .queryParam("coverageId", "pl_geopot")
				 .request().get(String.class);

		//http://earthserver.ecmwf.int/rasdaman/ows?service=WCS&version=2.0.1&request=DescribeCoverage&coverageId=pl_geopot

		String json = XmlJsonConverter.xmlToFemmeJson(xml);
		System.out.println(json);

		String xmlFromJson = XmlJsonConverter.femmeJsonToXml(json);
		System.out.println(xmlFromJson);
	}
}
