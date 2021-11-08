import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {
    
    private UTXOPool utxoPool;
    
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool checkPool = new UTXOPool(); // temporary UTXOPool to check condition (4)
        double inputTotal = 0, outputTotal = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input         = tx.getInput(i);
            UTXO utxo                       = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output prevOutput   = this.utxoPool.getTxOutput(utxo);

            /* Condition (1) */
            if (prevOutput == null) return false;

            PublicKey pubKey    = prevOutput.address;
            byte[] message      = tx.getRawDataToSign(i);
            byte[] signature    = input.signature;

            /* Condition (2) */
            if (!Crypto.verifySignature(pubKey, message, signature)) return false;

            /* Condition (3) */
            if (checkPool.contains(utxo)) return false;

            checkPool.addUTXO(utxo, prevOutput);
            inputTotal += prevOutput.value;
        }

        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);

            /* Condition (4) */
            if (output.value < 0) return false;
            outputTotal += output.value;
        }
        
        /* Condition (5) */
        return inputTotal >= outputTotal;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> tempResult = new ArrayList<Transaction>();
        
        for (Transaction tx : possibleTxs) {
            if (!this.isValidTx(tx)) continue;

            tempResult.add(tx);

            /* Remove spent UTXO */
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                this.utxoPool.removeUTXO(utxo);
            }

            /* Add newly created UTXO */
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output output = tx.getOutput(i);
                UTXO utxo = new UTXO(tx.getHash(), i);
                this.utxoPool.addUTXO(utxo, output);
            }
        }
        
        Transaction[] result = new Transaction[tempResult.size()];
        return tempResult.toArray(result);
    }

}
