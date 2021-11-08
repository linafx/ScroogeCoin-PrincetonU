// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.HashMap;

public class BlockChain {

    /**
     * A wrapper class for block containing its {@code UTXOPool}, height, and parent
     */
    private class BlockWrapper {
        public Block block;
        public UTXOPool utxoPool;
        public int height;
        public BlockWrapper parent;

        public BlockWrapper(Block _block, UTXOPool _utxoPool, BlockWrapper _parent) {
            this.block = _block;
            this.parent = _parent;
            this.height = parent == null ? 1 : _parent.height + 1;
            this.utxoPool = new UTXOPool(_utxoPool);
        }
    }

    public static final int CUT_OFF_AGE = 10;

    private HashMap<ByteArrayWrapper, BlockWrapper> blockChain;
    private TransactionPool transactionPool;
    private BlockWrapper maxHeightBlockWrapper;

    /**
     * create an empty block chain with just a genesis block. Assume {@code _genesisBlock} is a valid
     * block
     */
    public BlockChain(Block _genesisBlock) {
        /* Initialize variables */
        transactionPool = new TransactionPool();
        blockChain = new HashMap<>();

        UTXOPool genesisUTXOPool = addCoinbaseTxs(_genesisBlock.getCoinbase(), null);
        BlockWrapper genesisBlockWrapper = new BlockWrapper(_genesisBlock, genesisUTXOPool, null);
        blockChain.put(new ByteArrayWrapper(_genesisBlock.getHash()), genesisBlockWrapper);

        maxHeightBlockWrapper = genesisBlockWrapper;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightBlockWrapper.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return new UTXOPool(maxHeightBlockWrapper.utxoPool);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block _block) {
        /* Return false if parent hash is null */
        byte[] prevBlockHash = _block.getPrevBlockHash();
        if (prevBlockHash == null) return false;

        /* Return false if parent doesn't exist in the blockchain */
        BlockWrapper parentBlockWrapper = blockChain.get(new ByteArrayWrapper(prevBlockHash));
        if (parentBlockWrapper == null) return false;

        /* Return false if new block is not high enough */
        if (parentBlockWrapper.height + 1 + CUT_OFF_AGE <= maxHeightBlockWrapper.height) return false;

        /* Return false if any of the transactions are invalid */
        TxHandler txHandler = new TxHandler(parentBlockWrapper.utxoPool);
        for (Transaction tx : _block.getTransactions()) {
            if (!txHandler.isValidTx(tx)) return false;
        }

        /* Since the block is valid, we add it to the blockchain */
        UTXOPool newUTXOPool = addCoinbaseTxs(_block.getCoinbase(), parentBlockWrapper.utxoPool);
        BlockWrapper newBlockWrapper = new BlockWrapper(_block, newUTXOPool, parentBlockWrapper);
        blockChain.put(new ByteArrayWrapper(_block.getHash()), newBlockWrapper);

        /* Update {@code maxHeightBlockWrapper} */
        if (newBlockWrapper.height > maxHeightBlockWrapper.height)
            maxHeightBlockWrapper = newBlockWrapper;

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction _tx) {
        transactionPool.addTransaction(_tx);
    }

    /** Create a new UTXOPool with coinbase outputs added */
    private UTXOPool addCoinbaseTxs(Transaction _coinbase, UTXOPool _utxoPool) {
        UTXOPool newUTXOPool = _utxoPool == null ? new UTXOPool() : new UTXOPool(_utxoPool);
        byte[] txHash = _coinbase.getHash();

        for (int i = 0; i < _coinbase.numOutputs(); i++) {
            Transaction.Output output = _coinbase.getOutput(i);
            UTXO utxo = new UTXO(txHash, i);

            newUTXOPool.addUTXO(utxo, output);
        }

        return newUTXOPool;
    }

}