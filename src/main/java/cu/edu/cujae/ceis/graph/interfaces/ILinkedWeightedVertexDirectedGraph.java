package cu.edu.cujae.ceis.graph.interfaces;

import com.example.edfinal.Flower;

/**
 * <h1>Interface para grafos dirigidos con v�rtices ponderados</h1>
 */
public interface ILinkedWeightedVertexDirectedGraph extends
		ILinkedDirectedGraph {
	public boolean insertWVertex(Flower info, Object weight);
}
