import java.util.ArrayList;
import java.util.Set;

/**
 * CompliantNode refers to a node that follows the rules (not malicious).
 * A naive implementation that somehow passes with around 80/100.
 * Does not handle malicious nodes.
 */
public class CompliantNode implements Node {

    private double p_graph, p_malicious, p_txDistribution;
    private int numRounds;

    private boolean[] followees;
    private Set<Transaction> pendingTransactions;

    /* Constructor */
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    /* A is B's followee if there is an A -> B edge */
    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    /* {@code pendingTransactions} is the starting state of transactions */
    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    /* Simply sends {@code pendingTransactions} to its followers */
    public Set<Transaction> sendToFollowers() {
        return pendingTransactions;
    }

    /* Simply adds all candidate transactions to {@code pendingTransactions} */
    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            pendingTransactions.add(candidate.tx);
        }
    }
}
