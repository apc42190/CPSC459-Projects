import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.HashMap;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double graph;
    private double malicious;
    private double txDistribution;
    private int numRounds;

    private boolean[] followees;
    private Set<Transaction> pending_transactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.graph = p_graph;
        this.malicious = p_malicious;
        this.txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pending_transactions = pendingTransactions;
    }

    public Set<Transaction> getProposals() {
        return this.pending_transactions;
    }

    public void receiveCandidates(ArrayList<Integer[]> candidates) {
        Set<Transaction> new_pending_transactions = new HashSet<Transaction>();
        Set<Integer> senders = new HashSet<>();
        for (Integer[] candidate : candidates) {
          if (this.followees[candidate[1]]) {
            senders.add(candidate[1]);
            Transaction trans = new Transaction(candidate[0]);
            new_pending_transactions.add(trans);
          }
        }

        for (int i = 0; i < this.followees.length; ++i) {
            this.followees[i] = senders.contains(i);
        }

        this.setPendingTransaction(new_pending_transactions);
    }
}
