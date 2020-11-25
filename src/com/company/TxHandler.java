package com.company;

import java.util.*;

public class TxHandler {

    class Graph {
        private int V;
        private ArrayList<ArrayList<Integer> > adj;

        public Graph(int v) {
            V = v;
            adj = new ArrayList<ArrayList<Integer> >(v);
            for (int i = 0; i < v; ++i)
                adj.add(new ArrayList<Integer>());
        }

        void addEdge(int v, int w) { adj.get(v).add(w); }

        void topologicalSortUtil(int v, boolean visited[], Stack<Integer> stack) {
            visited[v] = true;
            Integer i;
            Iterator<Integer> it = adj.get(v).iterator();
            while (it.hasNext()) {
                i = it.next();
                if (!visited[i]) {
                    topologicalSortUtil(i, visited, stack);
                }
            }
            stack.push(v);
        }

        ArrayList<Integer> topologicalSort() {
            Stack<Integer> stack = new Stack<>();
            ArrayList<Integer> ret = new ArrayList<>();
            boolean visited[] = new boolean[V];
            for (int i = 0; i < V; i++) {
                visited[i] = false;
            }

            for (int i = 0; i < V; i++)
                if (visited[i] == false)
                    topologicalSortUtil(i, visited, stack);

            // Print contents of stack
            while (stack.empty() == false)
                ret.add(stack.pop());
            return ret;
        }

    }

    static class ByteArrayWrapper {

        private byte[] contents;

        public ByteArrayWrapper(byte[] b) {
            contents = new byte[b.length];
            for (int i = 0; i < contents.length; i++)
                contents[i] = b[i];
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (getClass() != other.getClass()) {
                return false;
            }

            ByteArrayWrapper otherB = (ByteArrayWrapper) other;
            byte[] b = otherB.contents;
            if (contents == null) {
                if (b == null)
                    return true;
                else
                    return false;
            } else {
                if (b == null)
                    return false;
                else {
                    if (contents.length != b.length)
                        return false;
                    for (int i = 0; i < b.length; i++)
                        if (contents[i] != b[i])
                            return false;
                    return true;
                }
            }
        }

        public int hashCode() {
            return Arrays.hashCode(contents);
        }
    }

    private UTXOPool utxoPool;
    private HashMap< Integer , Transaction > hashToTx;
    private HashMap< Transaction , Integer > txPos;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
        hashToTx = new HashMap<>();
        txPos = new HashMap<>();
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
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
        // IMPLEMENT THIS
        double income = 0 , outcome = 0;
        int inputSize = tx.getInputs().size();
        HashSet<UTXO> toBeDeleted = new HashSet<>();

        for(int i = 0 ; i < inputSize; i++) {
            Transaction.Input ip = tx.getInputs().get(i);
            // Check in the UTXPool
            // Create UTXO instance and check in UTXPool
            UTXO ux = new UTXO(ip.prevTxHash , ip.outputIndex);
            if(!utxoPool.contains(ux)){
                return false;
            }
            // Double spend [ use the same UTXO more than once ]
            if(toBeDeleted.contains(ux)) {
                return false;
            }
            // Check Sig
            boolean sig = Crypto.verifySignature( utxoPool.getTxOutput(ux).address , tx.getRawDataToSign(i) , ip.signature);
            if(!sig) {
                return false;
            }
            income += utxoPool.getTxOutput(ux).value;
            toBeDeleted.add(ux);
        }
        for(Transaction.Output op : tx.getOutputs()){
            outcome += op.value;
            if(op.value < 0){
                return false;
            }
        }
        if(outcome > income){
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        hashToTx = new HashMap<>();
        txPos = new HashMap<>();
        Graph g = new Graph((possibleTxs.length));
        int i = 0;
        for(Transaction T : possibleTxs) {
            txPos.put(T, i++);
            hashToTx.put( new ByteArrayWrapper(T.getHash()).hashCode() , T);
        }

        for(i = 0 ; i < possibleTxs.length ; i++){
            for(Transaction.Input ip : possibleTxs[i].getInputs()){
                if(!hashToTx.containsKey(new ByteArrayWrapper(ip.prevTxHash).hashCode())) continue;
                int to = txPos.get(hashToTx.get(new ByteArrayWrapper(ip.prevTxHash).hashCode()));
                g.addEdge(to , i);
            }
        }

        ArrayList<Integer> topSort = g.topologicalSort();
        ArrayList<Transaction> list = new ArrayList<>();
        for(Integer pos: topSort){
            Transaction T = possibleTxs[pos];
            if(isValidTx(T)) {
                list.add((T));
                for(i = 0 ; i < T.getInputs().size() ; i++){
                    Transaction.Input ip = T.getInput(i);
                    UTXO ux = new UTXO(ip.prevTxHash , ip.outputIndex);
                    utxoPool.removeUTXO(ux);
                }
                for(i = 0 ; i < T.getOutputs().size() ; i++){
                    Transaction.Output op = T.getOutput(i);
                    utxoPool.addUTXO( new UTXO( T.getHash()  , i ), op );
                }
            }
        }

        Transaction[] ret = new Transaction[list.size()];
        ret = list.toArray(ret);
        return ret;
    }
}