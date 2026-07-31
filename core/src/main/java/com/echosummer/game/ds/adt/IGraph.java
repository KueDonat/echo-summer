package com.echosummer.game.ds.adt;

import java.util.List;

/**
 * Abstract Data Type (ADT) Interface for Graph.
 *
 * @param <V> Vertex type
 */
public interface IGraph<V> {
    /**
     * Adds a vertex to the graph.
     */
    boolean addVertex(V vertex);

    /**
     * Adds an undirected edge between vertex u and vertex v with default weight 1.0.
     */
    void addEdge(V u, V v);

    /**
     * Adds an edge between vertex u and vertex v with a specified weight.
     */
    void addEdge(V u, V v, float weight, boolean bidirectional);

    /**
     * Returns a list of all adjacent vertices to the given vertex.
     */
    List<V> getNeighbors(V vertex);

    /**
     * Checks whether an edge exists between vertex u and vertex v.
     */
    boolean hasEdge(V u, V v);

    /**
     * Returns all vertices in the graph.
     */
    List<V> getVertices();

    /**
     * Returns the total number of vertices in the graph.
     */
    int vertexCount();
}
