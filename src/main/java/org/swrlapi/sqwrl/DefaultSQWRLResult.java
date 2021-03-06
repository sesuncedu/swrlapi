package org.swrlapi.sqwrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.swrlapi.sqwrl.exceptions.SQWRLException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidAggregateFunctionNameException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidColumnIndexException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidColumnNameException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidColumnTypeException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidQueryException;
import org.swrlapi.sqwrl.exceptions.SQWRLInvalidRowIndexException;
import org.swrlapi.sqwrl.exceptions.SQWRLResultStateException;
import org.swrlapi.sqwrl.values.SQWRLClassValue;
import org.swrlapi.sqwrl.values.SQWRLIndividualValue;
import org.swrlapi.sqwrl.values.SQWRLLiteralResultValue;
import org.swrlapi.sqwrl.values.SQWRLPropertyValue;
import org.swrlapi.sqwrl.values.SQWRLResultValue;
import org.swrlapi.sqwrl.values.SQWRLResultValueFactory;

/**
 * This class implements the interfaces {@link SQWRLResult} and {@link SQWRLResultGenerator}. It can be used to generate
 * a result structure and populate it with data; it can also be used to retrieve those data from the result.
 * <p>
 * This class operates in three phases:
 * <p>
 * (1) Configuration Phase: In this phase the structure of the result is defined. This phase opened by a call to the
 * {@link #configure} method (which will also clear any existing data). In this phase the columns are defined;
 * aggregation or ordering is also specified in this phase. This phase is closed by a call to the {@link #configured}
 * method.
 * <p>
 * (2) Preparation Phase: In this phase data are added to the result. This phase is implicitly opened by the call to the
 * {@link #configured} method. It is closed by a call to the {@link #prepared} method.
 * <p>
 * A convenience method {@link #addColumns} that takes a list of column names is also supplied.
 * <p>
 * There is also a convenience method {@link #addRow}, which takes a list of {@link SQWRResultValue} objects. This
 * method automatically does a row open and close. It is expecting the exact same number of list elements as there are
 * columns in the result.
 * <p>
 * The interface {@link SQWRLResultGenerator} defines the calls used in these two phases.
 * <p>
 * (3) Processing Phase: In this phase data may be retrieved from the result. This phase is implicitly opened by the
 * call to the {@link #closed} method.
 * <p>
 * The interface {@link SQWRLResult} defines the calls used in the processing phase.
 * <p>
 * An example configuration, data generation, and result retrieval is:
 * <p>
 * 
 * <pre>
 * DefaultSQWRLResult result = new DefaultSQWRLResult(&quot;TestResult&quot;);
 * 
 * result.addColumn(&quot;name&quot;);
 * result.addAggregateColumn(&quot;average&quot;, SQWRLResultNames.AvgAggregateFunction);
 * result.configured();
 * 
 * result.openRow();
 * result.addRowData(new SQWRLIndividualValue(&quot;Fred&quot;));
 * result.addRowData(new SQWRLLiteralValue(27));
 * result.closeRow();
 * result.openRow();
 * result.adRowdData(new SQWRLIndividualValue(&quot;Joe&quot;));
 * result.addRowData(new SQWRLLiteralValue(34));
 * result.closeRow();
 * result.openRow();
 * result.addRowData(new SQWRLIndividualValue(&quot;Joe&quot;));
 * result.addRowData(new SQWRLLiteralValue(21));
 * result.closeRow();
 * result.prepared();
 * </pre>
 * <p>
 * The result is now available for reading. The interface {@link SQWRLResult} defines the assessor methods. A row
 * consists of a list of objects defined by the interface {@link SQWRLResultValue}. There are four possible types of
 * values (1) {@link SQWRLLiteralValue}, representing literals; (2) {@link SQWRLIndividualValue}, representing OWL
 * individuals; (3) {@link SQWRLClassValue}, representing OWL classes; and (4) {@link SQWRLPropertyValue}, representing
 * OWL properties (object, data, and annotation).
 * <p>
 * 
 * <pre>
 * while (result.hasNext()) {
 * 	SQWRLIndividualValue nameValue = result.getIndividualValue(&quot;name&quot;);
 * 	SQWRLLiteralValue averageValue = result.getLiteralValue(&quot;average&quot;);
 * 	System.out.println(&quot;Name: &quot; + nameValue.getURI());
 * 	System.out.println(&quot;Average: &quot; + averageValue.getInt());
 * }
 * </pre>
 */
public class DefaultSQWRLResult implements SQWRLResult, SQWRLResultGenerator, Serializable
{
	private static final long serialVersionUID = -2945270777360073492L;

	private final SQWRLResultValueFactory sqwrlResultValueFactory;

	private final List<String> allColumnNames, columnDisplayNames;
	private final List<Integer> selectedColumnIndexes, orderByColumnIndexes;
	private final Map<Integer, String> aggregateColumnIndexes; // Map of (index, function) pairs
	private List<List<SQWRLResultValue>> rows; // List of List of SQWRLResultValue objects.
	private List<SQWRLResultValue> rowData; // List of SQWRLResultValue objects used when assembling a row.
	private Map<String, List<SQWRLResultValue>> columnValuesMap; // Column name -> List<SWRLAPILiteral>

