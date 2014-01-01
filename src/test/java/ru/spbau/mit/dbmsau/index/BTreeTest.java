package ru.spbau.mit.dbmsau.index;

import org.junit.Before;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import ru.spbau.mit.dbmsau.BaseTest;
import ru.spbau.mit.dbmsau.index.btree.BTree;
import ru.spbau.mit.dbmsau.index.btree.TreeTuple;
import ru.spbau.mit.dbmsau.pages.Page;
import ru.spbau.mit.dbmsau.relation.Type;
import ru.spbau.mit.dbmsau.table.TestTableTest;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;

public class BTreeTest extends BaseTest {

    @Test
    public void testPutGetPairs() {
        Type[] valueTypes = new Type[]{Type.getIntegerType(), Type.getIntegerType()};
        Type[] keyTypes = new Type[]{Type.getIntegerType()};
        Page rootPage = context.getPageManager().allocatePage();
        int rootPageId = rootPage.getId();
        context.getPageManager().releasePage(rootPage);

        BTree bTree = new BTree(keyTypes, valueTypes, rootPageId, context);
        bTree.initFirstTime();

        int n = 10000;
        for (int i = 0; i < n; i += 2) {
            TreeTuple key = TreeTuple.getOneIntTuple(i);
            TreeTuple val = TreeTuple.getTwoIntTuple(2 * i, 2 * i + 1);
            bTree.put(key, val);
        }

        for (int i = 0; i < n; i += 2) {
            TreeTuple key = TreeTuple.getOneIntTuple(i);
            TreeTuple res = bTree.get(key);

            if (i % 2 == 1) {
                assertNull(res);
            } else {
                int first = res.getInteger(0);
                int second = res.getInteger(4);

                assertThat(first, is(2 * i));
                assertThat(second, is(2 * i + 1));
            }
        }
    }

    @Test
    public void testGetRandom() {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        BTree bTree = initWithRandom(map);

        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            int key = entry.getKey();
            TreeTuple res = bTree.get(TreeTuple.getOneIntTuple(key));

            assertThat(res.getInteger(0), is(entry.getValue()));
        }
    }

    @Test
    public void testLowerBound() {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        BTree bTree = initWithRandom(map);

        int n = 10000;
        Random rnd = new Random(65432);
        for (int i = 0; i < n; ++i) {
            int cur = rnd.nextInt();

            Integer expected = map.ceilingKey(cur);
            BTree.ItemLocation loc = bTree.lower_bound(TreeTuple.getOneIntTuple(cur));
            TreeTuple res = bTree.getLowerBoundKey(loc);

            if (expected == null) {
                assertNull(res);
            } else {
                assertThat(res.getInteger(0), is(expected));
            }
        }
    }

    public BTree initWithRandom(TreeMap<Integer, Integer> map) {
        int n = 10000;

        Type[] valueTypes = new Type[]{Type.getIntegerType()};
        Type[] keyTypes = new Type[]{Type.getIntegerType()};
        Page rootPage = context.getPageManager().allocatePage();
        int rootPageId = rootPage.getId();
        context.getPageManager().releasePage(rootPage);

        BTree bTree = new BTree(keyTypes, valueTypes, rootPageId, context);
        bTree.initFirstTime();

        Random rnd = new Random(12345);
        for (int i = 0; i < n; ++i) {
            int key = rnd.nextInt();
            int val = rnd.nextInt();

            bTree.put(TreeTuple.getOneIntTuple(key), TreeTuple.getOneIntTuple(val));
            map.put(key, val);
        }

        return bTree;
    }
}
