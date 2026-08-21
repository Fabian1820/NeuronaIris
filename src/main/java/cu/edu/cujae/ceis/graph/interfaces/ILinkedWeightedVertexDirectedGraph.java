package cu.edu.cujae.ceis.graph.interfaces;

import com.example.edfinal.data.Sample;

/**
 * <h1>Interface para grafos dirigidos con vértices ponderados</h1>
 */
public interface ILinkedWeightedVertexDirectedGraph extends
		ILinkedDirectedGraph {
	public boolean insertWVertex(Sample info, Object weight);
}
