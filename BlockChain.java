import java.util.*;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    private BlockNode maxHeightNode;
    private Map<ByteArrayWrapper, BlockNode> hashToNode;
    private TransactionPool txPool;

    /**
     * 创建一条新的区块链，仅包含创世区块
     */
    public BlockChain(Block genesisBlock) {
        txPool = new TransactionPool();
        hashToNode = new HashMap<>();

        UTXOPool uPool = new UTXOPool();

        addCoinbaseToUPool(genesisBlock.getCoinbase(), uPool);

        BlockNode node = new BlockNode(genesisBlock, null, uPool);
        hashToNode.put(new ByteArrayWrapper(genesisBlock.getHash()), node);
        maxHeightNode = node;
    }

    /** 获取最大高度的区块 */
    public Block getMaxHeightBlock() {
        return this.maxHeightNode.b;
    }

    /** 最高的区块的UTXO Pool */
    public UTXOPool getMaxHeightUTXOPool() {
        return this.maxHeightNode.uPool;
    }

    /** 获得当前的Transaction Pool */
    public TransactionPool getTransactionPool() {
        return this.txPool;
    }

    /**
     * 1. 我们先验证区块的parent是否存在于区块
     * 2. 区块的深度不能太浅，比如最长链深度为100，我们不能把这个区块加入到深度为1的地方，因为这个区块哪怕加入，未来也不可能成为最长区块（算力不可能超过全网50%，无法追上）
     * 3. 每一个Transaction必须成立，从当前的TxPool中移除相关的Trsansaction（因为别人已经算出来nounce并产生新的区块加入了区块链）
     * 4. 产生一个新的UTXO Pool,产生一个新的BlockNode来包装UTXO
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        /* 1. 我们先验证区块的parent是否存在于区块 */
        ByteArrayWrapper parentHash = new ByteArrayWrapper(block.getPrevBlockHash());
        if (!this.hashToNode.containsKey(parentHash)) {
            return false;
        }
        BlockNode parent = this.hashToNode.get(parentHash);
        int proposeHeight = parent.height+1;

        /* 2. 区块的深度不能太浅，比如最长链深度为100，我们不能把这个区块加入到深度为1的地方，因为这个区块哪怕加入，未来也不可能成为最长区块（算力不可能超过全网50%，无法追上） */
        if (proposeHeight <= this.maxHeightNode.height - CUT_OFF_AGE) {
            return false;
        }

        /* 3. 每一个Transaction必须成立，从当前的TxPool中移除相关的Trsansaction（因为别人已经算出来nounce并产生新的区块加入了区块链） */
        TxHandler txHandler = new TxHandler(parent.getUTXOPoolCopy());
        Transaction[] proposedTx = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTx = txHandler.handleTxs(proposedTx);
        if (validTx.length != proposedTx.length) {
            return false;
        }

        /* 从当前TxPool中移除valid的transactions */
        for (Transaction tx: validTx) {
            this.txPool.removeTransaction(tx.getHash());
        }

        /* 4. 产生一个新的UTXO Pool,产生一个新的BlockNode来包装UTXO */
        UTXOPool proposeUPool = txHandler.getUTXOPool();
        addCoinbaseToUPool(block.getCoinbase(), proposeUPool);

        BlockNode proposedBlockNode = new BlockNode(block, parent, proposeUPool);
        this.hashToNode.put(new ByteArrayWrapper(block.getHash()), proposedBlockNode);
        if (proposeHeight > this.maxHeightNode.height) {
            maxHeightNode = proposedBlockNode;
        }

        return true;
    }

    /** 将一个新的Transaction加入到Pool */
    public void addTransaction(Transaction tx) {
        this.txPool.addTransaction(tx);
    }

    private void addCoinbaseToUPool(Transaction coinbase, UTXOPool uPool) {
        for (int i=0; i<coinbase.numOutputs(); i+=1) {
            uPool.addUTXO(new UTXO(coinbase.getHash(), i), coinbase.getOutput(i));
        }
    }
}