import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
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

   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    */


   private BlockNode _max_height_node;
   private HashMap<ByteArrayWrapper, BlockNode> _blockchain;
   private TransactionPool _tx_pool;

   public BlockChain(Block genesisBlock) {
      // IMPLEMENT THIS
      UTXOPool pool = new UTXOPool();
      this._blockchain = new HashMap<>();
      this._tx_pool = new TransactionPool();
      
      Transaction tx = genesisBlock.getCoinbase();
      for (int i = 0; i < tx.numOutputs(); ++i) {
         byte[] tx_hash = tx.getHash();
         UTXO utxo = new UTXO(tx_hash, i);
         Transaction.Output output = tx.getOutput(i);
         pool.addUTXO(utxo, output);
      }

      BlockNode genesis_block_node = new BlockNode(genesisBlock, null, pool);
      byte[] genesis_block_hash = genesisBlock.getHash();
      ByteArrayWrapper wrapped_genesis_block = new ByteArrayWrapper(genesis_block_hash);
      this._blockchain.put(wrapped_genesis_block, genesis_block_node);
      this._max_height_node = genesis_block_node;
   }

   /* Get the maximum height block
    */
   public Block getMaxHeightBlock() {
      return this._max_height_node.b;
   }
   
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
      return this._max_height_node.getUTXOPoolCopy();
   }
   
   /* Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
      return this._tx_pool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true of block is successfully added
    */
   public boolean addBlock(Block b) {
      //No new genesis blocks allowex
      if (b.getPrevBlockHash() == null) return false;
      ByteArrayWrapper wrapped_prev_block = new ByteArrayWrapper(b.getPrevBlockHash());
      BlockNode previous_node = this._blockchain.get(wrapped_prev_block); 
      if (previous_node == null) return false;
         
      //check for valid height
      int new_height = previous_node.height;
      new_height += 1;
      if (new_height <= this._max_height_node.height - CUT_OFF_AGE) return false;

      //verify all transactions in new block
      UTXOPool previous_utxo_pool = previous_node.getUTXOPoolCopy();
      for (Transaction tx : b.getTransactions()) {
         TxHandler tx_handler = new TxHandler(previous_utxo_pool);
         if (tx_handler.isValidTx(tx) == false) {
            return false;
         }

         //remove used utxo and add new utxo
         for (Transaction.Input input : tx.getInputs()) {
            byte[] hash_of_prev = input.prevTxHash;
            int output_index = input.outputIndex;
            UTXO utxo = new UTXO(hash_of_prev, output_index);
            previous_utxo_pool.removeUTXO(utxo);
         }

         for (int i = 0; i < tx.numOutputs(); ++i) {
            UTXO utxo = new UTXO(tx.getHash(), i);
            previous_utxo_pool.addUTXO(utxo, tx.getOutput(i));
         }
      }

      Transaction new_tx = b.getCoinbase();
      for( int i = 0; i < new_tx.numOutputs(); ++i) {
         Transaction.Output output = new_tx.getOutput(i);
         byte[] tx_hash = new_tx.getHash();
         UTXO utxo = new UTXO(tx_hash, i);
         previous_utxo_pool.addUTXO(utxo, output);
      }

      //clearing transaction pool of processed transactions
      for (Transaction t : b.getTransactions()) {
         this._tx_pool.removeTransaction(t.getHash());
      }
       
      //add new node to blockchain
      BlockNode new_node = new BlockNode(b, previous_node, previous_utxo_pool);
      ByteArrayWrapper wrapped_block = new ByteArrayWrapper(b.getHash());
      this._blockchain.put(wrapped_block, new_node);

      //Update max
      if (new_height >= this._max_height_node.height) this._max_height_node = new_node;
      
      return true;
   }

   /* Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
      this._tx_pool.addTransaction(tx);
   }
}
