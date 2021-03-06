package ru.spbau.mit.dbmsau.index.btree;

import ru.spbau.mit.dbmsau.Context;
import ru.spbau.mit.dbmsau.ContextContainer;
import ru.spbau.mit.dbmsau.pages.Page;
import ru.spbau.mit.dbmsau.relation.RelationRecord;
import ru.spbau.mit.dbmsau.relation.Type;
import ru.spbau.mit.dbmsau.table.TableRecord;

public class BTree extends ContextContainer {
    public static final int NODE_ID_SIZE = 4; // int size
    private int rootId;
    private int keySize, valSize;
    private Type[] keyType, valType;

    public BTree(Type[] keyType, Type[] valType, int rootId, Context context) {
        super(context);

        this.keyType = keyType;
        this.valType = valType;

        keySize = valSize = 0;
        for (Type aKeyType : keyType) {
            keySize += aKeyType.getSize();
        }
        for (Type aValType : valType) {
            valSize += aValType.getSize();
        }

        this.rootId = rootId;
    }

    public void initFirstTime() {
        Page page = context.getPageManager().getPageById(rootId, true);
        Node root = LeafNode.getNewLeafNode(page, keySize, valSize, this);
        releaseNode(root);
    }

    public void releaseNode(int nodeId) {
        context.getPageManager().releasePage(nodeId);
    }

    public void releaseNode(Node node) {
        releaseNode(node.nodeId);
    }

    public TreeTuple getNewNodeIdTuple(int nodeId) {
        return TreeTuple.getOneIntTuple(nodeId);
    }

    public TreeTuple getNewKeyTuple(int[] columnIndexes, RelationRecord record) {
        return TreeTuple.getTupleFromRecord(keySize, keyType, columnIndexes, record);
    }

    public TreeTuple getNewValTuple(TableRecord record) {
        int pageId = record.getRecord().getPageId();
        int slotIndex = record.getRecord().getSlotIndex();

        return TreeTuple.getTwoIntTuple(pageId, slotIndex);
    }

    public TreeTuple getNewKeyTuple(Object[] objects) {
        return TreeTuple.getTupleFromList(keySize, keyType, objects);
    }

    public TreeTuple getNewValueTuple(Object[] objects) {
        return TreeTuple.getTupleFromList(valSize, valType, objects);
    }

    public Node getNewNode(boolean isLeaf) {
        return getNewNode(
            context.getPageManager().allocatePage(),
            isLeaf
        );
    }

    public Node getNewNode(Page page, boolean isLeaf) {
        Node newNode;

        if (isLeaf) {
            newNode = LeafNode.getNewLeafNode(page, keySize, valSize, this);
        } else {
            newNode = GuideNode.getNewGuideNode(page, keySize, NODE_ID_SIZE, this);
        }

        return newNode;
    }

    public Node getNodeById(int nodeId, boolean isForWriting) {
        Page page = context.getPageManager().getPageById(nodeId, isForWriting);
        NodeData nodeData = NodeData.getDataFromPage(page, keySize, valSize);

        if (nodeData.isLeaf()) {
            nodeData.changeSizes(keySize, valSize);
            return new LeafNode(page.getId(), nodeData, this);
        } else {
            nodeData.changeSizes(keySize, 4);
            return new GuideNode(page.getId(), nodeData, this);
        }
    }

    public int cmp(TreeTuple first, TreeTuple second) {
        int offset = 0;
        for (int i = 0; i < keyType.length; i++) {
            int cur = 0;
            if (keyType[i].getType() == Type.TYPE_INTEGER) {
                cur = Integer.compare(first.getInteger(offset), second.getInteger(offset));
            } else {
                int maxLength = keyType[i].getLength();
                cur = first.getString(offset, maxLength).compareTo(second.getString(offset, maxLength));
            }

            if (cur != 0) {
                return cur;
            }

            offset += keyType[i].getSize();
        }

        return 0;
    }

    public int cmp(TreeTuple first, Node node, int secondKeyId) {
        int offset = 0;
        for (int i = 0; i < keyType.length; i++) {
            int cur = 0;
            if (keyType[i].getType() == Type.TYPE_INTEGER) {
                cur = Integer.compare(first.getInteger(offset), node.nodeData.getKeyInt(secondKeyId, offset));
            } else {
                int maxLength = keyType[i].getLength();
                cur = first.getString(offset, maxLength).compareTo(node.nodeData.getKeyString(secondKeyId, offset, maxLength));
            }

            if (cur != 0) {
                return cur;
            }

            offset += keyType[i].getSize();
        }

        return 0;
    }

