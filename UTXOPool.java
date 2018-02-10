import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class UTXOPool {

    /**
     * 构造函数，实现UTXO和Transaction的链接
     */
    private HashMap<UTXO, Transaction.Output> H;

    public UTXOPool() {
        H = new HashMap<UTXO, Transaction.Output>();
    }

    public UTXOPool(UTXOPool uPool) {
        H = new HashMap<UTXO, Transaction.Output>(uPool.H);
    }

    /**
     * 收到一个新的Transaction后，将UTXO加入目前的UTXO池内
     * @param utxo UTXO
     * @param txOut 未使用的交易输出
     */
    public void addUTXO(UTXO utxo, Transaction.Output txOut) {
        H.put(utxo, txOut);
    }

    /**
     * 使用一个UTXO后，将其移出池中
     * @param utxo UTXO
     */
    public void removeUTXO(UTXO utxo) {
        H.remove(utxo);
    }

    /**
     * 由UTXO得到Transaction的输出记录
     * @param ut UTXO
     * @return Transaction的Output
     */
    public Transaction.Output getTxOutput(UTXO ut) {
        return H.get(ut);
    }

    /**
     * 验证是否存在这个UTXO
     */
    public boolean contains(UTXO utxo) {
        return H.containsKey(utxo);
    }


    /**
     * 返回所有的UTXO
     * @return
     */
    public ArrayList<UTXO> getAllUTXO() {
        Set<UTXO> setUTXO = H.keySet();
        ArrayList<UTXO> allUTXO = new ArrayList<UTXO>();
        for (UTXO ut : setUTXO) {
            allUTXO.add(ut);
        }
        return allUTXO;
    }
}
