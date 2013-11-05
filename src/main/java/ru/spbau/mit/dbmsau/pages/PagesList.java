package ru.spbau.mit.dbmsau.pages;

import ru.spbau.mit.dbmsau.Context;

import java.util.Arrays;

public class PagesList {
    private Integer headPageId;
    private Context context;

    public PagesList(Integer headPageId, Context context) {
        this.headPageId = headPageId;
        this.context = context;
    }

    public void initList() {
        initNewLastPage(new Page(headPageId, null));
    }

    private DirectoryPage initNewLastPage(Page page) {
        byte[] cleanData = new byte[PageManager.PAGE_SIZE];
        Arrays.fill(cleanData, (byte) 0);

        page.setData(cleanData);

        DirectoryPage directoryPage = new DirectoryPage(page);
        directoryPage.getClearRecord().setIntegerValue(0, Page.NULL_PAGE_ID);

        context.getPageManager().savePage(directoryPage);

        return directoryPage;
    }

    public Integer peek() {
        DirectoryPage headPage = getHeadPage();

        Integer slotIndex = firstUsedSlotIndex(headPage);

        if (slotIndex == null) {
            return null;
        }

        return headPage.getRecordFromSlot(slotIndex).getIntegerValue(0);
    }

    public Integer pop() {
        DirectoryPage headPage = getHeadPage();
        Integer slotIndex = firstUsedSlotIndex(headPage);

        if (slotIndex == null) {
             return null;
        }

        Integer result = peek();

        headPage.freeRecord(slotIndex);

        if (headPage.isDirectoryEmpty()) {
            Integer nextPageId = headPage.nextDirectoryPageId();
            if (!nextPageId.equals(Page.NULL_PAGE_ID)) {
                Page nextPage = context.getPageManager().getPageById(nextPageId);
                headPage.assignDataFrom(nextPage);

                context.getPageManager().freePage(nextPageId);
            }
        }

        context.getPageManager().savePage(headPage);

        return result;
    }

    public void put(Integer newPageId) {
        DirectoryPage page = getHeadPage();

        while (page.isDirectoryFull()) {
            Integer nextPageId = page.nextDirectoryPageId();

            if (!nextPageId.equals(Page.NULL_PAGE_ID)) {
                page = new DirectoryPage(context.getPageManager().getPageById(nextPageId));
            } else {
                DirectoryPage oldPage = page;
                Page allocatedPage = context.getPageManager().allocatePage();
                page = initNewLastPage(allocatedPage);
                oldPage.setNextDirectoryPageId(page.getId());
                context.getPageManager().savePage(oldPage);
            }
        }

        page.getClearRecord().setIntegerValue(0, newPageId);
        context.getPageManager().savePage(page);
    }

    private DirectoryPage getHeadPage() {
        return new DirectoryPage(context.getPageManager().getPageById(headPageId));
    }

    private Integer firstUsedSlotIndex(DirectoryPage page) {
        for (int i = 1; i < page.getMaxRecordsCount(); i++) {
            if (page.isSlotUsed(i)) {
                return i;
            }
        }

        return null;
    }
}