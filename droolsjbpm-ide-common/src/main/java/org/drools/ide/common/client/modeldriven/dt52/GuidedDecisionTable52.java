/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.ide.common.client.modeldriven.dt52;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;
import org.drools.ide.common.client.modeldriven.brl.BaseSingleFieldConstraint;
import org.drools.ide.common.client.modeldriven.brl.DSLSentence;
import org.drools.ide.common.client.modeldriven.brl.FactPattern;
import org.drools.ide.common.client.modeldriven.brl.IAction;
import org.drools.ide.common.client.modeldriven.brl.IPattern;
import org.drools.ide.common.client.modeldriven.brl.PortableObject;

/**
 * This is a decision table model for a guided editor. It is not template or XLS
 * based. (template could be done relatively easily by taking a template, as a
 * String, and then String[][] data and driving the SheetListener interface in
 * the decision tables module). This works by taking the column definitions, and
 * combining them with the table of data to produce rule models.
 */
public class GuidedDecisionTable52
    implements
    PortableObject {

    private static final long                            serialVersionUID      = 510l;

    /**
     * Number of internal elements before ( used for offsets in serialization )
     */
    public static final int                              INTERNAL_ELEMENTS     = 2;

    /**
     * Various attribute names
     */
    public static final String                           SALIENCE_ATTR         = "salience";
    public static final String                           ENABLED_ATTR          = "enabled";
    public static final String                           DATE_EFFECTIVE_ATTR   = "date-effective";
    public static final String                           DATE_EXPIRES_ATTR     = "date-expires";
    public static final String                           NO_LOOP_ATTR          = "no-loop";
    public static final String                           AGENDA_GROUP_ATTR     = "agenda-group";
    public static final String                           ACTIVATION_GROUP_ATTR = "activation-group";
    public static final String                           DURATION_ATTR         = "duration";
    public static final String                           TIMER_ATTR            = "timer";
    public static final String                           CALENDARS_ATTR        = "calendars";
    public static final String                           AUTO_FOCUS_ATTR       = "auto-focus";
    public static final String                           LOCK_ON_ACTIVE_ATTR   = "lock-on-active";
    public static final String                           RULEFLOW_GROUP_ATTR   = "ruleflow-group";
    public static final String                           DIALECT_ATTR          = "dialect";
    public static final String                           NEGATE_RULE_ATTR      = "negate";

    private String                                       tableName;

    private String                                       parentName;

    private RowNumberCol52                               rowNumberCol          = new RowNumberCol52();

    private DescriptionCol52                             descriptionCol        = new DescriptionCol52();

    private List<MetadataCol52>                          metadataCols          = new ArrayList<MetadataCol52>();

    private List<AttributeCol52>                         attributeCols         = new ArrayList<AttributeCol52>();

    private List<CompositeColumn< ? extends BaseColumn>> conditionPatterns     = new ArrayList<CompositeColumn< ? extends BaseColumn>>();

    private List<ActionCol52>                            actionCols            = new ArrayList<ActionCol52>();

    // TODO verify that it's not stored in the repository, else add @XStreamOmitField
    private transient AnalysisCol52                      analysisCol;

    public enum TableFormat {
        EXTENDED_ENTRY,
        LIMITED_ENTRY
    }

    private TableFormat               tableFormat = TableFormat.EXTENDED_ENTRY;

    /**
     * First column is always row number. Second column is description.
     * Subsequent ones follow the above column definitions: attributeCols, then
     * conditionCols, then actionCols, in that order, left to right.
     */
    private List<List<DTCellValue52>> data        = new ArrayList<List<DTCellValue52>>();

    // TODO verify that it's not stored in the repository, else add @XStreamOmitField
    private transient List<Analysis>  analysisData;

    public GuidedDecisionTable52() {
        analysisCol = new AnalysisCol52();
        analysisCol.setHideColumn( true );
    }

    public List<ActionCol52> getActionCols() {
        return actionCols;
    }

    public List<AttributeCol52> getAttributeCols() {
        return attributeCols;
    }

    /**
     * Return an immutable list of Pattern columns
     * 
     * @return
     */
    public List<Pattern52> getPatterns() {
        List<Pattern52> patterns = new ArrayList<Pattern52>();
        for ( CompositeColumn< ? > cc : conditionPatterns ) {
            if ( cc instanceof Pattern52 ) {
                patterns.add( (Pattern52) cc );
            }
        }
        return Collections.unmodifiableList( patterns );
    }

    public List<CompositeColumn< ? extends BaseColumn>> getConditions() {
        return this.conditionPatterns;
    }

    public Pattern52 getConditionPattern(String boundName) {
        for ( CompositeColumn< ? > cc : conditionPatterns ) {
            if ( cc instanceof Pattern52 ) {
                Pattern52 p = (Pattern52) cc;
                if ( p.getBoundName().equals( boundName ) ) {
                    return p;
                }
            }
        }
        return null;
    }

    public Pattern52 getPattern(ConditionCol52 col) {
        for ( CompositeColumn< ? > cc : conditionPatterns ) {
            if ( cc instanceof Pattern52 ) {
                Pattern52 p = (Pattern52) cc;
                if ( p.getChildColumns().contains( col ) ) {
                    return p;
                }
            }
        }
        return new Pattern52();
    }

    public String getBoundFactType(String boundName) {
        for ( CompositeColumn< ? > cc : this.conditionPatterns ) {
            if ( cc instanceof Pattern52 ) {
                Pattern52 p = (Pattern52) cc;
                if ( p.isBound() && p.getBoundName().equals( boundName ) ) {
                    return p.getFactType();
                }
            } else if ( cc instanceof BRLConditionColumn ) {
                BRLConditionColumn brl = (BRLConditionColumn) cc;
                for ( IPattern p : brl.getDefinition() ) {
                    if ( p instanceof FactPattern ) {
                        FactPattern fp = (FactPattern) p;
                        if ( fp.isBound() && fp.getBoundName().equals( boundName ) ) {
                            return fp.getFactType();
                        }
                    }
                }
            }
        }
        return new String();
    }

    public BRLColumn< ? , ? > getBRLColumn(BRLVariableColumn col) {
        for ( CompositeColumn< ? > cc : conditionPatterns ) {
            if ( cc instanceof BRLConditionColumn ) {
                BRLConditionColumn brl = (BRLConditionColumn) cc;
                if ( brl.getChildColumns().contains( col ) ) {
                    return brl;
                }
            }
        }
        for ( ActionCol52 ac : actionCols ) {
            if ( ac instanceof BRLActionColumn ) {
                BRLActionColumn brl = (BRLActionColumn) ac;
                if ( brl.getChildColumns().contains( col ) ) {
                    return brl;
                }
            }
        }
        throw new IllegalStateException( "col is not a child of any of the defined BRLColumns." );
    }

    public BRLConditionColumn getBRLColumn(BRLConditionVariableColumn col) {
        for ( CompositeColumn< ? > cc : conditionPatterns ) {
            if ( cc instanceof BRLConditionColumn ) {
                BRLConditionColumn brl = (BRLConditionColumn) cc;
                if ( brl.getChildColumns().contains( col ) ) {
                    return brl;
                }
            }
        }
        throw new IllegalStateException( "col is not a child of any of the defined BRLColumns." );
    }

    public BRLActionColumn getBRLColumn(BRLActionVariableColumn col) {
        for ( ActionCol52 ac : actionCols ) {
            if ( ac instanceof BRLActionColumn ) {
                BRLActionColumn brl = (BRLActionColumn) ac;
                if ( brl.getChildColumns().contains( col ) ) {
                    return brl;
                }
            }
        }
        throw new IllegalStateException( "col is not a child of any of the defined BRLColumns." );
    }

    public long getConditionsCount() {
        long size = 0;
        for ( CompositeColumn< ? > cc : this.conditionPatterns ) {
            size = size + cc.getChildColumns().size();
        }
        return size;
    }

    public List<List<DTCellValue52>> getData() {
        return data;
    }

    public List<Analysis> getAnalysisData() {
        return analysisData;
    }

    /**
     * This method expands Composite columns into individual columns where
     * knowledge of individual columns is necessary; for example separate
     * columns in the user-interface or where individual columns need to be
     * analysed.
     * 
     * @return A List of individual columns
     */
    public List<BaseColumn> getExpandedColumns() {
        List<BaseColumn> columns = new ArrayList<BaseColumn>();
        columns.add( rowNumberCol );
        columns.add( descriptionCol );
        columns.addAll( metadataCols );
        columns.addAll( attributeCols );
        for ( CompositeColumn< ? > cc : this.conditionPatterns ) {
            boolean explode = !(cc instanceof LimitedEntryCol);
            if ( explode ) {
                for ( BaseColumn bc : cc.getChildColumns() ) {
                    columns.add( bc );
                }
            } else {
                columns.add( cc );
            }
        }
        for ( ActionCol52 ac : this.actionCols ) {
            if ( ac instanceof BRLActionColumn ) {
                if ( ac instanceof LimitedEntryCol ) {
                    columns.add( ac );
                } else {
                    BRLActionColumn bac = (BRLActionColumn) ac;
                    for ( BRLActionVariableColumn variable : bac.getChildColumns() ) {
                        columns.add( variable );
                    }
                }

            } else {
                columns.add( ac );
            }
        }
        columns.add( analysisCol );
        return columns;
    }

    public DescriptionCol52 getDescriptionCol() {
        // De-serialising old models sets this field to null
        if ( this.descriptionCol == null ) {
            this.descriptionCol = new DescriptionCol52();
        }
        return this.descriptionCol;
    }

    public List<MetadataCol52> getMetadataCols() {
        if ( null == metadataCols ) {
            metadataCols = new ArrayList<MetadataCol52>();
        }
        return metadataCols;
    }

    public String getParentName() {
        return parentName;
    }

    public RowNumberCol52 getRowNumberCol() {
        // De-serialising old models sets this field to null
        if ( this.rowNumberCol == null ) {
            this.rowNumberCol = new RowNumberCol52();
        }
        return this.rowNumberCol;
    }

    public void initAnalysisColumn() {
        analysisData = new ArrayList<Analysis>( data.size() );
        for ( int i = 0; i < data.size(); i++ ) {
            analysisData.add( new Analysis() );
        }
    }

    public AnalysisCol52 getAnalysisCol() {
        return analysisCol;
    }

    public String getTableName() {
        return tableName;
    }

    public String getType(BaseColumn col,
                          SuggestionCompletionEngine sce) {
        if ( col instanceof RowNumberCol52 ) {
            return getType( (RowNumberCol52) col,
                            sce );
        } else if ( col instanceof AttributeCol52 ) {
            return getType( (AttributeCol52) col,
                            sce );
        } else if ( col instanceof BRLConditionVariableColumn ) {
            return getType( (BRLConditionVariableColumn) col,
                            sce );
        } else if ( col instanceof ConditionCol52 ) {
            return getType( (ConditionCol52) col,
                            sce );
        } else if ( col instanceof ActionSetFieldCol52 ) {
            return getType( (ActionSetFieldCol52) col,
                            sce );
        } else if ( col instanceof ActionInsertFactCol52 ) {
            return getType( (ActionInsertFactCol52) col,
                            sce );
        } else if ( col instanceof BRLActionVariableColumn ) {
            return getType( (BRLActionVariableColumn) col,
                            sce );
        }
        return SuggestionCompletionEngine.TYPE_STRING;
    }

    private String getType(RowNumberCol52 col,
                           SuggestionCompletionEngine sce) {
        return SuggestionCompletionEngine.TYPE_NUMERIC_INTEGER;
    }

    private String getType(AttributeCol52 col,
                           SuggestionCompletionEngine sce) {
        String type = SuggestionCompletionEngine.TYPE_STRING;
        String attrName = col.getAttribute();
        if ( attrName.equals( GuidedDecisionTable52.SALIENCE_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_NUMERIC_INTEGER;
        } else if ( attrName.equals( GuidedDecisionTable52.ENABLED_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_BOOLEAN;
        } else if ( attrName.equals( GuidedDecisionTable52.NO_LOOP_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_BOOLEAN;
        } else if ( attrName.equals( GuidedDecisionTable52.DURATION_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_NUMERIC_LONG;
        } else if ( attrName.equals( GuidedDecisionTable52.TIMER_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_STRING;
        } else if ( attrName.equals( GuidedDecisionTable52.CALENDARS_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_STRING;
        } else if ( attrName.equals( GuidedDecisionTable52.AUTO_FOCUS_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_BOOLEAN;
        } else if ( attrName.equals( GuidedDecisionTable52.LOCK_ON_ACTIVE_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_BOOLEAN;
        } else if ( attrName.equals( GuidedDecisionTable52.DATE_EFFECTIVE_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_DATE;
        } else if ( attrName.equals( GuidedDecisionTable52.DATE_EXPIRES_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_DATE;
        } else if ( attrName.equals( GuidedDecisionTable52.DIALECT_ATTR ) ) {
            type = SuggestionCompletionEngine.TYPE_STRING;
        }
        return type;
    }

    private String getType(ConditionCol52 col,
                           SuggestionCompletionEngine sce) {
        final Pattern52 pattern = getPattern( col );
        return getType( pattern,
                        col,
                        sce );
    }

    private String getType(Pattern52 pattern,
                           ConditionCol52 col,
                           SuggestionCompletionEngine sce) {

        // Columns with "Value Lists" etc are always Text (for now)
        if ( hasValueList( col ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        // Operator "in" and "not in" requires a List as the value. These are always Text (for now)
        if ( SuggestionCompletionEngine.operatorRequiresList( col.getOperator() ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Literals without operators are always Text (as the user can specify the operator "in cell")
        if ( col.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_LITERAL ) {
            if ( col.getOperator() == null || "".equals( col.getOperator() ) ) {
                return SuggestionCompletionEngine.TYPE_STRING;
            }
        }

        //Formula are always Text (as the user can specify anything "in cell")
        if ( col.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_PREDICATE ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Predicates are always Text (as the user can specify anything "in cell")
        if ( col.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_RET_VALUE ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = pattern.getFactType();
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getType(BRLConditionVariableColumn col,
                           SuggestionCompletionEngine sce) {

        //If the parameter is not bound to a Fact or FactField use the explicit type. This is (currently) 
        //used when a BRL fragment does not contain any Template Keys and a single BRLConditionVariableColumn 
        //is created with type SuggestionCompletionEngine.TYPE_BOOLEAN i.e. Limited Entry
        if ( col.getFactType() == null && col.getFactField() == null ) {
            return col.getFieldType();
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = col.getFactType();
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getType(ActionSetFieldCol52 col,
                           SuggestionCompletionEngine sce) {

        // Columns with "Value Lists" etc are always Text (for now)
        if ( hasValueList( col ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = getBoundFactType( col.getBoundName() );
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getType(Pattern52 pattern,
                           ActionSetFieldCol52 col,
                           SuggestionCompletionEngine sce) {

        // Columns with "Value Lists" etc are always Text (for now)
        if ( hasValueList( col ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = pattern.getFactType();
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getType(ActionInsertFactCol52 col,
                           SuggestionCompletionEngine sce) {

        // Columns with "Value Lists" etc are always Text (for now)
        if ( hasValueList( col ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = col.getFactType();
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getType(BRLActionVariableColumn col,
                           SuggestionCompletionEngine sce) {

        //If the parameter is not bound to a Fact or FactField use the explicit type. This is (currently) 
        //used when a BRL fragment does not contain any Template Keys and a single BRLActionVariableColumn 
        //is created with type SuggestionCompletionEngine.TYPE_BOOLEAN i.e. Limited Entry
        if ( col.getFactType() == null && col.getFactField() == null ) {
            return col.getFieldType();
        }

        //Otherwise lookup from SuggestionCompletionEngine
        final String factType = col.getFactType();
        final String fieldName = col.getFactField();
        return getTypeFromSCE( factType,
                               fieldName,
                               sce );
    }

    private String getTypeFromSCE(String factType,
                                  String fieldName,
                                  SuggestionCompletionEngine sce) {

        // Columns with enumerations are always Text (for now)
        if ( sce.hasEnums( factType,
                           fieldName ) ) {
            return SuggestionCompletionEngine.TYPE_STRING;
        }

        //Otherwise lookup from SuggestionCompletionEngine
        String type = sce.getFieldType( factType,
                                        fieldName );
        return type;
    }

    // Get the Data Type corresponding to a given column
    public DTDataTypes52 getTypeSafeType(BaseColumn column,
                                         SuggestionCompletionEngine sce) {

        final String type = getType( column,
                                     sce );
        return convertToTypeSafeType( type );
    }

    // Get the Data Type corresponding to a given column
    public DTDataTypes52 getTypeSafeType(Pattern52 pattern,
                                         ConditionCol52 column,
                                         SuggestionCompletionEngine sce) {
        final String type = getType( pattern,
                                     column,
                                     sce );
        return convertToTypeSafeType( type );
    }

    // Get the Data Type corresponding to a given column
    public DTDataTypes52 getTypeSafeType(Pattern52 pattern,
                                         ActionSetFieldCol52 column,
                                         SuggestionCompletionEngine sce) {
        final String type = getType( pattern,
                                     column,
                                     sce );
        return convertToTypeSafeType( type );
    }

    private DTDataTypes52 convertToTypeSafeType(String type) {
        if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC ) ) {
            return DTDataTypes52.NUMERIC;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_BIGDECIMAL ) ) {
            return DTDataTypes52.NUMERIC_BIGDECIMAL;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_BIGINTEGER ) ) {
            return DTDataTypes52.NUMERIC_BIGINTEGER;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_BYTE ) ) {
            return DTDataTypes52.NUMERIC_BYTE;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_DOUBLE ) ) {
            return DTDataTypes52.NUMERIC_DOUBLE;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_FLOAT ) ) {
            return DTDataTypes52.NUMERIC_FLOAT;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_INTEGER ) ) {
            return DTDataTypes52.NUMERIC_INTEGER;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_LONG ) ) {
            return DTDataTypes52.NUMERIC_LONG;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_NUMERIC_SHORT ) ) {
            return DTDataTypes52.NUMERIC_SHORT;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_BOOLEAN ) ) {
            return DTDataTypes52.BOOLEAN;
        } else if ( type.equals( SuggestionCompletionEngine.TYPE_DATE ) ) {
            return DTDataTypes52.DATE;
        }
        return DTDataTypes52.STRING;
    }

    public String[] getValueList(BaseColumn col) {
        if ( col instanceof AttributeCol52 ) {
            return getValueList( (AttributeCol52) col );
        } else if ( col instanceof ConditionCol52 ) {
            return getValueList( (ConditionCol52) col );
        } else if ( col instanceof ActionSetFieldCol52 ) {
            return getValueList( (ActionSetFieldCol52) col );
        } else if ( col instanceof ActionInsertFactCol52 ) {
            return getValueList( (ActionInsertFactCol52) col );
        }
        return new String[0];
    }

    private String[] getValueList(AttributeCol52 col) {
        if ( "no-loop".equals( col.getAttribute() ) || "enabled".equals( col.getAttribute() ) ) {
            return new String[]{"true", "false"};
        }
        return new String[0];
    }

    private String[] getValueList(ConditionCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return col.getValueList().split( "," );
        }
        return new String[0];
    }

    private String[] getValueList(ActionSetFieldCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return col.getValueList().split( "," );
        }
        return new String[0];
    }

    private String[] getValueList(ActionInsertFactCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return col.getValueList().split( "," );
        }
        return new String[0];
    }

    public boolean hasValueList(AttributeCol52 col) {
        if ( "no-loop".equals( col.getAttribute() ) || "enabled".equals( col.getAttribute() ) ) {
            return true;
        }
        return false;
    }

    public boolean hasValueList(ConditionCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return true;
        }
        return false;
    }

    public boolean hasValueList(ActionSetFieldCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return true;
        }
        return false;
    }

    public boolean hasValueList(ActionInsertFactCol52 col) {
        if ( col.getValueList() != null && !"".equals( col.getValueList() ) ) {
            return true;
        }
        return false;
    }

    public boolean isConstraintValid(DTColumnConfig52 col) {
        if ( col instanceof RowNumberCol52 ) {
            return true;
        }
        if ( col instanceof DescriptionCol52 ) {
            return true;
        }
        if ( col instanceof MetadataCol52 ) {
            return true;
        }
        if ( col instanceof AttributeCol52 ) {
            return true;
        }
        if ( col instanceof ConditionCol52 ) {
            ConditionCol52 c = (ConditionCol52) col;
            if ( c.getConstraintValueType() == BaseSingleFieldConstraint.TYPE_LITERAL ) {
                if ( c.getFactField() == null
                     || c.getFactField().equals( "" ) ) {
                    return false;
                }
                if ( c.getOperator() == null
                     || c.getOperator().equals( "" ) ) {
                    return false;
                }
                return true;
            }
            return true;
        }
        if ( col instanceof ActionCol52 ) {
            return true;
        }
        return false;
    }

    public void setData(List<List<DTCellValue52>> data) {
        this.data = data;
    }

    public void setRowNumberCol(RowNumberCol52 rowNumberCol) {
        this.rowNumberCol = rowNumberCol;
    }

    public void setDescriptionCol(DescriptionCol52 descriptionCol) {
        this.descriptionCol = descriptionCol;
    }

    public void setMetadataCols(List<MetadataCol52> metadataCols) {
        this.metadataCols = metadataCols;
    }

    public void setAttributeCols(List<AttributeCol52> attributeCols) {
        this.attributeCols = attributeCols;
    }

    public void setConditionPatterns(List<CompositeColumn< ? extends BaseColumn>> conditionPatterns) {
        this.conditionPatterns = conditionPatterns;
    }

    public void setActionCols(List<ActionCol52> actionCols) {
        this.actionCols = actionCols;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public TableFormat getTableFormat() {
        //GUVNOR-1820: Not possible to give default value to action columns
        return tableFormat == null ? TableFormat.EXTENDED_ENTRY : tableFormat;
    }

    public void setTableFormat(TableFormat tableFormat) {
        this.tableFormat = tableFormat;
    }

    /**
     * Check is the model uses DSLSentences and hence requires expansion
     * 
     * @return true if any BRLColumn's contain DSLSentence's
     */
    public boolean hasDSLSentences() {
        for ( CompositeColumn< ? extends BaseColumn> column : this.conditionPatterns ) {
            if ( column instanceof BRLConditionColumn ) {
                BRLConditionColumn brlColumn = (BRLConditionColumn) column;
                for ( IPattern pattern : brlColumn.getDefinition() ) {
                    if ( pattern instanceof DSLSentence ) {
                        return true;
                    }
                }
            }
        }
        for ( ActionCol52 column : this.actionCols ) {
            if ( column instanceof BRLActionColumn ) {
                BRLActionColumn brlColumn = (BRLActionColumn) column;
                for ( IAction action : brlColumn.getDefinition() ) {
                    if ( action instanceof DSLSentence ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
