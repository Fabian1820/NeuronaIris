package cu.edu.cujae.ceis.graph.interfaces;

import com.example.edfinal.Flower;

/**
 * <h1>Interface para grafos no dirigidos con vértices ponderados</h1>
 */
public interface ILinkedWeightedVertexNotDirectedGraph extends
		ILinkedNotDirectedGraph {
	public boolean insertWVertex(Flower info, Object weight);
}
