
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class BlockHandler {
    private BlockChain blockChain;

    /** 创建区块链 */
    public BlockHandler(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    /**
     * 将区块加入区块链中
     */
    public boolean processBlock(Block block) {
        if (block == null)
            return false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(block.getRawBlock());
            byte[] h = md.digest();
            for (int i=0; i<h.length; i++) {
                if (h[i] != block.getHash()[i])
                    return false;
            }
            return blockChain.addBlock(block);

        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace(System.err);
            return false;
        }
    }

    /** 主动产生一个新的区块 */
    public Block createBlock(PublicKey myAddress) {
        Block parent = blockChain.getMaxHeightBlock();
        byte[] parentHash = parent.getHash();
        Block current = new Block(parentHash, myAddress);
        UTXOPool uPool = blockChain.getMaxHeightUTXOPool();
        TransactionPool txPool = blockChain.getTransactionPool();
        TxHandler handler = new TxHandler(uPool);
        Transaction[] txs = txPool.getTransactions().toArray(new Transaction[0]);
        Transaction[] rTxs = handler.handleTxs(txs);
        for (int i = 0; i < rTxs.length; i++)
            current.addTransaction(rTxs[i]);

        current.mine(2);
        if (blockChain.addBlock(current))
            return current;
        else
            return null;
    }

    /** 收到一个新的transaction将其添加进区块链的transaction池中 */
    public void processTx(Transaction tx) {
        blockChain.addTransaction(tx);
    }
}
