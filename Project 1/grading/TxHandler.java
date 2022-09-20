import java.util.ArrayList;
import java.io.*;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
  private UTXOPool utxoPool;
  
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
	  this.utxoPool = new UTXOPool(utxoPool); 
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {

    ArrayList<UTXO> current_utxo_pool = new ArrayList<UTXO>();

    float inputSum = 0;
    float outputSum = 0;
    
    //Inputs
    for (int i = 0; i < tx.numInputs(); ++i) {
      Transaction.Input input = tx.getInput(i);
      UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

      Transaction.Output output = utxoPool.getTxOutput(utxo);

      //(1) all outputs claimed by tx are in the current UTXO pool
      if (utxoPool.contains(utxo) == false) return false;

      //(3) no UTXO is claimed multiple times by tx
      if (current_utxo_pool.contains(utxo)) {
        return false;
      } else {
        current_utxo_pool.add(utxo);
      }

      //(2) the signatures on each input of tx are valid
      if (output.address.verifySignature(tx.getRawDataToSign(i), input.signature) == false) return false;
      
      //Get Sum of inputs
      inputSum += utxoPool.getTxOutput(utxo).value;
    }
    
    //Outputs 
    for (int i = 0; i < tx.numOutputs(); ++i) {
      Transaction.Output output = tx.getOutput(i);
  
      //(4) all of tx’s output values are non-negative
      if (output.value < 0) {
        return false;
      } else {
        //Get sum of outputs
        outputSum += output.value;
      }
    }

    //(5) the sum of tx’s input values is greater than or equal to the sum of its output values
    if (outputSum > inputSum) return false;
    
    return true;
  }

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
    ArrayList<Transaction> valid_transactions = new ArrayList<Transaction>(); 

    //Loop through every individual possible tx
    for (int i = 0; i < possibleTxs.length; ++i) {
      Transaction tx = possibleTxs[i];
      
      //Check if valid tx
      if(isValidTx(tx)) {
        valid_transactions.add(tx);

        //Remove all corresponding UTXO from UTXOPool for each input of valid tx
        for (int j = 0; j < tx.numInputs(); ++j) {
          Transaction.Input current_input = tx.getInput(j);
          UTXO utxo_to_remove = new UTXO(current_input.prevTxHash, current_input.outputIndex);
          utxoPool.removeUTXO(utxo_to_remove);
        }

        //Add UTXO to pool for each output of valid transaction
        for (int j = 0; j < tx.numOutputs(); ++j) {
          Transaction.Output current_output = tx.getOutput(j);
          UTXO utxo_to_add = new UTXO(tx.getHash(), j);
          utxoPool.addUTXO(utxo_to_add, current_output);
        }
      }
    }
		return valid_transactions.toArray(new Transaction[valid_transactions.size()]);
	}
} 