    public int findGuideIndex(Node node, TreeTuple key) {
        int b = 1;
        int e = node.nodeData.getAmountOfKeys() - 1;

        while (e - b > 0) {
            int c = (b + e) / 2;

            if (cmp(key, node, c) >= 0) {
                b = c + 1;
            } else {
                e = c;
            }
        }

        if (e < 0 || cmp(key, node, e) >= 0) {
            return node.nodeData.getAmountOfKeys() - 1;
        } else {
            return e - 1;
        }
    }

    public int findInsertIndex(Node node, TreeTuple key) {
        int b = 0;
        int e = node.nodeData.getAmountOfKeys() - 1;

        while (e - b > 0) {
            int c = (b + e) / 2;

            if (cmp(key, node, c) > 0) {
                b = c + 1;
            } else {
                e = c;
            }
        }

        if (e < 0 || cmp(key, node, e) > 0) {
            return node.nodeData.getAmountOfKeys();
        } else {
            return e;
        }
    }

    public void put(TreeTuple key, TreeTuple value) {
        if (key == null) {
            throw new NullPointerException();
        }

        // Insert the new key/value into the tree.
        Node root = getNodeById(rootId, true);
        Node newNode = root.put(key, value);

        // Create new root?
        if (newNode != null) {
            Page rootCopyPage = context.getPageManager().allocatePage();
            rootCopyPage.assignDataFrom(root.getNodeData().dataPage);
            context.getPageManager().releasePage(rootCopyPage);

            root.getNodeData().dataPage.clearData();
            Node newRoot = getNewNode(root.getNodeData().dataPage, false);

            newRoot.nodeData.addValue(0, getNewNodeIdTuple(rootCopyPage.getId()));
            newRoot.nodeData.setAmountOfKeys(1);

            newRoot.nodeData.addValue(1, getNewNodeIdTuple(newNode.nodeId));
            newRoot.nodeData.addKey(1, newNode.nodeData.getKey(0));

            assert rootId == newRoot.nodeId;

            releaseNode(newRoot);
            releaseNode(newNode);
        }

        releaseNode(root);
    }

    public boolean containsKey(TreeTuple key) {
        return get(key) != null;
    }

    public class ItemLocation {
        private int nodeId;
        private int index;

        public ItemLocation(int nodeId, int index) {
            this.nodeId = nodeId;
            this.index = index;
        }

        public int getNodeId() {
            return nodeId;
        }

        public int getIndex() {
            return index;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    public ItemLocation lower_bound(TreeTuple key) {
        Node cur = getNodeById(rootId, false);
        while (!cur.nodeData.isLeaf()) {
            int index = findGuideIndex(cur, key);
            int nextNodeIndex = cur.nodeData.getValue(index).getInteger(0);
            cur = getNodeById(nextNodeIndex, false);
        }

        int index = findInsertIndex(cur, key);
        int nodeId = cur.nodeId;

        return new ItemLocation(nodeId, index);
    }

    public TreeTuple getLowerBoundKey(ItemLocation loc) {
        Node node = this.getNodeById(loc.getNodeId(), false);

        if (loc.getIndex() < node.nodeData.getAmountOfKeys()) {
            return node.nodeData.getKey(loc.getIndex());
        } else {
            if (node.nodeData.getNextNodeId() == NodeData.NO_NODE_ID) {
                return null;
            } else {
                return getLowerBoundKey(new ItemLocation(node.nodeData.getNextNodeId(), 0));
            }
        }
    }

    /**
     * Returns the value to which this BPTree maps the specified key or null if it contains no mapping for the key.
     */
    public TreeTuple get(TreeTuple key) {
        ItemLocation loc = lower_bound(key);

        Node node = this.getNodeById(loc.nodeId, false);

        if (loc.getIndex() < node.nodeData.getAmountOfKeys()) {

            if (cmp(key, node, loc.getIndex()) == 0) {
                return node.nodeData.getValue(loc.getIndex());
            }
        }

        return null;
    }

    public void remove(TreeTuple key) {
        ItemLocation loc = lower_bound(key);

        Node node = this.getNodeById(loc.nodeId, true);

        if (loc.getIndex() < node.nodeData.getAmountOfKeys()) {

            if (cmp(key, node, loc.getIndex()) == 0) {
                node.nodeData.removeValue(loc.getIndex());
                node.nodeData.removeKey(loc.getIndex());
            }
        }

        releaseNode(node);
    }
}
