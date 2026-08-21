package cu.edu.cujae.ceis.graph.interfaces;

import com.example.edfinal.data.Sample;

/**
 * <h1>Interface para grafos no dirigidos con vértices ponderados</h1>
 */
public interface ILinkedWeightedVertexNotDirectedGraph extends
		ILinkedNotDirectedGraph {
	public boolean insertWVertex(Sample info, Object weight);
}
