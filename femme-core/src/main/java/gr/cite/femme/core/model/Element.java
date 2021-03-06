package gr.cite.femme.core.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class Element {

	@JsonProperty("id")
	private String id;
	
	@JsonProperty("name")
	private String name;
	
	@JsonProperty("endpoint")
	private String endpoint;
	
	@JsonProperty("type")
	private ElementType type;

	@JsonProperty("metadata")
	private List<Metadatum> metadata;

	@JsonProperty("systemicMetadata")
	private SystemicMetadata systemicMetadata;

	public Element() {
		this.metadata = new ArrayList<>();
		this.systemicMetadata = new SystemicMetadata();
	}
	
	public Element(String id, String name, String endpoint) {
		this.id = id;
		this.name = name;
		this.endpoint = endpoint;
		this.metadata = new ArrayList<>();
		//systemicMetadata = new SystemicMetadata();
	}
	
	public Element(String id, String name, String endpoint, List<Metadatum> metadata) {
		this.id = id;
		this.name = name;
		this.endpoint = endpoint;
		this.metadata = metadata;
		//systemicMetadata = new SystemicMetadata();
	}
	
	public Element(String id, String name, String endpoint, List<Metadatum> metadata, SystemicMetadata systemicMetadata) {
		this.id = id;
		this.name = name;
		this.endpoint = endpoint;
		if (metadata != null) {
			this.metadata = metadata;			
		} else {
			this.metadata = new ArrayList<>();
		}
		this.systemicMetadata = systemicMetadata;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	public ElementType getType() {
		return type;
	}
	
	public void setType(ElementType type) {
		this.type = type;
	}
	
	public List<Metadatum> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<Metadatum> metadata) {
		this.metadata = metadata;
	}
	
	public SystemicMetadata getSystemicMetadata() {
		return systemicMetadata;
	}

	public void setSystemicMetadata(SystemicMetadata systemicMetadata) {
		this.systemicMetadata = systemicMetadata;
	}

	/*@Override
	public String toString() {
		StringBuilder elementBuilder = new StringBuilder();
		if (this.id != null) {
			elementBuilder.append("ID: " + this.id);			
		}
		elementBuilder.append("\n");
		if (this.name != null) {
			elementBuilder.append("name: " + this.name);			
		}
		elementBuilder.append("\n");
		if (this.endpoint != null) {
			elementBuilder.append("endpoint: " + this.endpoint);			
		}
		elementBuilder.append("\n");
		if (this.metadata != null) {
			elementBuilder.append("metadata: [\n");
			for (Metadatum metadatum : this.metadata) {
				elementBuilder.append(metadatum.toString());
			}
			elementBuilder.append("]\n");
		}
		if (this.systemicMetadata != null) {
			if (this.systemicMetadata.getCreated() != null) {
				elementBuilder.append("created: " + this.systemicMetadata.getCreated().toString());
			}
			elementBuilder.append("\n");
			if (this.systemicMetadata.getModified() != null) {
				elementBuilder.append("modified: " + this.systemicMetadata.getModified().toString());
			}
			
		}
		elementBuilder.append("\n");
		return elementBuilder.toString();
	}*/
}