	private int numberOfColumns, currentRowIndex, currentRowDataColumnIndex;
	private boolean isConfigured, isPrepared, isRowOpen, isOrdered, isAscending, isDistinct, hasAggregates;
	private int limit = -1, nth = -1, firstN = -1, lastN = -1, sliceSize = -1;
	private boolean notNthSelection = false, firstSelection = false, lastSelection = false, notFirstSelection = false,
			notLastSelection = false, nthSliceSelection = false, notNthSliceSelection = false, nthLastSliceSelection = false,
			notNthLastSliceSelection = false;

	public DefaultSQWRLResult(SQWRLResultValueFactory sqwrlResultValueFactory)
	{
		this.sqwrlResultValueFactory = sqwrlResultValueFactory;

		this.isConfigured = false;
		this.isPrepared = false;
		this.isRowOpen = false;

		// The following variables will not be externally valid until configured() is called.
		this.allColumnNames = new ArrayList<String>();
		this.aggregateColumnIndexes = new HashMap<Integer, String>();
		this.selectedColumnIndexes = new ArrayList<Integer>();
		this.orderByColumnIndexes = new ArrayList<Integer>();
		this.columnDisplayNames = new ArrayList<String>();

		this.numberOfColumns = 0;
		this.isOrdered = this.isAscending = this.isDistinct = false;

		// The following variables will not be externally valid until prepared() is called.
		this.currentRowIndex = -1; // If there are no rows in the final result, it will remain at -1.
		this.rows = new ArrayList<List<SQWRLResultValue>>();
	}

	// Configuration phase methods

	@Override
	public boolean isConfigured()
	{
		return this.isConfigured;
	}

	@Override
	public boolean isRowOpen()
	{
		return this.isRowOpen;
	}

	public boolean isDistinct()
	{
		return this.isDistinct;
	}

	@Override
	public boolean isPrepared()
	{
		return this.isPrepared;
	}

	@Override
	public boolean isOrdered()
	{
		return this.isOrdered;
	}

	@Override
	public boolean isAscending()
	{
		return this.isAscending;
	}

	@Override
	public void addColumns(List<String> columnNames) throws SQWRLException
	{
		for (String columnName : columnNames)
			addColumn(columnName);
	}

	@Override
	public void addColumn(String columnName) throws SQWRLException
	{
		throwExceptionIfAlreadyConfigured();

		this.selectedColumnIndexes.add(Integer.valueOf(this.numberOfColumns));
		this.allColumnNames.add(columnName);
		this.numberOfColumns++;
	}

	@Override
	public void addAggregateColumn(String columnName, String aggregateFunctionName) throws SQWRLException
	{
		throwExceptionIfAlreadyConfigured();

		SQWRLResultNames.checkAggregateFunctionName(aggregateFunctionName);

		this.aggregateColumnIndexes.put(Integer.valueOf(this.numberOfColumns), aggregateFunctionName);
		this.allColumnNames.add(columnName);
		this.numberOfColumns++;
	}

	@Override
	public void addOrderByColumn(int orderedColumnIndex, boolean ascending) throws SQWRLException
	{
		throwExceptionIfAlreadyConfigured();

		if (orderedColumnIndex < 0 || orderedColumnIndex >= this.allColumnNames.size())
			throw new SQWRLException("ordered column index " + orderedColumnIndex + " out of range");

		if (this.isOrdered && (this.isAscending != ascending)) {
			if (this.isAscending)
				throw new SQWRLException("attempt to order column " + this.allColumnNames.get(orderedColumnIndex)
						+ " ascending when descending was previously specified");
			else
				throw new SQWRLException("attempt to order column " + this.allColumnNames.get(orderedColumnIndex)
						+ " descending when ascending was previously specified");
		}

		this.isOrdered = true;
		this.isAscending = ascending;

		this.orderByColumnIndexes.add(Integer.valueOf(orderedColumnIndex));
	}

	@Override
	public void addColumnDisplayName(String columnName) throws SQWRLException
	{
		if (columnName.length() == 0 || columnName.indexOf(',') != -1)
			throw new SQWRLException("invalid column name " + columnName + " - no commas or empty names allowed");

		this.columnDisplayNames.add(columnName);
	}

	@Override
	public void configured() throws SQWRLException
	{
		throwExceptionIfAlreadyConfigured();

		// We will already have checked that all ordered columns are selected or aggregated

		if (containsOneOf(this.selectedColumnIndexes, this.aggregateColumnIndexes.keySet()))
			throw new SQWRLInvalidQueryException("aggregate columns cannot also be selected columns");

		this.hasAggregates = !this.aggregateColumnIndexes.isEmpty();

		this.isConfigured = true;
	}

	// Methods used to retrieve the result structure after the result has been configured

	@Override
	public void setIsDistinct()
	{
		this.isDistinct = true;
	}

	@Override
	public int getNumberOfColumns() throws SQWRLException
	{
		throwExceptionIfNotConfigured();

		return this.numberOfColumns;
	}

