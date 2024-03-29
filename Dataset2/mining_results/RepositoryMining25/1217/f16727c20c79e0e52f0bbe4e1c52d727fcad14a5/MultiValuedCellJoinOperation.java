package com.metaweb.gridworks.operations;

import java.util.ArrayList;  
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.MassRowChange;

public class MultiValuedCellJoinOperation extends AbstractOperation {
    private static final long serialVersionUID = 3134524625206033285L;
    
    final protected String	_columnName;
    final protected String	_keyColumnName;
    final protected String  _separator;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new MultiValuedCellJoinOperation(
            obj.getString("columnName"),
            obj.getString("keyColumnName"),
            obj.getString("separator")
        );
    }
    
	public MultiValuedCellJoinOperation(
		String	  columnName,
		String	  keyColumnName,
		String    separator
	) {
		_columnName = columnName;
		_keyColumnName = keyColumnName;
		_separator = separator;
	}

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("keyColumnName"); writer.value(_keyColumnName);
        writer.key("separator"); writer.value(_separator);
        writer.endObject();
    }
    
	protected String getBriefDescription() {
		return "Join multi-valued cells in column " + _columnName;
	}

	protected HistoryEntry createHistoryEntry(Project project) throws Exception {
		Column column = project.columnModel.getColumnByName(_columnName);
		if (column == null) {
			throw new Exception("No column named " + _columnName);
		}
		int cellIndex = column.getCellIndex();
		
		Column keyColumn = project.columnModel.getColumnByName(_keyColumnName);
		if (column == null) {
			throw new Exception("No key column named " + _keyColumnName);
		}
		int keyCellIndex = keyColumn.getCellIndex();
		
		List<Row> newRows = new ArrayList<Row>();
		
		int oldRowCount = project.rows.size();
		for (int r = 0; r < oldRowCount; r++) {
		    Row oldRow = project.rows.get(r);
		    
		    if (oldRow.isCellBlank(keyCellIndex)) {
                newRows.add(oldRow.dup());
                continue;
		    }
		    
		    int r2 = r + 1;
		    while (r2 < oldRowCount && project.rows.get(r2).isCellBlank(keyCellIndex)) {
		        r2++;
		    }
		    
		    if (r2 == r + 1) {
                newRows.add(oldRow.dup());
                continue;
		    }
		    
		    StringBuffer sb = new StringBuffer();
		    for (int r3 = r; r3 < r2; r3++) {
		        Object value = project.rows.get(r3).getCellValue(cellIndex);
		        if (ExpressionUtils.isNonBlankData(value)) {
		            if (sb.length() > 0) {
		                sb.append(_separator);
		            }
		            sb.append(value.toString());
		        }
		    }
		    
		    for (int r3 = r; r3 < r2; r3++) {
		        Row newRow = project.rows.get(r3).dup();
		        if (r3 == r) {
		            newRow.setCell(cellIndex, new Cell(sb.toString(), null));
		        } else {
		            newRow.setCell(cellIndex, null);
		        }
		        
		        if (!newRow.isEmpty()) {
		            newRows.add(newRow);
		        }
		    }
		    
		    r = r2 - 1; // r will be incremented by the for loop anyway
		}
		
		return new HistoryEntry(
		    project, 
		    getBriefDescription(), 
		    this, 
		    new MassRowChange(newRows)
		);
	}

}
