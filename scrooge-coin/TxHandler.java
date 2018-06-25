import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {
	private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
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
        //set to keep track of claimed UTXOs for Step 3
    	double sumInput = 0;
    	Set<UTXO> utxoTracker = new HashSet<UTXO>();
    	for (int i = 0; i < tx.numInputs(); i++) {
    		Transaction.Input input = null;
			try {
				input = tx.getInput(i);
			} catch (Exception e) {
				e.printStackTrace();
			}
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            
            //Step 1 check
            if(!this.utxoPool.contains(utxo)) return false;
            Transaction.Output previousOutput = null;
            try {
            	previousOutput = this.utxoPool.getTxOutput(utxo);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            //Step 2 check
            PublicKey pubKey = previousOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if(!Crypto.verifySignature(pubKey, message, signature)) return false;
            //Step 3 check
            if(utxoTracker.contains(utxo)) return false;
            utxoTracker.add(utxo);
            sumInput += previousOutput.value;
    	}       
            //Step 4 check
            double sumOutput = 0;
            for (int i = 0; i < tx.numOutputs(); i++) {
            	Transaction.Output output = null;
                try {
					output = tx.getOutput(i);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                if(output.value < 0) return false;
                sumOutput += output.value;
        }
        if(sumInput < sumOutput) return false;
        return true;

    }
    

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTxs = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
                continue;
            }
            validTxs.add(tx);
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                this.utxoPool.removeUTXO(utxo);
            }
            int index = 0;
            byte[] txHash = tx.getHash();
            for (Transaction.Output output : tx.getOutputs()) {
                UTXO utxo = new UTXO(txHash,index);
                index += 1;
                this.utxoPool.addUTXO(utxo, output);
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);

    }

}