	@Override
	public List<String> getColumnNames() throws SQWRLException
	{
		List<String> result = new ArrayList<String>();

		throwExceptionIfNotConfigured();

		if (this.columnDisplayNames.size() < getNumberOfColumns()) {
			result.addAll(this.columnDisplayNames);
			result.addAll(this.allColumnNames.subList(this.columnDisplayNames.size(), this.allColumnNames.size()));
		} else
			result.addAll(this.columnDisplayNames);

		return Collections.unmodifiableList(result);
	}

	@Override
	public String getColumnName(int columnIndex) throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		checkColumnIndex(columnIndex);

		if (columnIndex < this.columnDisplayNames.size())
			return this.columnDisplayNames.get(columnIndex);
		else
			return this.allColumnNames.get(columnIndex);
	}

	// Methods used to add data after result has been configured

	@Override
	public void addRow(List<SQWRLResultValue> row) throws SQWRLException
	{
		if (row.size() != getNumberOfColumns())
			throw new SQWRLException("addRow expecting " + getNumberOfColumns() + ", got " + row.size() + " values");

		openRow();
		for (SQWRLResultValue value : row)
			addRowData(value);
		closeRow();
	}

	@Override
	public void openRow() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfAlreadyPrepared();
		throwExceptionIfRowOpen();

		this.currentRowDataColumnIndex = 0;
		this.rowData = new ArrayList<SQWRLResultValue>();
		this.isRowOpen = true;
	}

	@Override
	public void addRowData(SQWRLResultValue value) throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfAlreadyPrepared();
		throwExceptionIfRowNotOpen();

		if (this.currentRowDataColumnIndex == getNumberOfColumns())
			throw new SQWRLResultStateException("attempt to add data beyond the end of a row");

		if (this.aggregateColumnIndexes.containsKey(Integer.valueOf(this.currentRowDataColumnIndex))
				&& (!this.aggregateColumnIndexes.get(Integer.valueOf(this.currentRowDataColumnIndex)).equals(
						SQWRLResultNames.CountAggregateFunction))
				&& (!this.aggregateColumnIndexes.get(Integer.valueOf(this.currentRowDataColumnIndex)).equals(
						SQWRLResultNames.CountDistinctAggregateFunction)) && (!isNumericValue(value)))
			throw new SQWRLException("attempt to add non numeric value " + value
					+ " to min, max, sum, or avg aggregate column " + this.allColumnNames.get(this.currentRowDataColumnIndex));
		this.rowData.add(value);
		this.currentRowDataColumnIndex++;

		if (this.currentRowDataColumnIndex == getNumberOfColumns())
			closeRow(); // Automatically close the row
	}

	@Override
	public void closeRow() throws SQWRLException
	{ // Will ignore if row is already closed, assuming it was automatically closed in addRowData
		throwExceptionIfNotConfigured();
		throwExceptionIfAlreadyPrepared();

		if (this.isRowOpen)
			this.rows.add(this.rowData);

		this.isRowOpen = false;
	}

	@Override
	public void prepared() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfAlreadyPrepared();

		if (this.currentRowDataColumnIndex != 0)
			throwExceptionIfRowOpen(); // We allow prepared() with an open row if no data have been added.

		this.isPrepared = true;
		this.isRowOpen = false;
		this.currentRowDataColumnIndex = 0;
		if (getNumberOfRows() > 0)
			this.currentRowIndex = 0;
		else
			this.currentRowIndex = -1;

		if (this.hasAggregates)
			this.rows = aggregate(this.rows); // Aggregation implies killing duplicate rows
		else if (this.isDistinct)
			this.rows = distinct(this.rows);

		if (this.isOrdered)
			this.rows = orderBy(this.rows, this.isAscending);

		this.rows = processSelectionOperators(this.rows);

		prepareColumnVectors();
	}

	// Methods used to retrieve data after result has been prepared

	@Override
	public int getNumberOfRows() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();

		return this.rows.size();
	}

	@Override
	public boolean isEmpty() throws SQWRLException
	{
		return getNumberOfRows() == 0;
	}

	@Override
	public void reset() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();

		if (getNumberOfRows() > 0)
			this.currentRowIndex = 0;
	}

	@Override
	public void next() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();
		throwExceptionIfAtEndOfResult();

		if (this.currentRowIndex != -1 && this.currentRowIndex < getNumberOfRows())
			this.currentRowIndex++;
	}

	@Override
	public boolean hasNext() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();

		return (this.currentRowIndex != -1 && this.currentRowIndex < getNumberOfRows());
	}

	@Override
	public List<SQWRLResultValue> getRow() throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();
		throwExceptionIfAtEndOfResult();

		return this.rows.get(this.currentRowIndex);
	}

	@Override
	public SQWRLResultValue getValue(String columnName) throws SQWRLException
	{
		List<SQWRLResultValue> row;
		int columnIndex;

		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();
		throwExceptionIfAtEndOfResult();

		checkColumnName(columnName);

		columnIndex = this.allColumnNames.indexOf(columnName);

		row = this.rows.get(this.currentRowIndex);

		return row.get(columnIndex);
	}

	@Override
	public SQWRLResultValue getValue(int columnIndex) throws SQWRLException
	{
		List<SQWRLResultValue> row;

		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();
		throwExceptionIfAtEndOfResult();

		checkColumnIndex(columnIndex);

		row = this.rows.get(this.currentRowIndex);

		return row.get(columnIndex);
	}

	@Override
	public SQWRLResultValue getValue(int columnIndex, int rowIndex) throws SQWRLException
	{

		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();

		checkColumnIndex(columnIndex);
		checkRowIndex(rowIndex);

		return this.rows.get(rowIndex).get(columnIndex);
	}

	@Override
	public SQWRLIndividualValue getObjectValue(String columnName) throws SQWRLException
	{
		if (!hasObjectValue(columnName))
			throw new SQWRLInvalidColumnTypeException("expecting ObjectValue type for column " + columnName);
		return (SQWRLIndividualValue)getValue(columnName);
	}

	@Override
	public SQWRLIndividualValue getObjectValue(int columnIndex) throws SQWRLException
	{
		return getObjectValue(getColumnName(columnIndex));
	}

	@Override
	public SQWRLResultValue getLiteralValue(String columnName) throws SQWRLException
	{
		if (!hasLiteralValue(columnName))
			throw new SQWRLInvalidColumnTypeException("expecting LiteralValue type for column " + columnName);
		return getValue(columnName);
	}

	@Override
	public SQWRLClassValue getClassValue(String columnName) throws SQWRLException
	{
		if (!hasClassValue(columnName))
			throw new SQWRLInvalidColumnTypeException("expecting ClassValue type for column " + columnName);
		return (SQWRLClassValue)getValue(columnName);
	}

	@Override
	public SQWRLClassValue getClassValue(int columnIndex) throws SQWRLException
	{
		return getClassValue(getColumnName(columnIndex));
	}

	@Override
	public SQWRLPropertyValue getPropertyValue(int columnIndex) throws SQWRLException
	{
		return getPropertyValue(getColumnName(columnIndex));
	}

	@Override
	public SQWRLPropertyValue getPropertyValue(String columnName) throws SQWRLException
	{
		if (!hasPropertyValue(columnName))
			throw new SQWRLInvalidColumnTypeException("expecting PropertyValue type for column " + columnName);
		return (SQWRLPropertyValue)getValue(columnName);
	}

	@Override
	public SQWRLResultValue getLiteralValue(int columnIndex) throws SQWRLException
	{
		return getLiteralValue(getColumnName(columnIndex));
	}

	@Override
	public List<SQWRLResultValue> getColumn(String columnName) throws SQWRLException
	{
		throwExceptionIfNotConfigured();
		throwExceptionIfNotPrepared();

		checkColumnName(columnName);

		return this.columnValuesMap.get(columnName);
	}

	@Override
	public List<SQWRLResultValue> getColumn(int columnIndex) throws SQWRLException
	{
		return getColumn(getColumnName(columnIndex));
	}

	@Override
	public boolean hasObjectValue(String columnName) throws SQWRLException
	{
		return getValue(columnName) instanceof SQWRLIndividualValue;
	}

	@Override
	public boolean hasObjectValue(int columnIndex) throws SQWRLException
	{
		return getValue(columnIndex) instanceof SQWRLIndividualValue;
	}

	@Override
	public boolean hasLiteralValue(String columnName) throws SQWRLException
	{
		return getValue(columnName) instanceof SQWRLResultValue;
	}

	@Override
	public boolean hasLiteralValue(int columnIndex) throws SQWRLException
	{
		return getValue(columnIndex) instanceof SQWRLResultValue;
	}

	@Override
	public boolean hasClassValue(String columnName) throws SQWRLException
	{
		return getValue(columnName) instanceof SQWRLClassValue;
	}

	@Override
	public boolean hasClassValue(int columnIndex) throws SQWRLException
	{
		return getValue(columnIndex) instanceof SQWRLClassValue;
	}

	@Override
	public boolean hasPropertyValue(String columnName) throws SQWRLException
	{
		return getValue(columnName) instanceof SQWRLPropertyValue;
	}

	@Override
	public boolean hasPropertyValue(int columnIndex) throws SQWRLException
	{
		return getValue(columnIndex) instanceof SQWRLPropertyValue;
	}

	// nth, firstN, etc. are 1-indexed
	private List<List<SQWRLResultValue>> processSelectionOperators(List<List<SQWRLResultValue>> sourceRows)
			throws SQWRLException
	{
		List<List<SQWRLResultValue>> processedRows = new ArrayList<List<SQWRLResultValue>>();
		boolean hasSelection = false;

		if (hasLimit()) {
			int localLimit = this.limit > sourceRows.size() ? sourceRows.size() : this.limit;
			if (this.limit < 0)
				this.limit = 0;
			processedRows.addAll(sourceRows.subList(0, localLimit));
			hasSelection = true;
		} else {
			if (hasNth()) {
				if (this.nth < 1)
					this.nth = 1;
				if (this.nth <= sourceRows.size())
					processedRows.add(sourceRows.get(this.nth - 1));
				hasSelection = true;
			}

			if (hasNotNth()) {
				if (this.nth < 1)
					this.nth = 1;
				if (this.nth <= sourceRows.size()) {
					List<List<SQWRLResultValue>> localRows = new ArrayList<List<SQWRLResultValue>>(sourceRows);
					localRows.remove(this.nth - 1);
					processedRows.addAll(localRows);
				} else
					processedRows.addAll(sourceRows); // Add everything
				hasSelection = true;
			}

			if (hasFirstSelection()) {
				if (this.firstN < 1)
					this.firstN = 1;
				if (this.firstN <= sourceRows.size())
					processedRows.addAll(sourceRows.subList(0, this.firstN));
				hasSelection = true;
			}

			if (hasNotFirstSelection()) {
				if (this.firstN < 1)
					this.firstN = 1;
				if (this.firstN <= sourceRows.size())
					processedRows.addAll(sourceRows.subList(this.firstN, sourceRows.size()));
				else
					processedRows.addAll(sourceRows); // Add everything
				hasSelection = true;
			}

			if (hasLastSelection()) {
				if (this.lastN < 1)
					this.lastN = 1;
				if (this.lastN <= sourceRows.size())
					processedRows.addAll(sourceRows.subList(sourceRows.size() - this.lastN, sourceRows.size()));
				hasSelection = true;
			}

			if (hasNotLastSelection()) {
				if (this.lastN < 1)
					this.lastN = 1;
				if (this.lastN <= sourceRows.size())
					processedRows.addAll(sourceRows.subList(0, sourceRows.size() - this.lastN));
				else
					processedRows.addAll(sourceRows); // Add everything
				hasSelection = true;
			}

			if (hasNthSliceSelection()) {
				if (this.firstN < 1)
					this.firstN = 1;
				if (this.firstN <= sourceRows.size()) {
					int finish = (this.firstN + this.sliceSize > sourceRows.size()) ? sourceRows.size() : this.firstN
							+ this.sliceSize - 1;
					processedRows.addAll(sourceRows.subList(this.firstN - 1, finish));
				}
				hasSelection = true;
			}

			if (hasNotNthSliceSelection()) {
				if (this.firstN < 1)
					this.firstN = 1;
				if (this.firstN <= sourceRows.size()) {
					int finish = (this.firstN + this.sliceSize > sourceRows.size()) ? sourceRows.size() : this.firstN
							+ this.sliceSize - 1;
					processedRows.addAll(sourceRows.subList(0, this.firstN - 1));
					if (finish <= sourceRows.size())
						processedRows.addAll(sourceRows.subList(finish, sourceRows.size()));
				} else
					processedRows.addAll(sourceRows); // Add everything
				hasSelection = true;
			}

			if (hasNthLastSliceSelection()) {
				if (this.lastN < 1)
					this.lastN = 1;
				int finish = (this.lastN + this.sliceSize > sourceRows.size()) ? sourceRows.size() : this.lastN
						+ this.sliceSize;
				if (this.lastN <= sourceRows.size()) {
					processedRows.addAll(sourceRows.subList(this.lastN, finish));
				}
				hasSelection = true;
			}

			if (hasNotNthLastSliceSelection()) {
				if (this.lastN <= sourceRows.size()) {
					if (this.lastN < 1)
						this.lastN = 1;
					int finish = (this.lastN + this.sliceSize > sourceRows.size()) ? sourceRows.size() : this.lastN
							+ this.sliceSize;
					processedRows.addAll(sourceRows.subList(0, this.lastN));
					if (finish <= sourceRows.size())
						processedRows.addAll(sourceRows.subList(finish, sourceRows.size()));
				} else
					processedRows.addAll(sourceRows); // Add everything
				hasSelection = true;
			}
		}

		if (hasSelection)
			return processedRows;
		else
			return sourceRows;
	}

	// nth, firstN, etc. are 1-indexed
	public void setLimit(int limit)
	{
		this.limit = limit;
	}

	public void setNth(int nth)
	{
		this.nth = nth;
	}

	public void setNotNth(int nth)
	{
		this.notNthSelection = true;
		this.nth = nth;
	}

	public void setFirst()
	{
		this.firstSelection = true;
		this.firstN = 1;
	}

	public void setFirst(int n)
	{
		this.firstSelection = true;
		this.firstN = n;
	}

	public void setLast()
	{
		this.lastSelection = true;
		this.lastN = 1;
	}

	public void setLast(int n)
	{
		this.lastSelection = true;
		this.lastN = n;
	}

	public void setNotFirst()
	{
		this.notFirstSelection = true;
		this.firstN = 1;
	}

	public void setNotFirst(int n)
	{
		this.notFirstSelection = true;
		this.firstN = n;
	}

	public void setNotLast()
	{
		this.notLastSelection = true;
		this.lastN = 1;
	}

	public void setNotLast(int n)
	{
		this.notLastSelection = true;
		this.lastN = n;
	}

	public void setNthSlice(int n, int sliceSize)
	{
		this.nthSliceSelection = true;
		this.firstN = n;
		this.sliceSize = sliceSize;
	}

	public void setNotNthSlice(int n, int sliceSize)
	{
		this.notNthSliceSelection = true;
		this.firstN = n;
		this.sliceSize = sliceSize;
	}

	public void setNthLastSlice(int n, int sliceSize)
	{
		this.nthLastSliceSelection = true;
		this.lastN = n;
		this.sliceSize = sliceSize;
	}

	public void setNotNthLastSlice(int n, int sliceSize)
	{
		this.notNthLastSliceSelection = true;
		this.lastN = n;
		this.sliceSize = sliceSize;
	}

	private boolean hasLimit()
	{
		return this.limit != -1;
	}

	private boolean hasNth()
	{
		return !hasNotNth() && this.nth != -1;
	}

	private boolean hasNotNth()
	{
		return this.notNthSelection;
	}

	private boolean hasFirstSelection()
	{
		return this.firstSelection;
	}

	private boolean hasLastSelection()
	{
		return this.lastSelection;
	}

	private boolean hasNotFirstSelection()
	{
		return this.notFirstSelection;
	}

	private boolean hasNotLastSelection()
	{
		return this.notLastSelection;
	}

	private boolean hasNthSliceSelection()
	{
		return this.nthSliceSelection;
	}

	private boolean hasNotNthSliceSelection()
	{
		return this.notNthSliceSelection;
	}

	private boolean hasNthLastSliceSelection()
	{
		return this.nthLastSliceSelection;
	}

	private boolean hasNotNthLastSliceSelection()
	{
		return this.notNthLastSliceSelection;
	}

	private void prepareColumnVectors() throws SQWRLException
	{
		this.columnValuesMap = new HashMap<String, List<SQWRLResultValue>>();

		if (getNumberOfColumns() > 0) {
			List<List<SQWRLResultValue>> columns = new ArrayList<List<SQWRLResultValue>>(getNumberOfColumns());

			for (int c = 0; c < getNumberOfColumns(); c++)
				columns.add(new ArrayList<SQWRLResultValue>(getNumberOfRows()));

			for (int r = 0; r < getNumberOfRows(); r++)
				for (int c = 0; c < getNumberOfColumns(); c++)
					columns.get(c).add(this.rows.get(r).get(c));

			for (int c = 0; c < getNumberOfColumns(); c++)
				this.columnValuesMap.put(getColumnName(c), columns.get(c));
		}
	}

	@Override
	public String toString()
	{
		String result = "[numberOfColumns: " + this.numberOfColumns + ", isConfigured: " + this.isConfigured
				+ ", isPrepared: " + this.isPrepared + ", isRowOpen: " + this.isRowOpen + ", isOrdered: " + this.isOrdered
				+ ", isAscending " + this.isAscending + ", isDistinct: " + this.isDistinct + ", hasAggregates: "
				+ this.hasAggregates + "]\n";

		result += "[columnDisplayNames: ";
		for (String columnDisplayName : this.columnDisplayNames)
			result += "" + columnDisplayName + "";
		result += "]\n";

		for (List<SQWRLResultValue> row : this.rows) {
			for (SQWRLResultValue value : row) {
				result += "" + value + " ";
			}
			result += "\n";
		}

		result += "--------------------------------------------------------------------------------\n";

		return result;
	}

	// Phase verification exception throwing methods

	private void throwExceptionIfNotConfigured() throws SQWRLException
	{
		if (!isConfigured())
			throw new SQWRLResultStateException("attempt to add data to unconfigured result");
	}

	private void throwExceptionIfAtEndOfResult() throws SQWRLException
	{
		if (!hasNext())
			throw new SQWRLResultStateException("attempt to get data after end of result reached");
	}

	private void throwExceptionIfNotPrepared() throws SQWRLException
	{
		if (!isPrepared())
			throw new SQWRLResultStateException("attempt to process unprepared result");
	}

	private void throwExceptionIfAlreadyConfigured() throws SQWRLException
	{
		if (isConfigured())
			throw new SQWRLResultStateException("attempt to configure already configured result");
	}

	private void throwExceptionIfAlreadyPrepared() throws SQWRLException
	{
		if (isPrepared())
			throw new SQWRLResultStateException("attempt to modify prepared result");
	}

	private void checkColumnName(String columnName) throws SQWRLInvalidColumnNameException
	{
		if (!this.allColumnNames.contains(columnName))
			throw new SQWRLInvalidColumnNameException("invalid column name " + columnName);
	}

	private void throwExceptionIfRowNotOpen() throws SQWRLException
	{
		if (!this.isRowOpen)
			throw new SQWRLResultStateException("attempt to add data to an unopened row");
	} // throwExceptionIfRowNotOpen

	private void throwExceptionIfRowOpen() throws SQWRLException
	{
		if (this.isRowOpen)
			throw new SQWRLResultStateException("attempt to process result with a partially prepared row");
	}

	private void checkColumnIndex(int columnIndex) throws SQWRLException
	{
		if (columnIndex < 0 || columnIndex >= getNumberOfColumns())
			throw new SQWRLInvalidColumnIndexException("column index " + columnIndex + " out of bounds");
	}

	private void checkRowIndex(int rowIndex) throws SQWRLException
	{
		if (rowIndex < 0 || rowIndex >= getNumberOfRows())
			throw new SQWRLInvalidRowIndexException("row index " + rowIndex + " out of bounds");
	}

	private boolean containsOneOf(List<Integer> collection1, Set<Integer> collection2)
	{
		for (Integer i : collection2)
			if (collection1.contains(i))
				return true;

		return false;
	}

	private boolean isNumericValue(SQWRLResultValue value)
	{
		return ((value instanceof SQWRLLiteralResultValue) && (((SQWRLLiteralResultValue)value).isNumeric()));
	}

	// TODO: fix - very inefficient
	private List<List<SQWRLResultValue>> distinct(List<List<SQWRLResultValue>> sourceRows)
	{
		List<List<SQWRLResultValue>> localRows = new ArrayList<List<SQWRLResultValue>>(sourceRows);
		List<List<SQWRLResultValue>> processedRows = new ArrayList<List<SQWRLResultValue>>();
		RowComparator rowComparator = new RowComparator(this.allColumnNames, true); // Look at the entire row.

		Collections.sort(localRows, rowComparator); // binary search is expecting a sorted list
		for (List<SQWRLResultValue> row : localRows)
			if (Collections.binarySearch(processedRows, row, rowComparator) < 0)
				processedRows.add(row);

		return processedRows;
	}

	private List<List<SQWRLResultValue>> aggregate(List<List<SQWRLResultValue>> sourceRows) throws SQWRLException
	{
		List<List<SQWRLResultValue>> result = new ArrayList<List<SQWRLResultValue>>();
		RowComparator rowComparator = new RowComparator(this.allColumnNames, this.selectedColumnIndexes, true);
		// Key is index of aggregated row in result, value is hash map of aggregate column index to list of original values.
		HashMap<Integer, HashMap<Integer, List<SQWRLResultValue>>> aggregatesMap = new HashMap<Integer, HashMap<Integer, List<SQWRLResultValue>>>();
		HashMap<Integer, List<SQWRLResultValue>> aggregateRowMap; // Map of column indexes to value lists; used to
																															// accumulate
		// values for aggregation.
		List<SQWRLResultValue> values;
		SQWRLResultValue value;
		int rowIndex;

		for (List<SQWRLResultValue> row : sourceRows) {
			rowIndex = findRowIndex(result, row, rowComparator); // Find a row with the same values for non aggregated
																														// columns.

			if (rowIndex < 0) { // Row with same values for non aggregated columns not yet present in result.
				aggregateRowMap = new HashMap<Integer, List<SQWRLResultValue>>();
				// Find value for each aggregated column in row and add each to map indexed by result row
				for (Integer aggregateColumnIndex : this.aggregateColumnIndexes.keySet()) {
					values = new ArrayList<SQWRLResultValue>();
					value = row.get(aggregateColumnIndex.intValue());
					values.add(value);
					aggregateRowMap.put(aggregateColumnIndex, values);
				}
				aggregatesMap.put(Integer.valueOf(result.size()), aggregateRowMap); //
				result.add(row);
			} else { // We found a row that has the same values for the non aggregated columns.
				aggregateRowMap = aggregatesMap.get(Integer.valueOf(rowIndex)); // Find the aggregate map
				for (Integer aggregateColumnIndex : this.aggregateColumnIndexes.keySet()) {
					value = row.get(aggregateColumnIndex.intValue()); // Find value
					values = aggregateRowMap.get(aggregateColumnIndex); // Find row map
					values.add(value); // Add value
				}
			}
		}

		rowIndex = 0;
		for (List<SQWRLResultValue> row : result) {
			aggregateRowMap = aggregatesMap.get(Integer.valueOf(rowIndex));

			for (Integer aggregateColumnIndex : this.aggregateColumnIndexes.keySet()) {
				String aggregateFunctionName = this.aggregateColumnIndexes.get(aggregateColumnIndex);
				values = aggregateRowMap.get(aggregateColumnIndex);

				// We have checked in addRowData that only numeric data are added for sum, max, min, and avg
				if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.MinAggregateFunction))
					value = min(values);
				else if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.MaxAggregateFunction))
					value = max(values);
				else if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.SumAggregateFunction))
					value = sum(values);
				else if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.AvgAggregateFunction))
					value = avg(values);
				else if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.CountAggregateFunction))
					value = count(values);
				else if (aggregateFunctionName.equalsIgnoreCase(SQWRLResultNames.CountDistinctAggregateFunction))
					value = countDistinct(values);
				else
					throw new SQWRLInvalidAggregateFunctionNameException("invalid aggregate function " + aggregateFunctionName);

				row.set(aggregateColumnIndex.intValue(), value);
			}
			rowIndex++;
		}

		return result;
	}

	private List<List<SQWRLResultValue>> orderBy(List<List<SQWRLResultValue>> sourceRows, boolean ascending)
			throws SQWRLException
	{
		List<List<SQWRLResultValue>> result = new ArrayList<List<SQWRLResultValue>>(sourceRows);
		RowComparator rowComparator = new RowComparator(this.allColumnNames, this.orderByColumnIndexes, ascending);

		Collections.sort(result, rowComparator);

		return result;
	}

	private SQWRLResultValue min(List<SQWRLResultValue> values) throws SQWRLException
	{
		SQWRLResultValue result = null, value;

		if (values.isEmpty())
			throw new SQWRLException("empty aggregate list for " + SQWRLResultNames.MinAggregateFunction);

		for (SQWRLResultValue SWRLAPILiteral : values) {

			if (!(SWRLAPILiteral instanceof SQWRLResultValue))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.MinAggregateFunction
						+ " aggregate on non datatype " + SWRLAPILiteral);

			value = SWRLAPILiteral;

			if (!isNumericValue(value))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.MinAggregateFunction
						+ " aggregate on non numeric datatype " + value);

			if (result == null)
				result = value;
			else if (value.compareTo(result) < 0)
				result = value;
		}

		return result;
	}

	private SQWRLResultValue max(List<SQWRLResultValue> values) throws SQWRLException
	{
		SQWRLResultValue result = null, value;

		if (values.isEmpty())
			throw new SQWRLException("empty aggregate list for " + SQWRLResultNames.MaxAggregateFunction);

		for (SQWRLResultValue SWRLAPILiteral : values) {

			if (!(SWRLAPILiteral instanceof SQWRLResultValue))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.MaxAggregateFunction
						+ " aggregate on non datatype " + SWRLAPILiteral);

			value = SWRLAPILiteral;

			if (!isNumericValue(value))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.MaxAggregateFunction
						+ " aggregate on non numeric datatype " + value);

			if (result == null)
				result = value;
			else if (value.compareTo(result) > 0)
				result = value;
		}

		return result;
	}

	private SQWRLResultValue sum(List<SQWRLResultValue> values) throws SQWRLException
	{
		double sum = 0;

		if (values.isEmpty())
			throw new SQWRLException("empty aggregate list for " + SQWRLResultNames.SumAggregateFunction);

		for (SQWRLResultValue value : values) {

			if (!isNumericValue(value))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.SumAggregateFunction
						+ " aggregate on non numeric value: " + value);

			double d = ((SQWRLLiteralResultValue)value).getDouble();

			sum = sum + d;
		}

		return getSQWRLResultValueFactory().getLiteral(sum);
	}

	private SQWRLResultValue avg(List<SQWRLResultValue> values) throws SQWRLException
	{
		double sum = 0;
		int count = 0;

		if (values.isEmpty())
			throw new SQWRLException("empty aggregate list for function " + SQWRLResultNames.AvgAggregateFunction);

		for (SQWRLResultValue value : values) {

			if (!isNumericValue(value))
				throw new SQWRLException("attempt to use " + SQWRLResultNames.AvgAggregateFunction
						+ " aggregate on non literal value " + value);

			double d = ((SQWRLLiteralResultValue)value).getDouble();

			count++;
			sum = sum + d;
		}

		return getSQWRLResultValueFactory().getLiteral(sum / count);
	}

	private SQWRLResultValue count(List<SQWRLResultValue> values) throws SQWRLException
	{
		return getSQWRLResultValueFactory().getLiteral(values.size());
	}

	private SQWRLResultValue countDistinct(List<SQWRLResultValue> values) throws SQWRLException
	{
		Set<SQWRLResultValue> distinctValues = new HashSet<SQWRLResultValue>(values);

		return getSQWRLResultValueFactory().getLiteral(distinctValues.size());
	}

	// TODO: linear search is not very efficient.
	private int findRowIndex(List<List<SQWRLResultValue>> result, List<SQWRLResultValue> rowToFind,
			Comparator<List<SQWRLResultValue>> rowComparator)
	{
		int rowIndex = 0;

		for (List<SQWRLResultValue> row : result) {
			if (rowComparator.compare(rowToFind, row) == 0)
				return rowIndex;
			rowIndex++;
		}

		return -1;
	}

	// TODO Look at. This is quick and dirty - all checking left to the Java runtime.
	private static class RowComparator implements Comparator<List<SQWRLResultValue>>
	{
		private final List<Integer> orderByColumnIndexes;
		private final boolean ascending;

		// Need way to distinguish two constructors because of erasure
		public RowComparator(List<String> allColumnNames, List<Integer> orderByColumnIndexes, boolean ascending)
		{
			this.ascending = ascending;
			this.orderByColumnIndexes = orderByColumnIndexes;
		}

		public RowComparator(List<String> allColumnNames, boolean ascending)
		{
			this.ascending = ascending;
			this.orderByColumnIndexes = new ArrayList<Integer>();

			for (String columnName : allColumnNames)
				this.orderByColumnIndexes.add(allColumnNames.indexOf(columnName));
		}

		@Override
		public int compare(List<SQWRLResultValue> row1, List<SQWRLResultValue> row2)
		{
			for (Integer columnIndex : this.orderByColumnIndexes) {
				int result = row1.get(columnIndex).compareTo(row2.get(columnIndex));
				if (result != 0)
					if (this.ascending)
						return result;
					else
						return -result;
			}
			return 0;
		}
	}

	private SQWRLResultValueFactory getSQWRLResultValueFactory()
	{
		return this.sqwrlResultValueFactory;
	}
}
