package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.operations.ReconOperation;

public class ReconcileCommand extends EngineDependentCommand {
	
	@Override
	protected AbstractOperation createOperation(HttpServletRequest request,
			JSONObject engineConfig) throws Exception {
		
		String columnName = request.getParameter("columnName");
		String typeID = request.getParameter("typeID");
		String typeName = request.getParameter("typeName");
        boolean autoMatch = "true".equals(request.getParameter("autoMatch"));
        double minScore = autoMatch ? Double.parseDouble(request.getParameter("minScore")) : 0;
		
		return new ReconOperation(engineConfig, columnName, typeID, typeName, autoMatch, minScore);
	}
}
