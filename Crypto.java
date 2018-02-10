import java.security.PublicKey;
import java.security.Signature;

public class Crypto {
    public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature) {
        Signature sig;
        try {
            sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(message);
            return sig.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
