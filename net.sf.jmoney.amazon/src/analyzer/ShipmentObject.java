package analyzer;

public class ShipmentObject {

	/**
	 * When creating new (not previously in datastore), this is
	 * set when created.  When the order is already in the datastore
	 * we can match as soon as we match a single item in the shipment.
	 */
	public AmazonShipment shipment;

}
