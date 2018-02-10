import java.util.ArrayList;

public class TxHandler {
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * 1. 所有被Transaction的input使用的UTXO都存在
     * 2. 每一个input的signature都是由原来的所有者所sign的
     * 3. 没有一个UTXO被使用了两次
     * 4. 所有的output内，新创建的UTXO都为非负数
     * 5. 总的input输入UTXO的价值大于总的输出UTXO的价值
     */
    public boolean isValidTx(Transaction tx) {
        double totalInput = 0;
        double totalOutput = 0;

        for (int i=0; i<tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            /* 1. 所有被Transaction的input使用的UTXO都存在 */
            if (!this.utxoPool.contains(utxo)) {
                return false;
            }

            /* 2. 每一个input的signature都是由原来的所有者所sign的 */
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), in.signature)) {
                return false;
            }
            totalInput += output.value;

            /* 3. 没有一个UTXO被使用了两次 */
            for (int j=i+1; j<tx.numInputs(); j++) {
                Transaction.Input otherIn = tx.getInput(j);
                UTXO otherUTXO = new UTXO(otherIn.prevTxHash, otherIn.outputIndex);
                if (utxo.equals(otherUTXO)) {
                    return false;
                }
            }
        }

        for (int i=0; i<tx.numOutputs(); i++) {
            /* 4. 所有的output内，新创建的UTXO都为非负数 */
            double value = tx.getOutput(i).value;
            if (value < 0) {
                return false;
            }
            totalOutput += value;
        }

        /* 5. 总的input输入UTXO的价值大于总的输出UTXO的价值 */
        return totalInput >= totalOutput;
    }

    /**
     * 处理一堆可能被加入区块链的transaction，更改相应的UTXO Pool
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> txs = new ArrayList<>();
        for (Transaction tx: possibleTxs) {
            if(this.isValidTx(tx)) {
                /* 加入返回的队列 */
                txs.add(tx);

                /* 将Input所消耗的UTXO从pool中移除 */
                for (Transaction.Input input: tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }

                /* 将新的output加入Pool */
                for (int i=0; i<tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    this.utxoPool.addUTXO(utxo, tx.getOutput(i));
                }
            }
        }

        return txs.toArray(new Transaction[txs.size()]);
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
}
