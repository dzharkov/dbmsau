package ru.spbau.mit.dbmsau.pages;

import java.util.Iterator;

public class RecordsPage extends Page implements Iterable<Record> {
    private int recordsLength;
    final static int takenRecordsCountOffset = PageManager.PAGE_SIZE-4;

    public RecordsPage(Page page, int recordsLength) {
        super(page);
        this.recordsLength = recordsLength;
    }

    public int getRecordsLength() {
        return recordsLength;
    }

    public int getMaxRecordsCount() {
        return (8 * PageManager.PAGE_SIZE - 32) / (8 * getRecordsLength() + 1);
    }

    private int getBitmapOffset() {
        return getMaxRecordsCount() * getRecordsLength();
    }

    /**
     * @param slotIndex
     * @return индекс байта во всей странице, где лежит бит слота в битмапе
     */
    private int getBitmapSlotByteIndex(int slotIndex) {
        return getBitmapOffset() + slotIndex / 8;
    }

    public Boolean isSlotUsed(int slotIndex) {
        int byteNumber = getBitmapSlotByteIndex(slotIndex);
        return (getByteBuffer().get(byteNumber) & (1 << (slotIndex % 8))) != 0;
    }

    private void setSlotStatus(int slotIndex, boolean isUsed) {
        if (isSlotUsed(slotIndex) != isUsed) {
            int byteNumber = getBitmapSlotByteIndex(slotIndex);
            byte newValue = (byte) (getByteBuffer().get(byteNumber) ^ (1 << (slotIndex % 8)));
            getByteBuffer().put(byteNumber, newValue);

            int taken = getByteBuffer().getInt(takenRecordsCountOffset);
            taken += isUsed ? 1 : -1;
            getByteBuffer().putInt(takenRecordsCountOffset, taken);
        }
    }

    public int getSlotOffset(int slotIndex) {
        return getRecordsLength() * slotIndex;
    }

    public Record getRecordFromSlot(int slotIndex) {
        return new Record(this, slotIndex);
    }

    protected int getFreeSlotsCount() {
        int taken = getByteBuffer().getInt(takenRecordsCountOffset);
        return getMaxRecordsCount() - taken;
    }

    public boolean isEmpty() {
        return getFreeSlotsCount() == getMaxRecordsCount();
    }

    public boolean isFull() {
        return getFreeSlotsCount() == 0;
    }

    public boolean isAlmostFull() {
        return getFreeSlotsCount() == 1;
    }

    public Record getClearRecord() {
        int slotIndex = 0;
        while (slotIndex < getMaxRecordsCount() && isSlotUsed(slotIndex)) {
            slotIndex++;
        }

        if (slotIndex < getMaxRecordsCount()) {
            setSlotStatus(slotIndex, true);
            return getRecordFromSlot(slotIndex);
        } else {
            throw new Error("плохо 123");
        }
    }

    public void freeRecord(int slotIndex) {
        setSlotStatus(slotIndex, false);
    }

    @Override
    public Iterator<Record> iterator() {
        return new RecordsIterator();
    }

    private class RecordsIterator implements Iterator<Record> {
        int counter = 0;

        @Override
        public boolean hasNext() {
            while (counter < getMaxRecordsCount() && !isSlotUsed(counter)) {
                counter++;
            }
            return counter < getMaxRecordsCount();
        }

        @Override
        public Record next() {
            return getRecordFromSlot(counter++);
        }

        @Override
        public void remove() {
            setSlotStatus(counter - 1, false);
        }
    }
}
