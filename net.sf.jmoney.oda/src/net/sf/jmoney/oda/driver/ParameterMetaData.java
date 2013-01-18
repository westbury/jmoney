package net.sf.jmoney.oda.driver;

import java.util.Vector;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class ParameterMetaData implements IParameterMetaData {

	Vector<Parameter> parameters = new Vector<Parameter>();
	
	public ParameterMetaData(IFetcher fetcher) {
		fetcher.buildParameterList(parameters);
	}

	public int getParameterCount() throws OdaException {
		return parameters.size();
	}

	public int getParameterMode(int parameterNumber) throws OdaException {
		// All parameters are input only parameters
		return parameterModeIn;
	}

	public String getParameterName(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getName();
	}

	public int getParameterType(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getNativeType();
	}

	public String getParameterTypeName(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getNativeTypeName();
	}

	public int getPrecision(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getPrecision();
	}

	public int getScale(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).getColumnType().getScale();
	}

	public int isNullable(int parameterNumber) throws OdaException {
		return parameters.get(parameterNumber-1).isNullable() ? parameterNullable : parameterNoNulls;
	}
}
