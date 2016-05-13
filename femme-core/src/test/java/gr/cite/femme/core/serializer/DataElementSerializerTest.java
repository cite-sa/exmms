package gr.cite.femme.core.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.cite.femme.core.DataElement;
import gr.cite.femme.core.Metadatum;

public class DataElementSerializerTest {

	@Test
	public void serializeDataElement() {
		ObjectMapper mapper = new ObjectMapper();

		DataElement dataElement = new DataElement();
		dataElement.setId(UUID.randomUUID().toString());
		dataElement.setEndpoint(UUID.randomUUID().toString());
		dataElement.setMetadata(
				Arrays.asList(new Metadatum(UUID.randomUUID().toString(), "test", "testValue", "xml")));
		
		try {
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataElement);
			System.out.println(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		 
	}

}