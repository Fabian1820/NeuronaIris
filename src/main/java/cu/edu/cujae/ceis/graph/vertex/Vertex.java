package cu.edu.cujae.ceis.graph.vertex;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

import com.example.edfinal.Flower;
import cu.edu.cujae.ceis.graph.edge.Edge;


/**
 * <h1>Vértice elemental</h1>
 */
public class Vertex implements Serializable {
	
	/**
	 * Objeto que representa la información del vértice.
	 */
	private Flower info;
	
	/**
	 * Lista de aristas.
	 */
	private LinkedList<Edge> edgeList;	

	/**
	 * Inicia la instancia con la
	 * información del vértice.
	 * 
	 * @param info Información.
	 */
	public Vertex(Flower info) {
		this.info = info;
		edgeList  = new LinkedList<Edge>();
	}
	
	/**
	 * Elimina la arista hacia un vértice dado.
	 * 
	 * @param vertex Vértice.
	 * @return True si se realiza la operación. False si no 
	 * 			se encuentra una arista hacia el vértice
	 * 			dado.
	 */
	public boolean deleteEdge(Vertex vertex) {
		boolean success = false;
		Iterator<Edge> iter = edgeList.iterator();
        
		while(!success && iter.hasNext()) {
			if(iter.next().getVertex().equals(vertex)) {
				iter.remove();
				success = true;
			}
		}
		return success;		
	}
	
	/**
	 * Permite acceder a la lista de aristas.
	 * @return Lista de aristas.
	 */
	public LinkedList<Edge> getEdgeList() {
		return edgeList;
	}
	
	/**
	 * Obtiene la información del vértice.
	 * @return Información.
	 */
	public Object getInfo() {
		return info;
	}
	
	/**
	 * Define la información para este nodo.
	 * @param info Información.
	 */
	public void setInfo(Flower info) {
		this.info = info;
	}
	
	/**
	 * Permite acceder a la lista de aristas.
	 * @return Lista de aristas.
	 */
	public LinkedList<Vertex> getAdjacents() {
		LinkedList<Vertex> vertices = new LinkedList<Vertex>();		
		Iterator<Edge> iter = edgeList.iterator();
		
		while(iter.hasNext()) {
			vertices.add(iter.next().getVertex());
		}
		
		return vertices;
	}	
	
	/**
	 * Permite saber si este vértice es adyacente
	 * con un vértice dado.
	 * 
	 * @param vertex Vértice.
	 * @return True si son adyacente, false si no.
	 */
	public boolean isAdjacent(Vertex vertex) {
		boolean adjacent = false;
		Iterator<Edge> iter = edgeList.iterator();
		
		while(!adjacent && iter.hasNext()) {
			if(iter.next().getVertex().equals(vertex)) {
				adjacent = true;
			}
		}		
		return adjacent;
	}
	
	/**
	 * Esta función será útil para la depuración.
	 * @return Información.
	 */
	@Override
	public String toString() {
		return info.toString();
	}
}
