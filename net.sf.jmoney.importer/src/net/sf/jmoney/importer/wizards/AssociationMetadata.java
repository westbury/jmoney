package net.sf.jmoney.importer.wizards;

public class AssociationMetadata {

	private String id;
	private String label;
	private String description;
	
	public AssociationMetadata(String id, String label) {
		this.id = id;
		this.label = label;
		this.description = null;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}
	
	
}
