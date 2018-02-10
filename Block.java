import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;

public class Block {

    public static final double COINBASE = 25;

    private byte[] hash;
    private byte[] prevBlockHash;
    private long nounce;
    private Transaction coinbase;
    private ArrayList<Transaction> txs;

    /** 生成一个只有coinbase transaction的block */
    public Block(byte[] prevHash, PublicKey address) {
        prevBlockHash = prevHash;
        coinbase = new Transaction(COINBASE, address);
        txs = new ArrayList<Transaction>();
        nounce = 0;
    }

    public Transaction getCoinbase() {
        return coinbase;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getPrevBlockHash() {
        return prevBlockHash;
    }

    public ArrayList<Transaction> getTransactions() {
        return txs;
    }

    public Transaction getTransaction(int index) {
        return txs.get(index);
    }

    public void addTransaction(Transaction tx) {
        txs.add(tx);
    }

    /** 将Block字节流化表示 */
    public byte[] getRawBlock() {
        ArrayList<Byte> rawBlock = new ArrayList<Byte>();

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(nounce);
        byte[] n = buffer.array();
        for (byte b: n) {
            rawBlock.add(b);
        }

        if (prevBlockHash != null)
            for (int i = 0; i < prevBlockHash.length; i++)
                rawBlock.add(prevBlockHash[i]);
        for (int i = 0; i < txs.size(); i++) {
            byte[] rawTx = txs.get(i).getRawTx();
            for (int j = 0; j < rawTx.length; j++) {
                rawBlock.add(rawTx[j]);
            }
        }
        byte[] raw = new byte[rawBlock.size()];
        for (int i = 0; i < raw.length; i++)
            raw[i] = rawBlock.get(i);
        return raw;
    }

    private void setNounce(long nounce) {
        this.nounce = nounce;
    }

    private boolean matchDifficulty(byte[] h, int difficulty) {
        String prefix = "";
        int numOfZeroBits = difficulty%8;
        for (int t=0; t< numOfZeroBits; t++)
            prefix += "0";

        for (int j=0; j<=(difficulty-1)/8; j++) {
            if (j<(difficulty-1)/8-1 && h[j] != 0) {
                return false;
            }
            if (j == (difficulty-1)/8) {
                String b = String.format("%8s", Integer.toBinaryString(h[j] & 0xFF)).replace(' ', '0');
                if (!b.startsWith(prefix)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 挖矿 */
    public void mine(int difficulty) {
        try {
            for (long i=0; i<Long.MAX_VALUE; i++) {
                this.setNounce(i);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(getRawBlock());
                byte[] h = md.digest();

                if (matchDifficulty(h, difficulty)) {
                    hash = h;
                    break;
                }
            }
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace(System.err);
        }
    }
}
