package ru.spbau.mit.dbmsau.index;

import ru.spbau.mit.dbmsau.Context;
import ru.spbau.mit.dbmsau.index.exception.IndexException;
import ru.spbau.mit.dbmsau.relation.*;
import ru.spbau.mit.dbmsau.table.Table;
import ru.spbau.mit.dbmsau.table.TableRecord;

public class StubIndexManager extends IndexManager {
    public StubIndexManager(Context context) {
        super(context);
    }

    @Override
    protected Index buildIndex(String name, Table table, int[] columnIndexes) {
        return new StubIndex(name, table, columnIndexes);
    }

    @Override
    protected void saveIndex(Index index) throws IndexException {

    }

    private class StubIndex extends Index {
        private StubIndex(String name, Table table, int[] columnIndexes) {
            super(name, table, columnIndexes);
        }

        @Override
        public boolean isMatchingFor(int[] queryColumnIndexes, int matchingType) {
            return getColumnIndexes()[0] == queryColumnIndexes[0];
        }

        @Override
        public RecordSet buildRecordSetMatchingEqualityCondition(int[] queryColumnIndexes, final String[] values) {
            return new WhereMatcherRecordSet(
                context.getTableRecordManager().select(table),
                new WhereMatcher() {
                    @Override
                    public boolean matches(RelationRecord record) {
                        return record.getValueAsString(getColumnIndexes()[0]).equals(values[0]);
                    }

                    @Override
                    public void prepareFor(Relation relation) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                }
            );
        }

        @Override
        public void processNewRecord(TableRecord record) {

        }

        @Override
        public boolean isDuplicateViolation(RelationRecord record) {
            return false;
        }

        @Override
        public void processDeletedRecord(TableRecord record) {

        }
    }
}