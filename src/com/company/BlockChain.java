package com.company;
// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private int maxHeight, minHeight;
    private HashMap< Integer , Block >tree;
    private HashMap< ByteArrayWrapper, Integer >blockHeight;
    private HashMap< ByteArrayWrapper, UTXOPool >blockUTXOPool;
    private TransactionPool transactionPool;

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        tree = new HashMap<>();
        blockHeight = new HashMap<>();
        blockUTXOPool = new HashMap<>();
        transactionPool = new TransactionPool();
        maxHeight = 1;
        minHeight = 1;

        tree.put(1 , genesisBlock);
        UTXOPool utxoPool = new UTXOPool();
        UTXO ux = new UTXO( genesisBlock.getCoinbase().getHash() , 0 );
        utxoPool.addUTXO(ux , genesisBlock.getCoinbase().getOutput(0) );
        blockUTXOPool.put(getBlockHash(genesisBlock), utxoPool);
        blockHeight.put(getBlockHash(genesisBlock) , 1);
    }

    private ByteArrayWrapper getBlockHash(Block block){
        return new ByteArrayWrapper(block.getHash());
    }
    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return tree.get(maxHeight);
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return blockUTXOPool.get(getBlockHash(getMaxHeightBlock()));
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // Pretend to be genesis block
        if(block == null || block.getPrevBlockHash() == null) {
            return false;
        }
        // Depend on non existing block
        if( ! blockHeight.containsKey( new ByteArrayWrapper(block.getPrevBlockHash()) ) ) return false;
        int prevBlockHeight = blockHeight.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        int currentBlockHeight = prevBlockHeight + 1;
        // Check CUT OFF AGE validity
        if(currentBlockHeight <= maxHeight - CUT_OFF_AGE) return false;
        UTXOPool prevPool = blockUTXOPool.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        TxHandler txHandler = new TxHandler(prevPool);

        ArrayList<Transaction> blockTransactionsList = block.getTransactions();
        int blockTxSize = blockTransactionsList.size();
        Transaction [] blockTransactions = new Transaction[blockTxSize];

        for(int i = 0; i < blockTxSize; i++){
            blockTransactions[i] = blockTransactionsList.get(i);
        }
        Transaction [] validTransactions = txHandler.handleTxs(blockTransactions);

        if(validTransactions.length != blockTransactions.length)return false;

        // Valid block
        for(Transaction re : validTransactions){
            transactionPool.removeTransaction(re.getHash());
        }

        blockHeight.put(getBlockHash(block) , currentBlockHeight);
        UTXOPool currentUTXOPool = txHandler.getUTXOPool();
        UTXO ux = new UTXO( block.getCoinbase().getHash() , 0 );
        currentUTXOPool.addUTXO(ux , block.getCoinbase().getOutput(0) );
        blockUTXOPool.put(getBlockHash(block),currentUTXOPool);
        if(!tree.containsKey(currentBlockHeight)){
            tree.put(currentBlockHeight, block);
            maxHeight = currentBlockHeight;
        }

        // Remove pre cut off age levels
        while (minHeight < maxHeight - CUT_OFF_AGE){
            tree.remove(minHeight);
            minHeight++;
        }

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }
}