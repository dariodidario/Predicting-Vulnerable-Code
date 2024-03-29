package com.metaweb.gridworks.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.changes.ColumnRemovalChange;

public class ColumnRemovalOperation extends AbstractOperation {
	private static final long serialVersionUID = 8422079695048733734L;
	
	final protected String _columnName;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new ColumnRemovalOperation(
            obj.getString("columnName")
        );
    }
    
	public ColumnRemovalOperation(
		String columnName
	) {
		_columnName = columnName;
	}
	
   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Remove column " + _columnName);
       writer.key("columnName"); writer.value(_columnName);
       writer.endObject();
    }


	protected String getBriefDescription() {
		return "Remove column " + _columnName;
	}

	protected HistoryEntry createHistoryEntry(Project project) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);
		if (column == null) {
			throw new Exception("No column named " + _columnName);
		}
		
		String description = "Remove column " + column.getHeaderLabel();
		
		Change change = new ColumnRemovalChange(project.columnModel.columns.indexOf(column));
		
		return new HistoryEntry(project, description, ColumnRemovalOperation.this, change);
	}
}
